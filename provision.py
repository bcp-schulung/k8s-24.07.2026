#!/usr/bin/env python3
"""
Container Seminar — Hetzner + Cloudflare Provisioner
=====================================================

Provisions 3 VMs per student on Hetzner Cloud:
  - primary VM: existing code-server, Caddy, Docker, kubectl, Helm, and Trivy setup
  - cp VM: clean Ubuntu with only the 'student' account and SSH access configured
  - worker VM: clean Ubuntu with only the 'student' account and SSH access configured

For each student, a dedicated SSH keypair is generated on the primary VM.
The primary VM is configured so `ssh cp` and `ssh worker` connect directly
to that student's cp and worker VMs as the student user.

Each student's primary VM also receives a merged /home/student/.kube/config
granting cluster-admin access to the two shared seminar Kubernetes clusters
("staging" and "prod", provisioned separately via terraform/) as two contexts.
NOTE: this is shared cluster-admin access — every student can see/modify any
other student's resources on these two clusters. Acceptable for this seminar's
shared infra (Harbor/ArgoCD), but do not reuse this pattern for anything where
student isolation matters.

Prerequisites:
    pip install hcloud cloudflare fabric python-dotenv paramiko pyyaml
    Copy .env.example -> .env and fill in credentials.
    Run `terraform apply` in terraform/ first so kubeconfig-staging and
    kubeconfig-prod exist there.

Usage:
    python provision.py
"""

from __future__ import annotations

import io
import json
import os
import secrets
import string
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import cloudflare as cf_module
import yaml
from dotenv import load_dotenv
from fabric import Connection
from hcloud import Client as HCloudClient
from hcloud.firewalls.domain import FirewallResource, FirewallRule
from hcloud.images.domain import Image
from hcloud.locations.domain import Location
from hcloud.server_types.domain import ServerType
from hcloud.servers.domain import ServerCreatePublicNetwork
import paramiko

# ─────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────

EU_SERVER_TYPE       = "cx23"   # Cost-optimized; available in EU locations
GLOBAL_SERVER_TYPE   = "cpx22"  # AMD; available in EU, US, and Singapore
IMAGE_NAME           = "ubuntu-24.04"
LOCATION_NAME        = "hel1"
LOCATION_FALLBACKS   = ("fsn1", "nbg1", "ash", "hil", "sin")
CREATE_VM_ROUNDS     = 2        # Try the complete placement list this many times
CREATE_VM_WORKERS    = 4        # Keep placement/API bursts modest
DOMAIN_SUFFIX    = "container.it-scholar.com"
HETZNER_KEY_NAME = "container-seminar-provisioner"
FW_NAME          = "container-seminar-fw"
PASSWORDS_FILE   = Path(__file__).parent / ".passwords.json"
VM_ROLES         = ("primary", "cp", "worker")

# Admin kubeconfigs written by `terraform apply` (terraform/main.tf outputs),
# merged into one file with two contexts and distributed to every student.
TERRAFORM_DIR = Path(__file__).parent / "terraform"
CLUSTER_KUBECONFIGS = {
    "staging": TERRAFORM_DIR / "kubeconfig-staging",
    "prod": TERRAFORM_DIR / "kubeconfig-prod",
}


# slug   : used for the Hetzner VM name and DNS subdomain (ASCII, no umlauts)
# display: shown in the summary / credentials file
STUDENTS = [
    {"slug": "ben-coeppicus",        "display": "Ben Cöppicus"},
    {"slug": "student-01",     "display": "Student 01"},
    {"slug": "student-02", "display": "Student 02"},
    {"slug": "student-03",        "display": "Student 03"},
    {"slug": "student-04",        "display": "Student 04"},
    {"slug": "student-05",         "display": "Student 05"},
    {"slug": "student-06",        "display": "Student 06"},
    {"slug": "student-07",         "display": "Student 07"},
    {"slug": "student-08",        "display": "Student 08"},
    {"slug": "student-09",        "display": "Student 09"},
    {"slug": "student-10",       "display": "Student 10"},
]

# ─────────────────────────────────────────────────────────────────────────────
# Systemd service for code-server (runs as the student user)
# ─────────────────────────────────────────────────────────────────────────────

CODESERVER_SERVICE = """\
[Unit]
Description=code-server IDE (student)
After=network.target

[Service]
Type=simple
User=student
Group=student
WorkingDirectory=/home/student
ExecStart=/usr/bin/code-server --config /home/student/.config/code-server/config.yaml
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
"""

# ─────────────────────────────────────────────────────────────────────────────
# Credentials — loaded from .env
# ─────────────────────────────────────────────────────────────────────────────

