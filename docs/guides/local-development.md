# Local development

> **គោលបំណង:** run, test, និង debug app លើ laptop · **អ្នកអាន:** developer · **Update:** 2026-09-30

## តម្រូវការ

```bash
bash scripts/check-env.sh      # ពិនិត្យ Java 21, Docker, git, … ធៀបនឹង reference/tech-stack.md
```

រំពឹង: `N ok · 0 missing`។ Java **21** (Temurin), Docker Engine + Compose v2, Git។ Maven មិនចាំបាច់ — `./mvnw` (3.9.16)។

> Network ការិយាល័យ (TLS interception): `./mvnw` អាច download Maven មិនបាន (`curl: (35) schannel`) → ប្រើ `mvn` 3.9.16 ដែលតម្លើងរួច (version ដដែល)។ មើល [runbooks/ci-failure.md §៥](../runbooks/ci-failure.md)។

## ជម្រើស A — Docker Compose (app + DB, ងាយបំផុត)

```bash
cd mini-shop
cp .env.example .env              # កែ DB_PASSWORD បើចង់
docker compose up --build
```

រំពឹង: `db` healthy → `app` log `Started MiniShopApplication` (~15s)។ Seed 8 products (profile `dev`)។

```bash
curl localhost:8080/actuator/health      # {"status":"UP",...}
open http://localhost:8080/swagger-ui.html
docker compose down                      # រក្សា data   ·   down -v = លុប data (Flyway migrate ថ្មីពេល up)
```

## ជម្រើស B — app លើ JVM, DB ក្នុង container (debug ក្នុង IDE)

```bash
docker run -d --name minishop-db \
  -e POSTGRES_DB=minishop -e POSTGRES_USER=minishop -e POSTGRES_PASSWORD=minishop \
  -p 5432:5432 postgres:17

cd mini-shop
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minishop
export SPRING_DATASOURCE_USERNAME=minishop
export SPRING_DATASOURCE_PASSWORD=minishop
export SPRING_PROFILES_ACTIVE=dev       # optional seed
./mvnw spring-boot:run
```

ឬដាក់ `export` ក្នុង `mini-shop/env.sh` (gitignored) → `source env.sh`។ Env var ទាំង ៣ **គ្មាន default** — ភ្លេច = app fail fast (ដោយចេតនា)។ តារាងពេញ: [reference/configuration.md](../reference/configuration.md)។

## Test

```bash
cd mini-shop
./mvnw test                               # unit (Mockito) — គ្មាន DB, ~20s
./mvnw verify                             # + integration (Testcontainers postgres:17 — Docker ត្រូវ run)
./mvnw verify -DskipUnitTests             # integration ប៉ុណ្ណោះ (ដូច CI job)
./mvnw test -Dtest=OrderServiceTest#createOrder_insufficientStockOnSecondItem_decreasesNothing
```

ច្បាប់ test:
- `*Test.java` = unit (surefire) — **មិនត្រូវ**ត្រូវការ DB/network។ `@SpringBootTest` ដែលត្រូវការ DB ត្រូវដាក់ឈ្មោះ `*IT`។
- `*IT.java` = integration (failsafe) — Testcontainers, profile `it` (គ្មាន seed)។
- រាល់ business rule មាន test (CLAUDE.md §5.7)។ Reproduce CI: `env -u SPRING_DATASOURCE_URL -u SPRING_DATASOURCE_USERNAME -u SPRING_DATASOURCE_PASSWORD ./mvnw test`។

## Workflow git

```
branch develop (ឬ feature/*) ──push──▶ CI: unit-test → integration-test
      └─ PR → main ──(check ២ បៃតង)──▶ merge ──▶ CI: + docker → image ghcr.io :<sha>
```

`main` protected: push ផ្ទាល់ត្រូវបដិសេធ។ លម្អិត: [guides/release.md](release.md), [reference/ci-pipeline.md](../reference/ci-pipeline.md)។
