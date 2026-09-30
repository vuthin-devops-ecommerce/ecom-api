# Learning Log — Phase A3 (Kubernetes)

ការឆ្លុះបញ្ចាំងរបស់អ្នករៀន (ហេតុអ្វី)។ Claude review និងសួរបន្ត តែ**មិនឆ្លើយជំនួស**។
អ្វីដែលធ្វើពិត (command, លទ្ធផល) នៅ [worklog.md](worklog.md); symptom + ដំណោះស្រាយ នៅ [runbooks/](../../runbooks/)។

## Phase A3 / Task 0–1 — tool + cluster (2026-09-29)

Tool: kind v0.33.0 (`~/tools/kind`), kubectl v1.36.1 (Docker Desktop), `scripts/check-env.sh` (ថ្មី)។ File: `k8s/kind-config.yaml` (node image pin digest, port 80/443 mapping, label ingress-ready)។
Version drift ក្នុង `docs/journey/phase-a3/plan.md` កែរួច: `postgres:16-alpine` → `postgres:17`, ingress-nginx `main` → `controller-v1.15.1`, metrics-server `latest` → `v0.9.0`, kind `latest` → `v0.33.0`, `<username>` → `vuthin-devops-ecommerce`។

### សំណួរឆ្លុះបញ្ចាំង (ពី `docs/journey/phase-a3/plan.md` Task 1)

1. `coredns`, `kube-proxy`, `etcd`, `kube-apiserver` (និង `kube-scheduler`, `kube-controller-manager`, `kindnet`) — មួយៗធ្វើអ្វី? (១ បន្ទាត់រាល់មួយ, មើល `kubectl get pods -n kube-system -o wide`)

   _(សរសេរនៅទីនេះ)_

2. kind run node ជា Docker container → Pod = "container ក្នុង container"? production ពិតខុសអ្វី?

   _(សរសេរនៅទីនេះ)_

3. (ពី kind-config) ហេតុអ្វី 2 worker មិនមែន 1? ហេតុអ្វី ingress controller ត្រូវនៅ control-plane ក្នុង kind?

   _(សរសេរនៅទីនេះ)_

### លំហាត់ស្វែងយល់ cluster (½ ថ្ងៃ — កត់អ្វីដែលឃើញ)

```bash
kubectl get nodes -o wide                  # 3 node Ready? INTERNAL-IP? CONTAINER-RUNTIME?
docker ps                                  # node = container (image kindest/node)
kubectl get pods -A -o wide                # pod អ្វីខ្លះ K8s run ខ្លួនឯង? នៅ node ណា?
kubectl describe node minishop-worker      # Capacity/Allocatable cpu+memory? Conditions?
kubectl api-resources | head -40
kubectl -n ingress-nginx get pods -o wide  # controller នៅ control-plane?
```

