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
- Run Task 2 (unit-test + integration-test): សរុប 1m39s · job unit-test 42s (step 29s, cache miss ព្រោះ pom.xml ប្តូរ → key ថ្មី) · job integration-test 51s (step 40s, cache hit ពី key ដែល unit-test save)
- Testcontainers pull `postgres:17` ក្នុង runner: 8 វិនាទី (05:11:31 → 05:11:39), ryuk 1.5s; container start 1.0s; OrderControllerIT 22s
- យឺតជាង Task 1 ប៉ុន្មាន? ផ្នែកណាចំណាយច្រើនបំផុត?

### អ្វីដែលជួបពិតពេលធ្វើ Task 2

- **`-Dsurefire.skip=true` មិនដំណើរការ** — សាកលើ laptop: surefire នៅតែ run 6 unit test រួច failsafe run 3 IT (`BUILD SUCCESS`, គ្មាន error, គ្មាន warning!)។ Maven **មិនស្គាល់** property នោះ ហើយក៏មិនប្រាប់អ្នកដែរ — property ខុសឈ្មោះ = ស្ងាត់ៗមិនធ្វើអ្វី។ មេរៀន: ពេលបន្ថែម flag ត្រូវ**មើល log ថាវាមានឥទ្ធិពលពិត** មិនមែនមើលតែ exit code។
- ដំណោះស្រាយ: property `skipUnitTests` ក្នុង `pom.xml` (default `false`) wire ចូល surefire `<skipTests>${skipUnitTests}</skipTests>`។ ផ្ទៀងផ្ទាត់: `mvn -B verify -DskipUnitTests` → log ត្រូវបង្ហាញ "Tests are skipped" ពី surefire និង "Tests run: 3" ពី failsafe។
- IT លើ laptop: `postgres:17` start ក្នុង 0.8s (image មានស្រាប់), `OrderControllerIT` 27s សរុប (Spring boot + Flyway)។ ក្នុង CI runner image ត្រូវ pull ថ្មីរាល់ run — ប្រៀបធៀបពេលវេលា។

---

## Phase A2 / Task 3 — Build & push image ទៅ ghcr.io (2026-09-28)

File: `.github/workflows/ci.yml` job `docker` (run តែពេល push ទៅ `main`)។ Image: `ghcr.io/vuthin-devops-ecommerce/mini-shop:<short-sha>` និង `:latest`។

### ចំណុចត្រូវយល់ (ពី `phase-a2-plan.md` Task 3)

| ចំណុច | សំណួរ | ចម្លើយ |
|---|---|---|
| `GITHUB_TOKEN` | ហេតុអ្វីល្អជាង Personal Access Token? ពេលណា PAT នៅតែត្រូវការ? | _(សរសេរ)_ |
| tag `sha` + `latest` | ហេតុអ្វី**មិនត្រូវ** deploy `:latest` ទៅ production? (immutability) | _(សរសេរ)_ |
| `cache-from/to: type=gha` | build មុន cache ______ ក្រោយ ______ — layer ណា hit? | _(សរសេរ)_ |
| `if: github.ref == 'refs/heads/main'` | ហេតុអ្វី PR មិន push image? | _(សរសេរ)_ |

### សំណួរ review បន្ថែម (ពី comment ក្នុង job `docker`)

1. `permissions: packages: write` នៅកម្រិត job មិនមែន workflow — ខុសគ្នាអ្វីខាង security?
2. `setup-buildx-action` — ហេតុអ្វីត្រូវការសម្រាប់ `cache type=gha`? បើគ្មានកើតអ្វី?
3. `cache: maven` (setup-java) និង Docker layer cache ជា cache ២ ផ្សេងគ្នា — ហេតុអ្វី Docker build (multi-stage) មិនប្រើ `~/.m2` របស់ runner?
4. **ជម្លោះ `:latest`:** CLAUDE.md §4 ថា "គ្មាន tag latest" តែផែនការ Task 3 DoD ឱ្យ push `:latest`។ ខ្ញុំ (Claude) សម្រេចរក្សា `:latest` ជា tag "ផលិត" តែ**មិនដែលប្រើ** (compose/K8s ប្រើ `:<sha>`)។ អ្នកយល់ស្របឬចង់ដកចេញ? សរសេរហេតុផល — នេះជាការសម្រេចចិត្តរបស់អ្នក។
5. Image name `mini-shop` (តាម phase-a3-plan) ខណៈ repo ឈ្មោះ `ecom-api` — ghcr ភ្ជាប់ package ទៅ repo ដោយរបៀបណា? (hint: label `org.opencontainers.image.source`)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### របៀបសាកល្បង Task 3 (job run តែលើ `main`)

