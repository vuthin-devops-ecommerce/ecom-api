# Phase A3 — Worklog (អ្វីដែលធ្វើពិត / នឹងធ្វើ តាម Task)

> ខុសពី `docs/journey/phase-a3/plan.md` (ផែនការ/មេរៀន) និង `docs/journey/phase-a3/learning-log.md` (ការឆ្លុះបញ្ចាំងរបស់អ្នករៀន)។
> Task នីមួយៗ — គោលដៅ · អ្វីដែលធ្វើ/នឹងធ្វើ (ជំហាន, command) · file · លទ្ធផល/ភស្តុតាង · អ្វីដែលត្រូវមើល។
> ចាប់ផ្តើម 2026-09-29 · cluster `kind-minishop` លើ laptop · image ពី A2: `ghcr.io/vuthin-devops-ecommerce/mini-shop:e8f7f68`

## សង្ខេប

| Task | គោលដៅ | ស្ថានភាព | ភស្តុតាង |
|---|---|---|---|
| 0 | kubectl + kind | ✅ 2026-09-29 | `scripts/check-env.sh` 10 ok |
| 1 | cluster 3 node + ingress-nginx | ✅ 2026-09-29 | `kubectl get nodes` Ready ×3, `curl localhost` 404 nginx |
| 2 | namespace + Postgres StatefulSet | ✅ 2026-09-29 | `postgres-0` Running, PVC រស់ក្រោយ delete |
| 3 | app Deployment + ConfigMap + probes | ✅ 2026-09-30 | 2/2 Ready លើ worker ខុសគ្នា, health UP, 8 products; symptom ៤ ដោះស្រាយ |
| 4 | Service + Ingress `minishop.local` | 🟡 ingress ដើរ (curl --resolve) | 200, POST 201, LB 11/9; hosts file រង់ចាំអ្នក |
| 5 | rolling update 0 downtime, self-heal, rollback | ✅ 2026-09-30 | 0/69 ក្រោយ preStop; self-heal ~7s |
| 6 | Kustomize base + overlays | ⬜ | |
| 7 | metrics-server + HPA + k6 | ⬜ | |
| 8 | docs: ADR-006/007, runbooks, README | 🟡 ADR-006 Proposed | |

---

## Task 0 — Tool ✅

**គោលដៅ:** kubectl, kind លើ Windows, version pin ក្នុង `docs/reference/tech-stack.md`។

**ធ្វើ:** kubectl v1.36.1 មានស្រាប់ (Docker Desktop) → រក្សា (skew ±1 ពី server 1.37)។ kind **v0.33.0** download → `C:\Users\user\tools\kind\kind.exe` → user PATH (+ `tools\trivy`)។ `scripts/check-env.sh` ថ្មី (java/mvn/docker/git/gh/trivy/kubectl/kind/cluster)។ `docs/reference/tech-stack.md` §៨: kind v0.33.0, kindest/node v1.37.0, ingress-nginx controller-v1.15.1, metrics-server v0.9.0។

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

## Task 3 — App Deployment + ConfigMap + probes 🟡

**ធ្វើរួច (2026-09-29):** `k8s/app/configmap.yaml` (URL → Service `postgres`, profile `dev` សម្រាប់ seed, `JAVA_TOOL_OPTIONS`), `deployment.yaml` (2 replica, RollingUpdate 1/0, image `:e8f7f68`, secretKeyRef, startup/liveness/readiness, requests 250m/384Mi limit 512Mi, runAsUser 100 + readOnlyRootFilesystem + emptyDir /tmp), `service.yaml` (ClusterIP 80 → http)។ `kubectl apply -f k8s/app/` → pod ×2 **ImagePullBackOff**: events `401 Unauthorized … anonymous token` → អ្នកជ្រើស pattern production: PAT `read:packages` → `kubectl create secret docker-registry ghcr-creds` + `imagePullSecrets` → pull ✅។ បន្ទាប់: CrashLoop `Found non-empty schema` (table `t` ពីលំហាត់ Task 2 → `drop table t`) → Flyway V1/V2/seed ✅ → CrashLoop `UnknownHostException: postgres` (transient ក្រោយ Docker restart) → **Docker Desktop VM (Hyper-V) 1.9 GB ងាប់ ២ ដង** → `scale --replicas=0`; រង់ចាំអ្នកបង្កើន memory ≥ 8 GB (Settings → Resources) រួច `scale --replicas=2` → port-forward → curl។

