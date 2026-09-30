# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## ១. តើ repo នេះជាអ្វី

`DevOps-journey` គឺជា repo **monorepo** សម្រាប់ដំណើររៀន DevOps ជាជំហានរបស់អ្នកប្រើ — មាន docs (`docs/*.md`)
ព្រមទាំង app code។ Repo ត្រូវបាន `git init` រួច (remote `origin`, branch `main`)។

Code នឹងកើតឡើងនៅ Phase A Task 0 ជាគម្រោង Spring Boot ឈ្មោះ `mini-shop` (បង្កើតពី start.spring.io)។
**សម្រេចរួច:** `mini-shop/` ដាក់ជា subfolder ក្នុង repo នេះ (មិនមែន repo ដាច់ដោយឡែក)។

ឯកសារទាំងអស់សរសេរជា**ភាសាខ្មែរ** ដោយចេតនា។ រក្សាទម្លាប់នេះ: prose ជាខ្មែរ, technical identifier (path, command, class name, YAML key) ជាអង់គ្លេស។

---

## ២. កិច្ចសន្យាបង្រៀន (ច្បាប់សំខាន់បំផុត — ធ្លាក់ក្នុងឯកសារទាំង ៥)

អ្នកប្រើ **កំពុងរៀន** DevOps។ គោលដៅមិនមែនបញ្ចប់លឿន គឺយល់ជ្រៅ។ ដូច្នេះ:

| ត្រូវធ្វើ | មិនត្រូវធ្វើ |
|---|---|
| ផ្តល់**គ្រោង** (skeleton/outline) ឱ្យអ្នកប្រើបំពេញ | សរសេរ file ពេញលេញជំនួស |
| Review code/SQL/YAML/HCL របស់អ្នកប្រើ ចង្អុលបញ្ហា | កែជំនួសដោយស្ងាត់ |
| ពន្យល់ "ហេតុអ្វី" មុន "យ៉ាងណា" | ផ្តល់ចម្លើយចុងក្រោយភ្លាម |
| ណែនាំម្តងមួយជំហាន រង់ចាំអ្នកប្រើសាកល្បង | ធ្វើ Task ច្រើនក្នុងវេនតែមួយ |
| ជួយអាន error តាមលំដាប់ (`describe` → `logs` → `events`) | ទាយមូលហេតុដោយមិនមើល output ពិត |

**របៀបធ្វើការដែលអ្នកប្រើជ្រើស (2026-09-27, ចាប់ពី Task 1):** Claude សរសេរ app code ក្នុង `mini-shop/` ជា file ពេញ
ហើយ**ពន្យល់ការសម្រេចចិត្តរចនានីមួយៗ + សួរសំណួរ review**; អ្នកប្រើអាន review និងសួរ។ ការធ្វើបែបនេះជំនួសជួរ "ផ្តល់គ្រោង" ខាងលើ
— រួមទាំង Task 2 SQL និង ADR (អ្នកប្រើសុំ "plan, think, build it" នៅ Task 2)។ **សំណួរឆ្លុះបញ្ចាំង** ក្នុង learning-log នៅតែជារបស់អ្នកប្រើ។

ចំណុចដែលឯកសារបញ្ជាក់ដោយផ្ទាល់:
- Phase A Task 2 (order schema SQL): "**កុំសរសេរជំនួស**"
- Phase A2: "កុំសរសេរ YAML ទាំងអស់ជំនួស"
- Phase A3: "**កុំ generate manifest ទាំងអស់ជំនួស**"
- Phase A4: "**កុំ generate `.tf` ទាំងអស់ជំនួស**" — ផ្តល់គ្រោង រួច review `terraform plan` ជាមួយគ្នា

**សំណួរឆ្លុះបញ្ចាំង** ដែលរាយក្នុងផែនការនីមួយៗ ជាផ្នែកនៃមេរៀន — រំលឹកអ្នកប្រើឆ្លើយក្នុង `docs/journey/phase-<x>/learning-log.md` **កុំឆ្លើយជំនួស**។ Symptom ពិត + command + លទ្ធផល → `docs/journey/phase-<x>/worklog.md` និង `docs/runbooks/` មិនមែន learning-log។ ឯកសារ product (architecture, guides, reference) ដាច់ពីឯកសាររៀន (journey) — មើល `docs/README.md`។

---

## ៣. Source of truth និងជម្លោះដែលដឹងហើយ

