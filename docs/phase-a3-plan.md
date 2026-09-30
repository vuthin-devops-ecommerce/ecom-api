# Phase A3 — Kubernetes លើ kind

> **សម្រាប់ Claude Code:** សូមអាន `CLAUDE.md`, `docs/phase-a-plan.md`, `docs/phase-a2-plan.md` ជាមុន។
> តម្រូវការជាមុន: Phase A2 ចប់ — image `ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>` មាននៅ registry។
> អ្នកប្រើកំពុងរៈន — ពន្យល់ជាភាសាខ្មែរ ណែនាំជាជំហាន **កុំ generate manifest ទាំងអស់ជំនួស**។
> ពេលអ្នកប្រើ paste error ពី `kubectl` ជួយអានវាតាមលំដាប់: `describe` → `logs` → `events`។

**រយៈពេលប៉ាន់ស្មាន:** សប្តាហ៏ ៥–៦
**ឧបករណ៍:** kind · kubectl · k9s (optional) · ingress-nginx · metrics-server

---

## គោលដៈដំណាក់កាល A3

ដក `compose.yaml` ចេញ → app + Postgres run លើ Kubernetes cluster ក្នុងម៉ាស៊ីនអ្នក
ជាមួយ health probe, config/secret ដាច់ពី image, rolling update ដោយ downtime = 0។

**Definition of Done:**
- [ ] `kind create cluster` → cluster ១ control-plane + ២ worker
- [ ] `kubectl apply -k k8s/` → Postgres (StatefulSet) + app (Deployment ×2 replica) run
- [ ] `curl http://minishop.local/api/products` ឆ្លងកាត់ Ingress
- [ ] Liveness/Readiness probe ប្រើ `/actuator/health/liveness` និង `/readiness`
- [ ] ប្តូរ image tag → rolling update → `curl` loop មិនមាន error មួយ
- [ ] `kubectl delete pod <app-pod>` → pod ថ្មីកើតឡើងវិញ, traffic មិនដាច់
- [ ] HPA scale app ពី 2 → 4 replica ក្រោម load
- [ ] `docs/runbooks/k8s-pod-not-ready.md` សរសេររួច
- [ ] ADR 006, 007 សរសេររួច

---

## ទស្សនៈសំខាន់មុនចាប់ផ្តើម

Docker Compose និយាយថា: "**run** container នេះ"។
Kubernetes និយាយថា: "**ធានាថា** container នេះ**កំពុង run** ២ copy ជានិច្ច — បើងាប់ ធ្វើថ្មី។"

ភាពខុសគ្នា = **declarative + reconciliation loop**។ អ្នកប្រកាស "desired state", Kubernetes ខិតខំឱ្យ "actual state" ស្មៈវា រហូត។ រាល់ concept ក្នុង Phase នេះ គ្រាន់តែជាការពន្យល់ថា loop នេះធ្វើការយ៉ាងណា។

**ផែនទី Compose → Kubernetes:**
| Compose | Kubernetes | ហេតុអ្វីស្មុគស្មាញជាង |
|---|---|---|
| `services.app` | Deployment → ReplicaSet → Pod | ត្រូវ run ច្រើន copy លើម៉ាសៈនច្រើន |
| `services.db` | StatefulSet + PVC | DB ត្រូវការ identity ស្ថៈរ + disk ជាប់ |
| `ports: 8080:8080` | Service + Ingress | Pod IP ប្រែជានិច្ច ត្រូវការ IP ស្ថៈរ |
| `environment:` | ConfigMap + Secret | config ដាច់ពី deployment, secret ដាច់ពី config |
| `depends_on` | readiness probe + init container | គ្មាន "wait for" — គ្មាន pod "ready" = គ្មាន traffic |
| `volumes: pgdata` | PersistentVolumeClaim | disk ត្រូវរស់រានទោះ pod ងាប់, ទោះ node ងាប់ |
| `healthcheck` | liveness + readiness + startup probe | ៣ ប្រភេទ ព្រៈកឋា "ខូច" មានប្រភេទផ្សេងៈគ្នា |

---

## Task 0 — តម្លើងឧបករណ៍ (តម្លើងឥឡូវ មិនមែន Phase 00)

