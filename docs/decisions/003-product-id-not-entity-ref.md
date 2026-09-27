# ADR-003: `OrderItem.productId` ជា `Long` មិនមែន `@ManyToOne Product`

| | |
|---|---|
| **ស្ថានភាព** | Accepted |
| **ថ្ងៃ** | 2026-09-27 |
| **Code** | `mini-shop/src/main/java/com/devops/minishop/order/OrderItem.java`, `OrderService.java` |
| **ពាក់ព័ន្ធ** | ADR-001 (modular monolith), ADR-002 (FK `product_id` នៅ DB level) |

## បរិបទ

JPA "ធម្មតា" នឹង map `order_items.product_id` ជា `@ManyToOne Product product` — ងាយស្រួល (`item.getProduct().getName()`)
តែវាភ្ជាប់ `order` package ទៅ entity របស់ `catalog` ដោយផ្ទាល់។ Phase B នឹងបំបែក `order`/`payment` ជា service ដាច់
ដែល**គ្មាន** class `Product` និងគ្មាន table `products` ក្នុង DB របស់វា។

## ការសម្រេចចិត្ត

1. `OrderItem.productId` ជា `Long` ធម្មតា (`@Column(name = "product_id")`)។
2. `order` package ហៅ `catalog` តាម **`ProductService` ប៉ុណ្ណោះ** ហើយទទួលតែ DTO (`ProductResponse`) — មិនដែលឃើញ `Product` entity ឬ `ProductRepository`។
3. ទិន្នន័យ product ដែល order ត្រូវការ (តម្លៃ) ត្រូវ**ចម្លង**ចូល `OrderItem.priceAtOrder` នៅពេលបង្កើត (ADR-002) — មិន lazy-load ពី product ក្រោយ។
4. FK `product_id → products` នៅ DB (ADR-002) នៅតែមានក្នុង monolith ជា safety net — វាជា**ការសម្រេចចិត្ត DB** មិនមែន JPA; JPA មិនដឹងពី FK នោះទេ។

## ផលវិបាក

- (+) Phase B: ដក `catalog` ចេញ → `order` compile បានដដែល; ប្តូរ `ProductService` ជា HTTP client / event consumer ដោយ `OrderService` មិនប្តូរ interface
- (+) គ្មាន N+1 / lazy-loading surprise ពី `item.getProduct()`; `OrderResponse` មិនអាស្រ័យ catalog
- (−) `OrderResponse` មិនមានឈ្មោះ product — client ត្រូវហៅ `/api/products/{id}` ដាច់ ឬ Phase B ប្រើ API composition; ការដោះដូរនេះទទួលយកដោយចេតនា
- (−) Java មិនអាចធានា `productId` មាន — ពឹងលើ `productService.getById()` (404) ក្នុង `createOrder` និង FK នៅ DB (monolith ប៉ុណ្ណោះ)
- Test: `OrderServiceTest` mock `ProductService` (មិនមែន repository) — ភស្តុតាងថា boundary ត្រឹមត្រូវ
