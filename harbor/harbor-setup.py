#!/usr/bin/env python3
"""
Harbor Setup — Container Seminar
=================================

Creates Harbor users, a shared seminar project, and individual private
projects for each student. Run this after deploy.sh completes and Harbor
is accessible at https://harbor.container.it-scholar.com.

Usage:
    python3 harbor/harbor-setup.py

What it creates:
  Users      — one per student, username = VM slug (e.g. ben-coeppicus),
               password = same password used for their code-server VM
  Project    — "seminar" (public): all students get Developer role
             — "<slug>" (private): student gets ProjectAdmin role on their own project

Harbor role IDs:
  1 = ProjectAdmin, 2 = Maintainer, 3 = Developer, 4 = Guest
"""

from __future__ import annotations

import json
import sys
import time
from pathlib import Path

import requests
from requests.auth import HTTPBasicAuth

# ─────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────

HARBOR_URL = "https://harbor.container.it-scholar.com"

ROOT_DIR           = Path(__file__).resolve().parent.parent
PASSWORDS_FILE     = ROOT_DIR / ".passwords.json"
HARBOR_PWDS_FILE   = ROOT_DIR / ".harbor-passwords.json"

SHARED_PROJECT = "seminar"

ROLE_PROJECT_ADMIN = 1
ROLE_DEVELOPER     = 3

# Must stay in sync with STUDENTS in provision.py
STUDENTS = [
    {"slug": "ben-coeppicus",        "display": "Ben Cöppicus"},
    {"slug": "student-11",     "display": "Student 11"},
    {"slug": "student-12", "display": "Student 12"},
    {"slug": "student-13",        "display": "Student 13"},
    {"slug": "student-14",        "display": "Student 14"},
    {"slug": "student-15",         "display": "Student 15"},
    {"slug": "student-16",        "display": "Student 16"},
    {"slug": "student-17",         "display": "Student 17"},
    {"slug": "student-18",        "display": "Student 18"},
    {"slug": "student-19",        "display": "Student 19"},
    {"slug": "student-20",       "display": "Student 20"},
    {"slug": "student-21",         "display": "Student 21"},
]

# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────


def load_passwords() -> tuple[dict[str, str], str]:
    """Return (slug → vm_password, harbor_admin_password)."""
    if not PASSWORDS_FILE.exists():
        sys.exit(f"ERROR: {PASSWORDS_FILE} not found — run provision.py first.")
    if not HARBOR_PWDS_FILE.exists():
        sys.exit(f"ERROR: {HARBOR_PWDS_FILE} not found — run harbor/deploy.sh first.")
    vm_passwords     = json.loads(PASSWORDS_FILE.read_text())
    harbor_passwords = json.loads(HARBOR_PWDS_FILE.read_text())
    return vm_passwords, harbor_passwords["admin"]


def api(base_url: str) -> str:
    return f"{base_url}/api/v2.0"


def wait_for_harbor(base_url: str, auth: HTTPBasicAuth, timeout: int = 300) -> None:
    """Block until the Harbor health endpoint responds 200."""
    deadline = time.time() + timeout
    print("Waiting for Harbor API", end="", flush=True)
    while time.time() < deadline:
        try:
            r = requests.get(
                f"{api(base_url)}/systeminfo",
                auth=auth,
                timeout=10,
                verify=True,
            )
            if r.status_code == 200:
                print(" ready.")
                return
        except requests.exceptions.RequestException:
            pass
        print(".", end="", flush=True)
        time.sleep(5)
    print()
    sys.exit(f"ERROR: Harbor API not accessible at {base_url} after {timeout}s.")


# ─────────────────────────────────────────────────────────────────────────────
# Users
# ─────────────────────────────────────────────────────────────────────────────


def existing_usernames(base_url: str, auth: HTTPBasicAuth) -> set[str]:
    r = requests.get(
        f"{api(base_url)}/users",
        auth=auth,
        params={"page_size": 100},
        timeout=15,
    )
    r.raise_for_status()
    return {u["username"] for u in r.json()}


def _harbor_password(password: str) -> str:
    """Ensure password meets Harbor's policy: upper + lower + digit, 8-128 chars."""
    import re
    if not re.search(r"[0-9]", password):
        password = password + "1"
    if not re.search(r"[A-Z]", password):
        password = password + "A"
    if not re.search(r"[a-z]", password):
        password = password + "a"
    return password