load_dotenv()


def _require(key: str) -> str:
    val = os.getenv(key)
    if not val:
        sys.exit(f"ERROR: Missing required environment variable '{key}'. Check .env.")
    return val


HCLOUD_TOKEN    = _require("HCLOUD_TOKEN")
CF_API_TOKEN    = _require("CF_API_TOKEN")
CF_ZONE_ID      = _require("CF_ZONE_ID")
SSH_PRIVATE_KEY = os.path.expanduser(_require("SSH_PRIVATE_KEY_PATH"))
SSH_PUBLIC_KEY  = os.path.expanduser(_require("SSH_PUBLIC_KEY_PATH"))

hc = HCloudClient(token=HCLOUD_TOKEN)
cf = cf_module.Cloudflare(api_token=CF_API_TOKEN)

# ─────────────────────────────────────────────────────────────────────────────
# Utilities
# ─────────────────────────────────────────────────────────────────────────────


def log(msg: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def gen_password(length: int = 16) -> str:
    alphabet = string.ascii_letters + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(length))


def load_or_generate_passwords() -> dict[str, str]:
    """Load persisted passwords from disk; generate and save if absent."""
    slugs = [s["slug"] for s in STUDENTS]
    if PASSWORDS_FILE.exists():
        data = json.loads(PASSWORDS_FILE.read_text())
        # Generate only for new students not already in the file
        updated = False
        for slug in slugs:
            if slug not in data:
                data[slug] = gen_password()
                updated = True
        if updated:
            PASSWORDS_FILE.write_text(json.dumps(data, indent=2))
        log(f"Loaded passwords from {PASSWORDS_FILE}")
        return {slug: data[slug] for slug in slugs}
    passwords = {slug: gen_password() for slug in slugs}
    PASSWORDS_FILE.write_text(json.dumps(passwords, indent=2))
    log(f"Generated and saved passwords to {PASSWORDS_FILE}")
    return passwords


def connection(ip: str, user: str = "root") -> Connection:
    """Return a Fabric Connection for the given IP, forced over IPv4."""
    import socket as _socket
    sock = _socket.socket(_socket.AF_INET, _socket.SOCK_STREAM)
    sock.settimeout(90)
    sock.connect((ip, 22))
    conn = Connection(
        host=ip,
        user=user,
        connect_kwargs={
            "key_filename": SSH_PRIVATE_KEY,
            "timeout": 90,
            "look_for_keys": False,
            "allow_agent": False,
            "sock": sock,
        },
    )
    conn.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    return conn


def wait_for_ssh(ip: str, timeout: int = 600) -> None:
    """Block until port 22 on *ip* accepts TCP connections."""
    import subprocess
    log(f"  Waiting for SSH on {ip} ...")
    deadline = time.time() + timeout
    last_err = ""
    while time.time() < deadline:
        try:
            r = subprocess.run(["nc", "-zw3", ip, "22"], capture_output=True, timeout=10)
            if r.returncode == 0:
                log(f"  SSH ready: {ip}")
                return
            last_err = f"nc exited {r.returncode}: {r.stderr.decode().strip()}"
        except Exception as e:
            last_err = f"{type(e).__name__}: {e}"
        time.sleep(3)
    raise TimeoutError(
        f"SSH never became available on {ip} after {timeout}s (last: {last_err})"
    )


def put_text(c: Connection, content: str, remote_path: str) -> None:
    """Upload a string as a remote file via SFTP (avoids shell-quoting issues)."""
    c.put(io.BytesIO(content.encode("utf-8")), remote_path)


# ─────────────────────────────────────────────────────────────────────────────
# Phase 1 — Hetzner Infrastructure
# ─────────────────────────────────────────────────────────────────────────────


def ensure_hetzner_ssh_key() -> object:
    """Upload provisioner public key to Hetzner; reuse if already present."""
    existing = hc.ssh_keys.get_by_name(HETZNER_KEY_NAME)
    if existing:
        log(f"Hetzner SSH key '{HETZNER_KEY_NAME}' already exists, reusing.")
        return existing
    pub = Path(SSH_PUBLIC_KEY).read_text().strip()
    log(f"Uploading SSH key '{HETZNER_KEY_NAME}' to Hetzner ...")
    return hc.ssh_keys.create(name=HETZNER_KEY_NAME, public_key=pub)


