# Learning Log

កំណត់ត្រាចម្លើយ**សំណួរឆ្លុះបញ្ចាំង**ពីផែនការនីមួយៗ — សរសេរដោយអ្នករៀនផ្ទាល់។
Claude Code អាច review ចម្លើយ និងសួរបន្ត តែ**មិនឆ្លើយជំនួស**។

---

## Stage 0 / Task 0 — Project skeleton (2026-09-27)

### សំណួរ ១ (ពី `00-tech-stack.md` §៧)

ហេតុអ្វីយើង pin `postgres:17` ជំនួស `postgres:latest`? វានឹងជះឥទ្ធិពលអ្វីនៅ Stage 1 (compose) និង Stage 2 (CI)?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ២ (ពី `phase-a-plan.md` Task 0)

`ddl-auto: validate` ខុសពី `update` យ៉ាងណា? ហេតុអ្វី `update` គ្រោះថ្នាក់នៅ production?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 0

រោគសញ្ញាដែលកើតឡើងពិត (សម្រាប់ជួយចាំ — សរសេរអ្វីដែលអ្នករៀនបានពីវា):

- Initializr ផ្តល់ id `4.1.1.RELEASE` តែ Maven Central មានតែ `4.1.1` → parent POM resolve មិនបាន
- `JAVA_HOME` ចង្អុលទៅ JDK 17 ខណៈ `pom.xml` ទាមទារ 21 → `release version 21 not supported`
- Port 5432 និង 8080 ត្រូវ container របស់គម្រោងផ្សេង (`pharmacy-*`) កាន់ស្រាប់ → `Bind ... failed: port is already allocated`

**អ្វីដែលរៀនបាន:**

_(សរសេរនៅទីនេះ)_

---

## Task 1 — Catalog domain (2026-09-27)

### សំណួរ ១ (ពី `phase-a-plan.md` Task 1)

ហេតុអ្វី `double` មិនសាកសមសម្រាប់លុយ? សាកគណនា `0.1 + 0.2` ក្នុង Java (`jshell` → `0.1 + 0.2`) ហើយសរសេរលទ្ធផលពិត។

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ២ (ពី code review)

`products.created_at` មាន `DEFAULT NOW()` ក្នុង DB តែ `Product.java` set តម្លៃក្នុង Java (`@PrePersist`) ជំនួស។
ហេតុអ្វី DB default មិនត្រូវបានប្រើ ពេល Hibernate insert? ជម្រើសផ្សេង (`insertable = false`) មានគុណវិបត្តិអ្វី?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ៣ (ពី smoke test)

POST ឆ្លើយ `createdAt: ...18.1989469` (7 ខ្ទង់) តែ GET ក្រោយមកឆ្លើយ `...18.198947` (6 ខ្ទង់)។ តម្លៃណាជាការពិតក្នុង DB ហើយហេតុអ្វី?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

---

## Task 2 — Order schema (2026-09-27)

អាន [ADR-002](decisions/002-order-schema-fk.md) មុន រួចឆ្លើយដោយពាក្យផ្ទាល់ខ្លួន (កុំចម្លង ADR):

### សំណួរ ១ (ពី `phase-a-plan.md` Task 2)

ហេតុអ្វី `price_at_order` ត្រូវ**ចម្លង**តម្លៃ មិន reference ទៅ `products.price`? ឧទាហរណ៍ជាក់ស្តែងមួយដែលការ reference នឹងធ្វើឱ្យខូច។

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ២

FK ២ ក្នុង `order_items` បានទទួល `ON DELETE` ខុសគ្នា (`CASCADE` vs `RESTRICT`)។ បើប្តូរផ្ទុយគ្នា (order → RESTRICT, product → CASCADE) កើតអ្វីឡើងពេល admin លុប product? ហេតុអ្វីវាអាក្រក់?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ៣ (ពី probe)

Probe 2 បង្ហាញ `DELETE FROM products WHERE id = 1` ត្រូវបានបដិសេធ។ ដូច្នេះ catalog នឹង "លុប" product យ៉ាងណានៅពេលអនាគត? (ពាក្យគន្លឹះ: soft delete) — គុណវិបត្តិនៃវិធីនោះ?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

---

## Task 3 — Order domain (2026-09-27)

អាន [ADR-003](decisions/003-product-id-not-entity-ref.md) និង `OrderService.createOrder()` មុន។