1. **`docs/reference/tech-stack.md` ឈ្នះជានិច្ច** សម្រាប់ version របស់ tool។ កុំ upgrade major version ណាមួយដោយគ្មាន ADR ថ្មីក្នុង `docs/decisions/`។
2. ⚠️ **Version drift:** A3 plan កែរួច 2026-09-29 (`postgres:17`, ingress/metrics-server/kind pin)។ `docs/journey/phase-a4/plan.md` នៅមាន `latest` (បន្ទាត់ ~60, ~436) និង `postgres:16` (~121) — កែពេលចាប់ផ្តើម A4។ តម្លៃត្រឹមត្រូវគឺ **Spring Boot 4.1.x / `postgres:17`** តាម tech-stack។
3. File ដែលឯកសារយោង តែ**មិនទាន់មាន**: ADR 007–009, runbooks `k8s-pod-not-ready.md`, `k8s-rollback.md`, `terraform-*.md`។ បង្កើតនៅពេលដល់ Task របស់វា មិនមែនមុន។ (មានរួច 2026-09-30: ADR 001–006, `runbooks/ci-failure.md`, `scripts/check-env.sh`, `scripts/kind-trust-ca.sh`, `docs/journey/phase-{a,a2,a3}/{plan,worklog,learning-log}.md`, `docs/{README,architecture}.md`, `docs/guides/`, `docs/reference/`។)

---

## ៤. ច្បាប់សរុបនៃគម្រោង (ឆ្លងគ្រប់ Phase)

- **គ្មាន tag `latest`** — pin version គ្រប់ទីកន្លែង (`pom.xml`, maven-wrapper, `postgres:17`, kindest/node, Helm chart, GitHub Action, Trivy action)។ នេះជា pattern ដដែលដែលធ្វើម្តងហើយម្តងទៀតគ្រប់ Phase ដោយចេតនា។
- **One tool per stage** — ដំណាក់នីមួយៗបន្ថែម tool ថ្មី**តែមួយ** ដើម្បីឱ្យពេលអ្វីខូច ដឹងភ្លាមថា tool ណាបង្ក។ **កុំបន្ថែម tool មុនដំណាក់របស់វា។**
- **គ្មាន secret ក្នុង code ឬ git** — `${ENV_VAR}` ក្នុង `application.yml` (គ្មាន default សម្រាប់ password), `k8s/**/secret.yaml` និង `*.tfvars` ត្រូវ gitignore + commit `.example` ជំនួស។
- **ការសម្រេចចិត្តរចនា → ADR** ក្នុង `docs/decisions/NNN-slug.md`។ លេខបានកក់ទុករួច:
  `001` modular-monolith · `002` order-schema-fk · `003` product-id-not-entity-ref · `004` ci-job-structure · `005` security-scan-policy · `006` postgres-statefulset-vs-managed · `007` rolling-update-strategy · `008` terraform-scope-platform-not-app · `009` state-separation-and-backend
- **រោគសញ្ញាដែលជួបពិត → runbook** ក្នុង `docs/runbooks/` (`ci-failure.md`, `k8s-pod-not-ready.md`, `k8s-rollback.md`, `terraform-drift.md`, `terraform-state-recovery.md`)។
- ផែនការនីមួយៗបញ្ចប់ដោយ **Progress tracker** — ធ្វើបច្ចុប្បន្នភាព ⬜ → ✅ ពេល Task ចប់ ព្រមទាំងលេខវាស់ (ពេល CI run, ចំនួន CVE, វិនាទីដាច់ traffic)។

---

## ៥. ស្ថាបត្យកម្ម app (`mini-shop`) — ច្បាប់មិនអាចរំលោភ

ពី `docs/journey/phase-a/plan.md`:

1. **Package តាម domain មិនមែនតាម layer:** `com.devops.minishop.{catalog,order,common}`
2. **`catalog` និង `order` មិន import entity/repository របស់គ្នាទៅវិញទៅមក** — ឆ្លងកាត់ Service ប៉ុណ្ណោះ (`OrderService` ហៅ `ProductService` **មិនមែន** `ProductRepository`)
3. **`OrderItem.productId` ជា `Long` ធម្មតា មិនមែន `@ManyToOne Product`** — ត្រៀមបំបែក `payment`/`order` ជា service ដាច់នៅ Phase B
4. **Flyway គ្រប់គ្រង schema** — `ddl-auto: validate` ជានិច្ច, **មិនដែល** `update`
5. **Controller គ្មាន business logic** — map request → service → response ប៉ុណ្ណោះ
6. **`price` ជា `BigDecimal`** មិនដែល `double` សម្រាប់លុយ; `price_at_order` **ចម្លង**តម្លៃ មិន reference `products.price`
7. **រាល់ business rule ត្រូវមាន test** — ជាពិសេស: stock មិនគ្រប់ → ពិនិត្យ**ទាំងអស់មុន** កាត់ stock ណាមួយ, ហើយ integration test ត្រូវបញ្ជាក់ថា stock ក្នុង DB **មិនប្រែ** (rollback ពិត)

