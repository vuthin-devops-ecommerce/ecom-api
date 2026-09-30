# ADR-005: គោលការណ៍ស្កេន image (Trivy) — មុន push, CRITICAL block, HIGH report

| | |
|---|---|
| **ស្ថានភាព** | Proposed (ក្លាយជា Accepted ពេលអ្នករៀនឆ្លើយសំណួរខាងក្រោម និង run លើ `main` បៃតង) |
| **ថ្ងៃ** | 2026-09-29 |
| **Code** | `.github/workflows/ci.yml` job `docker`: step "Build image (load)" → "Scan image with Trivy" → "Trivy report HIGH" → "Push image" |
| **ពាក់ព័ន្ធ** | ADR-004 (job structure), phase-a2-plan Task 4, runbook `ci-failure.md` (Task 6) |

## បរិបទ

Image `mini-shop` ផលិតដោយ CI (Task 3) ហើយនឹង deploy ទៅ K8s (Phase A3)។ មុន image ចេញទៅ registry ត្រូវដឹងថាវាមាន
vulnerability ដែលគេស្គាល់ (CVE) ក្នុង OS package (alpine `apk`) ឬ Java dependency (jar ក្នុង `app.jar`) ឬអត់។
Tool ថ្មីតែមួយនៃដំណាក់នេះ: **Trivy** (`aquasecurity/trivy-action` pin SHA)។

សំណួរ ៣ ដែលត្រូវសម្រេច:
1. Scan **មុន** push ឬ **ក្រោយ** push?
2. Severity ណា block pipeline?
3. `ignore-unfixed` — រាប់ CVE ដែលគ្មាន fix ឬអត់?

## ជម្រើសដែលពិចារណា

### ១. ពេលណា scan

| ជម្រើស | ហេតុផលបដិសេធ / ទទួល |
|---|---|
| **A. Scan មុន push** (`load: true` → scan → `push: true`) | ✅ image ដែលមាន CRITICAL **មិនដែល**ដល់ registry → គ្មាននរណា pull `:latest` អាក្រក់ដោយចៃដន្យ; "gate" ពិត។ ❌ build-push ២ step (step ២ CACHED ~វិនាទី); image ដែល scan គឺ image ក្នុង daemon មិនមែន bytes ដែល push — ខុសគ្នាបានតែ digest manifest (content ដដែល) |
| B. Scan ក្រោយ push (`image-ref` ពី registry) | ✅ scan លើអ្វីដែល deploy ពិត (digest ដដែល)។ ❌ registry មាន image អាក្រក់ទោះ pipeline ក្រហម; `:latest` ផ្លាស់ទីទៅ image នោះរួច → ត្រូវ "untag" ដោយដៃ |
| C. Scan ដាច់ដោយឡែក (job/schedule) លើ registry | ✅ ចាប់ CVE **ថ្មី**ក្នុង image ចាស់ដែល deploy រួច (CVE database ប្តូររាល់ថ្ងៃ)។ ❌ មិនមែន gate; ជា **បំពេញ** មិនមែនជំនួស A — ពិចារណានៅ Phase ក្រោយ (schedule: cron) |

### ២. Severity ណា block

| ជម្រើស | ហេតុផល |
|---|---|
| **CRITICAL block · HIGH report** | ✅ ចាប់ផ្តើមតឹងលើអ្វីដែលអាចកែបានភ្លាម; HIGH ឃើញក្នុង log រាល់ run (step `exit-code: "0"`) → មានទិន្នន័យសម្រេចថាពេលណាតឹងបន្ថែម |
| CRITICAL,HIGH block | ❌ base image `eclipse-temurin:21-jre-alpine` ថ្មីៗអាចមាន HIGH ដែល fix ទើបចេញ → pipeline ក្រហមញឹក → "alert fatigue": មនុស្សចាប់ផ្តើម re-run ដោយមិនអាន → gate ឥតន័យ |
| MEDIUM+ | ❌ សម្រាប់ project រៀន ការក្រហមរាល់ថ្ងៃបង្រៀនតែការមិនអើពើ |

