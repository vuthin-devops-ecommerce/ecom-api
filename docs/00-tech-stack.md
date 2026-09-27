# 00 — Technical Stack (ដំណាក់កាល A · ដំណាក់ទី០ Baseline)

> **សម្រាប់ Claude Code:** ឯកសារនេះជា source of truth សម្រាប់ version របស់ tool ទាំងអស់។
> កុំ upgrade major version ណាមួយ ដោយគ្មាន ADR ថ្មីក្នុង `docs/decisions/`។
> អ្នកប្រើកំពុង**រៀន** DevOps — ណែនាំជាជំហាន ជាភាសាខ្មែរ ទុកឱ្យអ្នកប្រើសរសេរ code ខ្លួនឯង។

| | |
|---|---|
| **ថ្ងៃបង្កើត** | 2026-09-22 |
| **ស្ថានភាព** | អនុម័ត — ដំណាក់ទី០ |
| **ឯកសារពាក់ព័ន្ធ** | `docs/phase-a-plan.md`, `docs/decisions/001-modular-monolith.md`, `CLAUDE.md` |
| **ជំនួស** | បន្ទាត់ "Stack: Spring Boot 3.x · PostgreSQL 16" ក្នុង `phase-a-plan.md` (ចាស់) |

---

## ១. គោលការណ៍ដំណាក់ទី០

- App ដំណើរការ **manual** លើម៉ាស៉ីនអ្នកអភិវឌ្ឍ (`./mvnw spring-boot:run`)។
- មានតែ **PostgreSQL** ទេដែលដំណើរការក្នុង container — app ខ្លួនឯងមិនទាន់ containerize (Stage 1)។
- **One tool per stage:** ដំណាក់បន្ទាប់នីមួយៗបន្ថែម tool ថ្មី**តែមួយ** ដើម្បីឱ្យឥទ្ធិពលរបស់វាដាច់ដោយឡែក និងងាយយល់។
- Baseline ត្រូវ "ស្អាត" — វាជាចំណុចប្រៀបធៀបសម្រាប់ដំណាក់ក្រោយៗទាំងអស់។

---

## ២. Stack

| ស្រទាប់ | Tool / Version | តួនាទីក្នុងដំណើររៀន DevOps |
|---|---|---|
| Language / Runtime | **Java 21** LTS (Eclipse Temurin) | version ដូចគ្នាពី laptop → CI → container image |
| Build | **Maven** + wrapper `./mvnw` | wrapper ធានា Maven version ដូចគ្នាលើគ្រប់ម៉ាស៉ីន និងលើ CI |
| Framework | **Spring Boot 4.1.x** (បច្ចុប្បន្ន `4.1.1` — GA default នៅ start.spring.io — មិនយក SNAPSHOT/RC/M) | auto-configuration, embedded Tomcat, starters |
| Database | **PostgreSQL 17** — image `postgres:17` | pin version ជាក់លាក់ → ប្រើដដែលក្នុង compose (Stage 1) និង Testcontainers (លំហាត់ ៤) |
| Schema migration | **Flyway** + `spring.jpa.hibernate.ddl-auto: validate` | schema ជា code, versioned, repeatable — មូលដ្ឋាន Database Automation |
| Health / Metrics | **Spring Boot Actuator** | `/actuator/health` → ក្រោយក្លាយជា liveness/readiness probe (Stage 4) |
| Version control | **Git + GitHub** | CI សប្តាហ៍ទី៣ ត្រូវការ repo នៅលើ GitHub |
| Local tooling | Docker Engine (សម្រាប់ DB ប៉ុណ្ណោះ), IDE, `curl` ឬ httpie, **springdoc-openapi `3.1.1`** (Swagger UI `/swagger-ui.html` — បន្ថែម 2026-09-27 ក្រោយ Task 1, dev convenience មិនមែន stage tool) | |
| Documentation | `docs/decisions/` (ADR), `docs/learning-log.md`, `CLAUDE.md` | កត់ត្រាការសម្រេចចិត្ត + handoff Chat → Claude Code |

### Dependencies (ជ្រើសនៅ start.spring.io)

Spring Web · Spring Data JPA · Validation · Spring Boot Actuator · Flyway Migration · PostgreSQL Driver · Lombok

> Spring Boot 4.x មានឈ្មោះ starter ខ្លះខុសពី tutorial 3.x — យក `pom.xml` តាមដែល initializer បង្កើតឱ្យ កុំកែ artifactId ដោយដៈ។

---

## ៣. Version pinning — pin នៅទីណា