```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
kubectl version --client

# kind
[ $(uname -m) = x86_64 ] && curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.33.0/kind-linux-amd64   # ← pin (ធ្លាប់ latest)
chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind
kind version

# k9s (optional តែណែនាំខ្លាំង — TUI សម្រាប់មើល cluster)
# https://k9scli.io — ឬ: sdk/brew/apt តាម OS

# kubectl autocomplete + alias (សន្សៈពេលច្រើនណាស់)
echo 'source <(kubectl completion bash)' >> ~/.bashrc
echo 'alias k=kubectl' >> ~/.bashrc
echo 'complete -o default -F __start_kubectl k' >> ~/.bashrc
```

**macOS:** `brew install kubectl kind k9s`
**ធ្វើបច្ចុប្បន្នភាព** `scripts/check-env.sh` បន្ថែម `kubectl`, `kind`។

---

## Task 1 — បង្កើត Cluster

**File:** `k8s/kind-config.yaml`
```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:          # ← ឱ្យ Ingress ចេញមក localhost:80/443
      - containerPort: 80
        hostPort: 80
      - containerPort: 443
        hostPort: 443
  - role: worker
  - role: worker
```

```bash
kind create cluster --name minishop --config k8s/kind-config.yaml
kubectl cluster-info --context kind-minishop
kubectl get nodes                          # 3 node, STATUS Ready
docker ps                                  # ← សម្គាល់: node គឺជា container!
```

**Ingress controller:**
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/kind/deploy.yaml   # ← pin tag (ធ្លាប់ main)
kubectl wait --namespace ingress-nginx --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=90s
```

**ស្វែងយល់ cluster (½ ថ្ងៈ):**
```bash
kubectl get namespaces
kubectl get pods -A                        # pod អ្វីៈដែល K8s ខ្លួនឯង run?
kubectl get pods -n kube-system -o wide    # មួយៈនៅ node ណា?
kubectl describe node minishop-worker
kubectl api-resources | head -40           # ប្រភេទ object ទាំងអស់
```

**សំណួរឆ្លុះបញ្ចាំង:**
- `coredns`, `kube-proxy`, `etcd`, `kube-apiserver` — មួយៈធ្វើអ្វី? (សរសេរ ១ បន្ទាត់រាល់មួយ)
- kind run node ជា Docker container — ដូច្នេះ Pod ខាងក្នុងគឺ "container ក្នុង container"? តើនោះជា production ពិតឬអត់?

---

## Task 2 — Namespace + Postgres (StatefulSet)

**រចនាសម្ព័ន្ធ folder:**
```
k8s/
├── kind-config.yaml
├── kustomization.yaml          ← Task 6
├── namespace.yaml
├── postgres/
│   ├── secret.yaml             ← កុំ commit តម្លៈពិត! (មើលខាងក្រោម)
│   ├── configmap.yaml
│   ├── statefulset.yaml
│   └── service.yaml
└── app/
    ├── configmap.yaml
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    └── hpa.yaml                ← Task 7
```

**`namespace.yaml`:** `minishop`

**`postgres/secret.yaml` (អ្នកប្រើសរសេរ):**
- kind `Secret`, type `Opaque`, `stringData:` (មិន base64 ដោយដៈ — K8s encode ឱ្យ)
- keys: `POSTGRES_PASSWORD`
- ⚠️ **កុំ commit file នេះ** → បន្ថែម `k8s/**/secret.yaml` ទៅ `.gitignore` → commit `secret.example.yaml` ជំនួស
- ធ្វើបច្ចុប្បន្នភាព README: "copy secret.example.yaml → secret.yaml, កែ password"

**`postgres/statefulset.yaml` (គ្រោង — អ្នកប្រើបំពេញ):**
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: minishop
spec:
  serviceName: postgres          # ← ត្រូវស្មៈ headless Service name
  replicas: 1
  selector:
    matchLabels: { app: postgres }
  template:
    metadata:
      labels: { app: postgres }
    spec:
      containers:
        - name: postgres
          image: postgres:17            # ← កែ 2026-09-29: 00-tech-stack ឈ្នះ (ធ្លាប់ 16-alpine)
          ports: [{ containerPort: 5432 }]
          envFrom:
            - configMapRef: { name: postgres-config }
            - secretRef: { name: postgres-secret }
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "minishop"]
            periodSeconds: 5
          resources:
            requests: { cpu: 100m, memory: 128Mi }
            limits: { memory: 256Mi }
  volumeClaimTemplates:          # ← StatefulSet-only: PVC មួយ per pod, រស់ទោះ pod ងាប់
    - metadata: { name: data }
      spec:
        accessModes: [ReadWriteOnce]
        resources: { requests: { storage: 1Gi } }
```