def ensure_firewall() -> object:
    """Create a firewall allowing SSH + HTTP + HTTPS; reuse if present."""
    existing = hc.firewalls.get_by_name(FW_NAME)
    if existing:
        log(f"Firewall '{FW_NAME}' already exists, reusing.")
        return existing
    log(f"Creating firewall '{FW_NAME}' ...")
    rules = [
        FirewallRule(
            direction="in",
            protocol="tcp",
            port=p,
            source_ips=["0.0.0.0/0", "::/0"],
            description=f"Allow inbound TCP {p}",
        )
        for p in ["22", "80", "443"]
    ]
    result = hc.firewalls.create(name=FW_NAME, rules=rules)
    return result.firewall


def _placement_candidates() -> tuple[tuple[str, str], ...]:
    """Return ordered (location, server_type) placement candidates.

    CX plans are restricted to the EU locations. CPX plans are used for the
    United States and Singapore, and as a second server-type fallback in EU.
    HCLOUD_LOCATIONS may override the location order.
    """
    configured = os.getenv("HCLOUD_LOCATIONS", "")
    locations = (
        tuple(part.strip() for part in configured.split(",") if part.strip())
        if configured
        else (LOCATION_NAME, *LOCATION_FALLBACKS)
    )
    locations = tuple(dict.fromkeys(locations))
    eu_locations = {"hel1", "fsn1", "nbg1"}

    candidates: list[tuple[str, str]] = []
    for location in locations:
        server_type = EU_SERVER_TYPE if location in eu_locations else GLOBAL_SERVER_TYPE
        candidates.append((location, server_type))

    # If EU CX capacity is exhausted, retry EU using the globally available CPX type.
    for location in locations:
        if location in eu_locations:
            candidates.append((location, GLOBAL_SERVER_TYPE))

    return tuple(dict.fromkeys(candidates))

def _is_retryable_create_error(exc: Exception) -> bool:
    """Identify temporary Hetzner placement/capacity errors."""
    text = str(exc).lower()
    return any(
        marker in text
        for marker in (
            "resource_unavailable",
            "error during placement",
            "rate_limit_exceeded",
            "conflict",
            "temporarily unavailable",
            "timeout",
        )
    )


def create_vm(name: str, ssh_key: object) -> tuple[str, str]:
    """Create a VM, retrying temporary placement failures across locations."""
    existing = hc.servers.get_by_name(name)
    if existing:
        ip = existing.public_net.ipv4.ip
        log(f"  VM '{name}' already exists (IP: {ip}), skipping.")
        return name, ip

    candidates = _placement_candidates()
    max_attempts = len(candidates) * CREATE_VM_ROUNDS
    last_error: Exception | None = None
    successful_location = "unknown"
    successful_server_type = "unknown"

    for attempt in range(1, max_attempts + 1):
        location, server_type = candidates[(attempt - 1) % len(candidates)]

        # Another attempt or process may have completed this VM meanwhile.
        existing = hc.servers.get_by_name(name)
        if existing:
            ip = existing.public_net.ipv4.ip
            log(f"  VM '{name}' now exists (IP: {ip}), reusing.")
            return name, ip

        log(
            f"  Creating VM '{name}' ({server_type}, {IMAGE_NAME}, {location}) "
            f"attempt {attempt}/{max_attempts} ..."
        )
        try:
            hc.servers.create(
                name=name,
                server_type=ServerType(name=server_type),
                image=Image(name=IMAGE_NAME),
                location=Location(name=location),
                ssh_keys=[ssh_key],
                public_net=ServerCreatePublicNetwork(
                    enable_ipv4=True,
                    enable_ipv6=True,
                ),
            )
            last_error = None
            successful_location = location
            successful_server_type = server_type
            break
        except Exception as exc:
            last_error = exc
            if not _is_retryable_create_error(exc) or attempt == max_attempts:
                raise
            delay = min(60, 5 * attempt)
            next_location, next_server_type = candidates[attempt % len(candidates)]
            log(
                f"  VM '{name}' placement failed in {location}: {exc}. "
                f"Retrying in {delay}s using {next_server_type} in {next_location}."
            )
            time.sleep(delay)
    else:
        raise RuntimeError(f"Could not create VM '{name}': {last_error}")

    # Poll until the server exists and is running.
    deadline = time.time() + 600
    server = None
    while time.time() < deadline:
        server = hc.servers.get_by_name(name)
        if server and server.status == "running":
            break
        time.sleep(5)
    if not server or server.status != "running":
        raise RuntimeError(f"Server '{name}' did not reach running state after 600s")

    ip = server.public_net.ipv4.ip
    log(
        f"  VM '{name}' created using {successful_server_type} "
        f"in {successful_location} (IP: {ip})"
    )
    return name, ip


def vm_name(slug: str, role: str) -> str:
    """Return the Hetzner server name for a student's VM role."""
    return slug if role == "primary" else f"{slug}-{role}"