1. បើក PR `develop → main` លើ GitHub → CI run លើ PR (unit + IT, **គ្មាន** docker — មើលថា job docker បង្ហាញ "skipped")
2. Merge PR → run លើ `main` → job `docker` run → Summary page បង្ហាញ tag
3. ផ្ទៀងផ្ទាត់លើ laptop (package private → ត្រូវ login):
   ```bash
   echo $GH_PAT | docker login ghcr.io -u thiravuthin --password-stdin   # ឬ gh auth token | docker login ghcr.io -u thiravuthin --password-stdin
   docker pull ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>
   docker run --rm -e SPRING_DATASOURCE_URL=x -e SPRING_DATASOURCE_USERNAME=x -e SPRING_DATASOURCE_PASSWORD=x ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>
   ```
   (crash ព្រោះគ្មាន DB — តែ pull និង start បាន = image ត្រឹមត្រូវ)
4. លំហាត់: កែ `compose.yaml` ឱ្យ `app` ប្រើ `image: ghcr.io/.../mini-shop:<sha>` ជំនួស `build: .` → `docker compose up` លើម៉ាស៊ីនគ្មាន Java/Maven

### លំហាត់ — វាស់

- Docker build run ១ (cache miss, run 36391399001): step build-push 1m32s — pull base image 3–6s · `dependency:go-offline` 24s · `package` 12s · push manifest 8s · **export cache (mode=max) 45s** · run ២ (កែតែ src/): ______ · layer ណា hit / miss? ______
- Image push រួច: `ghcr.io/vuthin-devops-ecommerce/mini-shop:1279d03` និង `:latest`, digest `sha256:60c29aae…` (tag ២ ចង្អុលទៅ digest តែមួយ)
- Image size (`docker images`): ______ MB — ធៀបនឹង `mini-shop:local` ពី Phase A?

### អ្វីដែលជួបពិតពេលធ្វើ Task 3

- **Run ដំបូងលើ `main` ធ្លាក់ក្នុង 1s** (run 36386015912): `ERROR: invalid tag "ghcr.io/.../mini-shop:-tag-short-git-sha-7-.-dd44792-immutable-sha-image-56abbbb": invalid reference format`។ មូលហេតុ: comment `# ...` ដែលដាក់នៅចុងបន្ទាត់**ក្នុង** YAML block scalar (`tags: |`) មិនមែន comment ទេ — វាជាអត្ថបទ → metadata-action យកវាចូល tag។ លំដាប់រកឃើញ: job → step ណាក្រហម (build-push, 1s = មិនទាន់ build) → log បន្ទាត់ `docker buildx build ... --tag` ឃើញ tag ចម្លែក → ថយក្រោយទៅ YAML។ actionlint មិនចាប់។ កែ: ដក comment ចេញពី block ដាក់ខាងលើ។
- PR run (36385865494): job `docker` = skipped ✅ (`if:` ដំណើរការ)។ Commit ដដែលបាន run ២ ដង (event `push` លើ develop + `pull_request`) — concurrency group ខុសគ្នា (`refs/heads/develop` vs `refs/pull/2/merge`) ដូច្នេះមិន cancel គ្នា → ចម្លើយសំណួរ Task 1 "run ២ ដងឬ?" = បាទ។

_(សរសេរនៅទីនេះ)_

---

## Phase A2 / Task 4 — Security scan ជាមួយ Trivy (2026-09-29)

File: `.github/workflows/ci.yml` job `docker` (build load → Trivy CRITICAL block → Trivy HIGH report → push), ADR: `docs/decisions/005-security-scan-policy.md`

### ជម្រើសរចនា (ពី `phase-a2-plan.md` Task 4 — ADR-005 ស្នើរួច, អ្នកសម្រេច)

| សំណួរ | ADR-005 ស្នើ | អ្នកយល់ស្រប? ហេតុអ្វី? |
|---|---|---|
| Scan មុន push ឬក្រោយ push? | មុន (`load: true` → scan → `push: true`) | _(សរសេរ)_ |
| `CRITICAL` ប៉ុណ្ណោះ ឬ `CRITICAL,HIGH`? | CRITICAL block, HIGH report-only | _(សរសេរ)_ |
| `ignore-unfixed: true` មានន័យអ្វី? ហេតុអ្វីសមហេតុផល? | true | _(សរសេរ)_ |

សំណួរ ៣ ចុង ADR-005 → ចម្លើយ:

_(សរសេរនៅទីនេះ)_

### សំណួរ review ក្នុង `ci.yml` (Task 4)

1. `load: true` ជំនួស `push: true` ក្នុង step build ដំបូង — image ទៅណា? ហេតុអ្វី Trivy ត្រូវការវានៅទីនោះ?
2. Step push ចុងក្រោយ build "ម្តងទៀត" — ហេតុអ្វីវាចំណាយតែប៉ុន្មានវិនាទី? (មើល log: `CACHED` គ្រប់ layer?)
3. Step scan ធ្លាក់ → step push កើតអ្វី? (default behaviour របស់ step ពេល step មុន fail — ខុសពី `if: always()` យ៉ាងណា?)
4. `exit-code: "0"` លើ step HIGH — បើគ្មាននរណាអាន log តើ step នេះមានប្រយោជន៍អ្វី? អ្នកនឹងអានវាពេលណា?

### លំហាត់ — វាស់

