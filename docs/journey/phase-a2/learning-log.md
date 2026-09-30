# Learning Log — Phase A2 (CI)

ការឆ្លុះបញ្ចាំងរបស់អ្នករៀន (ហេតុអ្វី)។ Claude review និងសួរបន្ត តែ**មិនឆ្លើយជំនួស**។
អ្វីដែលធ្វើពិត (command, លទ្ធផល) នៅ [worklog.md](worklog.md); symptom + ដំណោះស្រាយ នៅ [runbooks/](../../runbooks/)។

## Phase A2 / Task 0 — Repo → GitHub (2026-09-28)

Repo មានលើ GitHub រួចហើយ (`vuthin-devops-ecommerce/ecom-api`, private, branch `main`)។ ថ្ងៃនេះបន្ថែម branch `develop` និង folder `.github/workflows/`។

### សំណួរ ១ (ពី `docs/journey/phase-a2/plan.md` Task 0)

ហេតុអ្វីមិន push ផ្ទាល់ទៅ `main`? ការងារជាក្រុមធំមានបញ្ហាអ្វីបើអ្នកគ្រប់គ្នាធ្វើដូចនោះ?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 0

- `git ls-files -s mini-shop/mvnw` បង្ហាញ mode `100644` (មិន executable) — Windows មិនរក្សា exec bit, ដូច្នេះ Linux runner នឹងបាន `Permission denied` ពេល `./mvnw`។ Dockerfile ដោះស្រាយដោយ `chmod +x` (រោគសញ្ញា) — ថ្ងៃនេះកែមូលហេតុដោយ `git update-index --chmod=+x mini-shop/mvnw` → `100755`។
- Maven wrapper (`./mvnw` ក្នុង Git Bash) download មិនបានលើ laptop: `curl: (35) schannel ... CRYPT_E_NO_REVOCATION_CHECK` — បញ្ហា TLS revocation check លើម៉ាស៊ីននេះ មិនមែនបញ្ហា project។ ផ្ទៀងផ្ទាត់ក្នុងមូលដ្ឋានដោយ `mvn` (3.9.16 ដដែលនឹង wrapper) ជំនួស។ CI runner មិនមានបញ្ហានេះ។

---

## Phase A2 / Task 1 — Build + unit test (2026-09-28)

File: `.github/workflows/ci.yml`

### ចំណុចត្រូវយល់ក្នុង YAML (ពី `docs/journey/phase-a2/plan.md` Task 1)

| បន្ទាត់ | សំណួរ | ចម្លើយ |
|---|---|---|
| `concurrency` + `cancel-in-progress` | បើអ្នក push ៣ ដងក្នុង ១ នាទី កើតអ្វី? ហេតុអ្វីសន្សំលុយ? | _(សរសេរនៅទីនេះ)_ |
| `permissions: contents: read` | បើមិនកំណត់ default ជាអ្វី? ហេតុអ្វីគ្រោះថ្នាក់? | _(សរសេរនៅទីនេះ)_ |
| `cache: maven` | cache key គឺអ្វី? ពេលណា cache miss? | _(សរសេរនៅទីនេះ)_ |
| `if: always()` | បើអត់ ហេតុអ្វី test report មិនដែលឃើញពេលបរាជ័យ? | _(សរសេរនៅទីនេះ)_ |
| `-B` | batch mode — ហេតុអ្វីសំខាន់ក្នុង CI? | _(សរសេរនៅទីនេះ)_ |

### សំណួរ review បន្ថែម (ពីការសម្រេចចិត្តរចនាក្នុង `ci.yml`)

1. Action pin ជា commit SHA (`actions/checkout@3d3c42e...` + comment `# v7.0.1`) ជំនួស `@v4` — អ្នកណាអាចផ្លាស់ទី tag `v4`? SHA ខុសគ្នាយ៉ាងណា? តម្លៃដែលត្រូវបង់គឺអ្វី (hint: Dependabot Task 5)?
2. `runs-on: ubuntu-24.04` ជំនួស `ubuntu-latest` — ទាក់ទងច្បាប់ណាក្នុង `CLAUDE.md` §4? បើ GitHub ប្តូរ `ubuntu-latest` ទៅ 26.04 កើតអ្វីចំពោះ build?
3. `defaults.run.working-directory: mini-shop` អនុវត្តលើ `run:` ប៉ុណ្ណោះ — ហេតុអ្វី `path:` ក្នុង `upload-artifact` និង `cache-dependency-path` ត្រូវសរសេរ `mini-shop/...` ពេញ?
4. Workflow run លើ push គ្រប់ branch **រួមទាំង docs-only commit** — គួរបន្ថែម `paths:` filter ឬអត់? (hint: Task 5 "Require status checks" — បើ workflow មិន run, check មិនដែល report → PR merge មិនបាន)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### លំហាត់