### សំណួរ ១ (ពី `phase-a-plan.md` Task 3)

`@Transactional` លើ `createOrder()` — ហេតុអ្វី? `createOrder` ពិនិត្យ stock ទាំងអស់**មុន**កាត់ ដូច្នេះ `InsufficientStockException` មិនដែលកើតក្រោយកាត់ stock ទេ។ បើអញ្ចឹង `@Transactional` នៅមានប្រយោជន៍អ្វី? (គិត: អ្វីអាច fail នៅជំហាន 3–4 ក្រោយកាត់ stock រួច?)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ២

Order ដំបូងបាន `id = 4` មិនមែន `1` ទោះ `orders` ទទេ — ព្រោះ probe Task 2 (INSERT + ROLLBACK) ។ ហេតុអ្វី ROLLBACK មិនប្រគល់លេខ sequence វិញ? វាជាបញ្ហាទេសម្រាប់ app? (ពាក្យគន្លឹះ: sequence non-transactional, gap)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ៣ (race condition — ចម្លើយបឋម, ដោះស្រាយ Phase B)

អ្នកប្រើ ២ នាក់ POST order សម្រាប់ product ដែលសល់ stock 1 ក្នុងពេលដំណាលគ្នា។ ទាំងពីរឆ្លងជំហាន 2 (check) មុនអ្នកណាម្នាក់ដល់ជំហាន 3 (decrement)។ កើតអ្វី? DB CHECK `stock >= 0` ជួយបានទេ? (ពាក្យគន្លឹះ: optimistic vs pessimistic locking, `SELECT … FOR UPDATE`)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

---

## Task 4 — Tests (2026-09-27)

### សំណួរ ១ (ពី `phase-a-plan.md` Task 4)

Testcontainers ខុសពី H2 in-memory យ៉ាងណា? ហេតុអ្វីសំខាន់សម្រាប់ DevOps? (គិត: `V2` ប្រើ `NUMERIC`, `CHECK`, `ON DELETE RESTRICT`, `ON CONFLICT` — H2 គាំទ្រទាំងអស់ដូច PostgreSQL ទេ? test ឆ្លងលើ H2 = ធានាអ្វីលើ production?)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ២ (ពី `OrderControllerIT`)

Test ទី ២ (stock មិនគ្រប់ → 400 → stock មិនប្រែ) និង test ទី ៣ (`save()` throw → 500 → stock ត្រឡប់វិញ) ទាំងពីរបញ្ជាក់ "stock មិនប្រែ" — តែពួកវាបញ្ជាក់**យន្តការខុសគ្នា**។ មួយណាបញ្ជាក់ check-all-first? មួយណាបញ្ជាក់ transaction rollback? បើដក `@Transactional` ចេញពី `createOrder` test មួយណានឹង fail?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ ៣

IT ប្រើ `postgres:17` (pin) តែ dev DB (Neon) ជា 18.6។ បើ Neon មាន behaviour ខុសពី 17 ក្នុងចំណុចណាមួយ test នឹងចាប់បានទេ? នេះជាហេតុផលដែល `00-tech-stack.md` ចង់ឱ្យ version ដូចគ្នាទាំង dev/test/CI — សរសេរផលវិបាកជាក់ស្តែងនៃ deviation នេះ។

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

---

## Task 5 — Containerize (2026-09-27)

លំហាត់ ៥ ពី `phase-a-plan.md` — កត់**លេខពិត**ដែលអ្នកវាស់ (Claude បានវាស់ខ្លះ — ផ្ទៀងផ្ទាត់ខ្លួនឯង):

### លំហាត់ ១ — ទំហំ image

`docker images mini-shop:local` → **403 MB** (Claude វាស់)។ ប្រៀបធៀប `eclipse-temurin:21-jre-alpine` (base) → ______ MB (`docker images` មើលខ្លួនឯង)។ jar ខ្លួនឯង (`target/*.jar`) → **59 MB**។ តើទំហំមកពីណាច្រើនជាងគេ? (403 − 59 = ?)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### លំហាត់ ២ — single-stage vs multi-stage

សរសេរ `Dockerfile.single` (stage តែមួយ: `eclipse-temurin:21-jdk` + build + run ក្នុង image ដដែល) → build → `docker images`។ ខុសគ្នាប៉ុន្មាន MB? អ្វីខ្លះដែលនៅក្នុង image single-stage តែ**មិនត្រូវការ** ពេល run?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### លំហាត់ ៣ — data នៅឬបាត់

