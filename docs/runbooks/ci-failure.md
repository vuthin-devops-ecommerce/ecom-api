# Runbook: CI ក្រហម — ត្រូវមើលអ្វីតាមលំដាប់

> សម្រាប់ workflow `.github/workflows/ci.yml` (Phase A2)។ រាល់ symptom ខាងក្រោម**ជួបពិត** ក្នុង repo នេះ (ថ្ងៃក្នុងវង់ក្រចក)។
> ច្បាប់: អាន output ពិតតាមលំដាប់ — **កុំទាយ** មូលហេតុមុនឃើញ log។

## ០. លំដាប់ ៣០ វិនាទីដំបូង

1. **Job ណាក្រហម?** ឈ្មោះ job ប្រាប់ស្រទាប់បញ្ហា:
   | Job | បញ្ហានៅ | មិនមែន |
   |---|---|---|
   | `unit-test` | logic Java, compile, test assertion | DB, Docker, network |
   | `integration-test` | DB/schema (Flyway), HTTP layer, Testcontainers/Docker | logic សុទ្ធ (unit ឆ្លងរួច) |
   | `docker` | Dockerfile, build context, registry auth, **CVE (Trivy)** | code (test ទាំង ២ ឆ្លងរួច) |
   | ទាំងអស់ skipped/ខូចមុន step ដំបូង | YAML syntax, permission, runner | code |
2. **Step ណាក្រហម ហើយចំណាយពេលប៉ុន្មាន?** — ធ្លាក់ក្នុង **< 2s** = មិនទាន់ធ្វើអ្វី (config/argument ខុស); ធ្លាក់ក្រោយ ~30s+ = ធ្វើការពិតរួចខូច (test, build, scan)។
3. **ពន្លា step ក្រហម → រកបន្ទាត់ `##[error]` ឬ `ERROR`** → ចម្លងបន្ទាត់នោះ (មិនមែន stack trace ទាំងមូល) → ផ្គូផ្គងនឹង §១–៥ ខាងក្រោម។
4. **Artifact:** `surefire-reports` / `failsafe-reports` upload ទោះ test ធ្លាក់ (`if: always()`) → file `.txt` មាន expected/actual ខ្លីជាង log។
5. បើមិនផ្គូផ្គងអ្វី → **reproduce លើ laptop** ជាមួយ command ដដែលនឹង step (§៦) មុនកែអ្វី។

---

## ១. `unit-test` ក្រហម

### 1a. Test assertion ធ្លាក់ (2026-09-28, លំហាត់)
```
[ERROR] OrderServiceTest.createOrder_stockAvailable_createsOrderAndDecreasesStock -- FAILURE!
expected: 99.99
 but was: 39.48
```
- **មូលហេតុ:** logic ឬ test ខុស — code ប្តូរ behaviour។
- **ធ្វើ:** download `surefire-reports` → file `<TestClass>.txt` → reproduce: `mvn -B test -Dtest=<TestClass>#<method>` → កែ code **ឬ** test (សួរ: behaviour ថ្មីត្រឹមត្រូវឬ?)។
- **កុំ:** `@Disabled` test ដើម្បីឱ្យបៃតង។

### 1b. `Failed to load ApplicationContext` ក្នុង unit test (2026-09-28)
```
MiniShopApplicationTests.contextLoads » IllegalState Failed to load ApplicationContext
Caused by: ... Error creating bean with name 'flyway' ... dataSource
```
- **មូលហេតុ:** test `@SpringBootTest` ត្រូវការ DB តែឈ្មោះ `*Test`/`*Tests` → surefire run វាជា unit test ក្នុង job ដែលគ្មាន DB (env `SPRING_DATASOURCE_*` គ្មាន default ដោយចេតនា)។ ឆ្លងលើ laptop ព្រោះ DB កំពុង run — មិនមែនព្រោះ test ត្រឹមត្រូវ។
- **ធ្វើ:** test ត្រូវការ DB → ប្តូរឈ្មោះជា `*IT` + Testcontainers (job `integration-test`) ឬលុបបើ IT ផ្សេងគ្របរួច។
- **Reproduce:** `env -u SPRING_DATASOURCE_URL -u SPRING_DATASOURCE_USERNAME -u SPRING_DATASOURCE_PASSWORD mvn -B test`។

### 1c. `./mvnw: Permission denied` (2026-09-28, ចាប់មុន push)
- **មូលហេតុ:** `mvnw` mode `100644` ក្នុង git (Windows មិនរក្សា exec bit)។
- **ធ្វើ:** `git update-index --chmod=+x mini-shop/mvnw` → commit។ **កុំ** `chmod +x` ក្នុង step CI (លាក់មូលហេតុ)។
- **ពិនិត្យ:** `git ls-files -s mini-shop/mvnw` → `100755`។

