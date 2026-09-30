# Phase A — Worklog (អ្វីដែលធ្វើពិត តាម Task)

> ខុសពី `docs/journey/phase-a/plan.md` (ផែនការ/មេរៀន) និង `docs/journey/phase-a/learning-log.md` (ការឆ្លុះបញ្ចាំងរបស់អ្នករៀន)។
> សរសេរឡើងវិញ 2026-09-30 ពី tracker ក្នុង `docs/journey/phase-a/plan.md`, code ក្នុង `mini-shop/`, និង ADR 001–003 — Phase A ធ្វើ 2026-09-27 មុនមាន worklog។
> git: Phase A ទាំងមូលស្ថិតក្នុង commit ដំបូង `cfab714 setup project` (2026-09-28) — history លម្អិតតាម Task មិនមាន។
> របៀបធ្វើការ (CLAUDE.md §2, ចាប់ពី Task 1): Claude សរសេរ code ពេញ + ពន្យល់ការសម្រេចចិត្ត; អ្នករៀន review។

## សង្ខេប

| Task | គោលដៅ | លទ្ធផល |
|---|---|---|
| 0 | project skeleton, Flyway, Actuator | ✅ Boot 4.1.1, health UP |
| 1 | `catalog` domain | ✅ V1, 9 smoke case |
| 2 | order schema (ADR-002) | ✅ V2, FK/CHECK/UNIQUE |
| 3 | `order` domain + business rule (ADR-003) | ✅ check-all-then-decrement |
| 4 | test: unit + Testcontainers IT | ✅ 7 unit + 3 IT, rollback ពិត |
| 5 | Docker + Compose | ✅ image 403 MB, non-root |
| 6 | README, ADR 001–003, learning-log | ✅ (ចម្លើយអ្នកនៅទទេ) |

App ចុងក្រោយ: modular monolith `com.devops.minishop.{catalog,order,common}` · Spring Boot 4.1.1 · Java 21 · PostgreSQL 17 · Flyway (`ddl-auto: validate`) · Maven wrapper 3.9.16។

---

## Task 0 — Skeleton

**ធ្វើ:** start.spring.io → `mini-shop/` (Web MVC, Data JPA, Flyway, Validation, Actuator, PostgreSQL, Lombok) · `application.yml` datasource `${SPRING_DATASOURCE_*}` **គ្មាន default** (fail fast) · `management.endpoint.health.probes.enabled: true` (ប្រើនៅ A3) · `env.sh` gitignored។

**ជួបពិត:** Initializr ផ្តល់ `4.1.1.RELEASE` តែ Maven Central មាន `4.1.1` → parent resolve មិនបាន → កែ។ `JAVA_HOME` default 17 → override 21។ port 5432/8080 ជាប់ container ផ្សេង (`pharmacy-*`) → `docker stop`។

**Deviation:** dev DB ប្តូរទៅ Neon (remote, PostgreSQL **18.6**) ≠ pin 17 — Testcontainers/compose នៅ 17។

**លទ្ធផល:** Started 4.4s · `/actuator/health` UP · `flyway_schema_history` បង្កើត។

## Task 1 — Catalog domain

**File:** `catalog/{Product, ProductRepository, ProductService, ProductController, dto/}` · `db/migration/V1__create_products.sql` · `common/{GlobalExceptionHandler, NotFoundException, ConflictException, BadRequestException, InsufficientStockException, RequestLoggingFilter}`។

**ការសម្រេចចិត្ត:** `price` = `BigDecimal`; controller គ្មាន business logic; error = RFC 9457 ProblemDetail; stock delta endpoint `PATCH /products/{id}/stock`។

**លទ្ធផល:** V1 apply (988ms) · validate ឆ្លង · test 3/3 · smoke 9 case: 201 / 409 SKU ស្ទួន / 400 validation / 200 / 404 / 400 stock។

## Task 2 — Order schema (ADR-002)

**File:** `db/migration/V2__create_orders.sql` · `docs/decisions/002-order-schema-fk.md`។

**ការសម្រេចចិត្ត:** FK `order_items.order_id` ON DELETE CASCADE · `product_id` RESTRICT (history មិនបាត់) · CHECK status/quantity > 0/price ≥ 0 · UNIQUE(order_id, product_id) · index `product_id` · `price_at_order` **ចម្លង** តម្លៃ។

**លទ្ធផល:** V2 apply (1.5s) · probe ៣: qty 0, លុប product ដែលមាន history, status ខុស → DB បដិសេធ។

## Task 3 — Order domain (ADR-003)

**File:** `order/{Order, OrderItem, OrderStatus, OrderRepository, OrderService, OrderController, dto/}` · `docs/decisions/003-product-id-not-entity-ref.md`, `001-modular-monolith.md`។

**ការសម្រេចចិត្ត:** `OrderService.createOrder` `@Transactional` — **ពិនិត្យ stock ទាំងអស់មុន** រួចកាត់; ហៅ `ProductService` (DTO) មិនមែន `ProductRepository`; `OrderItem.productId` ជា `Long` (ត្រៀមបំបែក Phase B); productId ស្ទួន → 400; `@EntityGraph` ចៀស LazyInit។

**លទ្ធផល:** smoke 8 case: 201 stock 8→6, insufficient → 400 + stock **មិនប្រែ**, 400 dup, 404, 400 validation, GET 200, 404។ order id ចាប់ពី 4 (sequence non-transactional)។

## Task 4 — Tests

**File:** `ProductServiceTest`, `OrderServiceTest` (Mockito, mock `ProductService`) · `OrderControllerIT` (Testcontainers `postgres:17`, profile `it`, `@MockitoSpyBean`) · `pom.xml` failsafe។

**លទ្ធផល:** `./mvnw verify` unit 7/7 + IT 3/3 (16s) · IT បញ្ជាក់ stock ថយពិតក្នុង DB, 400 → stock មិនប្រែ, **fail ក្រោយកាត់ stock → rollback ពិត** (spy `save()` throw)។

**ចំណាំក្រោយ (A2):** `MiniShopApplicationTests.contextLoads` ត្រូវការ DB តែឈ្មោះ `*Tests` → ឆ្លងលើ laptop តែព្រោះ DB run → លុបនៅ A2 Task 1។

## Task 5 — Containerize

**File:** `mini-shop/Dockerfile` (multi-stage: `eclipse-temurin:21-jdk` + `./mvnw dependency:go-offline` layer → `21-jre-alpine`, user `spring` uid 100, `-XX:MaxRAMPercentage=75`) · `.dockerignore` · `compose.yaml` (`postgres:17` + healthcheck, app `depends_on: service_healthy`, `DB_PASSWORD` required) · `.env.example`។

**លទ្ធផល:** image `mini-shop:local` 403 MB (jar 59 MB) · healthy ~15s · Flyway V1+V2+R លើ volume ថ្មី · seed 8 products · `down`→`up` data នៅ · cached rebuild 2s។

## Task 6 — Docs

`README.md` (run local/compose, API, tests) · ADR-001 (ផែនការថា "មានរួច" តែមិនមាន → សរសេរថ្មី), 002, 003 · `learning-log` ~18 សំណួរ — **ចម្លើយនៅទទេ**។

---

## អ្វីដែលអ្នកគួរអាចធ្វើបានក្រោយ A

- ពន្យល់ថាហេតុអ្វី `ddl-auto: validate` + Flyway (មិនមែន `update`)
- បង្ហាញ test ដែលបញ្ជាក់ rollback ពិត និងពន្យល់ថាហេតុអ្វី mock មិនគ្រប់
- ពន្យល់ថាហេតុអ្វី `OrderService` មិន import `ProductRepository`