def provision_hetzner() -> dict[str, dict[str, str]]:
    """Phase 1: create all primary/cp/worker VMs. Returns {slug: {role: ip}}."""
    log("=== Phase 1: Hetzner Infrastructure ===")
    ssh_key = ensure_hetzner_ssh_key()
    ensure_firewall()

    vm_ips: dict[str, dict[str, str]] = {s["slug"]: {} for s in STUDENTS}
    jobs = [
        (s["slug"], role, vm_name(s["slug"], role))
        for s in STUDENTS
        for role in VM_ROLES
    ]

    with ThreadPoolExecutor(max_workers=min(CREATE_VM_WORKERS, len(jobs))) as pool:
        futures = {
            pool.submit(create_vm, name, ssh_key): (slug, role)
            for slug, role, name in jobs
        }
        for f in as_completed(futures):
            slug, role = futures[f]
            _name, ip = f.result()
            vm_ips[slug][role] = ip

    fw = hc.firewalls.get_by_name(FW_NAME)
    for slug, roles in vm_ips.items():
        for role in roles:
            name = vm_name(slug, role)
            server = hc.servers.get_by_name(name)
            if server:
                try:
                    hc.firewalls.apply_to_resources(
                        firewall=fw,
                        resources=[
                            FirewallResource(
                                type=FirewallResource.TYPE_SERVER,
                                server=server,
                            )
                        ],
                    )
                    log(f"  Firewall applied to '{name}'.")
                except Exception as e:
                    if "firewall_already_applied" in str(e):
                        log(f"  Firewall already applied to '{name}', skipping.")
                    else:
                        log(f"  Warning: could not apply firewall to '{name}': {e}")

    log("Phase 1 complete.\n")
    return vm_ips


# ─────────────────────────────────────────────────────────────────────────────
# Phase 2 — Cloudflare DNS
# ─────────────────────────────────────────────────────────────────────────────


def setup_dns(vm_ips: dict[str, dict[str, str]]) -> None:
    """Phase 2: create/update A records for every student VM."""
    log("=== Phase 2: Cloudflare DNS ===")
    for student in STUDENTS:
        dns_name = f"{student['slug']}.{DOMAIN_SUFFIX}"
        ip       = vm_ips[student["slug"]]["primary"]

        all_records = list(cf.dns.records.list(zone_id=CF_ZONE_ID))
        existing = [
            r for r in all_records
            if getattr(r, "name", "") == dns_name and getattr(r, "type", "") == "A"
        ]

        if existing:
            rec = existing[0]
            if getattr(rec, "content", "") == ip:
                log(f"  DNS {dns_name} -> {ip} already correct, skipping.")
                continue
            log(f"  Updating DNS {dns_name} -> {ip} ...")
            cf.dns.records.update(
                dns_record_id=rec.id,
                zone_id=CF_ZONE_ID,
                name=dns_name,
                type="A",
                content=ip,
                proxied=False,
                ttl=300,
            )
        else:
            log(f"  Creating DNS {dns_name} -> {ip} ...")
            cf.dns.records.create(
                zone_id=CF_ZONE_ID,
                name=dns_name,
                type="A",
                content=ip,
                proxied=False,
                ttl=300,
            )
    log("Phase 2 complete.\n")


# ─────────────────────────────────────────────────────────────────────────────
# Phase 3 — Base OS Configuration (all VMs)
# ─────────────────────────────────────────────────────────────────────────────


