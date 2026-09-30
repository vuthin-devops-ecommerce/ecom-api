# Phase A — Modular Monolith (Catalog + Order)

> **សម្រាប់ Claude Code:** ឯកសារនេះជា plan ដែលបានសម្រេចរួច។ សូមអាន `CLAUDE.md` ជាមុន។
> អ្នកប្រើកំពុង**រៀន** DevOps — គោលដៅមិនមែនបញ្ចប់លឿនទេ គឺយល់ជ្រៅ។
> ណែនាំជាជំហាន ពន្យល់ជាភាសាខ្មែរ ទុកឱ្យអ្នកប្រើសរសេរ code ខ្លួនឯង រួចត្រួតពិនិត្យ។

**ថ្ងៃចាប់ផ្តើម:** 2026-09-21
**រយៈពេលប៉ាន់ស្មាន:** សប្តាហ៍ ១–២ (Task 0–5), សប្តាហ៍ ៣+ (Task 6–10)
**Stack:** Spring Boot 4.1.x · Java 21 · Maven (wrapper) · PostgreSQL 17 · Flyway · Docker — (កែ 2026-09-27 ឱ្យត្រូវ `docs/reference/tech-stack.md`)

---

## គោលដៅដំណាក់កាល A

បង្កើត Spring Boot app **តែមួយ** ដែលមាន domain ពីរ (`catalog`, `order`) រៀបចំបែប modular monolith
ដើម្បីឱ្យដំណាក់កាល B បំបែក `payment` ចេញជា service ដាច់បានដោយងាយ។

**លក្ខខណ្ឌបញ្ចប់ (Definition of Done):**
- [x] `docker compose up --build` → app + Postgres ដំណើរការដោយពាក្យបញ្ជាតែមួយ (2026-09-27)
- [x] `./mvnw verify` ឆ្លងទាំងអស់ (unit 7 + IT 3)
- [x] `GET /actuator/health` → `{"status":"UP"}`
- [x] Flyway migration V1, V2 apply ដោយស្វ័យប្រវត្តិ (+ `R__seed_dev_data` ក្រោម profile `dev`)
- [x] `docs/decisions/` មាន ADR យ៉ាងតិច ២ (001, 002, 003)
- [x] README មានជំហាន run ពេញលេញ
- [ ] `docs/journey/phase-a/learning-log.md` — ចម្លើយសំណួរឆ្លុះបញ្ចាំង (របស់អ្នកប្រើ — មិនទាន់)

---

## ច្បាប់ architecture (មិនអាចរំលោភ)

1. **Package តាម domain** មិនមែនតាម layer: `catalog/`, `order/`, `common/`
2. **`catalog` និង `order` មិន import entity/repository របស់គ្នាទៅវិញទៅមកទេ** — ឆ្លងកាត់ Service class ប៉ុណ្ណោះ
3. **Flyway គ្រប់គ្រង schema** — `ddl-auto: validate` ជានិច្ច, មិនដែល `update`
4. **គ្មាន secret ក្នុង code ឬ git** — ប្រើ `${ENV_VAR}` ក្នុង `application.yml`
5. **Controller មិនមាន business logic** — គ្រាន់តែ map request → service → response
6. **រាល់ business rule ត្រូវមាន test**

---

## Task 0 — Project skeleton

**គោលដៅ:** app run បាន, `/actuator/health` ឆ្លើយ UP

**ជំហាន:**
1. បង្កើតពី start.spring.io: Maven, Java 21, group `com.devops`, artifact `mini-shop`
2. Dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Actuator, Validation, Lombok
3. បង្កើត package: `com.devops.minishop.{catalog,order,common}`
4. `application.yml`:
   ```yaml
   spring:
     application:
       name: mini-shop
     datasource:
       url: ${SPRING_DATASOURCE_URL}          # គ្មាន default ដោយចេតនា (fail fast) — កែ 2026-09-27
       username: ${SPRING_DATASOURCE_USERNAME}
       password: ${SPRING_DATASOURCE_PASSWORD}
     jpa:
       hibernate:
         ddl-auto: validate
       open-in-view: false
     flyway:
       enabled: true
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,prometheus
     endpoint:
       health:
         probes:
           enabled: true
   ```
5. `.gitignore`: `target/ .idea/ *.iml .env *.log`
6. `git init` + commit ដំបូង

**ផ្ទៀងផ្ទាត់:** `mvn spring-boot:run` (Postgres local ត្រូវ run សិន) → `curl localhost:8080/actuator/health`

