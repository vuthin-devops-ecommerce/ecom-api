# ADR-007: Rolling update — maxSurge 1 / maxUnavailable 0 + preStop sleep + graceful shutdown

| | |
|---|---|
| **ស្ថានភាព** | Proposed (ក្លាយជា Accepted ពេលអ្នករៀនឆ្លើយសំណួរខាងក្រោម) |
| **ថ្ងៃ** | 2026-09-30 |
| **Code** | `k8s/app/deployment.yaml` (`strategy`, `lifecycle.preStop`, `terminationGracePeriodSeconds`), `k8s/app/configmap.yaml` (`SERVER_SHUTDOWN`, `SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE`) |
| **ពាក់ព័ន្ធ** | ADR-006 (DB), `guides/release.md`, journey A3 Task 5 |

## បរិបទ

Release ថ្មីត្រូវប្តូរ pod ទាំង ២ ដោយ **downtime = 0** (DoD A3)។ វាស់ពិត 2026-09-30: load ~2–3 req/s តាម Ingress (`curl -m 2` → `rollout.log`)។

| Run | Setup | Request | non-200 | សម្គាល់ពី nginx log |
|---|---|---|---|---|
| 5.1a | RollingUpdate 1/0, **គ្មាន preStop** | 51 | **2** | `connect() failed (111: Connection refused)` ទៅ IP pod ចាស់ |
| 5.1b | fix apply, តែ pod ចាស់ (កំពុងលុប) កើតពី template ចាស់ | 59 | **2** | 499 ក្រោយ 2s — pod ចាស់ទទួល SIGTERM ភ្លាម |
| **5.1c** | **pod ចាស់មាន preStop 10s + graceful** | 69 | **0** | គ្មាន error |
| 5.2 | `rollout undo` | 66 | 0 | |
| 5.3 | `delete pod` **ទាំង ២** | 36 | **16** (503, ~7s) | គ្មាន endpoint Ready — startup ~5–7s |
| 5.4 | deploy ខូច (DB URL ខុស) | 165 | 0 | pod ថ្មី CrashLoop; pod ចាស់នៅបម្រើ |

មូលហេតុ 5.1a: ពេល pod ត្រូវលុប K8s ផ្ញើ SIGTERM **ព្រមគ្នា** នឹងការដក pod ពី EndpointSlice — ingress-nginx ដឹងយឺតពីរបីវិនាទី ហើយនៅផ្ញើទៅ pod ដែលបិទរួច។

## ជម្រើសដែលពិចារណា

### Strategy

| ជម្រើស | ហេតុផល |
|---|---|
| **maxSurge 1 / maxUnavailable 0** | ✅ មិនដែលតិចជាង 2 Ready; pod ថ្មីខូច → rollout ជាប់, ចាស់នៅ (5.4 បញ្ជាក់)។ ❌ ត្រូវការ resource បន្ថែម 1 pod ពេល rollout |
| maxSurge 0 / maxUnavailable 1 | ✅ គ្មាន resource បន្ថែម។ ❌ ពេល rollout capacity ពាក់កណ្តាល; pod ថ្មីខូច → នៅសល់ 1 pod |
| Recreate | ❌ downtime = startup time (ដូច 5.3) |
| Canary / blue-green | ✅ ចាប់ bug តាម traffic ពិតមុន 100%។ ❌ ត្រូវការ tool ថ្មី (Argo Rollouts/service mesh) — Phase E |

### Shutdown

| ជម្រើស | ហេតុផល |
|---|---|
| គ្មានអ្វី (default) | ❌ 5.1a: 2/51 fail |
| graceful shutdown តែឯង | ❌ Tomcat ឈប់ទទួល connection ថ្មី ខណៈ nginx នៅផ្ញើ → 499/502 (5.1b) |
| **preStop sleep 10s + graceful 20s** | ✅ 5.1c: 0/69។ ❌ រាល់ pod termination យឺត +10s; rollout 2 pod ~30s |
| readiness គ្មាន / fail លឿនក្រោយ SIGTERM | ❌ endpoint removal មិនអាស្រ័យលើ readiness នៅពេល termination |

## ការសម្រេចចិត្ត

1. `strategy: RollingUpdate`, `maxSurge: 1`, `maxUnavailable: 0`, `replicas: 2`។
2. `lifecycle.preStop.sleep.seconds: 10` (action ដើមរបស់ K8s — មិនពឹង `/bin/sleep` ក្នុង image)។
3. `SERVER_SHUTDOWN=graceful`, `SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=20s` តាម ConfigMap (មិន rebuild image)។
4. `terminationGracePeriodSeconds: 30` ≥ preStop 10 + drain 20។
5. Rollback = `kubectl rollout undo` ឬ `set image` sha ចាស់ ([guides/release.md](../guides/release.md))។

## ផលវិបាក

- (+) Rolling update / rollback / deploy ខូច: 0 client error (វាស់)
- (+) Readiness + maxUnavailable 0 = deploy ខូចមិនប៉ះ traffic (5.4)
- (−) Self-healing **មិន**ការពារពេល pod **ទាំងអស់**ងាប់ព្រមគ្នា (5.3: ~7s 503) — replicas ការពារការខូច **ម្តងមួយ**។ Mitigation: `PodDisruptionBudget minAvailable: 1` (ការពារ `kubectl drain`/eviction តែមិនការពារ `delete pod`), `topologySpreadConstraints` (pod លើ node ខុសគ្នាដោយធានា)
- (−) Pod termination យឺត 10s — HPA scale-down (Task 7) ក៏យឺត 10s ដែរ
- (−) `kubectl rollout undo` មិន update `last-applied-configuration` → git manifest ត្រូវ update ដោយដៃ (ឬ Phase E GitOps)

## ពេល review ឡើងវិញ

Phase E (canary), ឬបើ startup > 30s (startupProbe budget), ឬ replicas > 5 (maxSurge % ជំនួសលេខ)។

## សំណួរសម្រាប់អ្នករៀន (`docs/journey/phase-a3/learning-log.md`)

1. ហេតុអ្វី 5.1b នៅតែ fail ទោះ apply fix រួច? (hint: preStop ជារបស់ pod template — pod ណាត្រូវលុប?)
2. 5.3 ឱ្យ 503 ~7s — ហេតុអ្វី preStop មិនជួយ? អ្វីអាចជួយ?
3. `maxSurge 1 / maxUnavailable 0` vs `0 / 1` — resource និង availability ខុសគ្នាយ៉ាងណា?
4. RollingUpdate ខ្វះអ្វីដែល canary មាន? (hint: 5.4 ចាប់បានព្រោះ crash — bug ដែល app start បាន តែ response ខុស?)