### ៣. `ignore-unfixed`

| ជម្រើស | ហេតុផល |
|---|---|
| **true** | ✅ block តែ CVE ដែល**មានវិធីកែ** (upgrade package/base image)។ CVE គ្មាន fix → block ក៏អ្នកធ្វើអ្វីមិនបាន ក្រៅពី rebuild ដដែលៗ |
| false | ❌ pipeline ក្រហមរហូតដល់ upstream ចេញ patch — ថ្ងៃ/សប្តាហ៍ — release ទាំងអស់ជាប់ដោយគ្មានសកម្មភាពអាចធ្វើ |

## ការសម្រេចចិត្ត

1. **Scan មុន push** (ជម្រើស A): `build-push-action` `load: true` (គ្មាន push) → Trivy → `build-push-action` `push: true` (cache hit)។
2. **Block: `severity: CRITICAL`, `exit-code: "1"`, `ignore-unfixed: true`**។
3. **Report: `severity: HIGH`, `exit-code: "0"`** — step ដាច់ដោយឡែក, មិនដែលធ្វើឱ្យ job ក្រហម, `skip-setup-trivy: true`។
4. Trivy version pin ដោយ SHA របស់ action (v0.36.0); CVE database update ដោយ Trivy ខ្លួនឯងរាល់ run (ដោយចេតនា — database ចាស់ = scan ឥតន័យ)។
5. **ថ្ងៃ review:** ក្រោយ HIGH report មាន ២ សប្តាហ៍ (ឬ Phase A3 ចប់) → សម្រេចថាតឹងទៅ `CRITICAL,HIGH` ឬអត់ ដោយផ្អែកលើចំនួន HIGH ពិតក្នុង tracker។

## ផលវិបាក

- (+) Registry មានតែ image ដែលឆ្លង test + scan → Phase A3 `kubectl set image …:<sha>` ទុកចិត្តបាន
- (+) HIGH count ក្នុង log រាល់ run = ទិន្នន័យសម្រាប់ review date មិនមែនអារម្មណ៍
- (−) job `docker` យឺតជាង: Trivy download DB (~10–30s) + scan (~10s) ×2 step (step ២ មិន download binary តែ scan ម្តងទៀត)
- (−) `ignore-unfixed: true` = CVE unfixed **មើលមិនឃើញ**ក្នុង gate — ត្រូវពឹងលើ Dependabot (Task 5) និង scan schedule (ជម្រើស C ក្រោយ) ដើម្បីដឹងពេល fix ចេញ
- (−) Scan image ក្នុង daemon (A) មិនមែន digest ដែល push — ទទួលយក: content ដដែល, ខុសតែ metadata manifest
- (−) CVE ថ្មីចេញក្រោយ image push រួច → pipeline នេះមិនដឹង (scan តែពេល build) — ចន្លោះដែលជម្រើស C បំពេញ

## សំណួរសម្រាប់អ្នករៀន (ឆ្លើយក្នុង `docs/journey/phase-a2/learning-log.md` មុនប្តូរ status ជា Accepted)

1. ជម្រើស B "scan លើអ្វីដែល deploy ពិត" ស្តាប់ទៅសុវត្ថិភាពជាង — ហេតុអ្វី ADR នេះនៅតែជ្រើស A? តើមានករណីណាដែល A ខុស B?
2. `ignore-unfixed: true` ធ្វើឱ្យ CVE មួយចំនួន "មើលមិនឃើញ" — អ្នកនឹងដឹងដោយរបៀបណាពេល fix ចេញ? (hint: Dependabot docker ecosystem, Task 5)
3. បើថ្ងៃស្អែក Trivy រកឃើញ CRITICAL ក្នុង `eclipse-temurin:21-jre-alpine` ហើយ Temurin មិនទាន់ចេញ image ថ្មី — pipeline ក្រហម, អ្នកត្រូវ release ជាបន្ទាន់។ ធ្វើអ្វី? (hint: `.trivyignore` + ADR/issue កំណត់ថ្ងៃផុត — មិនមែន `exit-code: "0"`)
