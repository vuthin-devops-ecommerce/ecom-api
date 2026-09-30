# ADR-004: រចនាសម្ព័ន្ធ CI job — unit-test និង integration-test ដាច់ពីគ្នា

| | |
|---|---|
| **ស្ថានភាព** | Proposed (ក្លាយជា Accepted ពេលអ្នករៀនឆ្លើយសំណួរខាងក្រោម និង CI run បៃតង) |
| **ថ្ងៃ** | 2026-09-28 |
| **Code** | `.github/workflows/ci.yml` (job `unit-test`, `integration-test`), `mini-shop/pom.xml` (property `skipUnitTests`) |
| **ពាក់ព័ន្ធ** | ADR-005 (security scan policy — job `docker` នឹង `needs` job ទាំង ២ នេះ), phase-a2-plan Task 2 & 5 |

## បរិបទ

Phase A2 Task 2 ត្រូវឱ្យ `OrderControllerIT` (Testcontainers + `postgres:17`) run ក្នុង CI។ សំណួរ: ដាក់វាក្នុង job តែមួយ
ជាមួយ unit test ឬបំបែកជា job ដាច់?

ទិន្នន័យវាស់ពិត (2026-09-28, laptop):

| ផ្នែក | ពេលវេលា | ត្រូវការ |
|---|---|---|
| unit test (`./mvnw test`, 6 test) | ~15s ក្រោយ compile | Java ប៉ុណ្ណោះ |
| integration test (`OrderControllerIT`, 3 test) | ~27s (pull image + boot Spring + Flyway) | Docker daemon + image `postgres:17` |
| checkout + setup-java + cache restore (ក្នុង CI) | ~10s ក្នុង ១ job | — |

ច្បាប់ ៣ ក្នុង `docs/journey/phase-a2/plan.md`: **fail fast**, **reproducible**, **least privilege**។ Task 5 ត្រូវការឈ្មោះ status check
ជាក់លាក់សម្រាប់ branch protection។ Task 3 ត្រូវការ job `docker` ដែល run តែពេល test ទាំងអស់ឆ្លង។

## ជម្រើសដែលពិចារណា

| ជម្រើស | ហេតុផលបដិសេធ / ទទួល |
|---|---|
| **A. Job តែមួយ** — `./mvnw verify` (unit → IT ក្នុង step ដដែល) | សាមញ្ញ, VM ១, checkout ១ (~10s ថោកជាង)។ Step ក៏ fail fast ដែរ (surefire ធ្លាក់ → Maven ឈប់មុន failsafe)។ ❌ តែ status check តែ ១ ("build") → branch protection មិនអាចបែងចែក "unit ខូច" ពី "IT ខូច"; report ២ ប្រភេទលាយក្នុង artifact ១; ពេលក្រោយបន្ថែម job (lint, docker) ត្រូវ `needs` job ធំ ១ ដែលយឺត |
| **B. ២ job, `integration-test` `needs: unit-test`** (sequential) | ✅ signal ច្បាស់: ក្រហមនៅ job ណា ដឹងភ្លាមថាបញ្ហានៅ logic (unit) ឬ DB/schema/HTTP (IT)។ Status check ២ ឈ្មោះសម្រាប់ Task 5។ Unit ខូច → IT skip → មិនចំណាយ Docker ឥតប្រយោជន៍។ ❌ តម្លៃ: VM ២, checkout + compile ២ ដង (~20s បន្ថែម); ពេលសរុបយឺតជាង A បន្តិចពេលទាំងអស់ឆ្លង |
| C. ២ job **parallel** (គ្មាន `needs`) | លឿនបំផុតពេលទាំងអស់ឆ្លង (max(unit, IT) ជំនួស unit + IT)។ ❌ unit ខូច ក៏ IT នៅតែ run — រំលោភ fail fast; ចំណាយ Docker + pull image ដោយដឹងថា code ខូចរួច |
| D. `services: postgres:17` របស់ GitHub Actions ជំនួស Testcontainers | មិនត្រូវការ Docker-in-job, DB ចាប់ផ្តើមមុន step។ ❌ version DB pin ក្នុង YAML ដាច់ពី test code → laptop (Testcontainers) និង CI (services) ខុសគ្នា = រំលោភ reproducible; test មិន run បានលើ laptop ដោយគ្មាន DB ដោយដៃ។ Testcontainers = test **ខ្លួនឯង**ប្រកាស dependency របស់វា |

## ការសម្រេចចិត្ត

1. **ជម្រើស B**: job `unit-test` (`./mvnw -B test`) និង job `integration-test` (`./mvnw -B verify -DskipUnitTests`, `needs: unit-test`)។
2. ឈ្មោះ job `unit-test` / `integration-test` ជា **contract** ជាមួយ branch protection (Task 5) — ប្តូរឈ្មោះ = ត្រូវប្តូរ setting ដែរ។
3. Property `skipUnitTests` កំណត់ក្នុង `pom.xml` (default `false`) ហើយ wire ចូល surefire `<skipTests>` — ព្រោះ surefire គ្មាន property "រំលង unit ប៉ុណ្ណោះ" ស្រាប់ (`skipTests` រំលង failsafe ដែរ)។ លើ laptop `./mvnw verify` នៅតែ run ទាំង ២។
4. Testcontainers រក្សាទុក (មិនប្តូរទៅ `services:`) — DB version pin នៅ**ក្នុង test code** ដដែលនឹង laptop។
5. Report បំបែក: artifact `surefire-reports` (unit) និង `failsafe-reports` (IT)។

## ផលវិបាក

- (+) CI ក្រហម → job ឈ្មោះប្រាប់ថាមើលអ្វីមុន (runbook `ci-failure.md` Task 6 នឹងយោងឈ្មោះ job ទាំងនេះ)
- (+) Task 3 `docker` job: `needs: [unit-test, integration-test]` — image build តែពេលទាំង ២ បៃតង
- (+) ថ្ងៃក្រោយបន្ថែម job លឿន (lint, dependency check) ជា sibling របស់ `unit-test` បានដោយមិនប៉ះ IT
- (−) compile ២ ដង (VM នីមួយៗ) — ទទួលយក: ~20s លើ project តូច; ជម្រើសកែក្រោយ: upload `target/classes` ជា artifact រវាង job (ស្មុគស្មាញ មិនសមតម្លៃឥឡូវ)
- (−) ពេលសរុប (ទាំងអស់ឆ្លង) = unit + IT sequential — ទទួលយកព្រោះ fail fast សំខាន់ជាង ~20s
- (−) Testcontainers pull `postgres:17` រាល់ run (runner ថ្មី, គ្មាន image cache) — វាស់ក្នុង tracker; បើយឺតពេក ជម្រើសក្រោយ: Docker layer cache ឬ image pre-pull

## សំណួរសម្រាប់អ្នករៀន (ឆ្លើយក្នុង `docs/journey/phase-a2/learning-log.md` មុនប្តូរ status ជា Accepted)

1. ជម្រើស A ក៏ fail fast នៅកម្រិត step ដែរ (Maven ឈប់ពេល surefire ធ្លាក់)។ ដូច្នេះអត្ថប្រយោជន៍**ពិត**របស់ B គឺអ្វី — លឿនជាង ឬ ច្បាស់ជាង?
2. បើគម្រោងមាន IT 200 test (10 នាទី) តើអ្នកនៅជ្រើស B ឬប្តូរទៅ C? ហេតុអ្វី?
3. `services: postgres:17` (D) ក៏ pin version ដែរ — ហេតុអ្វីនៅតែថា "មិន reproducible"? (hint: test run លើ laptop យ៉ាងណា?)
