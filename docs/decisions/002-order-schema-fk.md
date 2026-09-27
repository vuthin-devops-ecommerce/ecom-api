# ADR-002: Order schema — FK behaviour និង price snapshot

| | |
|---|---|
| **ស្ថានភាព** | Accepted |
| **ថ្ងៃ** | 2026-09-27 |
| **Migration** | `mini-shop/src/main/resources/db/migration/V2__create_orders.sql` |
| **ពាក់ព័ន្ធ** | ADR-001 (modular monolith), ADR-003 (productId មិនមែន entity ref) |

## បរិបទ

`order` domain ត្រូវការតារាង `orders` និង `order_items`។ `order_items` មាន FK ២ ដែលមាន lifecycle ខុសគ្នា:
- `order_id → orders` — item ជាផ្នែកមួយនៃ order (composition); គ្មាន order ក៏គ្មានន័យ
- `product_id → products` — item ជា**ប្រវត្តិ**អំពី product (invoice); product អាចប្តូរតម្លៃ ឬត្រូវដកចេញពី catalog ក្រោយមក

Phase B នឹងបំបែក `order` ជា service ដាច់ → FK ទៅ `products` នឹង**មិនអាច**មាននៅ DB level ទៀត។

## ការសម្រេចចិត្ត

| ចំណុច | ជម្រើស | ហេតុផល |
|---|---|---|
| `order_items.order_id` | `ON DELETE CASCADE` | item ជា composition — លុប order = លុប line ទាំងអស់; `RESTRICT` បង្ខំលុប ២ ជំហានដោយគ្មានប្រយោជន៍ |
| `order_items.product_id` | `ON DELETE RESTRICT` | product ដែលធ្លាប់លក់ **មិនអាច hard-delete** — បើលុប invoice ចាស់នឹងខូច (accounting/audit)។ ក្រោយមកប្រើ soft-delete (`active` flag) ជំនួស |
| `price_at_order` | **ចម្លង**តម្លៃ (`NUMERIC(10,2)`) មិន join `products.price` | តម្លៃ product ប្តូរ តែ invoice មិនប្តូរ; ក៏ជាការត្រៀម Phase B ដែល join ឆ្លង service មិនអាចធ្វើបាន |
| `total_amount` | `NUMERIC(12,2)` | ធំជាង `price(10,2)` ព្រោះជាផលបូក — 10 line × 99,999,999.99 លើស (10,2) |
| `status` | `VARCHAR(20)` + `CHECK IN ('CREATED','PAID','CANCELLED')` | invariant នៅ DB តែបន្ថែម status ថ្មី = migration ១ ធម្មតា; PG `ENUM` type កែពិបាក (`ALTER TYPE`, មិន transactional ក្នុង PG ចាស់) |
| `UNIQUE (order_id, product_id)` | ១ line ក្នុង ១ order សម្រាប់ product ១ | ការពារ stock ត្រូវកាត់ ២ ដងសម្រាប់ product ដដែល; Task 3 ត្រូវបដិសេធ request ដែលមាន `productId` ស្ទួន (400) ឬ merge quantity។ Index នេះក៏ serve lookup "items by order" (leading column) |
| `INDEX (product_id)` | បង្កើតដោយដៃ | PostgreSQL **មិន** index FK column ស្វ័យប្រវត្តិ; `RESTRICT` check និង query "order ណាមាន product X" ត្រូវការ |
| ឈ្មោះ constraint | ដាក់ឈ្មោះច្បាស់ (`fk_…`, `chk_…`, `uq_…`) | error message អានបាន (`violates check constraint "chk_order_items_quantity"`) ជំនួសឈ្មោះ auto |

## ផលវិបាក

- (+) ទិន្នន័យ order ជា immutable history — តម្លៃ/product ប្តូរមិនប៉ះពាល់ order ចាស់
- (+) DB បដិសេធ order ខូច (quantity ≤ 0, status មិនស្គាល់, product មិនមាន) ដោយមិនពឹងលើ app code
- (−) `DELETE FROM products` fail បើមាន order history → ត្រូវការ soft-delete ក្នុង catalog នាពេលក្រោយ
- (−) status ថ្មី = migration (មិនមែន code-only)
- (Phase B) FK `product_id → products` **បាត់** ពេល `order` ក្លាយជា service ដាច់ — integrity ផ្លាស់ទៅ application (validate តាម `ProductService`/HTTP)។ ADR-003 ត្រៀម JPA side (`productId` ជា `Long`) ឱ្យការបំបែកនេះមិនប៉ះ entity