def configure_base_vm(name: str, ip: str) -> None:
    """
    On a single VM:
      - upgrade packages, install essentials
      - create 'student' user with passwordless sudo
      - place provisioner's public key in student's authorized_keys
    """
    pub_key      = Path(SSH_PUBLIC_KEY).read_text().strip()
    sudoers_line = "student ALL=(ALL) NOPASSWD:ALL\n"

    log(f"  [{name}] Configuring base OS ...")
    for attempt in range(1, 6):
        try:
            with connection(ip) as c:
                # Wait for any cloud-init / unattended-upgrades apt lock to clear
                c.run(
                    "systemctl stop unattended-upgrades 2>/dev/null || true && "
                    "flock --timeout 120 /var/lib/dpkg/lock-frontend true 2>/dev/null || true",
                    hide=True,
                )
                # Remove any stale/broken third-party apt sources from a previous
                # partial run so that apt-get update doesn't fail on them.
                c.run(
                    "rm -f /etc/apt/sources.list.d/helm-stable-debian.list "
                    "/usr/share/keyrings/helm.gpg",
                    hide=True,
                )
                c.run(
                    "apt-get update -qq && "
                    "NEEDRESTART_SUSPEND=1 DEBIAN_FRONTEND=noninteractive "
                    "apt-get upgrade -y -qq",
                    hide=True,
                )
                c.run(
                    "NEEDRESTART_SUSPEND=1 DEBIAN_FRONTEND=noninteractive "
                    "apt-get install -y -qq "
                    "curl git vim net-tools gnupg lsb-release "
                    "apt-transport-https ca-certificates",
                    hide=True,
                )
                # Create student user (idempotent)
                c.run("id student &>/dev/null || useradd -m -s /bin/bash student", hide=True)

                # Passwordless sudo
                put_text(c, sudoers_line, "/etc/sudoers.d/student")
                c.run("chmod 440 /etc/sudoers.d/student", hide=True)

                # SSH authorised key for student
                c.run(
                    "install -d -m 700 -o student -g student /home/student/.ssh",
                    hide=True,
                )
                put_text(c, pub_key + "\n", "/home/student/.ssh/authorized_keys")
                c.run(
                    "chmod 600 /home/student/.ssh/authorized_keys && "
                    "chown student:student /home/student/.ssh/authorized_keys",
                    hide=True,
                )
            log(f"  [{name}] Base configuration done.")
            return
        except Exception as e:
            log(f"  [{name}] Attempt {attempt}/5 failed: {type(e).__name__}: {e}")
            if attempt < 5:
                time.sleep(15)
    raise RuntimeError(f"[{name}] All 5 base-configuration attempts failed")


def configure_clean_node(name: str, ip: str) -> None:
    """Configure only the student account and provisioner SSH access."""
    pub_key = Path(SSH_PUBLIC_KEY).read_text().strip()
    sudoers_line = "student ALL=(ALL) NOPASSWD:ALL\n"

    log(f"  [{name}] Configuring clean Ubuntu node access ...")
    for attempt in range(1, 6):
        try:
            with connection(ip) as c:
                c.run("id student &>/dev/null || useradd -m -s /bin/bash student", hide=True)
                put_text(c, sudoers_line, "/etc/sudoers.d/student")
                c.run("chmod 440 /etc/sudoers.d/student", hide=True)
                c.run(
                    "install -d -m 700 -o student -g student /home/student/.ssh",
                    hide=True,
                )
                put_text(c, pub_key + "\n", "/home/student/.ssh/authorized_keys")
                c.run(
                    "chmod 600 /home/student/.ssh/authorized_keys && "
                    "chown student:student /home/student/.ssh/authorized_keys",
                    hide=True,
                )
            log(f"  [{name}] Clean node access configuration done.")
            return
        except Exception as e:
            log(f"  [{name}] Attempt {attempt}/5 failed: {type(e).__name__}: {e}")
            if attempt < 5:
                time.sleep(15)
    raise RuntimeError(f"[{name}] All 5 clean-node configuration attempts failed")


def configure_all_base(vm_ips: dict[str, dict[str, str]]) -> None:
    """Phase 3: configure primary fully; keep cp/worker minimal."""
    log("=== Phase 3: Base OS Configuration ===")
    all_vms = [
        (slug, role, vm_name(slug, role), ip)
        for slug, roles in vm_ips.items()
        for role, ip in roles.items()
    ]

    for _slug, _role, _name, ip in all_vms:
        wait_for_ssh(ip)

    with ThreadPoolExecutor(max_workers=min(16, len(all_vms))) as pool:
        futures = {}
        for _slug, role, name, ip in all_vms:
            fn = configure_base_vm if role == "primary" else configure_clean_node
            futures[pool.submit(fn, name, ip)] = name
        for f in as_completed(futures):
            f.result()
    log("Phase 3 complete.\n")


# ─────────────────────────────────────────────────────────────────────────────
# Phase 4 — code-server + Caddy + Docker + kubectl + Helm
# ─────────────────────────────────────────────────────────────────────────────

_CADDY_INSTALL = """\
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
    | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg 2>/dev/null
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
    | tee /etc/apt/sources.list.d/caddy-stable.list >/dev/null
apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq caddy
"""

_DOCKER_INSTALL = """\
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    | tee /etc/apt/sources.list.d/docker.list >/dev/null
apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin
systemctl enable docker
usermod -aG docker student
"""

_KUBECTL_INSTALL = """\
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.31/deb/Release.key \
    | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
chmod a+r /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] \
https://pkgs.k8s.io/core:/stable:/v1.31/deb/ /' \
    | tee /etc/apt/sources.list.d/kubernetes.list >/dev/null
apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq kubectl
"""

_HELM_INSTALL = """\
curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
"""

