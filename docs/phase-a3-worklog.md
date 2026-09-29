# Phase A3 — Worklog (អ្វីដែលធ្វើពិត / នឹងធ្វើ តាម Task)

> ខុសពី `phase-a3-plan.md` (ផែនការ/មេរៀន) និង `learning-log.md` (ការឆ្លុះបញ្ចាំងរបស់អ្នករៀន)។
> Task នីមួយៗ — គោលដៅ · អ្វីដែលធ្វើ/នឹងធ្វើ (ជំហាន, command) · file · លទ្ធផល/ភស្តុតាង · អ្វីដែលត្រូវមើល។
> ចាប់ផ្តើម 2026-09-29 · cluster `kind-minishop` លើ laptop · image ពី A2: `ghcr.io/vuthin-devops-ecommerce/mini-shop:e8f7f68`

## សង្ខេប

| Task | គោលដៅ | ស្ថានភាព | ភស្តុតាង |
|---|---|---|---|
| 0 | kubectl + kind | ✅ 2026-09-29 | `scripts/check-env.sh` 10 ok |
| 1 | cluster 3 node + ingress-nginx | ✅ 2026-09-29 | `kubectl get nodes` Ready ×3, `curl localhost` 404 nginx |
| 2 | namespace + Postgres StatefulSet | ✅ 2026-09-29 | `postgres-0` Running, PVC រស់ក្រោយ delete |
| 3 | app Deployment + ConfigMap + probes | ⬜ នឹងធ្វើ | |
| 4 | Service + Ingress `minishop.local` | ⬜ | |
| 5 | rolling update 0 downtime, self-heal, rollback | ⬜ | |
| 6 | Kustomize base + overlays | ⬜ | |
| 7 | metrics-server + HPA + k6 | ⬜ | |
| 8 | docs: ADR-006/007, runbooks, README | 🟡 ADR-006 Proposed | |

---

## Task 0 — Tool ✅

**គោលដៅ:** kubectl, kind លើ Windows, version pin ក្នុង `00-tech-stack.md`។

**ធ្វើ:** kubectl v1.36.1 មានស្រាប់ (Docker Desktop) → រក្សា (skew ±1 ពី server 1.37)។ kind **v0.33.0** download → `C:\Users\user\tools\kind\kind.exe` → user PATH (+ `tools\trivy`)។ `scripts/check-env.sh` ថ្មី (java/mvn/docker/git/gh/trivy/kubectl/kind/cluster)។ `00-tech-stack.md` §៨: kind v0.33.0, kindest/node v1.37.0, ingress-nginx controller-v1.15.1, metrics-server v0.9.0។

**កែ plan drift:** `postgres:16-alpine` → `postgres:17`; URL ingress `main` → tag; metrics-server `latest` → v0.9.0; kind `latest` → v0.33.0; `<username>` → org។

---

## Task 1 — Cluster + ingress ✅

**File:** `k8s/kind-config.yaml` — 1 control-plane (label `ingress-ready=true`, port 80/443 → laptop) + 2 worker, image `kindest/node:v1.37.0@sha256:a1ed56cf…` (pin digest)។

**Command:**
```bash
kind create cluster --name minishop --config k8s/kind-config.yaml        # ~1 នាទី
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/kind/deploy.yaml
kubectl -n ingress-nginx patch deployment ingress-nginx-controller --type=merge \
  -p '{"spec":{"template":{"spec":{"nodeSelector":{"kubernetes.io/os":"linux","ingress-ready":"true"}}}}}'
bash scripts/kind-trust-ca.sh <somansa-root-ca.pem> minishop              # network ការិយាល័យប៉ុណ្ណោះ
```

**ជួបពិត ២:**
1. `ErrImagePull … x509: certificate signed by unknown authority` — kind node (Linux container) មិនទុកចិត្ត Somansa CA → `scripts/kind-trust-ca.sh` (docker cp CA → `update-ca-certificates` → restart containerd, node ទាំង ៣) → pod retry → Running។ ត្រូវ run ម្តងទៀតរាល់ `kind create`។
2. Controller schedule លើ `worker2` (manifest v1.15.1 លែងមាន `nodeSelector: ingress-ready`) → hostPort លើ node គ្មាន port mapping → patch nodeSelector → control-plane។

**លទ្ធផល:** 3 node Ready v1.37.0 · controller Running លើ control-plane · `curl localhost` → 404 nginx។

---

## Task 2 — Namespace + Postgres StatefulSet ✅ (ADR-006 Proposed)

**File:** `k8s/namespace.yaml`, `k8s/postgres/configmap.yaml` (DB, USER, PGDATA subfolder), `secret.example.yaml` (commit) + `secret.yaml` (gitignored, `.gitignore` `k8s/**/secret.yaml`), `statefulset.yaml` (`postgres:17`, envFrom, readiness/liveness `pg_isready`, requests/limits, `volumeClaimTemplates` 1Gi RWO), `service.yaml` (headless `clusterIP: None`), `.gitattributes` (LF), `docs/decisions/006-postgres-statefulset-vs-managed.md`។

