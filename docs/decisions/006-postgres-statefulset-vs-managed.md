# ADR-006: Postgres ជា StatefulSet ក្នុង cluster (សម្រាប់រៀន) — production ត្រូវ managed DB

| | |
|---|---|
| **ស្ថានភាព** | Proposed (ក្លាយជា Accepted ពេលអ្នករៀនឆ្លើយសំណួរខាងក្រោម និងលំហាត់ Task 2 ធ្វើរួច) |
| **ថ្ងៃ** | 2026-09-29 |
| **Code** | `k8s/postgres/{statefulset,service,configmap,secret.example}.yaml` |
| **ពាក់ព័ន្ធ** | ADR-002 (order schema), ADR-007 (rolling update — app), ADR-008 (Terraform ព្រំដែន platform/app), phase-a3-plan Task 2 |

## បរិបទ

Phase A3 ដក `compose.yaml` ចេញ — DB ត្រូវ run ក្នុង Kubernetes ដែរ។ សំណួរ ២:
1. ក្នុង K8s: **Deployment** ឬ **StatefulSet**?
2. Production ពិត: Postgres **ក្នុង cluster** ឬ **managed service** (RDS, Cloud SQL, …)?

អ្វីដែល DB ត្រូវការ ដែល stateless app (`minishop-app`) មិនត្រូវការ: disk ដែលរស់ក្រោយ pod ងាប់, identity ស្ថិរ (client ភ្ជាប់ទៅ primary ជាក់លាក់), start/stop តាមលំដាប់, backup, failover។

ទិន្នន័យពិត (2026-09-29, kind): PVC `data-postgres-0` Bound លើ StorageClass `standard` (rancher local-path) → PV ជា folder ក្នុង node container `minishop-worker2` → pod ជាប់ node នោះ (RWO + local)។ `kind delete cluster` = data បាត់។

## ជម្រើសដែលពិចារណា

### ១. ក្នុង K8s: Deployment vs StatefulSet

| ជម្រើស | ហេតុផលបដិសេធ / ទទួល |
|---|---|
| Deployment + PVC ធម្មតា | ❌ pod ឈ្មោះចៃដន្យ (`postgres-7f9c…`); replicas > 1 = pod ២ mount PVC ដដែល (RWO ខូច, ឬ data corrupt); rolling update បង្កើត pod ថ្មីមុនលុបចាស់ → ២ Postgres លើ data ដដែល។ សម្រាប់ DB "RollingUpdate default" គឺគ្រោះថ្នាក់ |
| **StatefulSet + volumeClaimTemplates** | ✅ ឈ្មោះ/DNS ស្ថិរ `postgres-0.postgres`, PVC per pod រស់ក្រោយ delete, update តាមលំដាប់ (OrderedReady), ១ pod ក្នុងពេលតែមួយ។ ❌ replicas: 1 = គ្មាន HA; replicas > 1 **មិនមែន** replication ដោយស្វ័យប្រវត្តិ (pod ២ = DB ទទេ ២ ដាច់ពីគ្នា) |
| Postgres Operator (CloudNativePG, Zalando) | ✅ replication, failover, backup ជា CRD។ ❌ tool ថ្មី ១ ទៀត — "one tool per stage" រំលោភ; សិក្សាក្រោយ (Phase B/D) បើនៅក្នុង cluster |

### ២. Production: ក្នុង cluster vs managed