**សំណួរឆ្លុះបញ្ចាំង (សរសេរចម្លើយក្នុង docs/journey/phase-a/learning-log.md):**
- `ddl-auto: validate` ខុសពី `update` យ៉ាងណា? ហេតុអ្វី `update` គ្រោះថ្នាក់នៅ production?

---

## Task 1 — Catalog domain

**គោលដៅ:** CRUD ផលិតផលពេញលេញ + Flyway V1

**Files ដែលត្រូវបង្កើត:**
```
catalog/
├── Product.java              entity
├── ProductRepository.java    extends JpaRepository<Product, Long>
├── ProductService.java
├── ProductController.java
└── dto/
    ├── CreateProductRequest.java   (record, @Valid)
    └── ProductResponse.java        (record)
resources/db/migration/
└── V1__create_products.sql
```

**Schema V1:**
```sql
CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(50)   NOT NULL UNIQUE,
    name        VARCHAR(200)  NOT NULL,
    price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    stock       INTEGER       NOT NULL DEFAULT 0 CHECK (stock >= 0),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);
```

**Endpoints:**
| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/products` | `{sku, name, price, stock}` | 201 + ProductResponse |
| GET | `/api/products` | — | 200 + List |
| GET | `/api/products/{id}` | — | 200 / 404 |
| PATCH | `/api/products/{id}/stock` | `{delta}` | 200 |

**ច្បាប់:**
- `price` ជា `BigDecimal` (មិនដែល `double` សម្រាប់លុយ)
- SKU ស្ទួន → 409 Conflict
- ID មិនមាន → 404 (ប្រើ `common/NotFoundException` + `GlobalExceptionHandler`)

**Test (`ProductServiceTest`):**
- បង្កើត product → រក្សាទុកបាន
- SKU ស្ទួន → throw exception

**សំណួរឆ្លុះបញ្ចាំង:**
- ហេតុអ្វី `double` មិនសាកសមសម្រាប់លុយ? សាកគណនា `0.1 + 0.2` ក្នុង Java។

---

## Task 2 — Order schema (អ្នកប្រើសរសេរខ្លួនឯង)

**គោលដៅ:** `V2__create_orders.sql` ជាមួយតារាង ២

**តម្រូវការ (មិនផ្តល់ SQL — អ្នកប្រើសរសេរ):**
- តារាង `orders`: id, status (VARCHAR 20), total_amount (NUMERIC), created_at
- តារាង `order_items`: id, order_id (FK → orders), product_id (FK → products), quantity (> 0), price_at_order (NUMERIC)
- `status` អាចជា: `CREATED`, `PAID`, `CANCELLED` (Phase A ប្រើតែ CREATED)

**ចំណុចត្រូវគិត:**
- `ON DELETE` behavior សម្រាប់ FK ត្រូវជាអ្វី? (`CASCADE`? `RESTRICT`?) — សរសេរហេតុផលក្នុង ADR-002
- ហេតុអ្វី `price_at_order` ត្រូវ**ចម្លង** តម្លៃ មិន reference ទៅ `products.price`?

**Claude Code:** review SQL របស់អ្នកប្រើ, ចង្អុលបញ្ហា, **កុំសរសេរជំនួស**។

---

## Task 3 — Order domain + business rule

**គោលដៅ:** បង្កើត order ដោយពិនិត្យ និងកាត់ stock

**Files:**
```
order/
├── Order.java
├── OrderItem.java
├── OrderStatus.java          enum
├── OrderRepository.java
├── OrderService.java
├── OrderController.java
└── dto/
    ├── CreateOrderRequest.java     { items: [{productId, quantity}] }
    └── OrderResponse.java
common/
└── InsufficientStockException.java  → 400
```

**Logic `OrderService.createOrder()`:**
```
1. សម្រាប់រាល់ item: ហៅ productService.getById(productId)
2. បើ product.stock < item.quantity → throw InsufficientStockException
   (ត្រូវពិនិត្យ**ទាំងអស់**មុន កុំកាត់ stock ណាមួយ)
