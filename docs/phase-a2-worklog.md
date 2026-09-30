# Phase A2 — Worklog (អ្វីដែលធ្វើពិត តាម Task)

> ឯកសារនេះខុសពី `phase-a2-plan.md` (ផែនការ/មេរៀន) និង `learning-log.md` (ការឆ្លុះបញ្ចាំងរបស់អ្នករៀន)។
> វាជា **កំណត់ហេតុអនុវត្ត**: Task នីមួយៗ — គោលដៅ · អ្វីដែលធ្វើ (ជំហាន/command) · file · លទ្ធផល/ភស្តុតាង · អ្វីដែលត្រូវមើល។
> ថ្ងៃ: 2026-09-28 → 2026-09-29 · branch `develop` → PR → `main` · repo `vuthin-devops-ecommerce/ecom-api`

## សង្ខេប

| Task | គោលដៅ | លទ្ធផល | PR |
|---|---|---|---|
| 0 | repo លើ GitHub, branch `develop` | ✅ | — |
| 1 | CI build + unit test | ✅ 1m09s → 33s (cache) | #2 |
| 2 | integration test ក្នុង CI, ២ job | ✅ 1m39s | #2 |
| 3 | image → ghcr.io លើ `main` | ✅ `mini-shop:1279d03` | #2, #3 |
| 4 | Trivy gate | ✅ block CVE ពិត → fix → `mini-shop:e8f7f68` | #4, #5 |
| 5 | branch protection, badge, Dependabot | ✅ repo public | #6 |
| 6 | docs: runbook, ADR, DoD | ✅ (ADR → Accepted នៅរង់ចាំអ្នក) | #7 |

Pipeline ចុងក្រោយ: `push` → `unit-test` → `integration-test` → (`main` ប៉ុណ្ណោះ) `docker`: build → Trivy CRITICAL → Trivy HIGH report → push `ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>` + `:latest`។

---

## Task 0 — Repo → GitHub

**គោលដៅ:** code លើ GitHub, `main` = release, `develop` = ធ្វើការ។

**ធ្វើ:** repo មានស្រាប់ (`ecom-api`, private ពេលនោះ) → `git switch -c develop` → `git push -u origin develop` → folder `.github/workflows/`។

**រកឃើញមុន push:** `mini-shop/mvnw` mode `100644` ក្នុង git (Windows មិនរក្សា exec bit) → Linux runner នឹង `Permission denied` → `git update-index --chmod=+x mini-shop/mvnw`។

---

## Task 1 — CI: build + unit test

**គោលដៅ:** រាល់ push → compile + unit test ក្នុង ~1 នាទី។

**File:** `.github/workflows/ci.yml` (job `build` → ក្រោយប្តូរឈ្មោះ `unit-test`)។

**ការសម្រេចចិត្តរចនា:**
- `defaults.run.working-directory: mini-shop` (monorepo) — path ក្នុង action (`cache-dependency-path`, upload `path`) សរសេរពី root
- action pin SHA + comment version (checkout v7.0.1, setup-java v6.0.1, upload-artifact v7.0.1); runner `ubuntu-24.04` មិនមែន `-latest`
- `./mvnw -B verify -DskipITs`; `if: always()` លើ upload `surefire-reports`
- comment ជាខ្មែរ ៣ ផ្នែក (អ្វី / សេណារីយ៉ូ / សំណួរ) រាល់បន្ទាត់

**ជួបពិត:** run local ដោយដក env `SPRING_DATASOURCE_*` (ដូច runner) → `MiniShopApplicationTests.contextLoads` ធ្លាក់ (ត្រូវការ DB តែឈ្មោះ `*Tests` = unit) → លុប (IT គ្របរួច)។ `./mvnw` លើ laptop download មិនបាន (`curl (35) schannel`) → ប្រើ `mvn` 3.9.16 ដដែល។

**លទ្ធផល:** run ១ 1m09s (cache miss, build 52s) · run ២ 33s (cache hit, build 17s) · លំហាត់ "ធ្វើឱ្យខូច" → ក្រហមក្នុង 28s, artifact upload ទោះធ្លាក់។

---

## Task 2 — Integration test, ២ job (ADR-004)

**គោលដៅ:** `OrderControllerIT` (Testcontainers `postgres:17`) run ក្នុង CI, fail fast។

**File:** `ci.yml` (job `unit-test` `./mvnw test` → job `integration-test` `needs: unit-test`, `./mvnw verify -DskipUnitTests`), `pom.xml` (property `skipUnitTests` → surefire `<skipTests>`), `docs/decisions/004-ci-job-structure.md`។

**ជួបពិត:** `-Dsurefire.skip=true` (សាកដំបូង) — Maven មិនស្គាល់, unit test run ២ ដង **ដោយស្ងាត់** → មេរៀន: ផ្ទៀងផ្ទាត់ flag ក្នុង log មិនមែន exit code។

**លទ្ធផល:** សរុប 1m39s · unit 42s (cache miss ព្រោះ pom ប្តូរ) · IT 51s (pull `postgres:17` 8s, IT 22s) · log "Tests are skipped." ពី surefire ✅។

