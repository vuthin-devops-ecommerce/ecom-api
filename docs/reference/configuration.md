# Configuration reference

> **គោលបំណង:** env var, profile, secret និងកន្លែងដែល set វាតាម environment · **អ្នកអាន:** អ្នក run/deploy app · **Update:** 2026-09-30

## App (Spring Boot) — env var

App អាន config ពី env var តែប៉ុណ្ណោះ (`application.yml` → `${…}`)។ Image ដដែលគ្រប់ environment; config ប្តូរ។

| Env var | ចាំបាច់ | Default | ឧទាហរណ៍ | ចំណាំ |
|---|---|---|---|---|
| `SPRING_DATASOURCE_URL` | ✅ | **គ្មាន** (fail fast) | `jdbc:postgresql://postgres:5432/minishop` | host = Service/compose name |
| `SPRING_DATASOURCE_USERNAME` | ✅ | **គ្មាន** | `minishop` | |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | **គ្មាន** | — | **secret** — មិនដែលក្នុង git |
| `SPRING_PROFILES_ACTIVE` | | (គ្មាន) | `dev` | `dev` = Flyway បន្ថែម `db/dev` seed (8 products); `it` = test |
| `JAVA_TOOL_OPTIONS` | | (គ្មាន) | `-XX:MaxRAMPercentage=75.0` | JVM អានស្វ័យប្រវត្តិ; Dockerfile ENTRYPOINT មាន flag ដដែលរួច |

Config ថេរក្នុង `application.yml` (មិនមែន env): `ddl-auto: validate`, `open-in-view: false`, actuator expose `health,info,metrics,prometheus`, `health.probes.enabled: true`។

## កន្លែង set តាម environment

| Env var | Laptop (`./mvnw spring-boot:run`) | Docker Compose | Kubernetes | CI |
|---|---|---|---|---|
| `SPRING_DATASOURCE_URL` | `export` / `mini-shop/env.sh` | `compose.yaml` (`db:5432`) | ConfigMap `app-config` | Testcontainers `@ServiceConnection` |
| `SPRING_DATASOURCE_USERNAME` | ដដែល | `compose.yaml` | ConfigMap `app-config` | Testcontainers |
| `SPRING_DATASOURCE_PASSWORD` | ដដែល | `.env` → `DB_PASSWORD` | Secret `postgres-secret` key `POSTGRES_PASSWORD` | Testcontainers |
| `SPRING_PROFILES_ACTIVE` | `export` | `.env` | ConfigMap `app-config` | `it` (ក្នុង test) |
| `JAVA_TOOL_OPTIONS` | — | — | ConfigMap `app-config` | — |

## Docker Compose — `mini-shop/.env` (gitignored; template `.env.example`)

| Key | Default | ចំណាំ |
|---|---|---|
| `DB_PASSWORD` | — (required, `:?` error) | ប្រើទាំង `db` និង `app` |
| `SPRING_PROFILES_ACTIVE` | `dev` | |
| `APP_PORT` | `8080` | host port; container តែងតែ 8080 |

## Kubernetes objects (namespace `minishop`)

| Object | Kind | Keys | មកពី | Commit? |
|---|---|---|---|---|
| `app-config` | ConfigMap | datasource URL/USERNAME, profile, JAVA_TOOL_OPTIONS | `k8s/app/configmap.yaml` | ✅ |
| `postgres-config` | ConfigMap | `POSTGRES_DB`, `POSTGRES_USER`, `PGDATA` | `k8s/postgres/configmap.yaml` | ✅ |
| `postgres-secret` | Secret Opaque | `POSTGRES_PASSWORD` | `k8s/postgres/secret.yaml` (copy ពី `secret.example.yaml`) | ❌ gitignored |
| `ghcr-creds` | Secret dockerconfigjson | ghcr.io PAT `read:packages` | `kubectl create secret docker-registry` | ❌ មិនមាន file |

ចំណាំ: ConfigMap/Secret ប្តូរ → pod ដែល run **មិនឃើញ** (env អានពេល start) → `kubectl -n minishop rollout restart deployment/minishop-app`។

## Secret — សង្ខេប

| Secret | រស់នៅ | Rotate |
|---|---|---|
| DB password (dev) | `.env`, `k8s/postgres/secret.yaml` លើ laptop | ដោយដៃ; Phase B → Vault/External Secrets |
| ghcr pull PAT | Secret `ghcr-creds` ក្នុង cluster | ផុតកំណត់ 30 ថ្ងៃ → បង្កើតថ្មី (`kubectl delete secret` + `create`) |
| `GITHUB_TOKEN` (CI) | GitHub បង្កើតរាល់ run | ស្វ័យប្រវត្តិ (អស់សុពលភាពពេល run ចប់) |

ច្បាប់: គ្មាន secret ក្នុង git (CLAUDE.md §4) — `.env`, `env.sh`, `k8s/**/secret.yaml` gitignored; commit `.example` ជំនួស។