| Tool | ទីតាំង pin | ហេតុផល |
|---|---|---|
| Java 21 | `pom.xml` → `<java.version>21</java.version>` | compiler target ជាក់លាក់ |
| Maven | `.mvn/wrapper/maven-wrapper.properties` | `./mvnw` download version ដដែលគ្រប់ទីកន្លែង |
| Spring Boot 4.1.x | `pom.xml` → `<parent>` version (`4.1.1` — គ្មាន `.RELEASE` suffix; Initializr id មាន តែ Maven Central គ្មាន) | BOM គ្រប់គ្រង version របស់ Flyway, Hibernate, PostgreSQL driver ដោយស្វ័យប្រវត្តិ |
| PostgreSQL 17 | `docker run ... postgres:17` (ឥឡូវ) → `compose.yaml` (Stage 1) → Testcontainers (លំហាត់ ៤) | version ដូចគ្នាទាំង dev / test / CI |

**ច្បាប់:** មិនប្រើ tag `latest` ណាមួយក្នុងគម្រោងនេះ។

---

## ៤. Run ក្នុងមូលដ្ឋាន (Stage 0)

```bash
# 1. Database
docker run -d --name minishop-db \
  -e POSTGRES_DB=minishop -e POSTGRES_USER=minishop -e POSTGRES_PASSWORD=minishop \
  -p 5432:5432 postgres:17

# 2. Environment (application.yml អាន env var ទាំង ៣ — គ្មាន default ដោយចេតនា → fail fast បើភ្លេច)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minishop
export SPRING_DATASOURCE_USERNAME=minishop
export SPRING_DATASOURCE_PASSWORD=minishop
# ឬ: source mini-shop/env.sh (gitignored — dev DB ផ្ទាល់ខ្លួន)
# Seed data (dev ប៉ុណ្ណោះ): SPRING_PROFILES_ACTIVE=dev → Flyway បន្ថែម location db/dev (R__seed_dev_data.sql, idempotent)
export SPRING_PROFILES_ACTIVE=dev

# 3. App
./mvnw spring-boot:run
```

---

## ៥. Verification checklist

```bash
java -version                        # → 21
./mvnw -v                            # → wrapper ដំណើរការ, Java 21
docker ps                            # → minishop-db (postgres:17) Up
./mvnw spring-boot:run               # → log: Flyway "Successfully applied 1 migration"
curl localhost:8080/actuator/health  # → {"status":"UP"}
```

- [ ] តារាង `flyway_schema_history` ក្នុង DB មាន row `V1`
- [ ] `./mvnw test` ឆ្លង (context loads)
- [ ] `git log` មាន commit ដំបូង, remote ជា GitHub

---

## ៦. អ្វីដែល *មិន* ស្ថិតក្នុងដំណាក់ទី០ (ដោយចេតនា)

| ដំណាក់ / លំហាត់ | Tool ដែលនឹងបន្ថែម |
|---|---|
| លំហាត់ ៤ | Testcontainers (integration test ជាមួយ `postgres:17`) |
| Stage 1 | Dockerfile + `compose.yaml` សម្រាប់ app |
| Stage 2 | GitHub Actions (CI + tests) |
| Stage 3 | Flyway ស៉ីជម្រៅ (repeatable migrations, rollback strategy) |
| Stage 4 | Kubernetes (kind), probes ពី Actuator |
| Stage 5 | Terraform (basic) |

កុំបន្ថែម tool ខាងលើមុនដំណាក់របស់វា — បើអ្វីខូច អ្នកត្រូវដឹងភ្លាមថា tool មួយណាបង្ក។

---

## ៧. សំណួរឆ្លុះបញ្ចាំង (ឆ្លើយក្នុង `docs/learning-log.md`)

ហេតុអ្វីយើង pin `postgres:17` ជំនួស `postgres:latest`? វានឹងជះឥទ្ធិពលអ្វីនៅ Stage 1 (compose) និង Stage 2 (CI)?

---

## Changelog

| ថ្ងៃ | ការផ្លាស់ប្តូរ |
|---|---|
| 2026-09-22 | បង្កើតឯកសារ; កែ stack ពី Spring Boot 3.x / PostgreSQL 16 → Spring Boot 4.0.x / PostgreSQL 17 ឱ្យត្រូវនឹងគម្រោងពិត |
| 2026-09-27 | Spring Boot 4.0.x → **4.1.x** (`4.1.1`) — GA default នៅ start.spring.io បានផ្លាស់ប្តូរ (minor version, គ្មាន ADR)។ ចំណាំ: Initializr metadata ផ្តល់ id `4.1.1.RELEASE` តែ Maven Central មានតែ `4.1.1` — ត្រូវកែ `<parent>` version ដោយដៃក្រោយ generate |