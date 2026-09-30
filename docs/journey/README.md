# Journey — ដំណើររៀន DevOps

> **គោលបំណង:** ផែនការ, កំណត់ហេតុ, និងការឆ្លុះបញ្ចាំងរបស់ Phase នីមួយៗ · **អ្នកអាន:** អ្នករៀន, mentor · **Update:** 2026-09-30
> ឯកសារទាំងនេះជា**ប្រវត្តិ** — ការពិតបច្ចុប្បន្នរបស់ system នៅ [architecture](../architecture.md), [guides](../guides/), [reference](../reference/)។

## ស្ថានភាព

Source of truth = tracker ចុង `plan.md` នីមួយៗ។ តារាងនេះគ្រាន់តែសង្ខេប (2026-09-30)។

| Phase | ប្រធានបទ | Task | សំណួរនៅទទេ | Folder |
|---|---|---|---|---|
| A | modular monolith, Flyway, Testcontainers, Docker | ✅ 7/7 | 20 | [phase-a/](phase-a/) |
| A2 | GitHub Actions, ghcr.io, Trivy, branch protection | 6/7 (ADR 004/005 → Accepted) | 38 | [phase-a2/](phase-a2/) |
| A3 | Kubernetes (kind), probes, Ingress, rolling update, HPA | 4/9 (Task 4 ដើរ) | 11 | [phase-a3/](phase-a3/) |
| A4 | Terraform | 0/10 | — | [phase-a4/](phase-a4/) (plan មាន version drift — កែពេលចាប់ផ្តើម) |
| B | បំបែក `payment`, Helm, Vault | មិនទាន់សរសេរ | | |

## File ក្នុង Phase នីមួយៗ

| File | សំណួរ | អ្នកសរសេរ |
|---|---|---|
| `plan.md` | **ត្រូវធ្វើអ្វី?** — មេរៀន, DoD, Progress tracker | ផែនការដើម (+ Claude update tracker) |
| `worklog.md` | **បានធ្វើអ្វីពិត?** — command, file, លទ្ធផល, លេខ, symptom | Claude |
| `learning-log.md` | **ហេតុអ្វី?** — ចម្លើយសំណួរឆ្លុះបញ្ចាំង | **អ្នករៀន** (Claude review, មិនឆ្លើយជំនួស) |

## ច្បាប់

- Phase ថ្មី → folder `phase-<x>/` ជាមួយ file ទាំង ៣; worklog រាយ Task ទាំងអស់ជា "នឹងធ្វើ" តាំងពីដំបូង។
- Symptom ពិតដែលអាចកើតម្តងទៀត → **runbook** ([../runbooks/](../runbooks/)) មិនមែនតែ worklog។
- ការសម្រេចចិត្តរចនា → **ADR** ([../decisions/](../decisions/))។
- ការពិតដែល Phase បង្កើត (config ថ្មី, endpoint, job CI) → update **reference** ក្នុង PR ដដែល។