| លក្ខណៈ | Postgres ក្នុង K8s (StatefulSet/operator) | Managed (RDS/Cloud SQL) |
|---|---|---|
| Backup / PITR | អ្នកសរសេរ (CronJob + pg_dump/WAL-G) និង**សាក restore** ខ្លួនឯង | ចុចប៊ូតុង, តេស្តដោយ provider |
| Failover | operator ត្រូវការ; StatefulSet ធម្មតា = គ្មាន | Multi-AZ, ស្វ័យប្រវត្តិ |
| Upgrade major (17 → 18) | អ្នក (pg_upgrade ក្នុង pod, downtime) | maintenance window |
| Disk | PV ជាប់ node/zone; node ងាប់ = pod Pending (ឃើញពិតលើ kind) | provider គ្រប់គ្រង |
| ចំណាយ | compute ដដែល តែ**ពេលមនុស្ស** ច្រើន | ថ្លៃជាង VM ~1.5–2× តែគ្មាន on-call DB |
| Portability | ដូចគ្នាគ្រប់ cloud/kind | lock-in ផ្នែក config, ចេញបានដោយ dump |
| មេរៀន DevOps | ឃើញ PVC/probe/StatefulSet ពិត | មើលមិនឃើញខាងក្នុង |

## ការសម្រេចចិត្ត

1. **Phase A3–A4 (kind, រៀន): StatefulSet `replicas: 1`** + headless Service + `volumeClaimTemplates` + `postgres:17` (00-tech-stack)។ គោលដៅ: យល់ identity/PVC/probe — មិនមែន HA។
2. **មិនប្រើ Deployment** សម្រាប់ DB ទោះ replicas: 1 — ដើម្បីកុំឱ្យ rolling update បង្កើត Postgres ២ លើ disk ដដែល។
3. **Production ពិត (ក្រៅ scope repo នេះ): managed DB** — ADR នេះកត់ថា StatefulSet ទីនេះជា **simulation** មិនមែន pattern ដែលត្រូវចម្លងទៅ production ដោយគ្មាន operator + backup + failover ដែលបានសាកល្បង។ Terraform (A4, ADR-008) នឹងបង្កើត "platform" — DB managed ស្ថិតក្នុងស្រទាប់នោះ មិនមែនក្នុង `k8s/`។
4. Password តាម Secret (`secret.yaml` gitignored, `.example` commit) — Phase B ជំនួសដោយ External Secrets/Vault។

## ផលវិបាក

- (+) ADR-002 schema/Flyway មិនប្តូរ — app មើលឃើញ `jdbc:postgresql://postgres:5432/minishop` ដូច compose
- (+) លំហាត់ delete pod / delete STS បង្ហាញ PVC lifecycle ពិត — មេរៀនដែលចង់បាន
- (−) គ្មាន backup: `kind delete cluster` = data បាត់ — ទទួលយក (dev data តែប៉ុណ្ណោះ, seed ដោយ Flyway `R__seed_dev_data.sql`)
- (−) pod ជាប់ node worker2 (local-path RWO) — self-healing Task 5 សម្រាប់ **app** ប៉ុណ្ណោះ; DB node ងាប់ = app 503 (readiness) — សេណារីយ៉ូល្អសម្រាប់ runbook `k8s-pod-not-ready.md`
- (−) `kubectl apply -f k8s/postgres/` apply `secret.example.yaml` ដែរ (ឈ្មោះ object ដដែល) — Task 6 Kustomize រាយ resource ជាក់លាក់

## សំណួរសម្រាប់អ្នករៀន (ឆ្លើយក្នុង `learning-log.md` មុនប្តូរ status ជា Accepted)

1. StatefulSet `replicas: 2` នឹងឱ្យអ្វី? (hint: PVC ២, DB ២ ដាច់ពីគ្នា, គ្មាន replication) — ហេតុអ្វី "scale" DB ខុសពី scale app?
2. លំហាត់ ២: `delete statefulset` → PVC នៅ។ បើអ្នកចង់លុប data ពិត ត្រូវធ្វើអ្វី? ហេតុអ្វី K8s ធ្វើឱ្យវាពិបាកដោយចេតនា?
3. បើក្រុមអ្នកមាន ៣ នាក់ គ្មាន DBA — production ជ្រើសអ្វី? ហើយបើមាន DBA ១ នាក់ + តម្រូវការ on-prem?
