# ADR-001: Modular monolith មុន microservices

| | |
|---|---|
| **ស្ថានភាព** | Accepted |
| **ថ្ងៃ** | 2026-09-21 (សម្រេចក្នុងផែនការ) · សរសេរជា ADR 2026-09-27 |
| **Code** | `mini-shop/src/main/java/com/devops/minishop/{catalog,order,common}` |
| **ពាក់ព័ន្ធ** | ADR-002 (order schema), ADR-003 (productId ជា Long) |

## បរិបទ

គោលដៅនៃ repo នេះគឺរៀន DevOps (CI, container, Kubernetes, Terraform) — មិនមែនរៀន distributed system។
តែ Phase B ត្រូវការ app ដែល**អាចបំបែក**ជា service ដាច់ (`payment`/`order`) ដើម្បីរៀន service-to-service,
contract test, secret sharing។ ដូច្នេះ app ដំបូងត្រូវសាមញ្ញគ្រប់គ្រាន់ដើម្បី deploy បានឆាប់ ប៉ុន្តែរចនាសម្ព័ន្ធ
ត្រូវត្រៀមការបំបែកទុកមុន។

## ជម្រើសដែលពិចារណា

| ជម្រើស | ហេតុផលបដិសេធ / ទទួល |
|---|---|
| Microservices ពីដំបូង (`catalog-svc`, `order-svc`) | ❌ ២ repo/pipeline/DB/Deployment មុនមាន CI ឬ K8s — "one tool per stage" រំលោភ; debug ពិបាកដោយគ្មាន observability |
| Monolith តាម layer (`controller/`, `service/`, `repository/`) | ❌ domain លាយគ្នាក្នុង package ដដែល — បំបែកក្រោយត្រូវកាត់ code ឆ្លង package ទាំងអស់ |
| **Modular monolith តាម domain** | ✅ deploy unit តែមួយ តែព្រំដែន domain ច្បាស់ក្នុង code — បំបែក = ដក package + ប្តូរ Service call ជា HTTP |

## ការសម្រេចចិត្ត

1. Spring Boot app **តែមួយ**, DB **តែមួយ**, deployable **តែមួយ** (jar/image)។
2. Package តាម **domain**: `catalog`, `order`, `common` — មិនមែនតាម layer។ Layer (controller/service/repository/dto) ស្ថិត**ក្នុង** domain នីមួយៗ។
3. Domain ឆ្លងគ្នាតាម **Service class ប៉ុណ្ណោះ** — `OrderService → ProductService`; មិនដែល `→ ProductRepository` ឬ `Product` entity។ Service ផ្តល់ DTO (`ProductResponse`) មិនមែន entity។
4. Reference ឆ្លង domain ជា **id ធម្មតា** (`OrderItem.productId: Long`) មិនមែន JPA relation (ADR-003)។
5. `common` មានតែអ្វីដែលគ្មាន domain: exception, error handler, filter។ **មិន**មាន business logic។
6. Schema ចែក table តាម domain (`products` / `orders`, `order_items`) — FK ឆ្លង domain (`order_items.product_id`) អនុញ្ញាតក្នុង monolith ជា safety net តែដឹងថានឹង**បាត់**ពេលបំបែក (ADR-002)។

## ផលវិបាក

- (+) Phase A–A4 ធ្វើការលើ artifact តែមួយ: CI pipeline ១, image ១, Deployment ១, HPA ១ — មេរៀន DevOps ច្បាស់ មិនលាយជាមួយបញ្ហា distributed
- (+) Phase B: បំបែក `order` = ចម្លង package ចេញ + ជំនួស `ProductService` ដោយ HTTP client/event — `OrderService` មិនប្តូរ; test `OrderServiceTest` (mock `ProductService`) នៅប្រើបាន
- (+) Transaction ឆ្លង domain (`createOrder` កាត់ stock + save order ក្នុង tx ដដែល) **ងាយ**ក្នុង monolith — Phase B ត្រូវដោះស្រាយដោយ saga/outbox: ចំណុចនេះជាមេរៀនដែលចង់បាន
- (−) ព្រំដែនរក្សាដោយ**វិន័យ** (code review) មិនមែន compiler — `order` អាច import `catalog.Product` បានដោយបច្ចេកទេស។ Mitigation ក្រោយ: ArchUnit test ឬ Java module/Spring Modulith
- (−) DB ដដែល = schema ចែក; migration ១ ជួរ (`V1..Vn`) សម្រាប់ domain ទាំងអស់ — ពេលបំបែក ត្រូវបំបែក migration history ដែរ
- (−) Scale ទាំងអស់រួមគ្នា — `catalog` (read-heavy) និង `order` (write) មិនអាច scale ដាច់ — ទទួលយកនៅ Phase A