1. Run ទី១ (push `develop`): ______ (បៃតង/ក្រហម) — ពេលវេលា ______
2. ធ្វើឱ្យខូចដោយចេតនា: កែ test មួយឱ្យបរាជ័យ → push → run ក្រហម → download `surefire-reports` artifact → ឃើញអ្វីក្នុង file `.txt`?
   - _(សរសេរនៅទីនេះ)_
3. Run ទី២ (cache hit): ពេលវេលា ______ — ខុសពីទី១ ______ វិនាទី។ step ណាលឿនជាង? ហេតុអ្វី?
   - _(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 1

- **ច្បាប់ "Reproducible" ត្រូវសាកល្បងមុន push:** run `mvn -B verify -DskipITs` លើ laptop ដោយ**ដក** env var `SPRING_DATASOURCE_*` ចេញ (ដូច CI runner) → `MiniShopApplicationTests.contextLoads` **ធ្លាក់** (`Failed to load ApplicationContext` — datasource url ទទេ)។ Test នេះឆ្លងលើ laptop កាលពី Phase A តែព្រោះ env var ចង្អុលទៅ DB ដែលកំពុង run — មិនមែនព្រោះ test ត្រឹមត្រូវ។
- **ការសម្រេចចិត្ត:** លុប `MiniShopApplicationTests` ចោល។ ហេតុផល: (ក) វាជា `@SpringBootTest` ដែលត្រូវការ DB ពិត តែឈ្មោះ `*Tests` ធ្វើឱ្យ surefire run វាជា unit test; (ខ) `OrderControllerIT` boot context ពេញជាមួយ `postgres:17` រួចហើយ → "context loads" ត្រូវបានគ្របដណ្តប់ក្នុង Task 2។ ជម្រើសផ្សេង: ប្តូរឈ្មោះជា `*IT` + Testcontainers (container ទី២ → IT យឺតជាង ដោយគ្មានតម្លៃបន្ថែម)។
  - សំណួរ: ប្រសិនបើថ្ងៃក្រោយមាន bean ដែល `OrderControllerIT` មិនប៉ះ (ឧ. scheduler) ហើយ config ខុស — test ណានឹងចាប់បាន? ត្រូវការ `contextLoads` ត្រឡប់វិញឬអត់?

### លំហាត់ ២ — លទ្ធផលពិត (2026-09-28)

កែ `"39.48"` → `"99.99"` ក្នុង `OrderServiceTest` → push `develop` → run ក្រហមក្នុង ២៨ វិនាទី → step "Upload test report" នៅតែ run (`if: always()`) → artifact `surefire-reports` 9.7 KB។ កែត្រឡប់ → push → បៃតង។

**អ្វីដែលឃើញក្នុង file `.txt` របស់ artifact ខុសពី log យ៉ាងណា?**

_(សរសេរនៅទីនេះ)_

---

## Phase A2 / Task 2 — Integration test ក្នុង CI (2026-09-28)

File: `.github/workflows/ci.yml` (job `unit-test` + `integration-test`), `mini-shop/pom.xml` (property `skipUnitTests`), ADR: `docs/decisions/004-ci-job-structure.md`

### សំណួរឆ្លុះបញ្ចាំង (ពី `docs/journey/phase-a2/plan.md` Task 2)

**១. ជម្រើស A (job តែមួយ) vs B (២ job) — មួយណាល្អជាងសម្រាប់គម្រោងនេះ?**
ADR-004 ស្នើ B ជាមួយហេតុផល — អ្នកយល់ស្របឬអត់? ឆ្លើយសំណួរ ៣ ចុង ADR-004 នៅទីនេះ រួចប្តូរ status ADR ជា Accepted (ឬកែការសម្រេចចិត្ត)។

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

**២. Testcontainers ក្នុង CI ខុសពី `services: postgres:` របស់ GitHub Actions យ៉ាងណា? ហេតុអ្វីយើងជ្រើស Testcontainers?**
hint: អ្នកណាកំណត់ version DB — YAML ឬ test code? test run លើ laptop ដោយគ្មាន DB ដោយដៃបានឬអត់?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### សំណួរ review ក្នុង `ci.yml` (Task 2)

| បន្ទាត់ | សំណួរ | ចម្លើយ |
|---|---|---|
| `./mvnw -B test` (job unit) | Maven lifecycle `validate → compile → test → package → integration-test → verify`: `test` ឈប់ត្រង់ណា? ហេតុអ្វី failsafe មិន run? | _(សរសេរ)_ |
| `needs: unit-test` | parallel vs sequential — ខុសគ្នាពេលវេលាប៉ុន្មានពេលទាំងអស់ឆ្លង? ពេល unit ខូច? | _(សរសេរ)_ |
| `-DskipUnitTests` | ហេតុអ្វីត្រូវកំណត់ក្នុង `pom.xml` ខ្លួនឯង? `skipTests` ធ្វើអ្វី? | _(សរសេរ)_ |
| `cache: maven` ក្នុង job ២ | job ២ cache hit ឬ miss? ហេតុអ្វី? | _(សរសេរ)_ |
| `failsafe-reports` | ហេតុអ្វី report នៅ folder ផ្សេងពី surefire? | _(សរសេរ)_ |

### លំហាត់ — វាស់ពេលវេលា

- Run Task 1 (job តែមួយ, unit ប៉ុណ្ណោះ): 33s (cache hit)
- Run Task 2 (unit-test + integration-test): សរុប 1m39s · job unit-test 42s (step 29s, cache miss ព្រោះ pom.xml ប្តូរ → key ថ្មី) · job integration-test 51s (step 40s, cache hit ពី key ដែល unit-test save)
- Testcontainers pull `postgres:17` ក្នុង runner: 8 វិនាទី (05:11:31 → 05:11:39), ryuk 1.5s; container start 1.0s; OrderControllerIT 22s
- យឺតជាង Task 1 ប៉ុន្មាន? ផ្នែកណាចំណាយច្រើនបំផុត?

### អ្វីដែលជួបពិតពេលធ្វើ Task 2

- **`-Dsurefire.skip=true` មិនដំណើរការ** — សាកលើ laptop: surefire នៅតែ run 6 unit test រួច failsafe run 3 IT (`BUILD SUCCESS`, គ្មាន error, គ្មាន warning!)។ Maven **មិនស្គាល់** property នោះ ហើយក៏មិនប្រាប់អ្នកដែរ — property ខុសឈ្មោះ = ស្ងាត់ៗមិនធ្វើអ្វី។ មេរៀន: ពេលបន្ថែម flag ត្រូវ**មើល log ថាវាមានឥទ្ធិពលពិត** មិនមែនមើលតែ exit code។
- ដំណោះស្រាយ: property `skipUnitTests` ក្នុង `pom.xml` (default `false`) wire ចូល surefire `<skipTests>${skipUnitTests}</skipTests>`។ ផ្ទៀងផ្ទាត់: `mvn -B verify -DskipUnitTests` → log ត្រូវបង្ហាញ "Tests are skipped" ពី surefire និង "Tests run: 3" ពី failsafe។
- IT លើ laptop: `postgres:17` start ក្នុង 0.8s (image មានស្រាប់), `OrderControllerIT` 27s សរុប (Spring boot + Flyway)។ ក្នុង CI runner image ត្រូវ pull ថ្មីរាល់ run — ប្រៀបធៀបពេលវេលា។

---

## Phase A2 / Task 3 — Build & push image ទៅ ghcr.io (2026-09-28)

File: `.github/workflows/ci.yml` job `docker` (run តែពេល push ទៅ `main`)។ Image: `ghcr.io/vuthin-devops-ecommerce/mini-shop:<short-sha>` និង `:latest`។

### ចំណុចត្រូវយល់ (ពី `docs/journey/phase-a2/plan.md` Task 3)

| ចំណុច | សំណួរ | ចម្លើយ |
|---|---|---|
| `GITHUB_TOKEN` | ហេតុអ្វីល្អជាង Personal Access Token? ពេលណា PAT នៅតែត្រូវការ? | _(សរសេរ)_ |
| tag `sha` + `latest` | ហេតុអ្វី**មិនត្រូវ** deploy `:latest` ទៅ production? (immutability) | _(សរសេរ)_ |
| `cache-from/to: type=gha` | build មុន cache ______ ក្រោយ ______ — layer ណា hit? | _(សរសេរ)_ |
| `if: github.ref == 'refs/heads/main'` | ហេតុអ្វី PR មិន push image? | _(សរសេរ)_ |

### សំណួរ review បន្ថែម (ពី comment ក្នុង job `docker`)

1. `permissions: packages: write` នៅកម្រិត job មិនមែន workflow — ខុសគ្នាអ្វីខាង security?
2. `setup-buildx-action` — ហេតុអ្វីត្រូវការសម្រាប់ `cache type=gha`? បើគ្មានកើតអ្វី?
3. `cache: maven` (setup-java) និង Docker layer cache ជា cache ២ ផ្សេងគ្នា — ហេតុអ្វី Docker build (multi-stage) មិនប្រើ `~/.m2` របស់ runner?
4. **ជម្លោះ `:latest`:** CLAUDE.md §4 ថា "គ្មាន tag latest" តែផែនការ Task 3 DoD ឱ្យ push `:latest`។ ខ្ញុំ (Claude) សម្រេចរក្សា `:latest` ជា tag "ផលិត" តែ**មិនដែលប្រើ** (compose/K8s ប្រើ `:<sha>`)។ អ្នកយល់ស្របឬចង់ដកចេញ? សរសេរហេតុផល — នេះជាការសម្រេចចិត្តរបស់អ្នក។
5. Image name `mini-shop` (តាម phase-a3-plan) ខណៈ repo ឈ្មោះ `ecom-api` — ghcr ភ្ជាប់ package ទៅ repo ដោយរបៀបណា? (hint: label `org.opencontainers.image.source`)

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### របៀបសាកល្បង Task 3 (job run តែលើ `main`)

1. បើក PR `develop → main` លើ GitHub → CI run លើ PR (unit + IT, **គ្មាន** docker — មើលថា job docker បង្ហាញ "skipped")
2. Merge PR → run លើ `main` → job `docker` run → Summary page បង្ហាញ tag
3. ផ្ទៀងផ្ទាត់លើ laptop (package private → ត្រូវ login):
   ```bash
   echo $GH_PAT | docker login ghcr.io -u thiravuthin --password-stdin   # ឬ gh auth token | docker login ghcr.io -u thiravuthin --password-stdin
   docker pull ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>
   docker run --rm -e SPRING_DATASOURCE_URL=x -e SPRING_DATASOURCE_USERNAME=x -e SPRING_DATASOURCE_PASSWORD=x ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>
   ```
   (crash ព្រោះគ្មាន DB — តែ pull និង start បាន = image ត្រឹមត្រូវ)
4. លំហាត់: កែ `compose.yaml` ឱ្យ `app` ប្រើ `image: ghcr.io/.../mini-shop:<sha>` ជំនួស `build: .` → `docker compose up` លើម៉ាស៊ីនគ្មាន Java/Maven

### លំហាត់ — វាស់

- Docker build run ១ (cache miss, run 36391399001): step build-push 1m32s — pull base image 3–6s · `dependency:go-offline` 24s · `package` 12s · push manifest 8s · **export cache (mode=max) 45s** · run ២ (កែតែ src/): ______ · layer ណា hit / miss? ______
- Image push រួច: `ghcr.io/vuthin-devops-ecommerce/mini-shop:1279d03` និង `:latest`, digest `sha256:60c29aae…` (tag ២ ចង្អុលទៅ digest តែមួយ)
- Image size (`docker images`): ______ MB — ធៀបនឹង `mini-shop:local` ពី Phase A?

### អ្វីដែលជួបពិតពេលធ្វើ Task 3

- **Run ដំបូងលើ `main` ធ្លាក់ក្នុង 1s** (run 36386015912): `ERROR: invalid tag "ghcr.io/.../mini-shop:-tag-short-git-sha-7-.-dd44792-immutable-sha-image-56abbbb": invalid reference format`។ មូលហេតុ: comment `# ...` ដែលដាក់នៅចុងបន្ទាត់**ក្នុង** YAML block scalar (`tags: |`) មិនមែន comment ទេ — វាជាអត្ថបទ → metadata-action យកវាចូល tag។ លំដាប់រកឃើញ: job → step ណាក្រហម (build-push, 1s = មិនទាន់ build) → log បន្ទាត់ `docker buildx build ... --tag` ឃើញ tag ចម្លែក → ថយក្រោយទៅ YAML។ actionlint មិនចាប់។ កែ: ដក comment ចេញពី block ដាក់ខាងលើ។
- PR run (36385865494): job `docker` = skipped ✅ (`if:` ដំណើរការ)។ Commit ដដែលបាន run ២ ដង (event `push` លើ develop + `pull_request`) — concurrency group ខុសគ្នា (`refs/heads/develop` vs `refs/pull/2/merge`) ដូច្នេះមិន cancel គ្នា → ចម្លើយសំណួរ Task 1 "run ២ ដងឬ?" = បាទ។

_(សរសេរនៅទីនេះ)_

---

## Phase A2 / Task 4 — Security scan ជាមួយ Trivy (2026-09-29)

File: `.github/workflows/ci.yml` job `docker` (build load → Trivy CRITICAL block → Trivy HIGH report → push), ADR: `docs/decisions/005-security-scan-policy.md`

### ជម្រើសរចនា (ពី `docs/journey/phase-a2/plan.md` Task 4 — ADR-005 ស្នើរួច, អ្នកសម្រេច)

| សំណួរ | ADR-005 ស្នើ | អ្នកយល់ស្រប? ហេតុអ្វី? |
|---|---|---|
| Scan មុន push ឬក្រោយ push? | មុន (`load: true` → scan → `push: true`) | _(សរសេរ)_ |
| `CRITICAL` ប៉ុណ្ណោះ ឬ `CRITICAL,HIGH`? | CRITICAL block, HIGH report-only | _(សរសេរ)_ |
| `ignore-unfixed: true` មានន័យអ្វី? ហេតុអ្វីសមហេតុផល? | true | _(សរសេរ)_ |

សំណួរ ៣ ចុង ADR-005 → ចម្លើយ:

_(សរសេរនៅទីនេះ)_

### សំណួរ review ក្នុង `ci.yml` (Task 4)

1. `load: true` ជំនួស `push: true` ក្នុង step build ដំបូង — image ទៅណា? ហេតុអ្វី Trivy ត្រូវការវានៅទីនោះ?
2. Step push ចុងក្រោយ build "ម្តងទៀត" — ហេតុអ្វីវាចំណាយតែប៉ុន្មានវិនាទី? (មើល log: `CACHED` គ្រប់ layer?)
3. Step scan ធ្លាក់ → step push កើតអ្វី? (default behaviour របស់ step ពេល step មុន fail — ខុសពី `if: always()` យ៉ាងណា?)
4. `exit-code: "0"` លើ step HIGH — បើគ្មាននរណាអាន log តើ step នេះមានប្រយោជន៍អ្វី? អ្នកនឹងអានវាពេលណា?

### លំហាត់ — វាស់

- Run ដំបូងជាមួយ Trivy: CRITICAL ______ · HIGH ______ · ពេល step scan ______ · ពេល job `docker` សរុប ______
- លំហាត់ base image: ប្តូរ `eclipse-temurin:21-jre-alpine` → `eclipse-temurin:17-jre` (Debian, គ្មាន alpine) ក្នុង Dockerfile → push (branch ណាក៏បាន? — មិនទេ: job docker run តែលើ main → ត្រូវ PR+merge, ឬ run Trivy លើ laptop ជំនួស):
  ```bash
  docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:0.69.1 image --severity CRITICAL,HIGH --ignore-unfixed ghcr.io/vuthin-devops-ecommerce/mini-shop:1279d03
  ```
  លទ្ធផលពិត 2026-09-29 (Trivy 0.74.0 លើ Windows, scan base image ផ្ទាល់ ព្រោះ build local ធ្វើមិនបាន — មើលខាងក្រោម):
  - `eclipse-temurin:21-jre-alpine` (287MB): **0** CVE (គ្រប់ severity, ទោះមិន ignore-unfixed)
  - `eclipse-temurin:17-jre` (ubuntu 26.04, 430MB): **42** CVE — LOW 4, MEDIUM 38, HIGH 0, CRITICAL 0 — **ទាំង 42 unfixed** → ជាមួយ `--ignore-unfixed` = 0
  - ហេតុអ្វី alpine 0 តែ ubuntu 42? ហេតុអ្វី gate `CRITICAL` + `ignore-unfixed` ឱ្យលទ្ធផលដូចគ្នាទាំង ២? _(សរសេរ)_
- Trivy លើ laptop vs CI: លទ្ធផលដូចគ្នាឬអត់? បើខុស ហេតុអ្វី? (hint: CVE database date)

### អ្វីដែលជួបពិតពេលធ្វើ Task 4

- **`docker build` លើ laptop ធ្លាក់** នៅ `[builder 5/7] ./mvnw dependency:go-offline`: `wget: Failed to fetch .../apache-maven-3.9.16-bin.tar.gz` ក្នុង 0.3s។ លំដាប់ debug: Dockerfile ដដែល build បានក្នុង CI → បញ្ហា environment មិនមែន code → run `eclipse-temurin:21-jdk` ដោយដៃ: DNS ✅, `wget --spider https://repo.maven.apache.org` → **`The certificate of repo.maven.apache.org is not trusted`** → `openssl s_client` → issuer `C=KR, O=Somansa, CN=Somansa Root CA`។ សន្និដ្ឋាន: network ការិយាល័យមាន TLS interception; Windows ទុកចិត្ត CA នោះ តែ Linux container មិនស្គាល់។ ដូចគ្នានឹង `curl: (35) schannel` ពី Task 0។ **មិនកែ Dockerfile** (CA របស់ក្រុមហ៊ុនមិនមែនរបស់ project) — build ធ្វើក្នុង CI, laptop ប្រើ Windows-native tool។
- Trivy ក្នុង container (`aquasec/trivy`) ក៏នឹងជួបបញ្ហាដដែល (download DB ពី ghcr.io) → តម្លើង Trivy 0.74.0 Windows ផ្ទាល់នៅ `C:/Users/user/tools/trivy/trivy.exe` (Go ប្រើ Windows cert store → ដើរបាន)។ សំណួរ: CI runner មិនមានបញ្ហានេះ — តើនេះជាហេតុផលមួយដែល "build លើ CI មិនមែន laptop" សំខាន់?

_(សរសេរនៅទីនេះ)_
- **Run ដំបូងលើ `main` ជាមួយ Trivy (run 36507202982) — gate block ពិត, មិនមែន test:** step scan ក្រហម, step push **skipped**, registry គ្មាន tag ថ្មី។ រកឃើញ `Total: 3 (CRITICAL: 3)` ក្នុង `app/app.jar` — `org.apache.tomcat.embed:tomcat-embed-core 11.0.24` (CVE-2026-65182, CVE-2026-65905, CVE-2026-68525 — security constraint/auth bypass), `Status: fixed`, `Fixed Version: 11.0.25`។ OS layer (alpine 3.24.2): 0។ ចំណាំ: base image scan លើ laptop (0 CVE) មិនឃើញនេះ — ព្រោះ CVE នៅក្នុង **jar dependency** របស់ app មិនមែន OS; ដូច្នេះ scan image ពេញក្នុង CI ចាំបាច់។
- Build step (load) 14s — cache gha hit ទាំងអស់ (run មុន 1m32s) ✅។ Scan 61s: Trivy download vuln DB + Java DB (រាល់ run, runner ថ្មី) — ចំណាយសំខាន់ជាង scan ខ្លួនឯង។
- **ការសម្រេចចិត្តកែ:** ADR-005 block តែ CVE ដែលមាន fix → ត្រូវ upgrade មិនមែន ignore។ Spring Boot 4.1.1 គ្រប់គ្រង Tomcat 11.0.24 → override property `<tomcat.version>11.0.25</tomcat.version>` ក្នុង `pom.xml` (Boot BOM ប្រើ property នេះ — patch version តែប៉ុណ្ណោះ, មិនមែន major upgrade → គ្មាន ADR ថ្មី តាម CLAUDE.md §3)។ ជម្រើសផ្សេង: upgrade Spring Boot 4.1.x ថ្មីដែលមាន Tomcat 11.0.25 (បើមាន) — ធំជាង, ធ្វើពេល Dependabot ស្នើ (Task 5)។
  - សំណួរ: ហេតុអ្វី override property ល្អជាង `<dependency>` tomcat-embed-core ផ្ទាល់ក្នុង pom? (hint: tomcat-embed-el, tomcat-embed-websocket ត្រូវ version ដូចគ្នា)
- **Run ២ លើ `main` ក្រោយ fix (run 36511263071) — gate ឆ្លង, image push:** scan CRITICAL `Total: 0` → step HIGH report `Total: 2 (HIGH: 2)` — `com.fasterxml.jackson.core:jackson-databind 2.21.5` (fixed 2.21.6) — **មិន block** (report-only តាម ADR-005) → step push run 7s (layer CACHED ទាំងអស់) → image ថ្មីលើ ghcr.io។ Build step 1m56s (cache miss ព្រោះ `pom.xml` ប្តូរ → layer `go-offline` rebuild + export 45s)។
- Trivy DB cache: `Cache not found for input keys: cache-trivy-2026-09-29` ទាំង ២ step (key តាមថ្ងៃ, save ក្រោយ job) → run ក្រោយក្នុងថ្ងៃដដែលគួរ hit។ Trivy binary cache hit (v0.70.0 — action pin v0.36.0 ប្រើ Trivy 0.70, laptop 0.74 — version ខុសគ្នា, database ដូចគ្នា)។
- សំណួរ Task 6 / review date ADR-005: HIGH 2 ក្នុង jackson-databind មាន fix — ទុកឱ្យ Dependabot (Task 5) ស្នើ ឬ override ដូច Tomcat? អ្វីជាលក្ខណៈវិនិច្ឆ័យ? (hint: CVSS, exploitability, ថាតើ app ប្រើ feature នោះ)

---

## Phase A2 / Task 5 — Branch protection + badge + Dependabot (2026-09-29)

File: `README.md` (badge, Development workflow), `.github/dependabot.yml`

### អ្វីដែលជួបពិតពេលធ្វើ Task 5

- **Branch protection API → 403** `Upgrade to GitHub Pro or make this repository public to enable this feature.` — ទាំង classic branch protection និង repository ruleset។ Repo `ecom-api` private, org plan `free`។ GitHub: protected branch/ruleset មានសម្រាប់ repo **public** លើ Free; repo private ត្រូវការ Pro/Team។
- **ការសម្រេចចិត្តរបស់អ្នក (មិនមែន Claude):** ជម្រើស (ក) ធ្វើ repo public — code រៀន, គ្មាន secret ក្នុង git (CLAUDE.md §4 ធានា) → protection ដើរបាន, ghcr package អាចនៅ private ដដែល; (ខ) នៅ private, រំលង protection, រក្សាវិន័យ "PR ជានិច្ច" ដោយខ្លួនឯង (CI នៅតែ run លើ PR តែ merge button មិន disabled); (គ) GitHub Pro។ **សម្រេច 2026-09-29: (ក) public** — `gh repo edit --visibility public` → protection apply បាន: require PR (0 approval — solo), status check `unit-test` + `integration-test`, `enforce_admins: true` (owner ក៏ bypass មិនបាន), no force-push, no delete។ `strict: false` ដោយចេតនា: ជាមួយ merge commit `develop → main`, `main` មាន commit ដែល `develop` គ្មាន → `strict: true` នឹងទាមទារ merge `main` ចូល `develop` មុនរាល់ PR — friction ដោយគ្មានតម្លៃសម្រាប់ solo dev (សំណួរ ១ ខាងក្រោម)។ ហេតុផលរបស់អ្នក:

  _(សរសេរនៅទីនេះ)_

- `dependabot.yml`: ផែនការសរសេរ `directory: /` សម្រាប់ maven/docker — ខុសសម្រាប់ monorepo នេះ (`pom.xml`, `Dockerfile` នៅ `mini-shop/`) → `/mini-shop`; github-actions នៅ root ត្រឹមត្រូវ។ បន្ថែម `ignore: semver-major` សម្រាប់ Spring Boot, eclipse-temurin, postgres (major = ADR មិនមែន PR bot, CLAUDE.md §3)។

### សំណួរឆ្លុះបញ្ចាំង / លំហាត់ (ពី `docs/journey/phase-a2/plan.md` Task 5)

| លំហាត់ | លទ្ធផល |
|---|---|
| សាក push ផ្ទាល់ទៅ `main` → ត្រូវបានបដិសេធ? | _(បើ protection បើក: `git push origin develop:main` → error "protected branch")_ |
| PR ដែល test បរាជ័យ → merge disabled? | _(សរសេរ)_ |
| Dependabot PR ដំបូងមកពេលណា? CI run លើ PR របស់ bot ឬអត់? job `docker` skipped? | _(សរសេរ — ចាំថ្ងៃច័ន្ទ ឬ trigger ដោយដៃ: Insights → Dependency graph → Dependabot → "Check for updates")_ |
| Dependabot ស្នើ jackson-databind 2.21.6 (HIGH ពី Trivy report) ឬអត់? | _(សរសេរ)_ |

សំណួរ:
1. `strict: true` ("Require branches to be up to date before merging") — PR ដែលបើកមុន `main` ប្តូរ ត្រូវ rebase/merge មុន — ហេតុអ្វីសំខាន់ពេលមាន ២ PR ព្រមគ្នា?
2. Status check ត្រូវការ**ឈ្មោះ job** ជាក់លាក់ (`unit-test`, `integration-test`) — បើប្តូរឈ្មោះ job ក្នុង `ci.yml` កើតអ្វី?
3. Badge ចង្អុល `?branch=main` — បើគ្មាន param តើបង្ហាញ run ណា?
4. Dependabot update SHA របស់ action + comment `# vX.Y.Z` — បើអ្នក pin SHA ដោយគ្មាន comment version, Dependabot ធ្វើអ្វី?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

---

## Phase A2 / Task 6 — Documentation & Reflection (2026-09-29)

| Deliverable | ស្ថានភាព | អ្នកណា |
|---|---|---|
| `docs/decisions/004-ci-job-structure.md` | Proposed → **Accepted** ពេលអ្នកឆ្លើយសំណួរ ៣ ចុង ADR | អ្នក |
| `docs/decisions/005-security-scan-policy.md` | Proposed → **Accepted** ពេលអ្នកឆ្លើយសំណួរ ៣ ចុង ADR (gate block ពិតរួច 2026-09-29) | អ្នក |
| `docs/runbooks/ci-failure.md` | ✅ សរសេររួច ពី symptom ពិត ៩ (1a–1d, 2a–2c, 3a–3e) | Claude |
| README "Development workflow" | ✅ | Claude |
| ចម្លើយសំណួរឆ្លុះបញ្ចាំង Task 0–5 ក្នុង file នេះ | _(សរសេរនៅទីនេះ)_ | អ្នក |

### សំណួរធំ (សម្រាប់ Phase ក្រោយ — ពី `docs/journey/phase-a2/plan.md` Task 6)

1. CI push image រួច — **អ្នកណា** deploy វា? ឥឡូវនៅតែជាអ្នកដោយដៃ។ Phase A3 (`kubectl set image …:<sha>`) និង E (GitOps) ដោះស្រាយយ៉ាងណា?

   _(សរសេរនៅទីនេះ)_

2. បើ CI ឆ្លង តែ app crash នៅ production — CI ខ្វះអ្វី? (hint: image ដែល push មិនដែលត្រូវ **start** ក្នុង CI — smoke test `docker run` + `/actuator/health`? contract test?)

   _(សរសេរនៅទីនេះ)_

3. Secret `DB_PASSWORD` នៅក្នុង `.env` លើម៉ាស៊ីនអ្នក — ពេល deploy ទៅ server ពិត វាទៅនៅឯណា? (Phase A3 K8s Secret → Phase B Vault)

   _(សរសេរនៅទីនេះ)_

### Definition of Done Phase A2 (ពិនិត្យ 2026-09-29)

- [x] Push ទៅ branch ណាមួយ → `ci.yml` run: compile + unit + integration
- [x] PR → CI ត្រូវឆ្លងមុន merge (branch protection, repo public)
- [x] Merge ទៅ `main` → image `ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>` + `:latest` (ឈ្មោះ `mini-shop` តាម phase-a3-plan, មិនមែន `<username>/mini-shop`)
- [x] Trivy CRITICAL → pipeline ក្រហម (បញ្ជាក់ដោយ CVE ពិត tomcat 11.0.24)
- [x] README badge
- [x] Build ទី២ លឿនជាង ២ ដង (33s vs 1m09s unit; docker build 14s vs 1m32s ពេល cache hit)
- [ ] ADR-004 (ផែនការសរសេរ `004-ci-strategy.md`; CLAUDE.md កក់ `004-ci-job-structure.md` — ប្រើឈ្មោះ CLAUDE.md) → Accepted
- [ ] Dependabot PR ដំបូង review (រង់ចាំ)