**`postgres/service.yaml`:** `clusterIP: None` (headless) — សួរ Claude Code ថាហេតុអ្វី StatefulSet ត្រូវការ headless service។

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres/
kubectl -n minishop get pods -w            # រង់ចាំ postgres-0 Running 1/1
kubectl -n minishop get pvc                # Bound?
kubectl -n minishop exec -it postgres-0 -- psql -U minishop -c '\l'
```

**លំហាត់:**
1. `kubectl -n minishop delete pod postgres-0` → pod ថ្មីឈ្មោះអ្វី? (`postgres-0` ម្ដងទៈត — ហេតុអ្វី?) ទិន្នន័យនៈឬបាត់?
2. `kubectl -n minishop delete statefulset postgres` → PVC នៈឬបាត់? ហេតុអ្វី K8s **មិន**លុប PVC?

**សំណួរឆ្លុះបញ្ចាំង (ADR-006):** ហេតុអ្វី DB ជា StatefulSet មិនមែន Deployment? ហើយ**នៈ production ពិត** តើគួរ run Postgres ក្នុង K8s ឬប្រើ managed DB (RDS/Cloud SQL)? សរសេរ trade-off។

---

## Task 3 — App Deployment + ConfigMap + Probes

**កែ Spring Boot មុន (Phase A app):** បន្ថែម `application-k8s.yml`? **មិនចាំបាច់** — យើងប្រើ env var ដដែល។ ប៉ុន្តែពិនិត្យថា `management.endpoint.health.probes.enabled: true` មាន (Phase A Task 0) → `/actuator/health/liveness` និង `/readiness` ដំណើរការ។

**`app/configmap.yaml`:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata: { name: app-config, namespace: minishop }
data:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/minishop   # ← "postgres" = Service name
  SPRING_DATASOURCE_USERNAME: minishop
  SPRING_PROFILES_ACTIVE: k8s
  JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=75.0"                     # ← សួរ: ហេតុអ្វី?
```

**`app/deployment.yaml` (គ្រោង — អ្នកប្រើបំពេញ):**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: minishop-app, namespace: minishop }
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate: { maxSurge: 1, maxUnavailable: 0 }   # ← 0 downtime: ថ្មីឡើងសិន ចាស់ចុះក្រោយ
  selector:
    matchLabels: { app: minishop }
  template:
    metadata:
      labels: { app: minishop }
    spec:
      containers:
        - name: app
          image: ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>     # ← pin sha! មិនដែល :latest
          ports: [{ containerPort: 8080, name: http }]
          envFrom:
            - configMapRef: { name: app-config }
          env:
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef: { name: postgres-secret, key: POSTGRES_PASSWORD }
          startupProbe:                                  # ← Spring Boot ចាប់ផ្តើមយឺត
            httpGet: { path: /actuator/health/liveness, port: http }
            failureThreshold: 30
            periodSeconds: 2
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            periodSeconds: 5
          resources:
            requests: { cpu: 250m, memory: 384Mi }
            limits: { memory: 512Mi }
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true                 # ← អាចខូច! Spring ត្រូវសរសេរ /tmp → បន្ថែម emptyDir
```

**Image pull ពី ghcr.io private:**
```bash
kubectl -n minishop create secret docker-registry ghcr-creds \
  --docker-server=ghcr.io --docker-username=<username> \
  --docker-password=<GitHub PAT read:packages>