**លទ្ធផលចុងក្រោយ (2026-09-30, Docker VM 8 GB, node allocatable 7.7 GiB):** `scale --replicas=2` → rollout ជោគជ័យ; pod ×2 `1/1 Running` លើ `worker` និង `worker2`; EndpointSlice ready=true ×2; `Started MiniShopApplication in 4.7s`; port-forward → `/actuator/health/readiness` UP, `/liveness` UP, `/api/products` = 8 (seed dev)។ `kubectl top` → "Metrics API not available" (Task 7 metrics-server)។

**ផែនការដើម:**

**គោលដៅ:** `minishop-app` ×2 replica run ពី image ghcr.io, config តាម ConfigMap, password តាម Secret ដដែល, probe ៣ ប្រភេទ។

**នឹងធ្វើ:**
1. ពិនិត្យ package `mini-shop` លើ ghcr public ឬ private → private: `kubectl create secret docker-registry ghcr-creds` + `imagePullSecrets`; public: មិនត្រូវការ។
2. `k8s/app/configmap.yaml`: `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/minishop`, USERNAME, `SPRING_PROFILES_ACTIVE`, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0`។
3. `k8s/app/deployment.yaml`: replicas 2, `RollingUpdate maxSurge 1 / maxUnavailable 0`, image `…/mini-shop:e8f7f68` (pin sha), `env SPRING_DATASOURCE_PASSWORD ← secretKeyRef postgres-secret`, startupProbe (`/actuator/health/liveness`, failureThreshold 30 × 2s), livenessProbe, readinessProbe (`/readiness`), resources (250m/384Mi, limit 512Mi), securityContext non-root + `readOnlyRootFilesystem` + emptyDir `/tmp`។
4. `kubectl apply -f k8s/app/` → `get pods -w` → `describe pod` → `logs` → `port-forward svc 8080` → `curl /actuator/health`។
5. ចង្អុលឱ្យខូចដោយចេតនា ១ (tag ខុស → ImagePullBackOff, password ខុស → CrashLoopBackOff/0/1 Ready, limit 64Mi → OOMKilled) → កត់ក្នុង runbook `k8s-pod-not-ready.md`។

**ត្រូវមើល:** Events ក្នុង `describe pod` (អានពីក្រោមឡើង), `logs --previous` ពេល CrashLoop, probe fail message។

---

## Task 4 — Service + Ingress 🟡

**ធ្វើរួច (2026-09-30):** `k8s/app/ingress.yaml` (`ingressClassName: nginx`, host `minishop.local`, path `/` Prefix → Service `minishop-app` port `http`; ដក annotation `rewrite-target` ចេញពី plan ដោយចេតនា) → `kubectl apply -f k8s/app/ingress.yaml`។ Test ដោយ `curl --resolve minishop.local:80:127.0.0.1` (មិនត្រូវ hosts file): `/actuator/health/readiness` UP, `/api/products` 8, `POST /api/products` 201, `curl http://localhost/` (Host ខុស) 404។ Load balancing (20 request, ingress-nginx access log): pod 10.244.1.3 = 11, pod 10.244.2.2 = 9។ នៅសល់: អ្នកបន្ថែម hosts file → `curl http://minishop.local/…` ផ្ទាល់ + browser Swagger។

