# Phase A2 — Continuous Integration (GitHub Actions)

> **សម្រាប់ Claude Code:** សូមអាន `CLAUDE.md` និង `docs/phase-a-plan.md` ជាមុន។
> តម្រូវការជាមុន: Phase A Task 0–5 ត្រូវចប់ (ជាពិសេស Dockerfile ត្រូវ build បាន)។
> អ្នកប្រើកំពុងរៀន — ពន្យល់ជាភាសាខ្មែរ ណែនាំជាជំហាន កុំសរសេរ YAML ទាំងអស់ជំនួស។

**រយៈពេលប៉ាន់ស្មាន:** សប្តាហ៍ ៣–៤
**ឧបករណ៍:** GitHub Actions · GitHub Container Registry (ghcr.io) · Trivy · Testcontainers

---

## គោលដៅដំណាក់កាល A2

រាល់ `git push` → GitHub build, test, scan, និង push Docker image **ដោយស្វ័យប្រវត្តិ**។
បើអ្វីមួយខូច អ្នកដឹងក្នុង ៥ នាទី មិនមែន ៥ ថ្ងៃ។

**Definition of Done:**
- [ ] Push ទៅ branch ណាមួយ → workflow `ci.yml` run: compile + unit test + integration test
- [ ] Pull Request → CI ត្រូវឆ្លងមុនអាច merge (branch protection)
- [ ] Merge ទៅ `main` → image push ទៅ `ghcr.io/<username>/mini-shop:<sha>` និង `:latest`
- [ ] Trivy scan image — CRITICAL vulnerability → pipeline បរាជ័យ
- [ ] README មាន CI status badge
- [ ] Build ទី២ លឿនជាងទី១ យ៉ាងតិច ២ ដង (cache ដំណើរការ)
- [ ] `docs/decisions/004-ci-strategy.md` សរសេររួច

---

## ទស្សនៈសំខាន់មុនចាប់ផ្តើម

CI មិនមែនជា "ឧបករណ៏ run test" ទេ — វាជា **feedback loop**។ សំណួរដែលត្រូវសួររាល់ step:
"បើ step នេះបរាជ័យ តើអ្នកដឹងអ្វី? ហើយត្រូវការពេលប៉ុន្មានដើម្បីដឹង?"

ច្បាប់ ៣ យ៉ាង:
1. **Fail fast** — step រហ័ស (compile, lint) មុន step យឺត (integration test, image build)
2. **Reproducible** — CI run ដូចម៉ាស៊ីនអ្នកទាំងស្រុង (Java version ដូចគ្នា, DB ដូចគ្នា)
3. **Least privilege** — workflow មាន permission ត្រឹមតែអ្វីដែលវាត្រូវការ

---

## Task 0 — Repo ទៅ GitHub

**ជំហាន:**
1. បង្កើត repo `mini-shop` លើ GitHub (private ក៏បាន — ghcr.io free សម្រាប់ private ដែរ)
2. `git remote add origin ...` → `git push -u origin main`
3. បង្កើត branch `develop` សម្រាប់ធ្វើការ; `main` សម្រាប់ "release" ប៉ុណ្ណោះ
4. បន្ថែម folder `.github/workflows/`

**សំណួរឆ្លុះបញ្ចាំង:**
- ហេតុអ្វីមិន push ផ្ទាល់ទៅ `main`? ការងារជាក្រុមធំមានបញ្ហាអ្វីបើអ្នកគ្រប់គ្នាធ្វើដូចនោះ?

---

## Task 1 — Workflow ដំបូង: Build + Unit Test

**File:** `.github/workflows/ci.yml`

**គោលដៅ:** push ណាមួយ → `mvn -B verify -DskipITs` (unit test ប៉ុណ្ណោះ)

**គ្រោង (អ្នកប្រើសរសេរតាមគ្រោងនេះ — Claude Code review):**
```yaml
name: CI

on:
  push:
    branches: ["**"]
  pull_request:
    branches: [main, develop]

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven          # ← cache ~/.m2 ដោយស្វ័យប្រវត្តិ

      - name: Build & unit test
        run: mvn -B verify -DskipITs

      - name: Upload test report
        if: always()            # ← upload ទោះ test បរាជ័យ
        uses: actions/upload-artifact@v4
        with:
          name: surefire-reports
          path: target/surefire-reports/
```

**ចំណុចត្រូវយល់ក្នុង YAML នេះ (សរសេរចម្លើយក្នុង learning-log):**
| បន្ទាត់ | សំណួរ |
|---|---|
| `concurrency` + `cancel-in-progress` | បើអ្នក push ៣ ដងក្នុង ១ នាទី កើតអ្វី? ហេតុអ្វីសន្សំលុយ? |
| `permissions: contents: read` | បើមិនកំណត់ default ជាអ្វី? ហេតុអ្វីគ្រោះថ្នាក់? |
| `cache: maven` | cache key គឺអ្វី? ពេលណា cache miss? |
| `if: always()` | បើអត់ ហេតុអ្វី test report មិនដែលឃើញពេលបរាជ័យ? |
| `-B` | batch mode — ហេតុអ្វីសំខាន់ក្នុង CI? |