- Run ដំបូងជាមួយ Trivy: CRITICAL ______ · HIGH ______ · ពេល step scan ______ · ពេល job `docker` សរុប ______
- លំហាត់ base image: ប្តូរ `eclipse-temurin:21-jre-alpine` → `eclipse-temurin:17-jre` (Debian, គ្មាន alpine) ក្នុង Dockerfile → push (branch ណាក៏បាន? — មិនទេ: job docker run តែលើ main → ត្រូវ PR+merge, ឬ run Trivy លើ laptop ជំនួស):
  ```bash
  docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:0.69.1 image --severity CRITICAL,HIGH --ignore-unfixed ghcr.io/vuthin-devops-ecommerce/mini-shop:1279d03
  ```
  លទ្ធផលពិត 2026-09-29 (Trivy 0.74.0 លើ Windows, scan base image ផ្ទាល់ ព្រោះ build local ធ្វើមិនបាន — មើលខាងក្រោម):
  - `eclipse-temurin:21-jre-alpine` (287MB): **0** CVE (គ្រប់ severity, ទោះមិន ignore-unfixed)
  - `eclipse-temurin:17-jre` (ubuntu 26.04, 430MB): **42** CVE — LOW 4, MEDIUM 38, HIGH 0, CRITICAL 0 — **ទាំង 42 unfixed** → ជាមួយ `--ignore-unfixed` = 0
  - ហេតុអ្វី alpine 0 តែ ubuntu 42? ហេតុអ្វី gate `CRITICAL` + `ignore-unfixed` ឱ្យលទ្ធផលដូចគ្នាទាំង ២? _(សរសេរ)_
- Trivy លើ laptop vs CI: លទ្ធផលដូចគ្នាឬអត់? បើខុស ហេតុអ្វី? (hint: CVE database date)

### អ្វីដែលជួបពិតពេលធ្វើ Task 4

- **`docker build` លើ laptop ធ្លាក់** នៅ `[builder 5/7] ./mvnw dependency:go-offline`: `wget: Failed to fetch .../apache-maven-3.9.16-bin.tar.gz` ក្នុង 0.3s។ លំដាប់ debug: Dockerfile ដដែល build បានក្នុង CI → បញ្ហា environment មិនមែន code → run `eclipse-temurin:21-jdk` ដោយដៃ: DNS ✅, `wget --spider https://repo.maven.apache.org` → **`The certificate of repo.maven.apache.org is not trusted`** → `openssl s_client` → issuer `C=KR, O=Somansa, CN=Somansa Root CA`។ សន្និដ្ឋាន: network ការិយាល័យមាន TLS interception; Windows ទុកចិត្ត CA នោះ តែ Linux container មិនស្គាល់។ ដូចគ្នានឹង `curl: (35) schannel` ពី Task 0។ **មិនកែ Dockerfile** (CA របស់ក្រុមហ៊ុនមិនមែនរបស់ project) — build ធ្វើក្នុង CI, laptop ប្រើ Windows-native tool។
- Trivy ក្នុង container (`aquasec/trivy`) ក៏នឹងជួបបញ្ហាដដែល (download DB ពី ghcr.io) → តម្លើង Trivy 0.74.0 Windows ផ្ទាល់នៅ `C:/Users/user/tools/trivy/trivy.exe` (Go ប្រើ Windows cert store → ដើរបាន)។ សំណួរ: CI runner មិនមានបញ្ហានេះ — តើនេះជាហេតុផលមួយដែល "build លើ CI មិនមែន laptop" សំខាន់?

_(សរសេរនៅទីនេះ)_
- **Run ដំបូងលើ `main` ជាមួយ Trivy (run 36507202982) — gate block ពិត, មិនមែន test:** step scan ក្រហម, step push **skipped**, registry គ្មាន tag ថ្មី។ រកឃើញ `Total: 3 (CRITICAL: 3)` ក្នុង `app/app.jar` — `org.apache.tomcat.embed:tomcat-embed-core 11.0.24` (CVE-2026-65182, CVE-2026-65905, CVE-2026-68525 — security constraint/auth bypass), `Status: fixed`, `Fixed Version: 11.0.25`។ OS layer (alpine 3.24.2): 0។ ចំណាំ: base image scan លើ laptop (0 CVE) មិនឃើញនេះ — ព្រោះ CVE នៅក្នុង **jar dependency** របស់ app មិនមែន OS; ដូច្នេះ scan image ពេញក្នុង CI ចាំបាច់។
- Build step (load) 14s — cache gha hit ទាំងអស់ (run មុន 1m32s) ✅។ Scan 61s: Trivy download vuln DB + Java DB (រាល់ run, runner ថ្មី) — ចំណាយសំខាន់ជាង scan ខ្លួនឯង។
- **ការសម្រេចចិត្តកែ:** ADR-005 block តែ CVE ដែលមាន fix → ត្រូវ upgrade មិនមែន ignore។ Spring Boot 4.1.1 គ្រប់គ្រង Tomcat 11.0.24 → override property `<tomcat.version>11.0.25</tomcat.version>` ក្នុង `pom.xml` (Boot BOM ប្រើ property នេះ — patch version តែប៉ុណ្ណោះ, មិនមែន major upgrade → គ្មាន ADR ថ្មី តាម CLAUDE.md §3)។ ជម្រើសផ្សេង: upgrade Spring Boot 4.1.x ថ្មីដែលមាន Tomcat 11.0.25 (បើមាន) — ធំជាង, ធ្វើពេល Dependabot ស្នើ (Task 5)។
  - សំណួរ: ហេតុអ្វី override property ល្អជាង `<dependency>` tomcat-embed-core ផ្ទាល់ក្នុង pom? (hint: tomcat-embed-el, tomcat-embed-websocket ត្រូវ version ដូចគ្នា)