**ផែនការដើម:** `k8s/app/service.yaml` (ClusterIP 80 → `http`), `k8s/app/ingress.yaml` (`ingressClassName: nginx`, host `minishop.local`), hosts file Windows `C:\Windows\System32\drivers\etc\hosts` (`127.0.0.1 minishop.local`, ត្រូវ admin), `curl http://minishop.local/api/products`, load-balancing តាម `logs -l app=minishop --prefix`។

## Task 5 — Rolling update / self-heal / rollback ✅ (ADR-007 Proposed)

**Setup:** load loop background `curl -s -m 2 --resolve minishop.local:80:127.0.0.1 http://minishop.local/api/products` រាល់ 0.2s (ពិត ~2–3 req/s លើ Windows) → `rollout.log` (time + code)។ Image ថ្មី: `afa3e91` (CI push ពី merge PR #9 — រាល់ merge ទៅ main push image ដូច្នេះមិនចាំបាច់ commit code)។ Baseline 45 req, 0 fail។

| Run | ធ្វើ | req | non-200 | ភស្តុតាង |
|---|---|---|---|---|
| 5.1a | `set image afa3e91` (គ្មាន preStop) | 51 | 2 (`000`) | nginx: `connect() failed (111: Connection refused)` → IP pod ចាស់ |
| 5.1b | apply `preStop sleep 10` + `SERVER_SHUTDOWN=graceful` (rollout ទៅ e8f7f68) | 59 | 2 | nginx 499 ក្រោយ 2.0s ទៅ pod ចាស់ — pod ទាំងនោះកើតពី template **គ្មាន** preStop |
| 5.1c | `set image afa3e91` — pod ចាស់មាន preStop | 69 | **0** | nginx គ្មាន error |
| 5.2 | `rollout undo` | 66 | 0 | image ត្រឡប់ e8f7f68 |
| 5.3 | `delete pod -l app=minishop` (ទាំង ២) | 36 | 16 (503) | 14:09:36→:43 ≈ 7s គ្មាន endpoint Ready; pod ថ្មី 1/1 ក្នុង 17s |
| 5.4 | `set env SPRING_DATASOURCE_URL=…no-such-db…` | 165 | 0 | pod ថ្មី CrashLoopBackOff `UnknownHostException: no-such-db`; pod ចាស់ 2/2 នៅបម្រើ → `rollout undo` |

**File:** `k8s/app/deployment.yaml` (`lifecycle.preStop.sleep.seconds: 10`), `k8s/app/configmap.yaml` (`SERVER_SHUTDOWN`, `SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE: 20s`), `docs/decisions/007-rolling-update-strategy.md`។ Cluster ចុងក្រោយ: image `e8f7f68` = manifest ក្នុង git។

**ជួបពិត:** `rollout undo` warning `last-applied-configuration` មិន update → git manifest ជា source (Phase E GitOps)។ `rollout status` ត្រឡប់ **មុន** pod ចាស់បញ្ចប់ termination (preStop) — វាស់ត្រូវរង់ចាំ `Terminating` បាត់។

**ផែនការដើម:**

## Task 6 — Kustomize ⬜

**នឹងធ្វើ:** `k8s/base` + `k8s/overlays/local` (images newTag, replicas, ingress patch, ingress-nginx nodeSelector patch, configMapGenerator); `kubectl diff -k` មុន `apply -k`។

## Task 7 — HPA + k6 ⬜

**នឹងធ្វើ:** metrics-server v0.9.0 + `--kubelet-insecure-tls`; `k8s/app/hpa.yaml` (cpu 60%, 2–5); `scripts/load-test.js` k6 (pin version) → `get hpa -w` → max replica ដល់?

## Task 8 — Docs 🟡

**នឹងធ្វើ:** ADR-006 → Accepted (អ្នក), ADR-007, `docs/runbooks/k8s-pod-not-ready.md` + `k8s-rollback.md`, README "Run on Kubernetes (kind)", learning-log + rollout.log figures, tracker។