---

## Task 3 — Image → ghcr.io

**គោលដៅ:** merge → `main` → image `<sha>` + `:latest` លើ registry។

**File:** `ci.yml` job `docker` (`needs` ទាំង ២, `if: main && push`, `permissions: packages: write` កម្រិត job, buildx, login `GITHUB_TOKEN`, metadata `type=sha,prefix=` + `latest`, build-push `context: mini-shop`, cache `type=gha,mode=max`, step summary)។

**ការសម្រេចចិត្ត:** image name `mini-shop` (តាម phase-a3-plan) មិនមែន `ecom-api`; `:latest` រក្សា (ផលិត តែមិន deploy — CLAUDE.md §4 ចំពោះ "ការប្រើ"); `setup-buildx-action` បន្ថែម (ចាំបាច់សម្រាប់ cache gha)។

**ជួបពិត:** run ដំបូងលើ `main` ធ្លាក់ 1s — `invalid tag "...:-tag-short-git-sha-7-..."`: comment `# …` **ក្នុង** YAML block scalar `tags: |` ជាអត្ថបទ → PR #3 fix។

**លទ្ធផល:** build 1m32s (go-offline 24s, package 12s, **export cache 45s**) → `mini-shop:1279d03` digest `60c29aae…`; run ក្រោយ build 14s (CACHED)។

---

## Task 4 — Trivy gate (ADR-005)

**គោលដៅ:** CRITICAL CVE → pipeline ក្រហម, image មិនដល់ registry។

**File:** `ci.yml` job `docker` ៤ step: build `load: true` → Trivy `severity: CRITICAL, exit-code 1, ignore-unfixed` → Trivy HIGH `exit-code 0` → push (CACHED); `docs/decisions/005-security-scan-policy.md`។

**ជួបពិត (gate block ពិត):** run ដំបូង `Total: 3 (CRITICAL: 3)` — `tomcat-embed-core 11.0.24` (CVE-2026-65182/-65905/-68525, fix 11.0.25) → step push **skipped** → `pom.xml` `<tomcat.version>11.0.25</tomcat.version>` (Boot 4.1.2 មិនទាន់មាន) → ផ្ទៀងផ្ទាត់ local: jar មាន 11.0.25, `trivy fs` Clean → PR #5 → run ២ CRITICAL 0, HIGH 2 (jackson-databind, report-only), push `mini-shop:e8f7f68`។

**ជួបពិត (laptop):** `docker build` local ធ្លាក់ `wget: Failed to fetch … apache-maven` → container មិនទុកចិត្ត **Somansa Root CA** (TLS interception network ការិយាល័យ) → មិនកែ Dockerfile; តម្លើង Trivy 0.74.0 Windows (`~/tools/trivy`) → scan base image: alpine 0 CVE vs ubuntu `17-jre` 42 (ទាំងអស់ unfixed)។

---

## Task 5 — Branch protection, badge, Dependabot

**គោលដៅ:** merge ទៅ `main` តែពេល check ឆ្លង; update dependency ដោយ PR។

**ធ្វើ:** API branch protection → **403** (private repo, plan Free) → សម្រេច (អ្នក): repo **public** → protection: PR required (0 approval), checks `unit-test` + `integration-test`, `enforce_admins`, no force-push/delete, `strict: false` (merge-commit flow)។ `.github/dependabot.yml`: maven/docker/docker-compose `/mini-shop`, github-actions `/`, weekly, Spring Boot grouped, major ignored។ README: badge, `.github/` រចនាសម្ព័ន្ធ, ផ្នែក "Development workflow"។

**លទ្ធផល:** PR #6 `mergeStateStatus: CLEAN` ក្រោយ check; Dependabot Updates run ×8 (4 ecosystem × version+security) — PR មិនទាន់មាន (2026-09-29)។

---

## Task 6 — Docs

**ធ្វើ:** `docs/runbooks/ci-failure.md` (លំដាប់ triage + symptom ពិត ៩ + laptop vs CI + command reproduce), learning-log Task 6 (DoD review 6/8 ✅), README `runbooks/`។

**នៅសល់ (អ្នក):** ADR-004/005 → Accepted (ឆ្លើយសំណួរចុង ADR), ចម្លើយ learning-log Task 0–5, review Dependabot PR ដំបូង។

---

## អ្វីដែលអ្នកគួរអាចធ្វើបានក្រោយ A2

- មើល run ក្រហម → ដឹងស្រទាប់បញ្ហាពីឈ្មោះ job ក្នុង 30 វិនាទី (runbook §០)
- ពន្យល់ថាហេតុអ្វី image `:<sha>` ជា artifact ដែល deploy — មិនមែន branch, មិនមែន `:latest`
- បន្ថែម step ថ្មីក្នុង `ci.yml` ដោយ pin SHA + permission តិចបំផុត + ផ្ទៀងផ្ទាត់ក្នុង log ថាវាមានឥទ្ធិពល