**លំហាត់:**
1. Push → មើល Actions tab → run ត្រូវបៃតង
2. **ធ្វើឱ្យខូចដោយចេតនា:** កែ test មួយឱ្យបរាជ័យ → push → run ក្រហម → download surefire report មើល
3. កត់ពេលវេលា run ទី១ និងទី២ — ខុសគ្នាប៉ុន្មាន?

---

## Task 2 — Integration Test ក្នុង CI (Testcontainers)

**គោលដៅ:** `mvn verify` ពេញលេញ រួមទាំង `OrderControllerIT` ដែលប្រើ Postgres ពិត

**បញ្ហាដែលអ្នកនឹងជួប:** Testcontainers ត្រូវការ Docker daemon។ `ubuntu-latest` runner **មាន** Docker ស្រាប់ — ដូច្នេះវាគួរដំណើរការ។ ប៉ុន្តែ:

**ជំហាន:**
1. រៀបចំ `pom.xml`: `maven-surefire-plugin` (unit `*Test.java`) និង `maven-failsafe-plugin` (integration `*IT.java`)
2. ដក `-DskipITs` ចេញពី workflow
3. Push → មើលពេលវេលា run — យឺតជាងប៉ុន្មាន?

**បំបែក job (optimization):**
```yaml
jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps: [...]   # mvn -B test

  integration-test:
    runs-on: ubuntu-latest
    needs: unit-test          # ← run តែពេល unit test ឆ្លង (fail fast)
    steps: [...]   # mvn -B verify -DskipUnitTests
```

**សំណួរឆ្លុះបញ្ចាំង:**
- ជម្រើស A: job តែមួយ run ទាំងអស់ (សាមញ្ញ, ១ VM) ។ ជម្រើស B: ២ job (fail fast, ២ VM, checkout ២ ដង)។ មួយណាល្អជាង**សម្រាប់គម្រោងនេះ**? សរសេរជា ADR-004។
- Testcontainers ក្នុង CI ខុសពី `services: postgres:` របស់ GitHub Actions យ៉ាងណា? ហេតុអ្វីយើងជ្រើស Testcontainers?

---

## Task 3 — Build & Push Docker Image ទៅ ghcr.io

**គោលដៅ:** merge ទៅ `main` → image `ghcr.io/<username>/mini-shop:<git-sha>` និង `:latest`

**Job ថ្មី (run តែលើ `main`):**
```yaml
  docker:
    runs-on: ubuntu-latest
    needs: [unit-test, integration-test]
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    permissions:
      contents: read
      packages: write           # ← ត្រូវការសម្រាប់ push ទៅ ghcr.io
    steps:
      - uses: actions/checkout@v4

      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}   # ← auto-provided, មិនត្រូវបង្កើតខ្លួនឯង

      - uses: docker/metadata-action@v5
        id: meta
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=sha,prefix=
            type=raw,value=latest

      - uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

**ចំណុចត្រូវយល់:**
| ចំណុច | ហេតុអ្វី |
|---|---|
| `GITHUB_TOKEN` | Token ដែល GitHub បង្កើតរាល់ run, អស់សុពលភាពពេល run ចប់ — ហេតុអ្វីល្អជាង Personal Access Token? |
| tag `sha` + `latest` | ហេតុអ្វី**មិនត្រូវ** deploy `:latest` ទៅ production? (ចម្លើយ: immutability) |
| `cache-from/to: type=gha` | Docker layer cache រវាង run — ប្រៀបធៀបពេលវេលា build មុន/ក្រោយ |
| `if: github.ref == 'refs/heads/main'` | ហេតុអ្វី PR មិន push image? |

**ផ្ទៀងផ្ទាត់:**
```bash
docker pull ghcr.io/<username>/mini-shop:latest
docker run -e SPRING_DATASOURCE_PASSWORD=x ghcr.io/<username>/mini-shop:latest
```
(វានឹង crash ព្រោះគ្មាន DB — ប៉ុន្តែពិសោធថា image pull និង start បាន)

**លំហាត់:**
- កែ `docker-compose.yml` ឱ្យ `app` ប្រើ `image: ghcr.io/...` ជំនួស `build: .` → ឥឡូវម៉ាស៊ីនណាក៏ run បាន **ដោយមិនចាំបាច់មាន Java ឬ Maven**។ នេះជាចំណុចដែល DevOps "ចុចទ្រូង" លើកដំបូង។

---

## Task 4 — Security Scan ជាមួយ Trivy

**គោលដៅ:** image មាន CRITICAL vulnerability → pipeline ក្រហម

**Step បន្ថែមក្នុង job `docker` (មុន push ឬក្រោយ? — គិត):**
```yaml
      - name: Scan image with Trivy
        uses: aquasecurity/trivy-action@0.28.0    # ← pin version, ពិនិត្យ version ចុងក្រោយ
        with:
          image-ref: ghcr.io/${{ github.repository }}:${{ github.sha }}
          format: table
          exit-code: "1"
          severity: CRITICAL
          ignore-unfixed: true