---

## ៦. ពាក្យបញ្ជា

**Phase A ចប់ (2026-09-27):** code ក្នុង `mini-shop/` — ពាក្យបញ្ជា (ពី `docs/reference/tech-stack.md` §៤–៥ និង `README.md`):

```bash
cd mini-shop && cp .env.example .env && docker compose up --build   # app + postgres:17 (Task 5)
docker compose down        # រក្សា data · down -v លុប data
```

```bash
# Database (Stage 0: មានតែ DB ក្នុង container)
docker run -d --name minishop-db \
  -e POSTGRES_DB=minishop -e POSTGRES_USER=minishop -e POSTGRES_PASSWORD=minishop \
  -p 5432:5432 postgres:17

export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minishop   # គ្មាន default ក្នុង yml — ទាំង ៣ ត្រូវ set
export SPRING_DATASOURCE_USERNAME=minishop
export SPRING_DATASOURCE_PASSWORD=minishop
./mvnw spring-boot:run                    # app run manual លើ laptop (ឬ source mini-shop/env.sh)

./mvnw test                               # unit test (*Test.java, surefire)
./mvnw verify                             # + integration test (*IT.java, failsafe + Testcontainers)
./mvnw verify -DskipITs                   # unit ប៉ុណ្ណោះ (CI job ដំបូង)
./mvnw test -Dtest=OrderServiceTest        # test class តែមួយ
./mvnw test -Dtest=OrderServiceTest#createOrder_insufficientStock   # method តែមួយ

curl localhost:8080/actuator/health        # → {"status":"UP"}
```

`./mvnw` (wrapper) ជានិច្ច — **មិនមែន** `mvn` ដែលតម្លើងលើម៉ាស៊ីន — ដើម្បីឱ្យ Maven version ដូចគ្នាលើ laptop និង CI។
(ផែនការចាស់ខ្លះសរសេរ `mvn` ទទេ — docs/reference/tech-stack.md បញ្ជាក់ wrapper។)

---

## ៧. ផែនទី Phase — tool ណាមកពេលណា

| Phase | ឯកសារ | Tool ថ្មី | លទ្ធផលសំខាន់ |
|---|---|---|---|
| Stage 0 | `docs/reference/tech-stack.md` | Java 21, Maven, Spring Boot 4.1.x, PostgreSQL 17, Flyway, Actuator | baseline "ស្អាត" សម្រាប់ប្រៀបធៀប |
| A | `docs/journey/phase-a/plan.md` | Testcontainers, Docker + Compose | modular monolith `catalog` + `order`, `docker compose up --build` |
| A2 | `docs/journey/phase-a2/plan.md` | GitHub Actions, ghcr.io, Trivy | push → build/test/scan/push image ស្វ័យប្រវត្តិ, branch protection |
| A3 | `docs/journey/phase-a3/plan.md` | kind, kubectl, ingress-nginx, metrics-server, Kustomize, k6 | compose → K8s, probes, rolling update downtime=0, HPA |
| A4 | `docs/journey/phase-a4/plan.md` | Terraform ≥1.9, tflint, Helm provider | cluster + platform ជា code, module + បំបែក state, drift detection |
| B | (មិនទាន់សរសេរ) | Helm, External Secrets/Vault, contract test | បំបែក `payment` ជា service ដាច់ |

**ព្រំដែនស្រទាប់ (ADR-008):** Terraform ទទួលខុសត្រូវ **platform + infrastructure** (cluster, ingress-nginx, namespace, secret, VM, DNS) — **មិន** deploy `minishop-app` Deployment។ App deploy តាម `kubectl apply -k` (ក្រោយ: ArgoCD)។

**ខ្សែស្រឡាយឆ្លង Phase** (មានប្រយោជន៍ពេលឆ្លើយសំណួរ "ហេតុអ្វី"): pin version → `postgres:16`(A) → Helm chart version(A4) → `kindest/node`(A4); secret → `.env`(A) → GitHub secret(A2) → K8s Secret(A3) → Terraform var + state(A4) → Vault(B); deploy ដោយដៃ → `set image`(A3) → GitOps(E)។

---

## ៨. ការនាំចូល config ពី agent ផ្សេង

រកឃើញ `~/.codex/` លើម៉ាស៊ីននេះ។ បើចង់នាំចូល (MCP servers, slash commands, subagents, skills, instructions) ទៅ Claude Code:
ឆ្លើយ `/import` ដើម្បី scan និងមើលបញ្ជីអ្វីដែលនាំចូលបាន រួច `/import --yes=<digest>` (digest មកពី output នៃ scan) ដើម្បីអនុវត្ត។