3. សម្រាប់រាល់ item: productService.decreaseStock(productId, quantity)
4. បង្កើត Order (status=CREATED, total=Σ price×qty), OrderItem (priceAtOrder=product.price)
5. រក្សាទុក, return response
```

**ច្បាប់:**
- `@Transactional` លើ `createOrder()` — ហេតុអ្វី? (សរសេរចម្លើយ)
- `OrderService` ហៅ `ProductService` **មិនមែន** `ProductRepository`
- `OrderItem.productId` ជា `Long` ធម្មតា **មិនមែន** `@ManyToOne Product` — ដើម្បីត្រៀមបំបែក service ក្រោយ

**Endpoints:**
| Method | Path | Response |
|---|---|---|
| POST | `/api/orders` | 201 / 400 (stock) / 404 (product) |
| GET | `/api/orders/{id}` | 200 / 404 |

---

## Task 4 — Tests សម្រាប់ business rule

**`OrderServiceTest` (unit, Mockito):**
- stock គ្រប់ → order បង្កើត, `decreaseStock` ត្រូវបានហៅ
- stock មិនគ្រប់ item ទី២ → exception, `decreaseStock` **មិនត្រូវ**បានហៅសោះ (សំខាន់!)
- product មិនមាន → NotFoundException

**`OrderControllerIT` (integration, `@SpringBootTest` + Testcontainers Postgres):**
- POST order ពិត → 201, stock ក្នុង DB ថយពិត
- POST order stock មិនគ្រប់ → 400, stock ក្នុង DB **មិនប្រែ** (ផ្ទៀងផ្ទាត់ transaction rollback)

**Dependency បន្ថែម:** `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`

**សំណួរឆ្លុះបញ្ចាំង:**
- Testcontainers ខុសពី H2 in-memory យ៉ាងណា? ហេតុអ្វីសំខាន់សម្រាប់ DevOps?

---

## Task 5 — Containerize

**Files:** `Dockerfile`, `.dockerignore`, `compose.yaml` (ឈ្មោះតាម tech-stack), `.env.example` (→ `.env` gitignored)

> កែ 2026-09-27 ឱ្យត្រូវ `docs/reference/tech-stack.md`: `postgres:17` (មិនមែន `16-alpine`), Maven **wrapper** ក្នុង builder (មិនមែន image `maven:3.9`)។
> Code ពិតនៅ `mini-shop/Dockerfile` និង `mini-shop/compose.yaml` — ខាងក្រោមជាចំណុចសំខាន់ មិនមែនចម្លងទាំងស្រុង។

**Dockerfile (multi-stage, non-root):**
```dockerfile
FROM eclipse-temurin:21-jdk AS builder          # wrapper ទាញ Maven 3.9.16 ដូច laptop/CI
COPY .mvn/ .mvn/ ; COPY mvnw pom.xml ./          # layer dependency ដាច់ពី src → cache
RUN ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B package -DskipTests             # test run ក្នុង CI (A2) មិនមែនក្នុង image build

FROM eclipse-temurin:21-jre-alpine               # JRE ប៉ុណ្ណោះ, non-root user "spring"
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

**compose.yaml — ចំណុចសំខាន់:**
- `db`: `image: postgres:17`, healthcheck `pg_isready`, volume `pgdata`; **មិន publish port 5432** (app ភ្ជាប់តាម network ខាងក្នុង `db:5432`)
- `app`: `build: .`, `depends_on: db: condition: service_healthy`, env `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/minishop`
- `${DB_PASSWORD:?…}` = **fail fast** បើ `.env` គ្មាន (ដូច `application.yml`); `${APP_PORT:-8080}` ប្តូរ host port បាន; `SPRING_PROFILES_ACTIVE` default `dev` (seed)

**លំហាត់ (ធ្វើទាំងអស់, កត់លទ្ធផលក្នុង learning-log):**
1. `docker images` → កត់ទំហំ image
2. សរសេរ Dockerfile single-stage ប្រៀបធៀបទំហំ → ខុសគ្នាប៉ុន្មាន?
3. `docker compose down` → `up` → data នៅឬបាត់? លុប `volumes:` សាកម្តងទៀត
4. កែ code ១ បន្ទាត់ → rebuild → លឿនជាងឬអត់? ហេតុអ្វី?
5. ប្តូរ `db` → `localhost` ក្នុង compose → អាន error ឱ្យយល់ → កែត្រឡប់

---

## Task 6 — Documentation

- [ ] `README.md`: prerequisites, run local, run docker, API examples (curl)
- [ ] `docs/decisions/001-modular-monolith.md` (មានរួច)
- [ ] `docs/decisions/002-order-schema-fk.md` (ពី Task 2)
- [ ] `docs/decisions/003-product-id-not-entity-ref.md` (ពី Task 3)
- [ ] `docs/journey/phase-a/learning-log.md`: ចម្លើយសំណួរឆ្លុះបញ្ចាំងទាំងអស់

---

## សំណួរធំសម្រាប់ដំណាក់កាលក្រោយ (កុំដោះស្រាយឥឡូវ)

សរសេរចម្លើយបឋមក្នុង `docs/journey/phase-a/learning-log.md`:

