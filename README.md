# mini-shop · DevOps journey

[![CI](https://github.com/vuthin-devops-ecommerce/ecom-api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/vuthin-devops-ecommerce/ecom-api/actions/workflows/ci.yml)

REST API ហាងតូច (`catalog` product/stock + `order`) — Spring Boot 4.1 · Java 21 · PostgreSQL 17 · Flyway — ប្រើជា app ពិតសម្រាប់រៀន DevOps ជាជំហាន៖
code → CI (GitHub Actions, Trivy, ghcr.io) → Kubernetes (kind) → Terraform → …។ App ជា modular monolith ត្រៀមបំបែកជា service នៅ Phase B។

## Quickstart (Docker Compose)

```bash
cd mini-shop
cp .env.example .env
docker compose up --build
curl localhost:8080/actuator/health        # {"status":"UP",...}
open http://localhost:8080/swagger-ui.html
```

លើ Kubernetes: [docs/guides/run-on-kubernetes.md](docs/guides/run-on-kubernetes.md)។

## ឯកសារ

ចាប់ផ្តើមទីនេះ: **[docs/README.md](docs/README.md)** — "អ្នកជានរណា → អានអ្វី"។

| | |
|---|---|
| System ធ្វើការយ៉ាងណា | [docs/architecture.md](docs/architecture.md) |
| Run / test លើ laptop | [docs/guides/local-development.md](docs/guides/local-development.md) |
| Release / rollback | [docs/guides/release.md](docs/guides/release.md) |
| API · config · CI · version | [docs/reference/](docs/reference/) |
| ហេតុអ្វីរចនាបែបនេះ (ADR) | [docs/decisions/](docs/decisions/) |
| ខូច → ធ្វើអ្វី | [docs/runbooks/](docs/runbooks/) |
| ដំណើររៀន + ស្ថានភាព Phase | [docs/journey/](docs/journey/README.md) |

## Repo

```
.github/          workflows/ci.yml (unit-test → integration-test → docker) · dependabot.yml
mini-shop/        Spring Boot app · Dockerfile · compose.yaml
k8s/              kind-config.yaml · namespace · postgres/ · app/
scripts/          check-env.sh · kind-trust-ca.sh
docs/             architecture · guides · reference · decisions · runbooks · journey
CLAUDE.md         ច្បាប់គម្រោង និងការងារជាមួយ Claude Code
```

## ច្បាប់សំខាន់

- Pin version គ្រប់ទីកន្លែង — គ្មាន `latest` សម្រាប់ប្រើ/deploy ([docs/reference/tech-stack.md](docs/reference/tech-stack.md))
- គ្មាន secret ក្នុង git — `.env`, `env.sh`, `k8s/**/secret.yaml` gitignored; commit `.example`
- Flyway គ្រប់គ្រង schema (`ddl-auto: validate`) — migration ដែល apply រួច**មិនកែ**
- `catalog` ↔ `order` ឆ្លងកាត់ Service ប៉ុណ្ណោះ ([ADR-003](docs/decisions/003-product-id-not-entity-ref.md))
- `main` protected: PR + `unit-test` + `integration-test` ត្រូវបៃតង