- **Run ២ លើ `main` ក្រោយ fix (run 36511263071) — gate ឆ្លង, image push:** scan CRITICAL `Total: 0` → step HIGH report `Total: 2 (HIGH: 2)` — `com.fasterxml.jackson.core:jackson-databind 2.21.5` (fixed 2.21.6) — **មិន block** (report-only តាម ADR-005) → step push run 7s (layer CACHED ទាំងអស់) → image ថ្មីលើ ghcr.io។ Build step 1m56s (cache miss ព្រោះ `pom.xml` ប្តូរ → layer `go-offline` rebuild + export 45s)។
- Trivy DB cache: `Cache not found for input keys: cache-trivy-2026-09-29` ទាំង ២ step (key តាមថ្ងៃ, save ក្រោយ job) → run ក្រោយក្នុងថ្ងៃដដែលគួរ hit។ Trivy binary cache hit (v0.70.0 — action pin v0.36.0 ប្រើ Trivy 0.70, laptop 0.74 — version ខុសគ្នា, database ដូចគ្នា)។
- សំណួរ Task 6 / review date ADR-005: HIGH 2 ក្នុង jackson-databind មាន fix — ទុកឱ្យ Dependabot (Task 5) ស្នើ ឬ override ដូច Tomcat? អ្វីជាលក្ខណៈវិនិច្ឆ័យ? (hint: CVSS, exploitability, ថាតើ app ប្រើ feature នោះ)

---

## Phase A2 / Task 5 — Branch protection + badge + Dependabot (2026-09-29)

File: `README.md` (badge, Development workflow), `.github/dependabot.yml`

### អ្វីដែលជួបពិតពេលធ្វើ Task 5

- **Branch protection API → 403** `Upgrade to GitHub Pro or make this repository public to enable this feature.` — ទាំង classic branch protection និង repository ruleset។ Repo `ecom-api` private, org plan `free`។ GitHub: protected branch/ruleset មានសម្រាប់ repo **public** លើ Free; repo private ត្រូវការ Pro/Team។
- **ការសម្រេចចិត្តរបស់អ្នក (មិនមែន Claude):** ជម្រើស (ក) ធ្វើ repo public — code រៀន, គ្មាន secret ក្នុង git (CLAUDE.md §4 ធានា) → protection ដើរបាន, ghcr package អាចនៅ private ដដែល; (ខ) នៅ private, រំលង protection, រក្សាវិន័យ "PR ជានិច្ច" ដោយខ្លួនឯង (CI នៅតែ run លើ PR តែ merge button មិន disabled); (គ) GitHub Pro។ **សម្រេច 2026-09-29: (ក) public** — `gh repo edit --visibility public` → protection apply បាន: require PR (0 approval — solo), status check `unit-test` + `integration-test`, `enforce_admins: true` (owner ក៏ bypass មិនបាន), no force-push, no delete។ `strict: false` ដោយចេតនា: ជាមួយ merge commit `develop → main`, `main` មាន commit ដែល `develop` គ្មាន → `strict: true` នឹងទាមទារ merge `main` ចូល `develop` មុនរាល់ PR — friction ដោយគ្មានតម្លៃសម្រាប់ solo dev (សំណួរ ១ ខាងក្រោម)។ ហេតុផលរបស់អ្នក:

  _(សរសេរនៅទីនេះ)_

- `dependabot.yml`: ផែនការសរសេរ `directory: /` សម្រាប់ maven/docker — ខុសសម្រាប់ monorepo នេះ (`pom.xml`, `Dockerfile` នៅ `mini-shop/`) → `/mini-shop`; github-actions នៅ root ត្រឹមត្រូវ។ បន្ថែម `ignore: semver-major` សម្រាប់ Spring Boot, eclipse-temurin, postgres (major = ADR មិនមែន PR bot, CLAUDE.md §3)។

### សំណួរឆ្លុះបញ្ចាំង / លំហាត់ (ពី `phase-a2-plan.md` Task 5)