# រួចបន្ថែម imagePullSecrets: [{ name: ghcr-creds }] ក្នុង pod spec
```
ឬ **ធ្វើ package ជា public** នៈ GitHub → មិនត្រូវការ secret។ (សម្រាប់រៈន: public ងាយជាង)

```bash
kubectl apply -f k8s/app/
kubectl -n minishop get pods -w
kubectl -n minishop describe pod <pod>     # Events នៈខាងក្រោម — អានពីក្រោមឡើង
kubectl -n minishop logs -f <pod>
kubectl -n minishop port-forward svc/minishop-app 8080:8080   # សាកមុន Ingress
```

**ចំណុចដែលអ្នក**នឹង**ខូច (ចេតនា — កត់ក្នុង runbook):**
| រោគសញ្ញា | មូលហេតុទំនង | ពាក្យបញ្ជា debug |
|---|---|---|
| `ImagePullBackOff` | image name/tag ខុស, private registry | `describe pod` → Events |
| `CrashLoopBackOff` | app crash — DB connect មិនបាន? | `logs --previous` |
| `Running` តែ `0/1 Ready` | readiness probe fail | `describe pod` → probe failed message |
| `OOMKilled` | memory limit តូចពេក | `describe pod` → Last State: Terminated: OOMKilled |
| `Pending` | resources request ធំជាង node | `describe pod` → "Insufficient cpu" |

**សំណួរឆ្លុះបញ្ចាំង:**
- Liveness fail → K8s ធ្វើអ្វី? Readiness fail → ធ្វើអ្វី? ហេតុអ្វីច្រឡំ ២ នេះ**គ្រោះថ្នាក់**? (hint: DB ដាច់ ១ នាទី — ចង់ restart app ឬចង់គ្រាន់តែឈប់ផ្ញៈ traffic?)
- `requests` vs `limits` — ហេតុអ្វីមិនកំណត់ CPU limit? (មានទស្សនៈ ២ ខាង — សរសេរទាំងពីរ)
- Pin `<sha>` ក្នុង manifest — ដូច្នេះរាល់ release ត្រូវកែ manifest ដោយដៈ? នោះជាបញ្ហាដែល Phase E (GitOps) ដោះស្រាយ។

---

## Task 4 — Service + Ingress

**`app/service.yaml`:** type `ClusterIP`, port 80 → targetPort `http`, selector `app: minishop`

**`app/ingress.yaml`:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: minishop
  namespace: minishop
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
    - host: minishop.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service: { name: minishop-app, port: { number: 80 } }
```

```bash
echo "127.0.0.1 minishop.local" | sudo tee -a /etc/hosts
curl -s http://minishop.local/actuator/health | jq
curl -s -X POST http://minishop.local/api/products -H 'Content-Type: application/json' \
  -d '{"sku":"K8S-001","name":"Kubernetes Book","price":29.99,"stock":10}' | jq
```

**លំហាត់ — ឃើញ load balancing:**
```bash
# បន្ថែម hostname ក្នុង response? ឬមើល log ២ pod ក្នុងពេលដំណាលគ្នា:
kubectl -n minishop logs -f -l app=minishop --prefix=true
# terminal ២:
for i in $(seq 1 20); do curl -s http://minishop.local/api/products > /dev/null; done
# request ទៀកទៅ pod ណា? ចែកស្មៈៈគ្នាឬអត់?
```

**សំណួរឆ្លុះបញ្ចាំង:** ClusterIP vs NodePort vs LoadBalancer vs Ingress — ពេលណាប្រើមួយណា? ហេតុអ្វី kind ត្រូវការ `extraPortMappings`?

---

## Task 5 — Zero-Downtime Rolling Update + Self-healing

**ការរៈបចំ:** terminal ១ run load ជាបន្ត:
```bash
while true; do
  code=$(curl -s -o /dev/null -w '%{http_code}' http://minishop.local/actuator/health)
  echo "$(date +%T) $code"; sleep 0.2
done | tee rollout.log
```

**លំហាត់ 5.1 — Rolling update:**
```bash
# terminal ២: push commit ថ្មីទៈ main (Phase A2 CI build image ថ្មី) → យក sha ថ្មី
kubectl -n minishop set image deployment/minishop-app app=ghcr.io/vuthin-devops-ecommerce/mini-shop:<new-sha>
kubectl -n minishop rollout status deployment/minishop-app
grep -v 200 rollout.log                    # ← ត្រូវ**ទទៈ** (គ្មាន non-200)
```
បើមាន 5xx → ហេតុអ្វី? (hint: `preStop` hook + `terminationGracePeriodSeconds` + Spring `server.shutdown: graceful`)