`docker compose down` → `up` → order នៅឬបាត់? រួច `docker compose down -v` → `up` → ? ហេតុអ្វី `-v` ខុសគ្នា? តើ Flyway ធ្វើអ្វីពេល volume ថ្មី?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### លំហាត់ ៤ — rebuild ក្រោយកែ code ១ បន្ទាត់

កែ string ១ ក្នុង `ProductService` → `docker compose build` → ______ វិនាទី (build ដំបូង ______ វិនាទី)។ layer ណាខ្លះ reuse, layer ណាខ្លះ rebuild? ហេតុអ្វី `COPY pom.xml` មុន `COPY src/` សំខាន់? បើកែ `pom.xml` (បន្ថែម dependency) កើតអ្វី?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### លំហាត់ ៥ — `db` → `localhost`

ប្តូរ `SPRING_DATASOURCE_URL` ក្នុង `compose.yaml` ពី `db:5432` ទៅ `localhost:5432` → `docker compose up` → ចម្លង error ពិតមកទីនេះ → ពន្យល់ថាហេតុអ្វី `localhost` ក្នុង container មិនមែន laptop អ្នក → កែត្រឡប់។

**Error ដែលឃើញ:**

_(ចម្លងនៅទីនេះ)_

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

---

## Phase A2 / Task 0 — Repo → GitHub (2026-09-28)

Repo មានលើ GitHub រួចហើយ (`vuthin-devops-ecommerce/ecom-api`, private, branch `main`)។ ថ្ងៃនេះបន្ថែម branch `develop` និង folder `.github/workflows/`។

### សំណួរ ១ (ពី `phase-a2-plan.md` Task 0)

ហេតុអ្វីមិន push ផ្ទាល់ទៅ `main`? ការងារជាក្រុមធំមានបញ្ហាអ្វីបើអ្នកគ្រប់គ្នាធ្វើដូចនោះ?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 0

- `git ls-files -s mini-shop/mvnw` បង្ហាញ mode `100644` (មិន executable) — Windows មិនរក្សា exec bit, ដូច្នេះ Linux runner នឹងបាន `Permission denied` ពេល `./mvnw`។ Dockerfile ដោះស្រាយដោយ `chmod +x` (រោគសញ្ញា) — ថ្ងៃនេះកែមូលហេតុដោយ `git update-index --chmod=+x mini-shop/mvnw` → `100755`។
- Maven wrapper (`./mvnw` ក្នុង Git Bash) download មិនបានលើ laptop: `curl: (35) schannel ... CRYPT_E_NO_REVOCATION_CHECK` — បញ្ហា TLS revocation check លើម៉ាស៊ីននេះ មិនមែនបញ្ហា project។ ផ្ទៀងផ្ទាត់ក្នុងមូលដ្ឋានដោយ `mvn` (3.9.16 ដដែលនឹង wrapper) ជំនួស។ CI runner មិនមានបញ្ហានេះ។

---

## Phase A2 / Task 1 — Build + unit test (2026-09-28)

File: `.github/workflows/ci.yml`

### ចំណុចត្រូវយល់ក្នុង YAML (ពី `phase-a2-plan.md` Task 1)

| បន្ទាត់ | សំណួរ | ចម្លើយ |
|---|---|---|
| `concurrency` + `cancel-in-progress` | បើអ្នក push ៣ ដងក្នុង ១ នាទី កើតអ្វី? ហេតុអ្វីសន្សំលុយ? | _(សរសេរនៅទីនេះ)_ |
| `permissions: contents: read` | បើមិនកំណត់ default ជាអ្វី? ហេតុអ្វីគ្រោះថ្នាក់? | _(សរសេរនៅទីនេះ)_ |
| `cache: maven` | cache key គឺអ្វី? ពេលណា cache miss? | _(សរសេរនៅទីនេះ)_ |
| `if: always()` | បើអត់ ហេតុអ្វី test report មិនដែលឃើញពេលបរាជ័យ? | _(សរសេរនៅទីនេះ)_ |
| `-B` | batch mode — ហេតុអ្វីសំខាន់ក្នុង CI? | _(សរសេរនៅទីនេះ)_ |