| លំហាត់ | លទ្ធផល |
|---|---|
| សាក push ផ្ទាល់ទៅ `main` → ត្រូវបានបដិសេធ? | _(បើ protection បើក: `git push origin develop:main` → error "protected branch")_ |
| PR ដែល test បរាជ័យ → merge disabled? | _(សរសេរ)_ |
| Dependabot PR ដំបូងមកពេលណា? CI run លើ PR របស់ bot ឬអត់? job `docker` skipped? | _(សរសេរ — ចាំថ្ងៃច័ន្ទ ឬ trigger ដោយដៃ: Insights → Dependency graph → Dependabot → "Check for updates")_ |
| Dependabot ស្នើ jackson-databind 2.21.6 (HIGH ពី Trivy report) ឬអត់? | _(សរសេរ)_ |

សំណួរ:
1. `strict: true` ("Require branches to be up to date before merging") — PR ដែលបើកមុន `main` ប្តូរ ត្រូវ rebase/merge មុន — ហេតុអ្វីសំខាន់ពេលមាន ២ PR ព្រមគ្នា?
2. Status check ត្រូវការ**ឈ្មោះ job** ជាក់លាក់ (`unit-test`, `integration-test`) — បើប្តូរឈ្មោះ job ក្នុង `ci.yml` កើតអ្វី?
3. Badge ចង្អុល `?branch=main` — បើគ្មាន param តើបង្ហាញ run ណា?
4. Dependabot update SHA របស់ action + comment `# vX.Y.Z` — បើអ្នក pin SHA ដោយគ្មាន comment version, Dependabot ធ្វើអ្វី?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

---

## Phase A2 / Task 6 — Documentation & Reflection (2026-09-29)

| Deliverable | ស្ថានភាព | អ្នកណា |
|---|---|---|
| `docs/decisions/004-ci-job-structure.md` | Proposed → **Accepted** ពេលអ្នកឆ្លើយសំណួរ ៣ ចុង ADR | អ្នក |
| `docs/decisions/005-security-scan-policy.md` | Proposed → **Accepted** ពេលអ្នកឆ្លើយសំណួរ ៣ ចុង ADR (gate block ពិតរួច 2026-09-29) | អ្នក |
| `docs/runbooks/ci-failure.md` | ✅ សរសេររួច ពី symptom ពិត ៩ (1a–1d, 2a–2c, 3a–3e) | Claude |
| README "Development workflow" | ✅ | Claude |
| ចម្លើយសំណួរឆ្លុះបញ្ចាំង Task 0–5 ក្នុង file នេះ | _(សរសេរនៅទីនេះ)_ | អ្នក |

### សំណួរធំ (សម្រាប់ Phase ក្រោយ — ពី `phase-a2-plan.md` Task 6)

1. CI push image រួច — **អ្នកណា** deploy វា? ឥឡូវនៅតែជាអ្នកដោយដៃ។ Phase A3 (`kubectl set image …:<sha>`) និង E (GitOps) ដោះស្រាយយ៉ាងណា?

   _(សរសេរនៅទីនេះ)_

2. បើ CI ឆ្លង តែ app crash នៅ production — CI ខ្វះអ្វី? (hint: image ដែល push មិនដែលត្រូវ **start** ក្នុង CI — smoke test `docker run` + `/actuator/health`? contract test?)

   _(សរសេរនៅទីនេះ)_

3. Secret `DB_PASSWORD` នៅក្នុង `.env` លើម៉ាស៊ីនអ្នក — ពេល deploy ទៅ server ពិត វាទៅនៅឯណា? (Phase A3 K8s Secret → Phase B Vault)

   _(សរសេរនៅទីនេះ)_

### Definition of Done Phase A2 (ពិនិត្យ 2026-09-29)

- [x] Push ទៅ branch ណាមួយ → `ci.yml` run: compile + unit + integration
- [x] PR → CI ត្រូវឆ្លងមុន merge (branch protection, repo public)
- [x] Merge ទៅ `main` → image `ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>` + `:latest` (ឈ្មោះ `mini-shop` តាម phase-a3-plan, មិនមែន `<username>/mini-shop`)
- [x] Trivy CRITICAL → pipeline ក្រហម (បញ្ជាក់ដោយ CVE ពិត tomcat 11.0.24)
- [x] README badge
- [x] Build ទី២ លឿនជាង ២ ដង (33s vs 1m09s unit; docker build 14s vs 1m32s ពេល cache hit)
- [ ] ADR-004 (ផែនការសរសេរ `004-ci-strategy.md`; CLAUDE.md កក់ `004-ci-job-structure.md` — ប្រើឈ្មោះ CLAUDE.md) → Accepted
- [ ] Dependabot PR ដំបូង review (រង់ចាំ)

---

# Phase A3 — Kubernetes លើ kind

## Phase A3 / Task 0–1 — tool + cluster (2026-09-29)

Tool: kind v0.33.0 (`~/tools/kind`), kubectl v1.36.1 (Docker Desktop), `scripts/check-env.sh` (ថ្មី)។ File: `k8s/kind-config.yaml` (node image pin digest, port 80/443 mapping, label ingress-ready)។
Version drift ក្នុង `phase-a3-plan.md` កែរួច: `postgres:16-alpine` → `postgres:17`, ingress-nginx `main` → `controller-v1.15.1`, metrics-server `latest` → `v0.9.0`, kind `latest` → `v0.33.0`, `<username>` → `vuthin-devops-ecommerce`។

