# Run on Kubernetes (kind)

> **គោលបំណង:** ពី cluster ទទេ ដល់ `curl http://minishop.local` · **អ្នកអាន:** អ្នក run/debug app លើ K8s · **Update:** 2026-09-30
> Architecture: [architecture.md §២](../architecture.md) · Config: [reference/configuration.md](../reference/configuration.md) · Pod មិន Ready: `runbooks/k8s-pod-not-ready.md` (Phase A3 Task 8)

## តម្រូវការ

| | |
|---|---|
| Docker Desktop memory | **≥ 8 GB** (Settings → Resources). Default Hyper-V 1.9 GB → VM ងាប់ (`kubectl: TLS handshake timeout`) |
| kind | v0.33.0 · kubectl ±1 minor ពី 1.37 — `bash scripts/check-env.sh` |
| Image | `ghcr.io/vuthin-devops-ecommerce/mini-shop:<sha>` ពី CI (package **private**) |
| GitHub PAT | classic, scope `read:packages` (សម្រាប់ pull image) |

## ១. បង្កើត cluster (~1 នាទី)

```bash
kind create cluster --name minishop --config k8s/kind-config.yaml
kubectl get nodes
```

រំពឹង: `minishop-control-plane`, `minishop-worker`, `minishop-worker2` — `Ready`, `v1.37.0`។

**Network ការិយាល័យ (TLS interception) ប៉ុណ្ណោះ** — បើ pod `ErrImagePull … x509: certificate signed by unknown authority`:

```bash
bash scripts/kind-trust-ca.sh <corporate-root-ca.pem> minishop    # ត្រូវ run ម្តងទៀតរាល់ kind create
```

## ២. Ingress controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/kind/deploy.yaml
kubectl -n ingress-nginx patch deployment ingress-nginx-controller --type=merge \
  -p '{"spec":{"template":{"spec":{"nodeSelector":{"kubernetes.io/os":"linux","ingress-ready":"true"}}}}}'
kubectl -n ingress-nginx rollout status deploy/ingress-nginx-controller
curl -s -o /dev/null -w '%{http_code}\n' http://localhost/
```

រំពឹង: `404` (nginx ឆ្លើយ, មិនទាន់មាន rule)។ Patch nodeSelector ចាំបាច់ព្រោះ manifest v1.15.1 លែងបង្ខំ controller ទៅ control-plane (node តែមួយដែលមាន port 80)។

## ៣. Namespace + Postgres

```bash
cp k8s/postgres/secret.example.yaml k8s/postgres/secret.yaml     # កែ password (file gitignored)
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres/
kubectl -n minishop rollout status statefulset/postgres
```

រំពឹង: `postgres-0` `1/1 Running`, PVC `data-postgres-0` `Bound`។

## ៤. Pull secret + app

```bash
read -s -p "GitHub PAT (read:packages): " GHCR_PAT; echo
kubectl -n minishop create secret docker-registry ghcr-creds \
  --docker-server=ghcr.io --docker-username=<github-user> --docker-password="$GHCR_PAT"
unset GHCR_PAT

kubectl apply -f k8s/app/
kubectl -n minishop rollout status deployment/minishop-app
```

រំពឹង: `minishop-app-…` ×2 `1/1 Running` (startup ~5–30s)។

## ៥. ផ្ទៀងផ្ទាត់

```bash
# hosts file ម្តង (Windows: Notepad ជា Administrator → C:\Windows\System32\drivers\etc\hosts)
#   127.0.0.1 minishop.local
curl http://minishop.local/actuator/health/readiness     # {"status":"UP"}
curl http://minishop.local/api/products                  # 8 products (profile dev)

# គ្មាន hosts file:
curl --resolve minishop.local:80:127.0.0.1 http://minishop.local/api/products
# ឬ bypass Ingress:
kubectl -n minishop port-forward svc/minishop-app 8080:80   # → http://localhost:8080/swagger-ui.html
```

## ប្រតិបត្តិការប្រចាំថ្ងៃ

| ចង់… | Command |
|---|---|
| មើលអ្វីៗទាំងអស់ | `kubectl -n minishop get all,ingress,pvc` |
| Log app ទាំង ២ pod | `kubectl -n minishop logs -f -l app=minishop --prefix` |
| ហេតុអ្វី pod មិន Ready | `kubectl -n minishop describe pod <pod>` (Events ពីក្រោមឡើង) → `logs <pod> --previous` |
| Restart ក្រោយប្តូរ ConfigMap/Secret | `kubectl -n minishop rollout restart deployment/minishop-app` |
| Release image ថ្មី | [guides/release.md](release.md) |
| psql | `kubectl -n minishop exec -it postgres-0 -- psql -U minishop -d minishop` |
| ឈប់ app (សន្សំ memory) | `kubectl -n minishop scale deployment/minishop-app --replicas=0` |
| លុបទាំងអស់ (**data បាត់**) | `kind delete cluster --name minishop` |

## ក្រោយ laptop/Docker restart

Node container ត្រឡប់ខ្លួនឯង; app អាច `CrashLoopBackOff` (`UnknownHostException: postgres`) ពីរបីនាទីរហូតដល់ DNS/DB Ready — kubelet restart ខ្លួនឯង (ធម្មតា)។ ពិនិត្យ: `docker ps | grep minishop` → `kubectl -n minishop get pods`។