**លំហាត់ 5.2 — Rollback:**
```bash
kubectl -n minishop rollout history deployment/minishop-app
kubectl -n minishop rollout undo deployment/minishop-app
```

**លំហាត់ 5.3 — Self-healing:**
```bash
kubectl -n minishop delete pod -l app=minishop --wait=false   # សម្លាប់ទាំង ២!
kubectl -n minishop get pods -w
grep -v 200 rollout.log                    # ដាច់ប៉ុន្មានវិនាទី? ហេតុអ្វី?
```

**លំហាត់ 5.4 — Deploy ដែលខូច:**
push image ដែល `/actuator/health/readiness` return 503 (ឧ. កែ config ឱ្យ DB URL ខុស) → `set image` → មើល:
- Pod ថ្មី Ready ឬអត់? Pod ចាស់ត្រូវសម្លាប់ឬអត់? Traffic នៈតែ 200 ឬអត់?
- **នេះជាហេតុផលពិតដែល readiness probe មាន។**

**សំណួរឆ្លុះបញ្ចាំង (ADR-007):** `maxSurge: 1, maxUnavailable: 0` vs `maxSurge: 0, maxUnavailable: 1` — ខុសគ្នាយ៉ាងណាលើ resource និង availability? Phase E នឹងបន្ថែម canary — RollingUpdate ខ្វះអ្វី?

---

## Task 6 — Kustomize (base + overlays)

**បញ្ហា:** ឥឡូវ `replicas: 2`, image sha, host `minishop.local` hardcode។ Production ត្រូវ `replicas: 3`, host ផ្សេង។

```
k8s/
├── base/
│   ├── kustomization.yaml
│   ├── namespace.yaml
│   ├── postgres/...
│   └── app/...
└── overlays/
    ├── local/
    │   ├── kustomization.yaml     # replicas: 2, image tag, host minishop.local
    │   └── patches/
    └── prod/                      # ត្រៈមសម្រាប់ Phase A4/E — មិនប្រើឥឡូវ
        └── kustomization.yaml
```

**`overlays/local/kustomization.yaml`:**
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources: [../../base]
namespace: minishop
images:
  - name: ghcr.io/vuthin-devops-ecommerce/mini-shop
    newTag: <sha>                  # ← កន្លែងតែមួយដែលត្រូវកែពេល release
replicas:
  - name: minishop-app
    count: 2
```

```bash
kubectl kustomize k8s/overlays/local | less    # មើល output មុន apply
kubectl apply -k k8s/overlays/local
kubectl diff -k k8s/overlays/local              # ← មុន apply រាល់ដង!
```

**សំណួរឆ្លុះបញ្ចាំង:** Kustomize vs Helm — ខុសគ្នាយ៉ាងណា? ហេតុអ្វីយើងជ្រៈស Kustomize ឥឡូវ តែ Phase B អាចប្តូរទៈ Helm?

---

## Task 7 — HPA (Autoscaling) + Load Test

```bash
# metrics-server សម្រាប់ kind (ត្រូវ --kubelet-insecure-tls)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.9.0/components.yaml   # ← pin (ធ្លាប់ latest)
kubectl -n kube-system patch deployment metrics-server --type=json \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
kubectl top nodes && kubectl top pods -n minishop    # ចាំ ~1 នាទី
```

**`app/hpa.yaml`:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata: { name: minishop-app, namespace: minishop }
spec:
  scaleTargetRef: { apiVersion: apps/v1, kind: Deployment, name: minishop-app }
  minReplicas: 2
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: { type: Utilization, averageUtilization: 60 }
```

