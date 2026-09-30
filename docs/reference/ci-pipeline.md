# CI pipeline reference

> **គោលបំណង:** job, trigger, permission, artifact, gate របស់ `.github/workflows/ci.yml` · **អ្នកអាន:** អ្នក debug CI ឬកែ workflow · **Update:** 2026-09-30
> ហេតុផល: [ADR-004](../decisions/004-ci-job-structure.md) (job structure), [ADR-005](../decisions/005-security-scan-policy.md) (scan policy) · CI ក្រហម: [runbooks/ci-failure.md](../runbooks/ci-failure.md)

## Trigger

| Event | Branch | Job ដែល run |
|---|---|---|
| `push` | ណាក៏បាន (`**`) | `unit-test`, `integration-test` · + `docker` បើ `main` |
| `pull_request` | → `main`, `develop` | `unit-test`, `integration-test` (`docker` skipped) |

`concurrency: ${{ github.workflow }}-${{ github.ref }}` + `cancel-in-progress` — push ថ្មីលើ branch ដដែល cancel run ចាស់។ Commit ដដែលអាច run ២ ដង (push + pull_request) ព្រោះ ref ខុសគ្នា។

## Job

| Job | `needs` | Runner | Command / step | Artifact | ពេល (cache hit, 2026-09) |
|---|---|---|---|---|---|
| `unit-test` | — | `ubuntu-24.04` | `./mvnw -B test` (surefire `*Test.java`) | `surefire-reports` | ~40s |
| `integration-test` | `unit-test` | `ubuntu-24.04` | `./mvnw -B verify -DskipUnitTests` (failsafe `*IT.java`, Testcontainers `postgres:17`) | `failsafe-reports` | ~50s |
| `docker` | ទាំង ២ | `ubuntu-24.04` | ខាងក្រោម | — | ~1–3 នាទី |

`docker` run តែ `github.ref == 'refs/heads/main' && github.event_name == 'push'`:

| # | Step | Gate? |
|---|---|---|
| 1 | checkout · setup-buildx · login ghcr.io (`GITHUB_TOKEN`) · metadata (tag `<short-sha>` + `latest`) | |
| 2 | Build image `load: true` (cache `type=gha,mode=max`) | |
| 3 | Trivy `severity: CRITICAL`, `ignore-unfixed: true`, `exit-code: 1` | ✅ **block** — fail → push skipped |
| 4 | Trivy `severity: HIGH`, `exit-code: 0` | report ប៉ុណ្ណោះ |
| 5 | Push `ghcr.io/vuthin-devops-ecommerce/mini-shop:<short-sha>` + `:latest` | |
| 6 | Step summary (tag + digest) | |

## Permission (least privilege)

| Scope | `contents` | `packages` |
|---|---|---|
| workflow (default ទាំងអស់) | read | — |
| job `docker` | read | **write** |

## Pinned actions (update ដោយ Dependabot)

| Action | Version |
|---|---|
| `actions/checkout` | v7.0.1 |
| `actions/setup-java` | v6.0.1 (temurin 21, `cache: maven`, key = hash `mini-shop/pom.xml`) |
| `actions/upload-artifact` | v7.0.1 |
| `docker/setup-buildx-action` | v4.4.1 |
| `docker/login-action` | v4.6.0 |
| `docker/metadata-action` | v6.2.0 |
| `docker/build-push-action` | v7.4.0 |
| `aquasecurity/trivy-action` | v0.36.0 (Trivy 0.70) |

គ្រប់ action pin ដោយ **commit SHA** + comment `# vX.Y.Z` (មើលក្នុង `ci.yml`)។

## Branch protection — `main`

| Rule | តម្លៃ |
|---|---|
| Require PR | ✅ (0 approval) |
| Required status checks | `unit-test`, `integration-test` (ឈ្មោះ job — ប្តូរឈ្មោះ = ត្រូវ update rule) |
| Up to date before merge (`strict`) | ❌ |
| Enforce for admins | ✅ |
| Force push / delete | ❌ |

## Dependabot — `.github/dependabot.yml`

| Ecosystem | Directory | Schedule | ចំណាំ |
|---|---|---|---|
| maven | `/mini-shop` | weekly Mon | Spring Boot grouped; major ignored |
| github-actions | `/` | weekly Mon | |
| docker | `/mini-shop` | weekly Mon | eclipse-temurin major ignored |
| docker-compose | `/mini-shop` | weekly Mon | postgres major ignored |

## Reproduce លើ laptop

```bash
cd mini-shop
./mvnw -B test                       # = unit-test
./mvnw -B verify -DskipUnitTests     # = integration-test (Docker ត្រូវ run)
```

`-DskipUnitTests` ជា property ដែលកំណត់ក្នុង `pom.xml` (surefire `<skipTests>${skipUnitTests}</skipTests>`) — មិនមែន flag ស្រាប់របស់ Maven។