**Command:**
```bash
cp k8s/postgres/secret.example.yaml k8s/postgres/secret.yaml   # កែ password
kubectl apply -f k8s/namespace.yaml && kubectl apply -f k8s/postgres/
kubectl -n minishop rollout status statefulset/postgres
kubectl -n minishop exec postgres-0 -- psql -U minishop -d minishop -c '\l'
```

**លទ្ធផល/ភស្តុតាង:** `postgres-0` Running 1/1 លើ `worker2`; PVC `data-postgres-0` Bound 1Gi `standard` (local-path); លំហាត់ delete pod + delete/apply STS → PVC UID ដដែល, table `t` 1 row នៅ។

**ជួបពិត:** `apply -f k8s/postgres/` apply `secret.example.yaml` ដែរ (object ដដែល) — Task 6 រាយ resource; PV local-path ជាប់ node → pod ជាប់ worker2 (ហេតុផល ADR-006)។

---

## Task 3 — App Deployment + ConfigMap + probes ⬜ (នឹងធ្វើ)

**គោលដៅ:** `minishop-app` ×2 replica run ពី image ghcr.io, config តាម ConfigMap, password តាម Secret ដដែល, probe ៣ ប្រភេទ។

**នឹងធ្វើ:**
1. ពិនិត្យ package `mini-shop` លើ ghcr public ឬ private → private: `kubectl create secret docker-registry ghcr-creds` + `imagePullSecrets`; public: មិនត្រូវការ។
2. `k8s/app/configmap.yaml`: `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/minishop`, USERNAME, `SPRING_PROFILES_ACTIVE`, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0`។
3. `k8s/app/deployment.yaml`: replicas 2, `RollingUpdate maxSurge 1 / maxUnavailable 0`, image `…/mini-shop:e8f7f68` (pin sha), `env SPRING_DATASOURCE_PASSWORD ← secretKeyRef postgres-secret`, startupProbe (`/actuator/health/liveness`, failureThreshold 30 × 2s), livenessProbe, readinessProbe (`/readiness`), resources (250m/384Mi, limit 512Mi), securityContext non-root + `readOnlyRootFilesystem` + emptyDir `/tmp`។
4. `kubectl apply -f k8s/app/` → `get pods -w` → `describe pod` → `logs` → `port-forward svc 8080` → `curl /actuator/health`។
5. ចង្អុលឱ្យខូចដោយចេតនា ១ (tag ខុស → ImagePullBackOff, password ខុស → CrashLoopBackOff/0/1 Ready, limit 64Mi → OOMKilled) → កត់ក្នុង runbook `k8s-pod-not-ready.md`។

**ត្រូវមើល:** Events ក្នុង `describe pod` (អានពីក្រោមឡើង), `logs --previous` ពេល CrashLoop, probe fail message។

---

## Task 4 — Service + Ingress ⬜

**នឹងធ្វើ:** `k8s/app/service.yaml` (ClusterIP 80 → `http`), `k8s/app/ingress.yaml` (`ingressClassName: nginx`, host `minishop.local`), hosts file Windows `C:\Windows\System32\drivers\etc\hosts` (`127.0.0.1 minishop.local`, ត្រូវ admin), `curl http://minishop.local/api/products`, load-balancing តាម `logs -l app=minishop --prefix`។

## Task 5 — Rolling update / self-heal / rollback ⬜ (ADR-007)

**នឹងធ្វើ:** loop `curl` 5/s → `rollout.log`; push commit ថ្មី → CI image sha ថ្មី → `kubectl set image` → `rollout status` → `grep -v 200 rollout.log` ត្រូវទទេ; `rollout undo`; `delete pod -l app=minishop` → វាស់វិនាទីដាច់; deploy ខូច (readiness 503) → pod ចាស់នៅ។ បើមាន 5xx → `preStop` + `terminationGracePeriodSeconds` + Spring `server.shutdown: graceful`។

## Task 6 — Kustomize ⬜

**នឹងធ្វើ:** `k8s/base` + `k8s/overlays/local` (images newTag, replicas, ingress patch, ingress-nginx nodeSelector patch, configMapGenerator); `kubectl diff -k` មុន `apply -k`។

## Task 7 — HPA + k6 ⬜

**នឹងធ្វើ:** metrics-server v0.9.0 + `--kubelet-insecure-tls`; `k8s/app/hpa.yaml` (cpu 60%, 2–5); `scripts/load-test.js` k6 (pin version) → `get hpa -w` → max replica ដល់?

## Task 8 — Docs 🟡

**នឹងធ្វើ:** ADR-006 → Accepted (អ្នក), ADR-007, `docs/runbooks/k8s-pod-not-ready.md` + `k8s-rollback.md`, README "Run on Kubernetes (kind)", learning-log + rollout.log figures, tracker។
