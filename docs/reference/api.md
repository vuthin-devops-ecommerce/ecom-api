# API reference

> **គោលបំណង:** endpoint, request body, status code · **អ្នកអាន:** client developer, tester · **Update:** 2026-09-30
> Interactive + schema ពេញ: **Swagger UI** `/swagger-ui.html` · OpenAPI JSON `/v3/api-docs` (springdoc)។

Base URL: `http://localhost:8080` (laptop/compose/port-forward) · `http://minishop.local` (kind Ingress)។ Content-Type: `application/json`។

## Products — `/api/products`

| Method | Path | Body | Success | Error |
|---|---|---|---|---|
| `POST` | `/api/products` | `CreateProductRequest` | **201** `ProductResponse` | 400 validation · 409 SKU ស្ទួន |
| `GET` | `/api/products` | — | 200 `ProductResponse[]` | — |
| `GET` | `/api/products/{id}` | — | 200 `ProductResponse` | 404 |
| `PATCH` | `/api/products/{id}/stock` | `AdjustStockRequest` | 200 `ProductResponse` | 400 stock នឹងក្រោម 0 · 404 |

```json
// CreateProductRequest
{ "sku": "SKU-100", "name": "Widget", "price": 9.99, "stock": 5 }
//  sku   : required, ≤ 50, unique
//  name  : required, ≤ 200
//  price : required, ≥ 0.00, ≤ 8 digit + 2 decimal
//  stock : required, ≥ 0

// AdjustStockRequest
{ "delta": -2 }            // required; +/−; លទ្ធផល < 0 → 400

// ProductResponse
{ "id": 10, "sku": "SKU-100", "name": "Widget", "price": 9.99, "stock": 5, "createdAt": "2026-09-30T00:51:28.039" }
```

## Orders — `/api/orders`

| Method | Path | Body | Success | Error |
|---|---|---|---|---|
| `POST` | `/api/orders` | `CreateOrderRequest` | **201** `OrderResponse` | 400 stock មិនគ្រប់ (**គ្មាន stock ណាត្រូវកាត់**) · 400 productId ស្ទួន · 400 validation · 404 product |
| `GET` | `/api/orders/{id}` | — | 200 `OrderResponse` | 404 |

```json
// CreateOrderRequest
{ "items": [ { "productId": 1, "quantity": 2 }, { "productId": 2, "quantity": 1 } ] }
//  items     : required, មិនទទេ
//  productId : required, មិនស្ទួនក្នុង order
//  quantity  : required, ≥ 1

// OrderResponse
{ "id": 1, "status": "CREATED", "totalAmount": 25.00, "createdAt": "…",
  "items": [ { "productId": 10, "quantity": 2, "priceAtOrder": 12.50, "lineTotal": 25.00 } ] }
```

`priceAtOrder` = តម្លៃ product **ពេល order** (ចម្លង) — ប្តូរតម្លៃ product ក្រោយមិនប៉ះ order ចាស់។

## Error format — RFC 9457 ProblemDetail

```json
{ "type": "about:blank", "title": "Bad Request", "status": 400,
  "detail": "Product 10: requested 99, available 3", "instance": "/api/orders" }
// validation: + "errors": { "<field>": "<message>" }   detail = "Validation failed"
```

| Status | មកពី |
|---|---|
| 400 | `InsufficientStockException`, `BadRequestException`, `MethodArgumentNotValidException` |
| 404 | `NotFoundException` |
| 409 | `ConflictException` (SKU ស្ទួន) |
| 500 | អ្វីផ្សេង — `"Unexpected error"` (stack trace មិនចេញទៅ client; មើល log) |

Mapping: `mini-shop/src/main/java/com/devops/minishop/common/GlobalExceptionHandler.java`។

## Operational endpoints (Actuator)

`/actuator/health` · `/actuator/health/liveness` · `/actuator/health/readiness` · `/actuator/info` · `/actuator/metrics` · `/actuator/prometheus` — មើល [architecture.md §៧](../architecture.md)។

## ឧទាហរណ៍ curl

```bash
B=localhost:8080/api; H='Content-Type: application/json'
curl -H "$H" -d '{"sku":"SKU-100","name":"Widget","price":9.99,"stock":5}' $B/products     # 201
curl -H "$H" -d '{"items":[{"productId":1,"quantity":2}]}' $B/orders                      # 201
curl -H "$H" -d '{"items":[{"productId":1,"quantity":999}]}' $B/orders                    # 400, stock មិនប្រែ
```