_(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 0–1

- **`kind create cluster`** ជោគជ័យ (~1 នាទី, pull `kindest/node:v1.37.0` តាម Docker Desktop) — 3 node Ready, `docker ps` បង្ហាញ container ៣ ឈ្មោះ `minishop-control-plane|worker|worker2`, control-plane ប៉ុណ្ណោះមាន port `0.0.0.0:80->80, 443->443`។
- **ingress-nginx pod `ErrImagePull`** — `describe pod` → Events: `tls: failed to verify certificate: x509: certificate signed by unknown authority` ពេល pull `registry.k8s.io/ingress-nginx/...`។ មូលហេតុដដែលនឹង A2 Task 4: kind node = Linux container → containerd មិនទុកចិត្ត Somansa CA (network ការិយាល័យ)។ Docker Desktop pull node image បាន (Windows trust) តែ **pull ក្នុង node** ខុសផ្លូវ។ ដំណោះស្រាយ: export CA ពី Windows cert store (PowerShell) → `scripts/kind-trust-ca.sh <pem> minishop` (docker cp → `update-ca-certificates` → restart containerd លើ node ទាំង ៣) → pod retry ខ្លួនឯង → Running។ **ត្រូវ run ម្តងទៀតរាល់ `kind create`**។ CA មិន commit; script commit (generic)។
- **Controller schedule លើ `minishop-worker2` មិនមែន control-plane** — manifest `controller-v1.15.1` provider/kind មាន `nodeSelector: kubernetes.io/os: linux` ប៉ុណ្ណោះ (version ចាស់មាន `ingress-ready: "true"`) → hostPort 80 បើកលើ worker2 ដែលគ្មាន `extraPortMappings` → laptop ចូលមិនដល់។ ដោះស្រាយ: `kubectl -n ingress-nginx patch deployment ingress-nginx-controller --type=merge -p '{"spec":{"template":{"spec":{"nodeSelector":{"kubernetes.io/os":"linux","ingress-ready":"true"}}}}}'` (toleration control-plane មានស្រាប់) → pod ថ្មីលើ control-plane → `curl localhost` = 404 ពី nginx (ត្រឹមត្រូវ — មិនទាន់មាន Ingress rule)។ Task 6 (Kustomize) គួរដាក់ patch នេះជា file ជំនួស command ដោយដៃ។
  - សំណួរ: ហេតុអ្វី label `ingress-ready` ក្នុង kind-config នៅតែសំខាន់ទោះ manifest មិនប្រើ? ជម្រើសផ្សេង: `extraPortMappings` លើ worker ទាំង ២? បញ្ហាអ្វី?

## Phase A3 / Task 2 — Namespace + Postgres StatefulSet (2026-09-29)

File: `k8s/namespace.yaml`, `k8s/postgres/{configmap,secret.example,statefulset,service}.yaml` (comment ពន្យល់ក្នុង file), ADR: `docs/decisions/006-postgres-statefulset-vs-managed.md` (Proposed)។
Apply: `kubectl apply -f k8s/namespace.yaml && kubectl apply -f k8s/postgres/` → `postgres-0` Running 1/1 លើ `minishop-worker2`, PVC `data-postgres-0` Bound 1Gi (StorageClass `standard` = local-path ក្នុង node), `psql \l` ឃើញ DB `minishop`។

### លំហាត់ (អ្នកធ្វើ — កត់លទ្ធផលពិត)

1. `kubectl -n minishop delete pod postgres-0` → `kubectl -n minishop get pods -w` → pod ថ្មីឈ្មោះអ្វី? ចំណាយប៉ុន្មានវិនាទី? ទិន្នន័យនៅឬបាត់? (បង្កើត table មុន: `kubectl -n minishop exec postgres-0 -- psql -U minishop -d minishop -c 'create table t(x int); insert into t values (1);'` រួច delete pod រួច `select * from t`)

   _(សរសេរនៅទីនេះ)_

2. `kubectl -n minishop delete statefulset postgres` → `kubectl -n minishop get pvc` → PVC នៅឬបាត់? ហេតុអ្វី K8s **មិន**លុប? រួច `kubectl apply -f k8s/postgres/` → data ត្រឡប់?

   _(សរសេរនៅទីនេះ)_

### សំណួរឆ្លុះបញ្ចាំង (ADR-006 — ឆ្លើយរួចប្តូរ status)

1. ហេតុអ្វី DB ជា StatefulSet មិនមែន Deployment? (identity, DNS, PVC per pod)
2. Production ពិត: Postgres ក្នុង K8s (StatefulSet/operator) ឬ managed DB (RDS/Cloud SQL)? trade-off (backup, failover, upgrade, cost, "អ្នកណាភ្ញាក់ពេល ២ យប់")។
3. (ពី service.yaml) headless Service vs ClusterIP ពេល replicas: 1 — ខុសគ្នាអ្វី? ពេលណាសំខាន់?
4. (ពី configmap.yaml) ConfigMap vs Secret — base64 មិនមែន encryption; អ្វី**ពិត**ដែលធ្វើឱ្យ Secret "សុវត្ថិភាពជាង"?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_

### អ្វីដែលជួបពិតពេលធ្វើ Task 2

- `kubectl apply -f k8s/postgres/` apply **ទាំង** `secret.example.yaml` និង `secret.yaml` (ឈ្មោះ object ដដែល `postgres-secret`) → log "created" រួច "configured" — file ក្រោយឈ្នះ (លំដាប់ alphabet: secret.example < secret.yaml → password ពិតឈ្នះ, តែដោយសំណាង)។ Task 6 Kustomize រាយ resource ជាក់លាក់ → បញ្ហានេះបាត់។
- PVC Bound ភ្លាមទោះ StorageClass local-path: PV បង្កើតពេល pod schedule (WaitForFirstConsumer) → PV ជាប់នឹង node `worker2` → pod postgres-0 នឹង**តែងតែ**ទៅ worker2 (RWO + local disk)។ សេណារីយ៉ូ: worker2 ងាប់ → pod Pending រហូត — នេះជាហេតុផលមួយក្នុង ADR-006។
- **លំហាត់ ១–២ (ភស្តុតាងពី cluster, 2026-09-29):** ក្រោយ delete pod + delete/apply StatefulSet — pod ឈ្មោះ `postgres-0` ដដែល (age ថ្មី), PVC `data-postgres-0` **UID ដដែល** `pvc-3ec56de9…` (មិនបានបង្កើតថ្មី), `select count(*) from t` = 1 → data រស់ទាំង ២ ករណី។ ចម្លើយ "ហេតុអ្វី" នៅជារបស់អ្នក (ខាងលើ)។

## Phase A3 / Task 3 — App Deployment + ConfigMap + probes (2026-09-29)

File: `k8s/app/{configmap,deployment,service}.yaml` (comment ក្នុង file: probe ៣ ប្រភេទ, securityContext, resources)។

### អ្វីដែលជួបពិតពេលធ្វើ Task 3

- **error ដំបូង (តាមតារាង "ចំណុចដែលអ្នកនឹងខូច"): `ImagePullBackOff`** — `kubectl -n minishop get events` → `Failed to pull image "ghcr.io/vuthin-devops-ecommerce/mini-shop:e8f7f68": … failed to fetch anonymous token: … 401 Unauthorized`។ អាន: មិនមែន x509 (CA បានដោះស្រាយ Task 1), មិនមែន tag ខុស — **401 = registry ត្រូវការ login**: package `mini-shop` លើ ghcr នៅ **private** (repo public តែ package visibility ដាច់ដោយឡែក)។ ជម្រើស: (ក) GitHub → Packages → mini-shop → Package settings → Change visibility → Public (ងាយសម្រាប់រៀន, image គ្មាន secret); (ខ) `kubectl -n minishop create secret docker-registry ghcr-creds --docker-server=ghcr.io --docker-username=<user> --docker-password=<PAT read:packages>` + `imagePullSecrets` ក្នុង deployment (production pattern)។
  - ការសម្រេចរបស់អ្នក + ហេតុផល: _(សរសេរនៅទីនេះ)_
- gotcha ដែលចៀសមុន: `runAsNonRoot: true` + image `USER spring` (ឈ្មោះ) → kubelet "cannot verify user is non-root" → បន្ថែម `runAsUser: 100` (uid ពី `adduser -S` alpine, ពិនិត្យដោយ `docker run … id spring`)។
- `readOnlyRootFilesystem: true` → emptyDir mount `/tmp` (Tomcat work dir) — សាកលុប emptyDir មើល error ពិត?

### សំណួរឆ្លុះបញ្ចាំង (ពី `docs/journey/phase-a3/plan.md` Task 3)

1. Liveness fail → K8s ធ្វើអ្វី? Readiness fail → ធ្វើអ្វី? DB ដាច់ ១ នាទី — ចង់ restart app ឬគ្រាន់តែឈប់ផ្ញើ traffic? ហេតុអ្វីច្រឡំ ២ នេះគ្រោះថ្នាក់?
2. `requests` vs `limits` — ហេតុអ្វីមិនកំណត់ CPU limit? (ទស្សនៈ ២ ខាង)
3. Pin `<sha>` ក្នុង manifest → រាល់ release កែ manifest ដោយដៃ — Phase E (GitOps) ដោះស្រាយយ៉ាងណា?
4. (ពី configmap) `JAVA_TOOL_OPTIONS` ទីនេះ + `-XX:MaxRAMPercentage` ក្នុង Dockerfile — អ្នកណាកែបានដោយមិន rebuild?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_
- **ក្រោយ secret `ghcr-creds` (PAT read:packages) → image pull ✅** (`Pulled … already present`) — pattern production: credential ក្នុង Secret type `kubernetes.io/dockerconfigjson`, ជាប់ namespace, មិន commit។ ផ្ទៀងផ្ទាត់ credential ដោយមិនបង្ហាញ: `curl -H "Authorization: Basic <auth ពី secret>" https://ghcr.io/token?scope=repository:…:pull` → 200។
- **symptom ទី ២: `CrashLoopBackOff`** — `logs --previous` → `FlywayException: Found non-empty schema(s) "public" but no schema history table. Use baseline() or set baselineOnMigrate to true`។ មូលហេតុ: table `t` ពីលំហាត់ Task 2 នៅក្នុង schema → Flyway បដិសេធ migrate schema ដែលមាន object ស្រាប់ដោយគ្មាន history (ការពារ DB ដែលមានទិន្នន័យ)។ ដោះស្រាយ: `drop table t` (dev data) — **មិន** `baselineOnMigrate: true` (វានឹងលាក់បញ្ហានេះនៅ production)។ ក្រោយនោះ Flyway apply V1 products, V2 orders, R seed ✅។
- **symptom ទី ៣: `CrashLoopBackOff` ម្តងទៀត — `UnknownHostException: postgres`** ភ្លាមក្រោយ Docker restart: app start មុន coredns/postgres ready → K8s គ្មាន `depends_on` → app crash → kubelet restart (backoff 10s, 20s, 40s…) រហូតដល់ DNS មក → ធម្មតា; នេះជាហេតុផលដែល app ត្រូវ "crash fast + restart" មិនមែន "wait forever"។ ជម្រើសផ្សេង: initContainer រង់ចាំ DB (plan Task 3 ផែនទី compose → K8s)។
- **symptom ទី ៤ (ធំ): Docker Desktop VM ងាប់ ២ ដង** — `kubectl`: `TLS handshake timeout`, `docker ps`: 500/hang, `wsl -l -v`: docker-desktop Stopped។ ស៊ើបអង្កេត: Docker Desktop ប្រើ **Hyper-V backend** (`WslEngineEnabled: false` → `.wslconfig` 10GB មិនមានឥទ្ធិពល) ជាមួយ default **1.9 GB RAM** (`docker info` MemTotal), 20 CPU។ kind ៣ node ទទេ = ~1.1 GB (control-plane 850 MiB) + app JVM ×2 (~400 MiB) + ingress → លើស → VM OOM → cluster ដាច់ទាំងអស់។ ដោះស្រាយបណ្តោះអាសន្ន: `scale deployment/minishop-app --replicas=0`។ ដោះស្រាយពិត (អ្នក): Docker Desktop → Settings → Resources → Memory ≥ **8 GB** (host មាន 31 GB), CPUs 4–6 → Apply & restart។ មេរៀន: `requests` ក្នុង manifest (384Mi ×2 + 128Mi) មានន័យតែពេល node មាន memory ពិត — `kubectl describe node` Allocatable ត្រូវមើលមុន deploy។
- **លទ្ធផល Task 3 (2026-09-30, ក្រោយ Docker VM → 8 GB):** `kubectl get nodes -o jsonpath=…allocatable.memory` = 8123180Ki (មុន ~1.9 GB) → `scale --replicas=2` → pod ×2 Ready លើ node ខុសគ្នា, startup 4.7s, health UP, 8 products។ សំណួរ: ហេតុអ្វី scheduler ដាក់ pod ២ លើ node ខុសគ្នាដោយគ្មាន affinity rule? តើវាធានាឬអត់? (hint: Task 5 self-heal, podAntiAffinity/topologySpreadConstraints)

## Phase A3 / Task 4 — Service + Ingress (2026-09-30)

File: `k8s/app/ingress.yaml` (service.yaml ពី Task 3)។ Test ដោយ Claude មុន hosts file: `curl --resolve minishop.local:80:127.0.0.1 …` → readiness UP, products 8, POST 201; Host ខុស (`localhost`) → 404 ពី nginx។ Load balancing 20 request → 11 / 9 ចែកលើ pod ២ (មើលក្នុង `kubectl -n ingress-nginx logs deploy/ingress-nginx-controller` — upstream IP:8080)។

### លំហាត់ (អ្នក)

1. hosts file (Notepad ជា Administrator) → `C:\Windows\System32\drivers\etc\hosts` បន្ថែម `127.0.0.1 minishop.local` → `curl http://minishop.local/api/products` និង browser `http://minishop.local/swagger-ui.html`
2. Load balancing ដោយភ្នែក: terminal ១ `kubectl -n minishop logs -f -l app=minishop --prefix` · terminal ២ `for i in $(seq 1 20); do curl -s http://minishop.local/api/products > /dev/null; done` — ចែកស្មើឬអត់?

   _(សរសេរនៅទីនេះ)_

### សំណួរឆ្លុះបញ្ចាំង

1. ClusterIP vs NodePort vs LoadBalancer vs Ingress — ពេលណាប្រើមួយណា? (hint: L4 vs L7, ប៉ុន្មាន IP ខាងក្រៅ, host/path routing)
2. ហេតុអ្វី kind ត្រូវការ `extraPortMappings`? (hint: node = container — port 80 របស់វាមិនមែន port 80 របស់ laptop)
3. `curl http://localhost/` → 404 ប៉ុន្តែ `curl --resolve minishop.local…` → 200 — ភាពខុសគ្នាគឺអ្វីក្នុង HTTP request? (hint: Host header)
4. ហេតុអ្វីខ្ញុំដក `rewrite-target: /` ចេញពី plan? វានឹងធ្វើអ្វីបើ path ជា `/api`?

**ចម្លើយ:**

_(សរសេរនៅទីនេះ)_