_TRIVY_INSTALL = """\
curl -fsSL https://aquasecurity.github.io/trivy-repo/deb/public.key \
    | gpg --dearmor -o /usr/share/keyrings/trivy.gpg
chmod a+r /usr/share/keyrings/trivy.gpg
echo 'deb [signed-by=/usr/share/keyrings/trivy.gpg] \
https://aquasecurity.github.io/trivy-repo/deb generic main' \
    | tee /etc/apt/sources.list.d/trivy.list >/dev/null
apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq trivy
"""


def configure_vm(ip: str, slug: str, password: str) -> None:
    """
    Install and configure the full toolstack on one VM:
      code-server, Caddy, Docker Engine, Docker Compose v2, kubectl, Helm.
    """
    fqdn = f"{slug}.{DOMAIN_SUFFIX}"

    cs_config = (
        "bind-addr: 127.0.0.1:8080\n"
        "auth: password\n"
        f"password: {password}\n"
        "cert: false\n"
    )

    caddyfile = (
        f"{fqdn} {{\n"
        f"    reverse_proxy localhost:8080\n"
        f"    log {{\n"
        f"        output file /var/log/caddy/access.log\n"
        f"    }}\n"
        f"}}\n"
    )

    with connection(ip) as c:
        log(f"  [{slug}] Installing code-server ...")
        c.run("curl -fsSL https://code-server.dev/install.sh | sh", hide=True)

        log(f"  [{slug}] Configuring code-server ...")
        c.run(
            "install -d -m 755 -o student -g student "
            "/home/student/.config/code-server",
            hide=True,
        )
        put_text(c, cs_config, "/home/student/.config/code-server/config.yaml")
        c.run(
            "chown student:student /home/student/.config/code-server/config.yaml",
            hide=True,
        )
        put_text(c, CODESERVER_SERVICE, "/etc/systemd/system/code-server.service")
        c.run(
            "systemctl daemon-reload && systemctl enable code-server && "
            "systemctl restart code-server",
            hide=True,
        )

        log(f"  [{slug}] Installing Caddy ...")
        c.run(_CADDY_INSTALL, hide=True)
        c.run("mkdir -p /var/log/caddy", hide=True)
        put_text(c, caddyfile, "/etc/caddy/Caddyfile")
        c.run("systemctl enable caddy && systemctl restart caddy", hide=True)

        log(f"  [{slug}] Installing Docker Engine + Compose v2 ...")
        c.run(_DOCKER_INSTALL, hide=True)

        log(f"  [{slug}] Installing kubectl ...")
        c.run(_KUBECTL_INSTALL, hide=True)

        log(f"  [{slug}] Installing Helm ...")
        c.run(_HELM_INSTALL, hide=True)

        log(f"  [{slug}] Installing Trivy ...")
        c.run(_TRIVY_INSTALL, hide=True)

    log(f"  [{slug}] VM setup complete.")


def configure_student_ssh(primary_ip: str, cp_ip: str, worker_ip: str, slug: str) -> None:
    """Create a per-student keypair on primary and configure ssh cp/worker aliases."""
    log(f"  [{slug}] Configuring SSH access to cp and worker ...")

    with connection(primary_ip) as c:
        c.run(
            "install -d -m 700 -o student -g student /home/student/.ssh && "
            "test -f /home/student/.ssh/id_ed25519 || "
            "sudo -u student ssh-keygen -t ed25519 -N '' "
            "-C 'container-seminar' -f /home/student/.ssh/id_ed25519",
            hide=True,
        )
        result = c.run("cat /home/student/.ssh/id_ed25519.pub", hide=True)
        student_pub = result.stdout.strip()

        ssh_config = (
            "Host cp\n"
            f"    HostName {cp_ip}\n"
            "    User student\n"
            "    IdentityFile ~/.ssh/id_ed25519\n"
            "    IdentitiesOnly yes\n"
            "    StrictHostKeyChecking accept-new\n"
            "\n"
            "Host worker\n"
            f"    HostName {worker_ip}\n"
            "    User student\n"
            "    IdentityFile ~/.ssh/id_ed25519\n"
            "    IdentitiesOnly yes\n"
            "    StrictHostKeyChecking accept-new\n"
        )
        put_text(c, ssh_config, "/home/student/.ssh/config")
        c.run(
            "chmod 600 /home/student/.ssh/config && "
            "chown student:student /home/student/.ssh/config",
            hide=True,
        )

    for role, ip in (("cp", cp_ip), ("worker", worker_ip)):
        with connection(ip) as c:
            put_text(c, student_pub + "\n", "/tmp/student-primary.pub")
            c.run(
                "install -d -m 700 -o student -g student /home/student/.ssh && "
                "touch /home/student/.ssh/authorized_keys && "
                "grep -qxF -f /tmp/student-primary.pub "
                "/home/student/.ssh/authorized_keys || "
                "cat /tmp/student-primary.pub >> /home/student/.ssh/authorized_keys; "
                "chmod 600 /home/student/.ssh/authorized_keys; "
                "chown student:student /home/student/.ssh/authorized_keys; "
                "rm -f /tmp/student-primary.pub",
                hide=True,
            )
        log(f"  [{slug}] SSH key installed on {role}.")