### សំណួរឆ្លុះបញ្ចាំង (ពី `phase-a3-plan.md` Task 1)

1. `coredns`, `kube-proxy`, `etcd`, `kube-apiserver` (និង `kube-scheduler`, `kube-controller-manager`, `kindnet`) — មួយៗធ្វើអ្វី? (១ បន្ទាត់រាល់មួយ, មើល `kubectl get pods -n kube-system -o wide`)

   _(សរសេរនៅទីនេះ)_

2. kind run node ជា Docker container → Pod = "container ក្នុង container"? production ពិតខុសអ្វី?

   _(សរសេរនៅទីនេះ)_

3. (ពី kind-config) ហេតុអ្វី 2 worker មិនមែន 1? ហេតុអ្វី ingress controller ត្រូវនៅ control-plane ក្នុង kind?

   _(សរសេរនៅទីនេះ)_

### លំហាត់ស្វែងយល់ cluster (½ ថ្ងៃ — កត់អ្វីដែលឃើញ)

```bash
kubectl get nodes -o wide                  # 3 node Ready? INTERNAL-IP? CONTAINER-RUNTIME?
docker ps                                  # node = container (image kindest/node)
kubectl get pods -A -o wide                # pod អ្វីខ្លះ K8s run ខ្លួនឯង? នៅ node ណា?
kubectl describe node minishop-worker      # Capacity/Allocatable cpu+memory? Conditions?
kubectl api-resources | head -40
kubectl -n ingress-nginx get pods -o wide  # controller នៅ control-plane?
```