```

**ជម្រើសរចនា (សរសេរក្នុង ADR-005):**
- Scan **មុន** push (image មិនល្អមិនដែលទៅ registry) vs Scan **ក្រោយ** push (registry មាន image ទោះខូច តែស្កេនលើអ្វីដែល deploy ពិត) — មួយណា?
- `severity: CRITICAL` ប៉ុណ្ណោះ ឬ `CRITICAL,HIGH`? តើអ្នកចង់ pipeline ក្រហមញឹកញាប់ប៉ុណ្ណា?
- `ignore-unfixed: true` មានន័យអ្វី? ហេតុអ្វីសមហេតុផល?

**លំហាត់:**
- ប្តូរ base image ទៅ version ចាស់ (ឧ. `eclipse-temurin:17-jre-alpine` ឬ `21-jre` ដោយគ្មាន alpine) → push → Trivy រកឃើញអ្វី? ចំនួន CVE ខុសគ្នាប៉ុន្មាន?
- run Trivy លើម៉ាស៊ីនអ្នក: `docker run aquasec/trivy image <image>` — លទ្ធផលដូច CI ឬអត់?

---

## Task 5 — Branch Protection + Badge

**GitHub Settings → Branches → Add rule សម្រាប់ `main`:**
- [x] Require a pull request before merging
- [x] Require status checks to pass: `unit-test`, `integration-test`
- [x] Do not allow bypassing (ទោះអ្នកជា owner)

**README badge:**
```markdown
![CI](https://github.com/<username>/mini-shop/actions/workflows/ci.yml/badge.svg)
```

**Dependabot (`.github/dependabot.yml`):**
```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: /
    schedule:
      interval: weekly
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
  - package-ecosystem: docker
    directory: /
    schedule:
      interval: weekly
```

**លំហាត់:**
- សាក push ផ្ទាល់ទៅ `main` → ត្រូវបានបដិសេធ
- បើក PR ដែល test បរាជ័យ → ប៉ូតុង merge ត្រូវ disabled
- ចាំ Dependabot PR ដំបូងមក → review វា → តើ CI run លើ PR របស់ bot ឬអត់?

---

## Task 6 — Documentation & Reflection

- [ ] `docs/decisions/004-ci-job-structure.md`
- [ ] `docs/decisions/005-security-scan-policy.md`
- [ ] `docs/runbooks/ci-failure.md` — "CI ក្រហម ត្រូវមើលអ្វីតាមលំដាប់?"
- [ ] `docs/learning-log.md` — ចម្លើយសំណួរឆ្លុះបញ្ចាំងទាំងអស់
- [ ] README: ផ្នែក "Development workflow" (branch → PR → CI → merge)

**សំណួរធំ (សម្រាប់ Phase ក្រោយ):**
1. CI push image រួច — **អ្នកណា** deploy វា? ឥឡូវនៅតែជាអ្នកដោយដៈ។ Phase A3 (Kubernetes) និង E (GitOps) ដោះស្រាយនេះ។
2. បើ CI ឆ្លង តែ app crash នៅ production — CI ខ្វះអ្វី? (smoke test? contract test?)
3. Secret `DB_PASSWORD` នៅក្នុង `.env` លើម៉ាស៊ីនអ្នក — ពេល deploy ទៅ server ពិត វាទៅនៅឯណា? (Phase B — Vault/K8s Secret)

---

## Progress tracker

| Task | ស្ថានភាព | ថ្ងៃបញ្ចប់ | ពេលវេលា CI run |
|---|---|---|---|
| 0 Repo → GitHub | ⬜ | | — |
| 1 Build + unit test | ⬜ | | ទី១: __ ទី២: __ |
| 2 Integration test | ⬜ | | |
| 3 Image → ghcr.io | ⬜ | | build មុន cache: __ ក្រោយ: __ |
| 4 Trivy | ⬜ | | CVE រកឃើញ: __ |
| 5 Protection + badge | ⬜ | | — |
| 6 Docs | ⬜ | | — |

**បន្ទាប់:** Phase A3 — Kubernetes លើ `kind` (Deployment, Service, ConfigMap, Secret, probes)