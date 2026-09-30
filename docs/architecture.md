# Architecture

> **គោលបំណង:** ពន្យល់ system mini-shop ទាំងមូល — component, ផ្លូវ request, data, ព្រំដែន · **អ្នកអាន:** engineer ថ្មី, reviewer · **Update:** 2026-09-30

## ១. System ជាអ្វី

`mini-shop` ជា REST API សម្រាប់ហាងតូច: គ្រប់គ្រង **product + stock** (`catalog`) និងបង្កើត **order** ដែលកាត់ stock (`order`)។
វាជា **modular monolith** — deployable តែមួយ (jar/image), DB តែមួយ, តែព្រំដែន domain ច្បាស់ក្នុង code ដើម្បីបំបែកជា service ដាច់បាននៅ Phase B ([ADR-001](decisions/001-modular-monolith.md))។

| | |
|---|---|
| Runtime | Java 21 (Temurin) · Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Actuator) |
| Data | PostgreSQL 17 · Flyway (`ddl-auto: validate`) |
| Packaging | Docker multi-stage → `ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>` |
| Runtime platform | Docker Compose (dev) · Kubernetes/kind (Phase A3) |

Version ពេញលេញ: [reference/tech-stack.md](reference/tech-stack.md)។

## ២. Component និងផ្លូវ request (Kubernetes)

```
 laptop / client
      │  http://minishop.local/api/...        (hosts: 127.0.0.1 minishop.local)
      ▼
 kind control-plane container  :80  (extraPortMappings)
      ▼
 ingress-nginx controller      ns ingress-nginx      ← ជ្រើស rule តាម Host header (L7)
      ▼
 Ingress minishop  → Service minishop-app (ClusterIP :80)   ns minishop
      ▼                  │ ផ្ញើតែទៅ pod ដែល readiness ✅ (EndpointSlice)
 Deployment minishop-app ×2  (RollingUpdate maxSurge 1 / maxUnavailable 0)
      │  Pod: image :<sha>, :8080, non-root uid 100, read-only FS + emptyDir /tmp
      │  config ← ConfigMap app-config · password ← Secret postgres-secret · pull ← Secret ghcr-creds
      ▼  jdbc:postgresql://postgres:5432/minishop
 Service postgres (headless)  →  StatefulSet postgres ×1  →  PVC data-postgres-0 (1Gi, local-path)
```

Manifest: `k8s/` · Guide: [guides/run-on-kubernetes.md](guides/run-on-kubernetes.md) · ហេតុផល DB ជា StatefulSet: [ADR-006](decisions/006-postgres-statefulset-vs-managed.md)។
Docker Compose (dev) មានរចនាដូចគ្នា ដោយ `app` → `db` (service name) — [guides/local-development.md](guides/local-development.md)។

## ៣. ខាងក្នុង app — ព្រំដែន domain

```
com.devops.minishop
├── catalog   Product · ProductRepository · ProductService · ProductController · dto/
├── order     Order · OrderItem · OrderStatus · OrderRepository · OrderService · OrderController · dto/
└── common    GlobalExceptionHandler (RFC 9457 ProblemDetail) · exceptions · RequestLoggingFilter
```

ច្បាប់ (មិនអាចរំលោភ — CLAUDE.md §5):
1. Package តាម **domain** មិនមែន layer។
2. `order` → `catalog` តាម **`ProductService` ប៉ុណ្ណោះ** (DTO) — មិនដែល `ProductRepository` ឬ entity `Product`។
3. `OrderItem.productId` ជា `Long` មិនមែន `@ManyToOne` ([ADR-003](decisions/003-product-id-not-entity-ref.md))។
4. Controller: map request → service → response ប៉ុណ្ណោះ។
5. លុយ = `BigDecimal`; `price_at_order` **ចម្លង** តម្លៃពេល order។

## ៤. Business rule សំខាន់: បង្កើត order

`OrderService.createOrder` (`@Transactional`):
1. បដិសេធ `productId` ស្ទួន → 400
2. ទាញ product **ទាំងអស់** → មិនមាន → 404
3. **ពិនិត្យ stock ទាំងអស់មុន** កាត់ណាមួយ → មិនគ្រប់ → 400 (គ្មានអ្វីប្រែ)
4. កាត់ stock + save order; exception ណាមួយ → **rollback ពិត**

បញ្ជាក់ដោយ `OrderControllerIT` លើ Postgres ពិត (Testcontainers): order ដែលធ្លាក់ → stock ក្នុង DB មិនប្រែ។

## ៥. Data model

```
products                         orders                        order_items
─────────                        ──────                        ───────────
id          BIGSERIAL PK         id           BIGSERIAL PK     id              BIGSERIAL PK
sku         VARCHAR(50) UNIQUE   status       VARCHAR(20)      order_id        FK → orders   ON DELETE CASCADE
name        VARCHAR(200)         total_amount NUMERIC(12,2)    product_id      FK → products RESTRICT
price       NUMERIC(10,2) ≥0     created_at   TIMESTAMP        quantity        INTEGER > 0
stock       INTEGER ≥0                                         price_at_order  NUMERIC(10,2)
created_at  TIMESTAMP                                          UNIQUE(order_id, product_id)
```

Schema គ្រប់គ្រងដោយ Flyway: `mini-shop/src/main/resources/db/migration/V*.sql` (+ `db/dev/R__seed_dev_data.sql` ពេល profile `dev`)។ FK ឆ្លង domain (`order_items.product_id`) ជា safety net ក្នុង monolith — នឹងបាត់ពេលបំបែក ([ADR-002](decisions/002-order-schema-fk.md))។

## ៦. Delivery pipeline

```
push ──▶ unit-test ──▶ integration-test ──▶ (main) docker: build → Trivy CRITICAL gate → push ghcr.io :<sha>
                                                                                   │
                         kubectl set image / apply (ដោយដៃ — Phase E: GitOps) ◀──────┘
```

លម្អិត: [reference/ci-pipeline.md](reference/ci-pipeline.md) · [guides/release.md](guides/release.md) · ADR [004](decisions/004-ci-job-structure.md), [005](decisions/005-security-scan-policy.md)។

## ៧. Health និង operability

| Endpoint | ប្រើដោយ | រាប់ DB? |
|---|---|---|
| `/actuator/health/liveness` | K8s startupProbe + livenessProbe → fail = restart | ទេ |
| `/actuator/health/readiness` | K8s readinessProbe → fail = ដកពី Service | បាទ |
| `/actuator/health` | មនុស្ស, compose healthcheck | បាទ |
| `/actuator/prometheus` | Phase D (metrics) | — |

## ៨. អ្វីដែលមិនទាន់មាន (ដឹងហើយ)

- Deploy ស្វ័យប្រវត្តិ (manual `kubectl`) → Phase E
- Secret management (Secret ដោយដៃ, PAT pull) → Phase B
- Postgres HA / backup (StatefulSet ×1 ក្នុង kind) → production = managed DB (ADR-006)
- Observability ក្រៅពី log + actuator → Phase D
- Authentication លើ API → មិនទាន់មាន scope
