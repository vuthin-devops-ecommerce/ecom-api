#!/usr/bin/env bash
# kind-trust-ca.sh — ដាក់ CA certificate (PEM) ចូល kind node ទាំងអស់ ឱ្យ containerd pull image បានលើ network
# ដែលមាន TLS interception (ឧ. corporate proxy)។ Symptom: pod ErrImagePull
#   "tls: failed to verify certificate: x509: certificate signed by unknown authority"
#
# ប្រើ: bash scripts/kind-trust-ca.sh <ca.pem> [cluster-name]
#   ឧ.  bash scripts/kind-trust-ca.sh ~/somansa-root-ca.crt minishop
#
# ► ហេតុអ្វីនៅទីនេះ មិនមែនក្នុង Dockerfile/manifest: CA របស់ក្រុមហ៊ុនជា environment របស់ laptop មិនមែនរបស់ project។
#   kind node = container ក្នុងម៉ាស៊ីនអ្នក → កែ trust store របស់ node មិនប៉ះ code ដែល commit។ CI/cluster ពិតមិនត្រូវការ។
# ► ត្រូវ run ម្តងទៀតក្រោយ `kind delete` + `create` (node ថ្មី = trust store ថ្មី)។ PEM file កុំ commit (មិនមែន secret តែមិនមែនរបស់ project)។
set -euo pipefail

ca="${1:?usage: $0 <ca.pem> [cluster-name]}"
cluster="${2:-minishop}"
[ -f "$ca" ] || { echo "CA file not found: $ca" >&2; exit 1; }
grep -q "BEGIN CERTIFICATE" "$ca" || { echo "not a PEM certificate: $ca" >&2; exit 1; }

KIND="${KIND:-kind}"; command -v "$KIND" >/dev/null 2>&1 || KIND="$HOME/tools/kind/kind.exe"
nodes=$("$KIND" get nodes --name "$cluster")
[ -n "$nodes" ] || { echo "no nodes for cluster '$cluster'" >&2; exit 1; }

export MSYS_NO_PATHCONV=1   # Git Bash: កុំបំប្លែង /usr/local/... ជា C:\...
for n in $nodes; do
  echo "== $n"
  docker cp "$ca" "$n:/usr/local/share/ca-certificates/corp-ca.crt"
  docker exec "$n" sh -c 'update-ca-certificates >/dev/null 2>&1; systemctl restart containerd'
  docker exec "$n" sh -c 'curl -s -o /dev/null -w "registry.k8s.io https=%{http_code}\n" https://registry.k8s.io/v2/ || true'
done
echo "done — pod ដែល ImagePullBackOff នឹង retry ខ្លួនឯង (kubectl get pods -A -w)"
