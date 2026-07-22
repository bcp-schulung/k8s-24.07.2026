# GitHub Actions Workflows

This directory contains GitHub Actions workflows for building and publishing container images for the Joke Platform services.

## Workflows

### 1. Build and Push Container Images (`build-images.yaml`)

**Triggers:**
- Push to `main` or `develop` branches
- Push of version tags (`v*`)
- Pull requests to `main` or `develop`

**What it does:**
- Builds all five services in parallel using a matrix strategy
- Pushes images to GitHub Container Registry (ghcr.io)
- Creates multiple tags:
  - Branch name (e.g., `main`, `develop`)
  - Git SHA with branch prefix (e.g., `main-abc1234`)
  - Semantic version tags for releases (e.g., `v1.0.0`, `1.0`, `1`)
  - `latest` tag for the default branch
- Uses GitHub Actions cache for faster builds
- Only pushes on actual pushes (not on PRs)

**Image naming:**
```
ghcr.io/{owner}/joke-platform/{service}:{tag}
```

### 2. Manual Image Build (`manual-build.yaml`)

**Trigger:** Manual workflow dispatch from GitHub Actions UI

**Options:**
- **Service**: Choose a specific service or `all` to build all services
- **Registry**: Choose between GitHub Container Registry (`ghcr.io`) or Harbor
- **Tag**: Optional custom tag (defaults to `manual-{run_number}`)

**What it does:**
- Builds selected service(s)
- Pushes to chosen registry
- Useful for:
  - Testing builds
  - Creating custom-tagged images
  - Pushing to alternative registries

**Harbor setup requirements:**
If using Harbor registry, configure these secrets:
- `HARBOR_REGISTRY`: Harbor registry URL (e.g., `harbor.example.com`)
- `HARBOR_USERNAME`: Harbor username
- `HARBOR_PASSWORD`: Harbor password or robot token

### 3. PR Validation (`pr-validation.yaml`)

**Triggers:**
- Pull requests to `main` or `develop`
- Only when changes affect:
  - `joke-platform/**` directory
  - Workflow files

**What it does:**
- Builds all services to validate they compile
- Does NOT push images (build-only validation)
- Fails fast if any service fails to build
- Provides summary of build results

## Services

The workflows build the following services:
- **joke-gateway**: API gateway service
- **joke-generator**: Joke generation service
- **punchline-service**: Punchline delivery service
- **audience-service**: Audience reaction service
- **chaos-comedian**: Chaos engineering service

## Usage Examples

### Automatic Builds

1. **On feature development:**
   ```bash
   git checkout -b feature/my-feature
   # Make changes
   git push origin feature/my-feature
   # Creates PR → PR Validation workflow runs (build only)
   ```

2. **On merge to main:**
   ```bash
   git checkout main
   git merge feature/my-feature
   git push origin main
   # Build and Push workflow runs → images tagged with 'main' and SHA
   ```

3. **Creating a release:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   # Build and Push workflow runs → images tagged with v1.0.0, 1.0, 1, latest
   ```

### Manual Builds

1. Go to **Actions** tab in GitHub
2. Select **Manual Image Build** workflow
3. Click **Run workflow**
4. Choose:
   - Service to build (or `all`)
   - Target registry
   - Optional custom tag
5. Click **Run workflow**

## Image Tags

Images are tagged based on the trigger:

| Trigger | Example Tags |
|---------|--------------|
| Push to main | `main`, `main-abc1234`, `latest` |
| Push to develop | `develop`, `develop-abc1234` |
| PR #123 | `pr-123` (build only, not pushed) |
| Tag v1.2.3 | `v1.2.3`, `1.2`, `1`, `latest` |
| Manual build | `manual-42` or custom tag |

## Required Secrets

### GitHub Container Registry (default)
- `GITHUB_TOKEN` (automatically provided)

### Harbor Registry (optional)
- `HARBOR_REGISTRY`
- `HARBOR_USERNAME`
- `HARBOR_PASSWORD`

Configure secrets at: Settings → Secrets and variables → Actions

## Caching

All workflows use GitHub Actions cache to speed up builds by caching:
- Docker layer cache
- Maven dependencies (via Dockerfile layer caching)

## Platform Support

Currently building for:
- `linux/amd64`

To add ARM support (e.g., for Apple Silicon or ARM-based cloud instances), modify the `platforms` setting in workflow files.
