# DevOps-Journey

[![CI](https://github.com/vuthin-devops-ecommerce/ecom-api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/vuthin-devops-ecommerce/ecom-api/actions/workflows/ci.yml)

ដំណើររៀន DevOps ជាជំហាន — monorepo មាន **ឯកសារ** (`docs/`) និង **app** (`mini-shop/`, Spring Boot)។
App ជា modular monolith (`catalog` + `order`) ដែលរៀបចំឱ្យបំបែកជា service ដាច់នៅ Phase B។

| Phase | ខ្លឹមសារ | ស្ថានភាព |
|---|---|---|
| A | modular monolith, Flyway, Testcontainers, Docker Compose | ✅ (`docs/phase-a-plan.md`) |
| A2 | GitHub Actions CI, Trivy, ghcr.io | 🟡 Task 0–4 ✅ (`docs/phase-a2-plan.md`) |
| A3 | Kubernetes (kind), probes, rolling update, HPA | ⬜ |
| A4 | Terraform | ⬜ |

## រចនាសម្ព័ន្ធ

```
.github/
  workflows/ci.yml        CI: unit-test → integration-test → docker (build · Trivy · push ghcr.io, main ប៉ុណ្ណោះ)
  dependabot.yml          update dependency ដោយ PR (maven, github-actions, docker) — weekly
docs/
  00-tech-stack.md        source of truth សម្រាប់ version tool
  phase-*-plan.md         ផែនការ + progress tracker នីមួយៗ
  decisions/              ADR (001 modular monolith, 002 order schema FK, 003 productId, 004 CI jobs, 005 scan policy)
  learning-log.md         ចម្លើយសំណួរឆ្លុះបញ្ចាំង + រោគសញ្ញាដែលជួបពិត
mini-shop/                Spring Boot 4.1 · Java 21 · Maven wrapper · PostgreSQL 17 · Flyway
```

## តម្រូវការ

- Java **21** (Eclipse Temurin) — `java -version`
- Docker Engine + Compose v2 — `docker compose version`
- Git

Maven មិនចាំបាច់តម្លើង — ប្រើ `./mvnw` (wrapper, Maven 3.9.16)។

## Run ក្នុងមូលដ្ឋាន (app លើ laptop, DB ក្នុង container)

```bash
docker run -d --name minishop-db \
  -e POSTGRES_DB=minishop -e POSTGRES_USER=minishop -e POSTGRES_PASSWORD=minishop \
  -p 5432:5432 postgres:17

cd mini-shop
export JAVA_HOME=/path/to/jdk-21            # បើ default មិនមែន 21
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minishop
export SPRING_DATASOURCE_USERNAME=minishop
export SPRING_DATASOURCE_PASSWORD=minishop  # គ្មាន default — app fail fast បើភ្លេច
export SPRING_PROFILES_ACTIVE=dev            # optional: seed 8 products (db/dev/R__seed_dev_data.sql)
./mvnw spring-boot:run
```

ឬដាក់ `export` ទាំងនោះក្នុង `mini-shop/env.sh` (gitignored) រួច `source env.sh`។

## Run ជាមួយ Docker Compose (app + DB)

```bash
cd mini-shop
cp .env.example .env            # កែ DB_PASSWORD / APP_PORT តាមចិត្ត
docker compose up --build       # build image (multi-stage, non-root) + postgres:17
docker compose down             # បញ្ឈប់ (រក្សា data)  ·  down -v = លុប data
```

## ផ្ទៀងផ្ទាត់

```bash
curl localhost:8080/actuator/health          # {"status":"UP"}
open http://localhost:8080/swagger-ui.html   # Swagger UI
```

## API

```bash
B=localhost:8080/api; H='Content-Type: application/json'

curl -H "$H" -d '{"sku":"SKU-100","name":"Widget","price":9.99,"stock":5}' $B/products   # 201 · 409 បើ SKU ស្ទួន
curl $B/products                                                                         # 200 list
curl $B/products/1                                                                       # 200 · 404
curl -X PATCH -H "$H" -d '{"delta":-2}' $B/products/1/stock                              # 200 · 400 បើ stock មិនគ្រប់

curl -H "$H" -d '{"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}' $B/orders
#   201 · 400 stock មិនគ្រប់ (មិនកាត់ stock ណាមួយ) · 400 productId ស្ទួន · 404 product មិនមាន
curl $B/orders/1                                                                         # 200 · 404
```

Error ឆ្លើយជា [RFC 9457 ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457): `{status, title, detail, instance}`
(+ `errors` សម្រាប់ validation)។

## Tests

```bash
cd mini-shop
./mvnw test                      # unit (Mockito) — CI job unit-test
./mvnw verify                    # + integration (*IT, Testcontainers postgres:17 — ត្រូវការ Docker)
./mvnw verify -DskipITs          # unit ប៉ុណ្ណោះ
./mvnw verify -DskipUnitTests    # integration ប៉ុណ្ណោះ — CI job integration-test (property ក្នុង pom.xml)
```

## Development workflow (Phase A2)

```
branch feature/develop ──push──▶ CI: unit-test → integration-test          (~2 នាទី, docker skipped)
        │
        └─ PR → main ──merge──▶ CI: unit-test → integration-test → docker
                                  build (cache gha) → Trivy CRITICAL gate → Trivy HIGH report → push
                                  ghcr.io/vuthin-devops-ecommerce/mini-shop:<short-sha> + :latest
```

- `develop` សម្រាប់ធ្វើការ, `main` = release ប៉ុណ្ណោះ — image push **តែ**ពេល push ទៅ `main`
- CI ក្រហម → មើលឈ្មោះ job មុន: `unit-test` = logic · `integration-test` = DB/HTTP/schema · `docker` = build/CVE (runbook `docs/runbooks/ci-failure.md`, Task 6)
- Trivy block តែ CRITICAL ដែលមាន fix (ADR-005) → ដោះស្រាយដោយ upgrade dependency (ឧ. `tomcat.version` ក្នុង `pom.xml`) មិនមែនបិទ scan
- Deploy ប្រើ `:<short-sha>` ជានិច្ច — `:latest` មានតែសម្រាប់មើល build ចុងក្រោយ (CLAUDE.md §4)
- Pull image (package private): `echo <PAT read:packages> | docker login ghcr.io -u <user> --password-stdin`
- Branch protection លើ `main` (require PR + status check): ត្រូវការ repo public ឬ GitHub Pro — មើល `docs/phase-a2-plan.md` Task 5

## ច្បាប់សំខាន់ (ពី `CLAUDE.md`)

- Pin version គ្រប់ទីកន្លែង — គ្មាន `latest`
- គ្មាន secret ក្នុង git — `env.sh`, `.env` gitignored; commit `.env.example`
- Flyway គ្រប់គ្រង schema (`ddl-auto: validate`); migration ដែល apply រួច**មិនកែ** — បន្ថែម `V<n+1>`
- `catalog` ↔ `order` ឆ្លងកាត់ Service ប៉ុណ្ណោះ (ADR-003)
