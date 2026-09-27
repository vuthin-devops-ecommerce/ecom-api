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