### 1d. Compile error
- Step ធ្លាក់ក្នុង phase `compile`/`testCompile`, artifact `surefire-reports` = warning "No files found" (ធម្មតា — មិនទាន់ដល់ test)។
- **ធ្វើ:** `mvn -B compile` លើ laptop — Java version ដូច CI (21)? `java -version`។

---

## ២. `integration-test` ក្រហម (unit ឆ្លងរួច)

### 2a. `Could not find a valid Docker environment`
- **មូលហេតុ:** Testcontainers រក Docker daemon មិនឃើញ — runner ខុស (macOS/windows) ឬ Docker service ខូច។
- **ធ្វើ:** បញ្ជាក់ `runs-on: ubuntu-24.04`; re-run job ម្តង (runner flake កម្រ)។

### 2b. Flyway / schema validate ខូច
```
Schema-validation: missing column [...]  ឬ  FlywayValidateException: Migration checksum mismatch
```
- **មូលហេតុ:** entity ប្តូរតែគ្មាន migration `V<n+1>` (ddl-auto `validate` ចាប់) ឬកែ migration ដែល apply រួច។
- **ធ្វើ:** បន្ថែម migration ថ្មី — **មិនកែ** file ចាស់ (README ច្បាប់ Flyway)។ Reproduce: `mvn -B verify -DskipUnitTests`។

### 2c. Unit test run ២ ដង / IT មិន run ទោះ step បៃតង (2026-09-28)
- **រោគសញ្ញា:** log job `integration-test` មាន `Tests run: 6` ពី surefire — flag `-Dsurefire.skip=true` **Maven មិនស្គាល់ ហើយមិនប្រាប់**។
- **ធ្វើ:** ប្រើ `-DskipUnitTests` (property ដែលកំណត់ក្នុង `pom.xml`) → log ត្រូវមាន `surefire ... Tests are skipped.` រួច `failsafe ... Tests run: 3`។ **មេរៀន:** flag ថ្មីត្រូវផ្ទៀងផ្ទាត់ក្នុង log ថាមានឥទ្ធិពល មិនមែនមើលតែ exit code។

---

## ៣. `docker` ក្រហម (test ទាំង ២ ឆ្លងរួច)

### 3a. `invalid tag "...:-tag-short-git-sha-7-..." : invalid reference format` — ធ្លាក់ក្នុង 1s (2026-09-28)
- **មូលហេតុ:** comment `# ...` នៅចុងបន្ទាត់**ក្នុង** YAML block scalar (`tags: |`) មិនមែន comment — ជាអត្ថបទ → metadata-action យកចូល tag។ actionlint មិនចាប់។
- **ធ្វើ:** ក្នុង block `|` ដាក់តែ value; comment ដាក់ខាងលើ block។ ពិនិត្យ log បន្ទាត់ `docker buildx build ... --tag` ឃើញ tag ពិត។

### 3b. Trivy: `Total: N (CRITICAL: N)` → `Process completed with exit code 1` (2026-09-29)
```
│ org.apache.tomcat.embed:tomcat-embed-core (app.jar) │ CVE-2026-65182 │ CRITICAL │ fixed │ 11.0.24 │ 11.0.25 ...
```
- **នេះមិនមែន error** — gate ធ្វើការ (ADR-005): step `Push image` skipped → registry មិនទទួល image នេះ។
- **ធ្វើ (តាមលំដាប់):**
  1. column `Status` = `fixed` + `Fixed Version` → **upgrade** dependency: version ដែល Spring Boot BOM គ្រប់គ្រង → override property ក្នុង `pom.xml` (ឧ. `<tomcat.version>11.0.25</tomcat.version>`); dependency ផ្ទាល់ → ប្តូរ version; OS package → base image tag ថ្មី។
  2. Reproduce លើ laptop **ដោយមិន build image** (build local ធ្វើមិនបានលើ network ការិយាល័យ — §៥): `mvn -B package -DskipTests` → `trivy fs --scanners vuln target/*.jar` → ត្រូវ `Clean`។
  3. PR → merge → run លើ `main` → `Total: 0` → push។
- **កុំ:** `exit-code: "0"`, ដក `severity`, ឬ `.trivyignore` ដោយគ្មាន issue + ថ្ងៃផុត។ ករណី CVE `affected` (គ្មាន fix) → `ignore-unfixed: true` មិនរាប់រួច; បើនៅតែ block = CVE មាន fix ពិត។