**Load test ជាមួយ k6** (តម្លើង: https://k6.io):
```javascript
// scripts/load-test.js
import http from 'k6/http';
export const options = { vus: 50, duration: '3m' };
export default function () {
  http.get('http://minishop.local/api/products');
}
```
```bash
k6 run scripts/load-test.js
# terminal ២:
kubectl -n minishop get hpa -w             # TARGETS 60% → REPLICAS 2→3→4?
```

**សំណួរឆ្លុះបញ្ចាំង:**
- HPA scale up ក្នុងប៉ុន្មានវិនាទី? scale down? ហេតុអ្វី down យៈតជាង up?
- CPU 60% គណនាពី `requests` ឬ `limits`? (ពិនិត្យ!) ដូច្នេះ `requests` ខុសមានន័យ HPA ខុស។
- បើ Postgres ក្លាយជា bottleneck (app 5 replica តែ DB 1) — HPA ជួយបានឬអត់? នេះនាំទៈ Phase D (observability) — អ្នកត្រូវ**មើលឃើញ** bottleneck នៈឯណា។

---

## Task 8 — Documentation

- [ ] `docs/decisions/006-postgres-statefulset-vs-managed.md`
- [ ] `docs/decisions/007-rolling-update-strategy.md`
- [ ] `docs/runbooks/k8s-pod-not-ready.md` — តារាង "រោគសញ្ញា → មូលហេតុ → debug" ពី Task 3 + អ្វីដែលអ្នកជួបពិត
- [ ] `docs/runbooks/k8s-rollback.md`
- [ ] README: ផ្នែក "Run on Kubernetes (kind)" — ពី `kind create` ដល់ `curl`
- [ ] `docs/learning-log.md` — ចម្លៈយទាំងអស់ + **កាលៈហក** rollout.log (ដាច់ប៉ុន្មានវិនាទីក្នុងលំហាត់ 5.3?)

**សំណួរធំ (Phase ក្រោយ):**
1. `set image` ដោយដៈរាល់ release — អ្នកណាចាំ? បើភ្លេច? → Phase E: ArgoCD មើល git → apply ស្វ័យប្រវត្តិ
2. Secret ក្នុង `secret.yaml` លើ laptop — ក្រុម ៥ នាក់ចែកគ្នាយ៉ាងណា? → Phase B: Sealed Secrets / External Secrets / Vault
3. kind ងាប់ = គ្រប់យ៉ាងបាត់។ Cluster ពិតបង្កើតយ៉ាងណាដោយ code? → Phase A4: Terraform
4. Pod ខូច តែ `kubectl get pods` បង្ហាញ Running — ដឹងយ៉ាងណា? → Phase D: metrics + logs + traces

---

## Progress tracker

| Task | ស្ថានភាព | ថ្ងៈបញ្ចប់ | កំណត់ចំណាំ |
|---|---|---|---|
| 0 kubectl + kind | ✅ | 2026-09-29 | kind v0.33.0 (~/tools), kubectl v1.36.1 (Docker Desktop), scripts/check-env.sh |
| 1 Cluster + Ingress ctrl | ✅ | 2026-09-29 | node v1.37.0 ×3 · ingress-nginx controller-v1.15.1 · ជួប x509 (kind-trust-ca.sh) + nodeSelector patch |
| 2 Postgres StatefulSet | ✅ | 2026-09-29 | postgres-0 (worker2), PVC data-postgres-0 Bound 1Gi standard · PVC រស់ក្រោយ delete pod + STS: ✅ (UID ដដែល, data នៅ) |
| 3 App Deployment + probes | ✅ | 2026-09-30 | error ដែលជួប: ImagePullBackOff 401 → PAT secret · CrashLoop Flyway non-empty schema · UnknownHost postgres · Docker VM 1.9GB OOM → 8GB · លទ្ធផល: 2/2 Ready, start 4.7s, 8 products |
| 4 Service + Ingress | ⬜ | | |
| 5 Rolling update | ⬜ | | non-200 count: __ / self-heal ដាច់: __s |
| 6 Kustomize | ⬜ | | |
| 7 HPA + k6 | ⬜ | | max replicas ដល់: __ |
| 8 Docs | ⬜ | | |

**បន្ទាប់:** Phase A4 — Terraform មូលដ្ឋាន (បង្កើត kind cluster + namespace + ingress ដោយ code, ត្រៈមផ្លាស់ទៈ cloud)