def create_user(
    base_url: str,
    auth: HTTPBasicAuth,
    slug: str,
    display: str,
    password: str,
) -> None:
    harbor_pw = _harbor_password(password)
    if harbor_pw != password:
        print(f"    [!] {slug}: password adjusted for Harbor policy → {harbor_pw}")
    r = requests.post(
        f"{api(base_url)}/users",
        auth=auth,
        json={
            "username": slug,
            "password": harbor_pw,
            "email": f"{slug}@container.it-scholar.com",
            "realname": display,
            "comment": "Container Seminar participant",
        },
        timeout=15,
    )
    if r.status_code == 201:
        print(f"    [+] user  {slug}")
    elif r.status_code in (409, 400) and "already exist" in r.text:
        print(f"    [=] user  {slug}  (already exists)")
    else:
        r.raise_for_status()


# ─────────────────────────────────────────────────────────────────────────────
# Projects
# ─────────────────────────────────────────────────────────────────────────────


def existing_projects(base_url: str, auth: HTTPBasicAuth) -> set[str]:
    r = requests.get(
        f"{api(base_url)}/projects",
        auth=auth,
        params={"page_size": 100},
        timeout=15,
    )
    r.raise_for_status()
    return {p["name"] for p in r.json()}


def create_project(
    base_url: str,
    auth: HTTPBasicAuth,
    name: str,
    *,
    public: bool,
) -> None:
    r = requests.post(
        f"{api(base_url)}/projects",
        auth=auth,
        json={
            "project_name": name,
            "public": public,
            "metadata": {"public": "true" if public else "false"},
        },
        timeout=15,
    )
    visibility = "public " if public else "private"
    if r.status_code == 201:
        print(f"    [+] project  {name}  ({visibility})")
    elif r.status_code == 409:
        print(f"    [=] project  {name}  (already exists)")
    else:
        r.raise_for_status()


def add_member(
    base_url: str,
    auth: HTTPBasicAuth,
    project_name: str,
    username: str,
    role_id: int,
) -> None:
    r = requests.post(
        f"{api(base_url)}/projects/{project_name}/members",
        auth=auth,
        json={"role_id": role_id, "member_user": {"username": username}},
        timeout=15,
    )
    role_label = {ROLE_PROJECT_ADMIN: "ProjectAdmin", ROLE_DEVELOPER: "Developer"}.get(
        role_id, str(role_id)
    )
    if r.status_code == 201:
        print(f"    [+] member  {username}  → {project_name}  as {role_label}")
    elif r.status_code == 409:
        print(f"    [=] member  {username}  → {project_name}  (already exists)")
    else:
        r.raise_for_status()


# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────


def main() -> None:
    vm_passwords, admin_password = load_passwords()
    auth = HTTPBasicAuth("admin", admin_password)

    wait_for_harbor(HARBOR_URL, auth)

    # ── Users ─────────────────────────────────────────────────────────────────
    print("\n── Creating users ──────────────────────────────────────────────────")
    known_users = existing_usernames(HARBOR_URL, auth)
    for s in STUDENTS:
        if s["slug"] not in known_users:
            create_user(HARBOR_URL, auth, s["slug"], s["display"], vm_passwords[s["slug"]])
        else:
            print(f"    [=] user  {s['slug']}  (already exists)")

    # ── Shared public project ─────────────────────────────────────────────────
    print("\n── Shared project 'seminar' (public) ───────────────────────────────")
    create_project(HARBOR_URL, auth, SHARED_PROJECT, public=True)
    for s in STUDENTS:
        add_member(HARBOR_URL, auth, SHARED_PROJECT, s["slug"], ROLE_DEVELOPER)

    # ── Private projects per student ──────────────────────────────────────────
    print("\n── Private projects (one per student) ──────────────────────────────")
    known_projects = existing_projects(HARBOR_URL, auth)
    for s in STUDENTS:
        slug = s["slug"]
        if slug not in known_projects:
            create_project(HARBOR_URL, auth, slug, public=False)
        else:
            print(f"    [=] project  {slug}  (already exists)")
        add_member(HARBOR_URL, auth, slug, slug, ROLE_PROJECT_ADMIN)

    # ── Summary ───────────────────────────────────────────────────────────────
    registry_host = HARBOR_URL.removeprefix("https://")
    print(f"""
────────────────────────────────────────────────────────────────────────
  Harbor setup complete
────────────────────────────────────────────────────────────────────────
  URL:            {HARBOR_URL}
  Admin password: {admin_password}

  Shared project: {HARBOR_URL}/harbor/seminar     (all students, Developer)
  Private:        {HARBOR_URL}/harbor/<slug>       (student is ProjectAdmin)

  Docker login example (students run this on their VM):
    docker login {registry_host}
    # username: <slug>   e.g. ben-coeppicus
    # password: <VM code-server password>

  Push example:
    docker tag myimage {registry_host}/ben-coeppicus/myimage:v1
    docker push {registry_host}/ben-coeppicus/myimage:v1
────────────────────────────────────────────────────────────────────────
""")


if __name__ == "__main__":
    main()