### សំណួរ review បន្ថែម (ពីការសម្រេចចិត្តរចនាក្នុង `ci.yml`)

1. Action pin ជា commit SHA (`actions/checkout@3d3c42e...` + comment `# v7.0.1`) ជំនួស `@v4` — អ្នកណាអាចផ្លាស់ទី tag `v4`? SHA ខុសគ្នាយ៉ាងណា? តម្លៃដែលត្រូវបង់គឺអ្វី (hint: Dependabot Task 5)?
2. `runs-on: ubuntu-24.04` ជំនួស `ubuntu-latest` — ទាក់ទងច្បាប់ណាក្នុង `CLAUDE.md` §4? បើ GitHub ប្តូរ `ubuntu-latest` ទៅ 26.04 កើតអ្វីចំពោះ build?
3. `defaults.run.working-directory: mini-shop` អនុវត្តលើ `run:` ប៉ុណ្ណោះ — ហេតុអ្វី `path:` ក្នុង `upload-artifact` និង `cache-dependency-path` ត្រូវសរសេរ `mini-shop/...` ពេញ?
4. Workflow run លើ push គ្រប់ branch **រួមទាំង docs-only commit** — គួរបន្ថែម `paths:` filter ឬអត់? (hint: Task 5 "Require status checks" — បើ workflow មិន run, check មិនដែល report → PR merge មិនបាន)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### លំហាត់

1. Run ទី១ (push `develop`): ______ (បៃតង/ក្រហម) — ពេលវេលា ______
2. ធ្វើឱ្យខូចដោយចេតនា: កែ test មួយឱ្យបរាជ័យ → push → run ក្រហម → download `surefire-reports` artifact → ឃើញអ្វីក្នុង file `.txt`?
   - _(សរសេរនៅទីនេះ)_