_(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 0–1

- **`kind create cluster`** ជោគជ័យ (~1 នាទី, pull `kindest/node:v1.37.0` តាម Docker Desktop) — 3 node Ready, `docker ps` បង្ហាញ container ៣ ឈ្មោះ `minishop-control-plane|worker|worker2`, control-plane ប៉ុណ្ណោះមាន port `0.0.0.0:80->80, 443->443`។
- **ingress-nginx pod `ErrImagePull`** — `describe pod` → Events: `tls: failed to verify certificate: x509: certificate signed by unknown authority` ពេល pull `registry.k8s.io/ingress-nginx/...`។ មូលហេតុដដែលនឹង A2 Task 4: kind node = Linux container → containerd មិនទុកចិត្ត Somansa CA (network ការិយាល័យ)។ Docker Desktop pull node image បាន (Windows trust) តែ **pull ក្នុង node** ខុសផ្លូវ។ ដំណោះស្រាយ: export CA ពី Windows cert store (PowerShell) → `scripts/kind-trust-ca.sh <pem> minishop` (docker cp → `update-ca-certificates` → restart containerd លើ node ទាំង ៣) → pod retry ខ្លួនឯង → Running។ **ត្រូវ run ម្តងទៀតរាល់ `kind create`**។ CA មិន commit; script commit (generic)។
- **Controller schedule លើ `minishop-worker2` មិនមែន control-plane** — manifest `controller-v1.15.1` provider/kind មាន `nodeSelector: kubernetes.io/os: linux` ប៉ុណ្ណោះ (version ចាស់មាន `ingress-ready: "true"`) → hostPort 80 បើកលើ worker2 ដែលគ្មាន `extraPortMappings` → laptop ចូលមិនដល់។ ដោះស្រាយ: `kubectl -n ingress-nginx patch deployment ingress-nginx-controller --type=merge -p '{"spec":{"template":{"spec":{"nodeSelector":{"kubernetes.io/os":"linux","ingress-ready":"true"}}}}}'` (toleration control-plane មានស្រាប់) → pod ថ្មីលើ control-plane → `curl localhost` = 404 ពី nginx (ត្រឹមត្រូវ — មិនទាន់មាន Ingress rule)។ Task 6 (Kustomize) គួរដាក់ patch នេះជា file ជំនួស command ដោយដៃ។
  - សំណួរ: ហេតុអ្វី label `ingress-ready` ក្នុង kind-config នៅតែសំខាន់ទោះ manifest មិនប្រើ? ជម្រើសផ្សេង: `extraPortMappings` លើ worker ទាំង ២? បញ្ហាអ្វី?

## Phase A3 / Task 2 — Namespace + Postgres StatefulSet (2026-09-29)

File: `k8s/namespace.yaml`, `k8s/postgres/{configmap,secret.example,statefulset,service}.yaml` (comment ពន្យល់ក្នុង file), ADR: `docs/decisions/006-postgres-statefulset-vs-managed.md` (Proposed)។
Apply: `kubectl apply -f k8s/namespace.yaml && kubectl apply -f k8s/postgres/` → `postgres-0` Running 1/1 លើ `minishop-worker2`, PVC `data-postgres-0` Bound 1Gi (StorageClass `standard` = local-path ក្នុង node), `psql \l` ឃើញ DB `minishop`។

### លំហាត់ (អ្នកធ្វើ — កត់លទ្ធផលពិត)

1. `kubectl -n minishop delete pod postgres-0` → `kubectl -n minishop get pods -w` → pod ថ្មីឈ្មោះអ្វី? ចំណាយប៉ុន្មានវិនាទី? ទិន្នន័យនៅឬបាត់? (បង្កើត table មុន: `kubectl -n minishop exec postgres-0 -- psql -U minishop -d minishop -c 'create table t(x int); insert into t values (1);'` រួច delete pod រួច `select * from t`)

   _(សរសេរនៅទីនេះ)_

2. `kubectl -n minishop delete statefulset postgres` → `kubectl -n minishop get pvc` → PVC នៅឬបាត់? ហេតុអ្វី K8s **មិន**លុប? រួច `kubectl apply -f k8s/postgres/` → data ត្រឡប់?

   _(សរសេរនៅទីនេះ)_

### សំណួរឆ្លុះបញ្ចាំង (ADR-006 — ឆ្លើយរួចប្តូរ status)

1. ហេតុអ្វី DB ជា StatefulSet មិនមែន Deployment? (identity, DNS, PVC per pod)
2. Production ពិត: Postgres ក្នុង K8s (StatefulSet/operator) ឬ managed DB (RDS/Cloud SQL)? trade-off (backup, failover, upgrade, cost, "អ្នកណាភ្ញាក់ពេល ២ យប់")។
3. (ពី service.yaml) headless Service vs ClusterIP ពេល replicas: 1 — ខុសគ្នាអ្វី? ពេលណាសំខាន់?
4. (ពី configmap.yaml) ConfigMap vs Secret — base64 មិនមែន encryption; អ្វី**ពិត**ដែលធ្វើឱ្យ Secret "សុវត្ថិភាពជាង"?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 2

- `kubectl apply -f k8s/postgres/` apply **ទាំង** `secret.example.yaml` និង `secret.yaml` (ឈ្មោះ object ដដែល `postgres-secret`) → log "created" រួច "configured" — file ក្រោយឈ្នះ (លំដាប់ alphabet: secret.example < secret.yaml → password ពិតឈ្នះ, តែដោយសំណាង)។ Task 6 Kustomize រាយ resource ជាក់លាក់ → បញ្ហានេះបាត់។
- PVC Bound ភ្លាមទោះ StorageClass local-path: PV បង្កើតពេល pod schedule (WaitForFirstConsumer) → PV ជាប់នឹង node `worker2` → pod postgres-0 នឹង**តែងតែ**ទៅ worker2 (RWO + local disk)។ សេណារីយ៉ូ: worker2 ងាប់ → pod Pending រហូត — នេះជាហេតុផលមួយក្នុង ADR-006។
- **លំហាត់ ១–២ (ភស្តុតាងពី cluster, 2026-09-29):** ក្រោយ delete pod + delete/apply StatefulSet — pod ឈ្មោះ `postgres-0` ដដែល (age ថ្មី), PVC `data-postgres-0` **UID ដដែល** `pvc-3ec56de9…` (មិនបានបង្កើតថ្មី), `select count(*) from t` = 1 → data រស់ទាំង ២ ករណី។ ចម្លើយ "ហេតុអ្វី" នៅជារបស់អ្នក (ខាងលើ)។

## Phase A3 / Task 3 — App Deployment + ConfigMap + probes (2026-09-29)

File: `k8s/app/{configmap,deployment,service}.yaml` (comment ក្នុង file: probe ៣ ប្រភេទ, securityContext, resources)។

### អ្វីដែលជួបពិតពេលធ្វើ Task 3

- **error ដំបូង (តាមតារាង "ចំណុចដែលអ្នកនឹងខូច"): `ImagePullBackOff`** — `kubectl -n minishop get events` → `Failed to pull image "ghcr.io/vuthin-devops-ecommerce/mini-shop:e8f7f68": … failed to fetch anonymous token: … 401 Unauthorized`។ អាន: មិនមែន x509 (CA បានដោះស្រាយ Task 1), មិនមែន tag ខុស — **401 = registry ត្រូវការ login**: package `mini-shop` លើ ghcr នៅ **private** (repo public តែ package visibility ដាច់ដោយឡែក)។ ជម្រើស: (ក) GitHub → Packages → mini-shop → Package settings → Change visibility → Public (ងាយសម្រាប់រៀន, image គ្មាន secret); (ខ) `kubectl -n minishop create secret docker-registry ghcr-creds --docker-server=ghcr.io --docker-username=<user> --docker-password=<PAT read:packages>` + `imagePullSecrets` ក្នុង deployment (production pattern)។
  - ការសម្រេចរបស់អ្នក + ហេតុផល: _(សរសេរនៅទីនេះ)_
- gotcha ដែលចៀសមុន: `runAsNonRoot: true` + image `USER spring` (ឈ្មោះ) → kubelet "cannot verify user is non-root" → បន្ថែម `runAsUser: 100` (uid ពី `adduser -S` alpine, ពិនិត្យដោយ `docker run … id spring`)។
- `readOnlyRootFilesystem: true` → emptyDir mount `/tmp` (Tomcat work dir) — សាកលុប emptyDir មើល error ពិត?

### សំណួរឆ្លុះបញ្ចាំង (ពី `phase-a3-plan.md` Task 3)

1. Liveness fail → K8s ធ្វើអ្វី? Readiness fail → ធ្វើអ្វី? DB ដាច់ ១ នាទី — ចង់ restart app ឬគ្រាន់តែឈប់ផ្ញើ traffic? ហេតុអ្វីច្រឡំ ២ នេះគ្រោះថ្នាក់?
2. `requests` vs `limits` — ហេតុអ្វីមិនកំណត់ CPU limit? (ទស្សនៈ ២ ខាង)
3. Pin `<sha>` ក្នុង manifest → រាល់ release កែ manifest ដោយដៃ — Phase E (GitOps) ដោះស្រាយយ៉ាងណា?
4. (ពី configmap) `JAVA_TOOL_OPTIONS` ទីនេះ + `-XX:MaxRAMPercentage` ក្នុង Dockerfile — អ្នកណាកែបានដោយមិន rebuild?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_
- **ក្រោយ secret `ghcr-creds` (PAT read:packages) → image pull ✅** (`Pulled … already present`) — pattern production: credential ក្នុង Secret type `kubernetes.io/dockerconfigjson`, ជាប់ namespace, មិន commit។ ផ្ទៀងផ្ទាត់ credential ដោយមិនបង្ហាញ: `curl -H "Authorization: Basic <auth ពី secret>" https://ghcr.io/token?scope=repository:…:pull` → 200។
- **symptom ទី ២: `CrashLoopBackOff`** — `logs --previous` → `FlywayException: Found non-empty schema(s) "public" but no schema history table. Use baseline() or set baselineOnMigrate to true`។ មូលហេតុ: table `t` ពីលំហាត់ Task 2 នៅក្នុង schema → Flyway បដិសេធ migrate schema ដែលមាន object ស្រាប់ដោយគ្មាន history (ការពារ DB ដែលមានទិន្នន័យ)។ ដោះស្រាយ: `drop table t` (dev data) — **មិន** `baselineOnMigrate: true` (វានឹងលាក់បញ្ហានេះនៅ production)។ ក្រោយនោះ Flyway apply V1 products, V2 orders, R seed ✅។
- **symptom ទី ៣: `CrashLoopBackOff` ម្តងទៀត — `UnknownHostException: postgres`** ភ្លាមក្រោយ Docker restart: app start មុន coredns/postgres ready → K8s គ្មាន `depends_on` → app crash → kubelet restart (backoff 10s, 20s, 40s…) រហូតដល់ DNS មក → ធម្មតា; នេះជាហេតុផលដែល app ត្រូវ "crash fast + restart" មិនមែន "wait forever"។ ជម្រើសផ្សេង: initContainer រង់ចាំ DB (plan Task 3 ផែនទី compose → K8s)។
- **symptom ទី ៤ (ធំ): Docker Desktop VM ងាប់ ២ ដង** — `kubectl`: `TLS handshake timeout`, `docker ps`: 500/hang, `wsl -l -v`: docker-desktop Stopped។ ស៊ើបអង្កេត: Docker Desktop ប្រើ **Hyper-V backend** (`WslEngineEnabled: false` → `.wslconfig` 10GB មិនមានឥទ្ធិពល) ជាមួយ default **1.9 GB RAM** (`docker info` MemTotal), 20 CPU។ kind ៣ node ទទេ = ~1.1 GB (control-plane 850 MiB) + app JVM ×2 (~400 MiB) + ingress → លើស → VM OOM → cluster ដាច់ទាំងអស់។ ដោះស្រាយបណ្តោះអាសន្ន: `scale deployment/minishop-app --replicas=0`។ ដោះស្រាយពិត (អ្នក): Docker Desktop → Settings → Resources → Memory ≥ **8 GB** (host មាន 31 GB), CPUs 4–6 → Apply & restart។ មេរៀន: `requests` ក្នុង manifest (384Mi ×2 + 128Mi) មានន័យតែពេល node មាន memory ពិត — `kubectl describe node` Allocatable ត្រូវមើលមុន deploy។
- **លទ្ធផល Task 3 (2026-09-30, ក្រោយ Docker VM → 8 GB):** `kubectl get nodes -o jsonpath=…allocatable.memory` = 8123180Ki (មុន ~1.9 GB) → `scale --replicas=2` → pod ×2 Ready លើ node ខុសគ្នា, startup 4.7s, health UP, 8 products។ សំណួរ: ហេតុអ្វី scheduler ដាក់ pod ២ លើ node ខុសគ្នាដោយគ្មាន affinity rule? តើវាធានាឬអត់? (hint: Task 5 self-heal, podAntiAffinity/topologySpreadConstraints)
