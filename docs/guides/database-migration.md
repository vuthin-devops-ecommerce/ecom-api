# Database migration (Flyway)

> **គោលបំណង:** ប្តូរ schema ដោយសុវត្ថិភាព · **អ្នកអាន:** developer · **Update:** 2026-09-30
> Schema បច្ចុប្បន្ន: [architecture.md §៥](../architecture.md) · ហេតុផល FK: [ADR-002](../decisions/002-order-schema-fk.md)

## ច្បាប់

1. **Flyway គ្រប់គ្រង schema តែមួយ** — `spring.jpa.hibernate.ddl-auto: validate` ជានិច្ច, **មិនដែល** `update` (CLAUDE.md §5.4)។ Entity ខុសពី DB → app មិន start (ល្អ: ចាប់មុន production)។
2. Migration ដែល apply រួច **មិនកែ** (checksum mismatch → app មិន start គ្រប់ environment)។ ប្តូរ = file ថ្មី `V<n+1>`។
3. Migration ត្រូវ **backward-compatible** មួយ release: rolling update run pod ចាស់ + ថ្មីលើ DB ដដែល។ លុប/ប្តូរឈ្មោះ column = ២ release (expand → contract)។
4. Seed dev នៅ `db/dev/R__*.sql` (repeatable, profile `dev` ប៉ុណ្ណោះ) — មិនដាក់ data dev ក្នុង `db/migration`។

## ជំហាន

```bash
ls mini-shop/src/main/resources/db/migration/      # V1__create_products.sql  V2__create_orders.sql
```

1. បង្កើត `V3__<verb>_<what>.sql` (ឧ. `V3__add_product_description.sql`) — lowercase, underscore ២ បន្ទាប់ version។
2. សរសេរ SQL (PostgreSQL 17)។ Constraint (CHECK/FK/UNIQUE) ដាក់ក្នុង DB មិនមែនតែក្នុង Java។
3. Update entity (`@Column`) ឱ្យត្រូវ — `validate` នឹងពិនិត្យ។
4. Test លើ Postgres ពិត:

   ```bash
   cd mini-shop && ./mvnw verify        # Testcontainers migrate V1..V3 លើ DB ថ្មី + IT
   ```

5. Compose: `docker compose up --build` (migrate លើ volume ដែលមាន V1–V2 → apply តែ V3)។
6. PR → CI → release ([guides/release.md](release.md)) — pod ថ្មី migrate ពេល start។

## ពិនិត្យស្ថានភាព

```bash
# compose
docker compose exec db psql -U minishop -d minishop -c 'select version, description, success from flyway_schema_history order by installed_rank'
# kind
kubectl -n minishop exec postgres-0 -- psql -U minishop -d minishop -c 'select version, description, success from flyway_schema_history order by installed_rank'
```

## Error ញឹកញាប់

| Error | មូលហេតុ | ធ្វើ |
|---|---|---|
| `Schema-validation: missing column [...]` | entity ប្តូរ គ្មាន migration | បន្ថែម `V<n+1>` |
| `Migration checksum mismatch for migration version N` | កែ file ដែល apply រួច | revert file; ប្តូរតាម file ថ្មី |
| `Found non-empty schema(s) "public" but no schema history table` | DB មាន table ដែល Flyway មិនស្គាល់ (ជួបពិត 2026-09-29: table លំហាត់) | dev: លុប object នោះ; **កុំ** `baselineOnMigrate` ដើម្បីលាក់ |
| `Detected failed migration to version N` | SQL ធ្លាក់ពាក់កណ្តាល | ជួសជុល DB ដោយដៃ + `flyway repair` (dev) ឬ restore (prod) |