### 3c. `denied: permission_denied` / `unauthorized` ពេល push
- **មូលហេតុ:** job គ្មាន `permissions: packages: write` ឬ package ជាប់នឹង repo ផ្សេង/owner ខុស (ghcr ត្រូវការ owner អក្សរតូច)។
- **ធ្វើ:** ពិនិត្យ `permissions:` នៅកម្រិត job `docker`, `images: ghcr.io/${{ github.repository_owner }}/mini-shop`; Settings → Packages → package → Manage Actions access។

### 3d. `COPY failed: file not found` / `pom.xml not found`
- **មូលហេតុ:** build context ខុស — monorepo: `context: mini-shop` + `file: mini-shop/Dockerfile` (មិនមែន `.`)។
- **Reproduce:** `docker build -t x mini-shop` ពី root (ឬ `docker build -t x .` ពី `mini-shop/`)។

### 3e. `cache export is not supported for the docker driver`
- **មូលហេតុ:** គ្មាន `docker/setup-buildx-action` មុន build-push ជាមួយ `cache-to: type=gha`។

---

## ៤. Workflow មិន run / job skipped ដោយមិនរំពឹង

- **`docker` skipped លើ PR ឬ develop** — ធម្មតា: `if: github.ref == 'refs/heads/main' && github.event_name == 'push'`។
- **`integration-test` skipped** — `needs: unit-test` ហើយ unit ក្រហម → មើល §១ មុន។
- **Run ២ លើ commit ដដែល** (push + pull_request) — ធម្មតា; concurrency group ខុសគ្នា (`refs/heads/develop` vs `refs/pull/N/merge`)។
- **Workflow មិនលេចឡើងទាល់តែសោះ** — YAML syntax error: Actions tab → workflow → បង្ហាញ "Invalid workflow file" + បន្ទាត់។ Lint local: `docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:1.7.7 -color=false .github/workflows/ci.yml` (Git Bash: `MSYS_NO_PATHCONV=1`)។
- **Merge button disabled** — branch protection: check `unit-test`/`integration-test` មិនទាន់បៃតង ឬឈ្មោះ job ប្តូរ (contexts ក្នុង protection ត្រូវផ្គូផ្គងឈ្មោះ job ពិតប្រាកដ)។

---

## ៥. ខុសគ្នា laptop vs CI (ពេល "លើម៉ាស៊ីនខ្ញុំបាន")

| រោគសញ្ញា laptop | មូលហេតុ | ធ្វើ |
|---|---|---|
| `./mvnw`: `curl: (35) schannel ... CRYPT_E_NO_REVOCATION_CHECK` | network ការិយាល័យ TLS interception (Somansa CA) | ប្រើ `mvn` 3.9.16 តម្លើងរួច (version ដដែលនឹង wrapper) ឬ `curl --ssl-no-revoke` |
| `docker build`: `wget: Failed to fetch .../apache-maven-3.9.16-bin.tar.gz` (0.3s) | ដដែល — container មិនទុកចិត្ត Somansa CA (`openssl s_client` → issuer Somansa) | **កុំ**ដាក់ CA ក្រុមហ៊ុនក្នុង Dockerfile; build ក្នុង CI; scan jar ដោយ Trivy Windows |
| Test ឆ្លងលើ laptop ធ្លាក់ក្នុង CI | env var/DB មានលើ laptop តែគ្មានក្នុង CI (§1b) | reproduce ដោយ `env -u ...` |
| Trivy result ខុសគ្នា laptop vs CI | CVE DB date ខុសគ្នា; Trivy version (action v0.36.0 → Trivy 0.70; laptop 0.74) | ធៀប DB date ក្នុង log `[vulndb]` |

---

## ៦. Command reproduce តាម step

```bash
cd mini-shop
mvn -B test                                          # job unit-test
mvn -B verify -DskipUnitTests                        # job integration-test (Docker ត្រូវ run)
mvn -B package -DskipTests && \
  C:/Users/user/tools/trivy/trivy.exe fs --scanners vuln --severity CRITICAL --ignore-unfixed target/*.jar   # gate Trivy (ដោយគ្មាន build image)
docker build -t mini-shop:local mini-shop            # job docker step build (ក្រៅ network ការិយាល័យ)
```

## ៧. ក្រោយដោះស្រាយ

- symptom ថ្មីដែលមិនមានក្នុង runbook → បន្ថែម § ថ្មី ជាមួយបន្ទាត់ error **ពិត** + ថ្ងៃ។
- កត់ពេលវេលា "ពី push ដល់ដឹង" ក្នុង `docs/journey/phase-a2/learning-log.md`; បើ > 5 នាទី សួរថាហេតុអ្វី (cache miss? DB download?)។