1. **Race condition:** អ្នកប្រើ ២ នាក់ order product ចុងក្រោយ ១ ក្នុងពេលដំណាលគ្នា — កើតអ្វី? (ចម្លើយ: optimistic/pessimistic locking — Phase B)
2. **Deploy:** បើ V3 migration បរាជ័យពាក់កណ្តាលនៅ production — កើតអ្វី? (Phase D/E)
3. **Split:** បើ `order` ក្លាយជា service ដាច់ តើ `productService.decreaseStock()` ក្លាយជាអ្វី? (Phase B — HTTP call ឬ event?)

---

## Progress tracker

| Task | ស្ថានភាព | ថ្ងៃបញ្ចប់ | កំណត់ចំណាំ |
|---|---|---|---|
| 0 Skeleton | ✅ | 2026-09-27 | Boot 4.1.1 · JDK 21 (`JAVA_HOME` default នៅ 17 — ត្រូវ override) · Started 4.4s · health UP · `./mvnw test` 1/1 pass (10.3s) · `flyway_schema_history` បង្កើត (0 migration) · ជាប់ port 5432/8080 ពី container `pharmacy-*` → `docker stop` · **Deviation:** dev DB ប្តូរទៅ Neon (remote, PostgreSQL **18.6**, pooler endpoint) ≠ pin PG 17 — Testcontainers/compose នៅ 17; credential ក្នុង `env.sh` (gitignored) |
| 1 Catalog | ✅ | 2026-09-27 | V1 applied on Neon (988ms) · `ddl-auto: validate` ឆ្លង · `./mvnw test` 3/3 (27s — context load លើ Neon 18s) · smoke test 9 cases: 201/409/400(validation)/200/404/200/400(stock) ត្រូវទាំងអស់ · Claude សរសេរ code, user review |
| 2 Order schema | ✅ | 2026-09-27 | V2 applied on Neon (1.5s) · FK `order_id` CASCADE / `product_id` RESTRICT · CHECK status/quantity/price · UNIQUE(order_id, product_id) · index product_id · probe 3 ករណី (qty 0, delete product មាន history, status ខុស) DB បដិសេធត្រឹមត្រូវ · ADR-002 សរសេររួច · Claude plan+build, user review |
| 3 Order domain | ✅ | 2026-09-27 | `createOrder`: check-all-then-decrement, `@Transactional`, ហៅ `ProductService` (DTO ប៉ុណ្ណោះ) · `productId` ជា `Long` (ADR-003) · duplicate productId → 400 · smoke 8 cases: 201, stock 8→6/3→2, insufficient → 400 + stock **មិនប្រែ**, 400 dup, 404 product, 400 validation, GET 200 (EntityGraph, គ្មាន LazyInit), 404 · context-load validate ឆ្លង · order id ចាប់ពី 4 (sequence non-transactional — probe Task 2) |
| 4 Tests | ✅ | 2026-09-27 | `./mvnw verify` BUILD SUCCESS: unit 7/7 (surefire, 0.2–1.2s) + IT 3/3 (failsafe, 16s ជាមួយ Testcontainers `postgres:17` 2.0.5) · IT បញ្ជាក់: stock ថយពិតក្នុង DB, 400 → stock មិនប្រែ, **fail ក្រោយកាត់ stock → rollback ពិត** (spy `save()` throw) · `OrderServiceTest` mock `ProductService` (ADR-003) · ចំណាំ: TC 2.x artifact `testcontainers-postgresql`, package `org.testcontainers.postgresql` |
| 5 Docker | ✅ | 2026-09-27 | `docker compose up --build` ដំណើរការ · image `mini-shop:local` **403 MB** (jar 59 MB, base `21-jre-alpine`) · non-root `uid=100(spring)` · healthy ~15s ក្រោយ `up` (Started 7.4s — DB local) · Flyway V1+V2+R apply លើ volume ថ្មី · seed 8 products · `down`→`up` data នៅ (order 1, stock 49) · cached rebuild **2s** · builder ប្រើ `./mvnw` (Maven 3.9.16) · port DB មិន publish; `APP_PORT` override |
| 6 Docs | ✅ | 2026-09-27 | `README.md` (run local/compose, API, tests) · ADR-001 (សរសេរថ្មី — ផែនការថា "មានរួច" តែមិនមាន), 002, 003 · `docs/journey/phase-a/learning-log.md` skeleton ~18 សំណួរ — **ចម្លើយអ្នកប្រើនៅមិនទាន់សរសេរ** |

**បន្ទាប់:** Phase A2 — CI ជាមួយ GitHub Actions (build → test → image → ghcr.io)