def configure_all_student_ssh(vm_ips: dict[str, dict[str, str]]) -> None:
    """Configure per-student primary -> cp/worker SSH access in parallel."""
    log("=== Phase 5: Student SSH Access ===")
    with ThreadPoolExecutor(max_workers=min(8, len(STUDENTS))) as pool:
        futures = {
            pool.submit(
                configure_student_ssh,
                vm_ips[s["slug"]]["primary"],
                vm_ips[s["slug"]]["cp"],
                vm_ips[s["slug"]]["worker"],
                s["slug"],
            ): s["slug"]
            for s in STUDENTS
        }
        for f in as_completed(futures):
            f.result()
    log("Phase 5 complete.\n")


# ────────────────────────────────────────────────────────────────────────────────────────────────────────────
# Phase 6 — Shared Kubernetes Cluster Access (staging + prod)
# ────────────────────────────────────────────────────────────────────────────────────────────────────────────


def build_merged_kubeconfig() -> str:
    """Merge the staging + prod admin kubeconfigs (written by `terraform apply`
    in terraform/) into a single kubeconfig with two contexts named 'staging'
    and 'prod', defaulting to 'staging'.

    NOTE: this distributes the cluster-admin credentials as-is to every
    student — there is no per-student RBAC scoping. Acceptable for this
    seminar's shared infra, but do not reuse for anything requiring isolation.
    """
    merged: dict = {
        "apiVersion": "v1",
        "kind": "Config",
        "preferences": {},
        "clusters": [],
        "users": [],
        "contexts": [],
        "current-context": "staging",
    }
    for env, path in CLUSTER_KUBECONFIGS.items():
        if not path.exists():
            raise FileNotFoundError(
                f"{path} not found — run `terraform apply` in terraform/ first "
                f"so it can write the {env} cluster's kubeconfig."
            )
        raw = yaml.safe_load(path.read_text())
        cluster = raw["clusters"][0]["cluster"]
        user = raw["users"][0]["user"]
        namespace = raw["contexts"][0]["context"].get("namespace")

        # Terraform names each generated cluster/user/context after the cluster
        # itself (e.g. "container-seminar-staging"); rename them to the short
        # env name so both fit in one file without colliding.
        merged["clusters"].append({"name": env, "cluster": cluster})
        merged["users"].append({"name": f"{env}-admin", "user": user})
        context = {"cluster": env, "user": f"{env}-admin"}
        if namespace:
            context["namespace"] = namespace
        merged["contexts"].append({"name": env, "context": context})

    return yaml.safe_dump(merged, default_flow_style=False, sort_keys=False)


def configure_student_kubeconfig(primary_ip: str, slug: str, merged_kubeconfig: str) -> None:
    """Install the merged staging+prod kubeconfig on one student's primary VM."""
    log(f"  [{slug}] Installing shared cluster kubeconfig (staging, prod) ...")
    with connection(primary_ip) as c:
        c.run("install -d -m 700 -o student -g student /home/student/.kube", hide=True)
        put_text(c, merged_kubeconfig, "/home/student/.kube/config")
        c.run(
            "chmod 600 /home/student/.kube/config && "
            "chown student:student /home/student/.kube/config",
            hide=True,
        )
        c.run("install -d -m 700 /root/.kube", hide=True)
        put_text(c, merged_kubeconfig, "/root/.kube/config")
        c.run("chmod 600 /root/.kube/config", hide=True)
    log(f"  [{slug}] Kubeconfig installed (contexts: staging, prod).")


def configure_all_student_kubeconfigs(vm_ips: dict[str, dict[str, str]]) -> None:
    """Phase 6: install the merged staging+prod kubeconfig on every primary VM."""
    log("=== Phase 6: Student Kubernetes Cluster Access ===")
    merged_kubeconfig = build_merged_kubeconfig()
    with ThreadPoolExecutor(max_workers=min(8, len(STUDENTS))) as pool:
        futures = {
            pool.submit(
                configure_student_kubeconfig,
                vm_ips[s["slug"]]["primary"],
                s["slug"],
                merged_kubeconfig,
            ): s["slug"]
            for s in STUDENTS
        }
        for f in as_completed(futures):
            f.result()
    log("Phase 6 complete.\n")