3. Run ទី២ (cache hit): ពេលវេលា ______ — ខុសពីទី១ ______ វិនាទី។ step ណាលឿនជាង? ហេតុអ្វី?
   - _(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 1

- **ច្បាប់ "Reproducible" ត្រូវសាកល្បងមុន push:** run `mvn -B verify -DskipITs` លើ laptop ដោយ**ដក** env var `SPRING_DATASOURCE_*` ចេញ (ដូច CI runner) → `MiniShopApplicationTests.contextLoads` **ធ្លាក់** (`Failed to load ApplicationContext` — datasource url ទទេ)។ Test នេះឆ្លងលើ laptop កាលពី Phase A តែព្រោះ env var ចង្អុលទៅ DB ដែលកំពុង run — មិនមែនព្រោះ test ត្រឹមត្រូវ។
- **ការសម្រេចចិត្ត:** លុប `MiniShopApplicationTests` ចោល។ ហេតុផល: (ក) វាជា `@SpringBootTest` ដែលត្រូវការ DB ពិត តែឈ្មោះ `*Tests` ធ្វើឱ្យ surefire run វាជា unit test; (ខ) `OrderControllerIT` boot context ពេញជាមួយ `postgres:17` រួចហើយ → "context loads" ត្រូវបានគ្របដណ្តប់ក្នុង Task 2។ ជម្រើសផ្សេង: ប្តូរឈ្មោះជា `*IT` + Testcontainers (container ទី២ → IT យឺតជាង ដោយគ្មានតម្លៃបន្ថែម)។
  - សំណួរ: ប្រសិនបើថ្ងៃក្រោយមាន bean ដែល `OrderControllerIT` មិនប៉ះ (ឧ. scheduler) ហើយ config ខុស — test ណានឹងចាប់បាន? ត្រូវការ `contextLoads` ត្រឡប់វិញឬអត់?

### លំហាត់ ២ — លទ្ធផលពិត (2026-09-28)

កែ `"39.48"` → `"99.99"` ក្នុង `OrderServiceTest` → push `develop` → run ក្រហមក្នុង ២៨ វិនាទី → step "Upload test report" នៅតែ run (`if: always()`) → artifact `surefire-reports` 9.7 KB។ កែត្រឡប់ → push → បៃតង។

**អ្វីដែលឃើញក្នុង file `.txt` របស់ artifact ខុសពី log យ៉ាងណា?**

_(សរសេរនៅទីនេះ)_

---

## Phase A2 / Task 2 — Integration test ក្នុង CI (2026-09-28)

File: `.github/workflows/ci.yml` (job `unit-test` + `integration-test`), `mini-shop/pom.xml` (property `skipUnitTests`), ADR: `docs/decisions/004-ci-job-structure.md`

### សំណួរឆ្លុះបញ្ចាំង (ពី `phase-a2-plan.md` Task 2)

**១. ជម្រើស A (job តែមួយ) vs B (២ job) — មួយណាល្អជាងសម្រាប់គម្រោងនេះ?**
ADR-004 ស្នើ B ជាមួយហេតុផល — អ្នកយល់ស្របឬអត់? ឆ្លើយសំណួរ ៣ ចុង ADR-004 នៅទីនេះ រួចប្តូរ status ADR ជា Accepted (ឬកែការសម្រេចចិត្ត)។

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

**២. Testcontainers ក្នុង CI ខុសពី `services: postgres:` របស់ GitHub Actions យ៉ាងណា? ហេតុអ្វីយើងជ្រើស Testcontainers?**
hint: អ្នកណាកំណត់ version DB — YAML ឬ test code? test run លើ laptop ដោយគ្មាន DB ដោយដៃបានឬអត់?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ review ក្នុង `ci.yml` (Task 2)

| បន្ទាត់ | សំណួរ | ចម្លើយ |
|---|---|---|
| `./mvnw -B test` (job unit) | Maven lifecycle `validate → compile → test → package → integration-test → verify`: `test` ឈប់ត្រង់ណា? ហេតុអ្វី failsafe មិន run? | _(សរសេរ)_ |
| `needs: unit-test` | parallel vs sequential — ខុសគ្នាពេលវេលាប៉ុន្មានពេលទាំងអស់ឆ្លង? ពេល unit ខូច? | _(សរសេរ)_ |
| `-DskipUnitTests` | ហេតុអ្វីត្រូវកំណត់ក្នុង `pom.xml` ខ្លួនឯង? `skipTests` ធ្វើអ្វី? | _(សរសេរ)_ |
| `cache: maven` ក្នុង job ២ | job ២ cache hit ឬ miss? ហេតុអ្វី? | _(សរសេរ)_ |
| `failsafe-reports` | ហេតុអ្វី report នៅ folder ផ្សេងពី surefire? | _(សរសេរ)_ |

### លំហាត់ — វាស់ពេលវេលា

- Run Task 1 (job តែមួយ, unit ប៉ុណ្ណោះ): 33s (cache hit)
- Run Task 2 (unit-test + integration-test): សរុប ______ · job unit-test ______ · job integration-test ______ · step IT ______
- Testcontainers pull `postgres:17` ក្នុង runner: ______ វិនាទី (មើលក្នុង log "Pulling docker image")
- យឺតជាង Task 1 ប៉ុន្មាន? ផ្នែកណាចំណាយច្រើនបំផុត?

### អ្វីដែលជួបពិតពេលធ្វើ Task 2

- **`-Dsurefire.skip=true` មិនដំណើរការ** — សាកលើ laptop: surefire នៅតែ run 6 unit test រួច failsafe run 3 IT (`BUILD SUCCESS`, គ្មាន error, គ្មាន warning!)។ Maven **មិនស្គាល់** property នោះ ហើយក៏មិនប្រាប់អ្នកដែរ — property ខុសឈ្មោះ = ស្ងាត់ៗមិនធ្វើអ្វី។ មេរៀន: ពេលបន្ថែម flag ត្រូវ**មើល log ថាវាមានឥទ្ធិពលពិត** មិនមែនមើលតែ exit code។
- ដំណោះស្រាយ: property `skipUnitTests` ក្នុង `pom.xml` (default `false`) wire ចូល surefire `<skipTests>${skipUnitTests}</skipTests>`។ ផ្ទៀងផ្ទាត់: `mvn -B verify -DskipUnitTests` → log ត្រូវបង្ហាញ "Tests are skipped" ពី surefire និង "Tests run: 3" ពី failsafe។
- IT លើ laptop: `postgres:17` start ក្នុង 0.8s (image មានស្រាប់), `OrderControllerIT` 27s សរុប (Spring boot + Flyway)។ ក្នុង CI runner image ត្រូវ pull ថ្មីរាល់ run — ប្រៀបធៀបពេលវេលា។