def configure_all_vms(vm_ips: dict[str, dict[str, str]]) -> dict[str, str]:
    """Phase 4: configure all VMs in parallel. Returns {slug: password}."""
    log("=== Phase 4: code-server + Caddy + Docker + kubectl + Helm ===")

    passwords = load_or_generate_passwords()

    with ThreadPoolExecutor(max_workers=len(STUDENTS)) as pool:
        futures = {
            pool.submit(
                configure_vm,
                vm_ips[s["slug"]]["primary"],
                s["slug"],
                passwords[s["slug"]],
            ): s["slug"]
            for s in STUDENTS
        }
        for f in as_completed(futures):
            f.result()

    log("Phase 4 complete.\n")
    return passwords


# ─────────────────────────────────────────────────────────────────────────────
# Phase 7 — Summary
# ─────────────────────────────────────────────────────────────────────────────


def print_summary(vm_ips: dict[str, dict[str, str]], passwords: dict[str, str]) -> None:
    sep = "=" * 72
    print(f"\n{sep}")
    print("  PROVISIONING COMPLETE — Container Seminar")
    print(sep)
    for s in STUDENTS:
        slug = s["slug"]
        primary_ip = vm_ips[slug]["primary"]
        cp_ip = vm_ips[slug]["cp"]
        worker_ip = vm_ips[slug]["worker"]
        url = f"https://{slug}.{DOMAIN_SUFFIX}"
        print(f"\n  Student    : {s['display']}")
        print(f"  Primary VM : {slug} ({primary_ip})")
        print(f"  CP VM      : {vm_name(slug, 'cp')} ({cp_ip})")
        print(f"  Worker VM  : {vm_name(slug, 'worker')} ({worker_ip})")
        print(f"  URL        : {url}")
        print(f"  Password   : {passwords[slug]}")
        print(f"  Primary SSH: ssh student@{primary_ip}")
        print("  From primary: ssh cp | ssh worker")
    print()
    print("  NOTES:")
    print("  • Only the primary VM receives code-server and the seminar toolchain.")
    print("  • cp and worker remain clean Ubuntu VMs with only the student account configured.")
    print("  • Each student has a dedicated SSH key stored on their primary VM.")
    print("  • From the primary VM, use exactly: ssh cp  or  ssh worker")
    print("  • /home/student/.kube/config on the primary VM has cluster-admin access")
    print("    to both shared clusters as contexts 'staging' and 'prod'.")
    print(sep)

    md_path = Path(__file__).parent / "container-seminar-credentials.md"
    rows = "\n".join(
        f"| {s['display']} "
        f"| https://{s['slug']}.{DOMAIN_SUFFIX} "
        f"| `{passwords[s['slug']]}` "
        f"| {vm_ips[s['slug']]['primary']} "
        f"| {vm_ips[s['slug']]['cp']} "
        f"| {vm_ips[s['slug']]['worker']} |"
        for s in STUDENTS
    )
    md_path.write_text(
        "# Container Seminar — Credentials\n\n"
        "| Student | URL | Password | Primary IP | CP IP | Worker IP |\n"
        "|---|---|---|---|---|---|\n"
        f"{rows}\n\n"
        "Primary login: `ssh student@<primary-ip>`  \n"
        "From the primary VM: `ssh cp` or `ssh worker`.  \n"
        "Only the primary VM has code-server and the seminar toolchain; cp and worker are clean Ubuntu VMs.  \n"
        "`~/.kube/config` on the primary VM has cluster-admin access to the shared "
        "`staging`/`prod` clusters (contexts of the same name; `staging` is default).\n"
    )
    print(f"\n  Credentials written to: {md_path}")


# ─────────────────────────────────────────────────────────────────────────────
# Entry point
# ─────────────────────────────────────────────────────────────────────────────


def main() -> None:
    vm_ips = provision_hetzner()

    # DNS setup and base OS config are independent — run them concurrently
    with ThreadPoolExecutor(max_workers=2) as pool:
        dns_f  = pool.submit(setup_dns, vm_ips)
        base_f = pool.submit(configure_all_base, vm_ips)
        dns_f.result()
        base_f.result()

    passwords = configure_all_vms(vm_ips)
    configure_all_student_ssh(vm_ips)
    configure_all_student_kubeconfigs(vm_ips)
    print_summary(vm_ips, passwords)


if __name__ == "__main__":
    main()