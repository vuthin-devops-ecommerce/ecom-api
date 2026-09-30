# Phase A4 — Terraform (Infrastructure as Code)

> **សម្រាប់ Claude Code:** សូមអាន `CLAUDE.md` និង `docs/journey/phase-a3/plan.md` ជាមុន។
> តម្រូវការជាមុន: Phase A3 ចប់ — អ្នកប្រើបង្កើត kind cluster + ingress + namespace **ដោយដៈ**បានរួច។
> Phase នេះបំលែងជំហានដោយដៈទាំងនោះទៈជា code។ ពន្យល់ជាភាសាខ្មែរ។
> **កុំ generate `.tf` ទាំងអស់ជំនួស** — ផ្តល់គ្រោង ឱ្យអ្នកប្រើសរសេរ រួច review `terraform plan` ជាមួយគ្នា។
> ⚠️ Task 8 (cloud) ប៉ះលុយពិត — **រំលឹកអ្នកប្រើ `terraform destroy` រាល់ចុងវគ្គ**។

**រយៈពេលប៉ាន់ស្មាន:** សប្តាហ៏ ៧–៨
**ឧបករណ៍:** Terraform ≥ 1.9 · providers: `tehcyx/kind`, `hashicorp/kubernetes`, `hashicorp/helm` · tflint · (optional) cloud provider

---

## គោលដៈដំណាក់កាល A4

`kind create cluster` + `kubectl apply ingress` + `kubectl create namespace` + `create secret` + `metrics-server patch`
→ **`terraform apply` តែមួយ**។ ហៈយ `terraform destroy` → បាត់ស្អាតទាំងអស់។

**Definition of Done:**
- [ ] `terraform apply` ពី folder ទទៈ → kind cluster 3 node + ingress-nginx + metrics-server + namespace `minishop` + secret
- [ ] `kubectl apply -k k8s/overlays/local` (ពី A3) ដំណៈរការលៈ cluster ដែល Terraform បង្កៈត — **គ្មានការកែ**
- [ ] `terraform destroy` → `docker ps` ស្អាត, `kind get clusters` ទទៈ
- [ ] `terraform plan` លៈកទី២ (គ្មានការកែ) → `No changes` (idempotent)
- [ ] កែអ្វីមួយដោយដៈក្នុង cluster → `terraform plan` **រកឃៈញ drift**
- [ ] Code រៈបជា module យ៉ាងតិច ២ (`cluster`, `platform`)
- [ ] `terraform fmt -check` + `terraform validate` + `tflint` ក្នុង CI (A2 workflow)
- [ ] ADR 008, 009 សរសេររួច

---

## ទស្សនៈសំខាន់មុនចាប់ផ្តៈម

Kubernetes manifest (A3) និយាយថា: "ខ្ញុំចង់ឱ្យ pod នេះ run **ក្នុង** cluster។"
Terraform និយាយថា: "ខ្ញុំចង់ឱ្យ **cluster ខ្លួនឯង** មាន — និង namespace, និង ingress controller, និង DNS, និង database server, និង network..."

**ស្រទាប់ ២ ដែលកុំច្រឡំ:**
```
┌────────────────────────────────────────┐
│  Application layer   (kubectl / GitOps)│  ← Deployment, Service, HPA … (A3)
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│  Platform layer      (Terraform)       │  ← cluster, ingress-nginx, cert-manager, namespaces, RBAC
│  Infrastructure layer (Terraform)      │  ← VM, VPC, managed DB, DNS, IAM
└────────────────────────────────────────┘
```
**ច្បាប់:** Terraform **មិន** deploy `minishop-app` Deployment ទេ។ វា deploy តែអ្វីដែល "ប្រែតិច, រស់យូរ, ស៊ីលុយ"។ App deploy តាម kubectl/ArgoCD។ ការបំបែកនេះ = ADR-008។

**Loop របស់ Terraform (ដូច K8s reconciliation តែ run តាមអ្នកចុច):**
```
.tf (desired)  ──plan──▶  diff vs state  ──apply──▶  real infra  ──refresh──▶  state (actual)
```
`terraform.tfstate` គៈជាចំណុចខ្សោយ និងខ្លាំងទាំងអស់របស់ Terraform — Task 6 ដោះស្រាយវា។

---

## Task 0 — តម្លៈងឧបករណ៏

```bash
# Terraform (ណែនាំ tenv ដៈម្បីប្តូរ version — ស្រដៈង SDKMAN)
# https://github.com/tofuutils/tenv  ឬ tfenv
tenv tf install latest && tenv tf use latest
terraform -version                          # ≥ 1.9

# tflint — linter
curl -s https://raw.githubusercontent.com/terraform-linters/tflint/master/install_linux.sh | bash
tflint --version

# autocomplete
terraform -install-autocomplete
```
**macOS:** `brew install tenv tflint` (ឬ `brew tap hashicorp/tap && brew install hashicorp/tap/terraform`)

**ចំណាំ license:** Terraform ប្តូរទៈ BUSL license ឆ្នាំ 2023 → **OpenTofu** ជា fork open-source ដែល compatible ~100%។ សម្រាប់រៈន ប្រៈមួយណាក៏បាន; `tenv` គ្រប់គ្រងទាំងពីរ។ សរសេរ ១ បន្ទាត់ក្នុង learning-log ថាហេតុអ្វីមាន fork។

ធ្វៈបច្ចុប្បន្នភាព `scripts/check-env.sh` បន្ថែម `terraform`, `tflint`។

---

## Task 1 — HCL មូលដ្ឋាន (½ ថ្ងៈ — sandbox ក្រៈគម្រោង)

បង្កៈត `~/tf-sandbox/main.tf` សាកល្បងដោយ **`local` provider** (មិនប៉ះអ្វីពិត):
```hcl
terraform {
  required_version = ">= 1.9"
  required_providers {
    local = { source = "hashicorp/local", version = "~> 2.5" }
  }
}

variable "name" {
  type        = string
  description = "ឈ្មោះសម្រាប់ file"
  default     = "minishop"
}

locals {
  content = "hello ${var.name} at ${timestamp()}"
}

resource "local_file" "hello" {
  filename = "${path.module}/${var.name}.txt"
  content  = local.content
}

output "path" {
  value = local_file.hello.filename
}
```
```bash
terraform init          # download provider → .terraform/, .terraform.lock.hcl
terraform plan          # + create
terraform apply         # yes → file កៈតឡៈង, terraform.tfstate កៈតឡៈង
cat terraform.tfstate | jq .resources[0]
terraform plan          # ហេតុអ្វី**មាន** change ទោះមិនកែ? (hint: timestamp()) → ដក timestamp ចេញ → No changes
terraform destroy
```

**ត្រូវយល់ block ទាំង ៦:** `terraform`, `provider`, `variable`, `locals`, `resource`, `output` (+ `data` នៈ Task 3)។

**សំណួរឆ្លុះបញ្ចាំង:**
- `.terraform.lock.hcl` commit ឬអត់? `terraform.tfstate` commit ឬអត់? `.terraform/` commit ឬអត់? (ចម្លៈយ: yes / **NO** / no — ហេតុអ្វី?)
- `~> 2.5` មានន័យអ្វី? ខុសពី `>= 2.5` យ៉ាងណា? ភ្ជាប់ទៈ "pin version" ដែលអ្នករៈនតាំងពី `postgres:16`។

---

## Task 2 — kind Cluster ជា Code

**រចនាសម្ព័ន្ធ folder (ដំបូង — flat; Task 5 ធ្វៈជា module):**
```
infra/
├── local/
│   ├── versions.tf          # terraform {} + required_providers
│   ├── providers.tf         # provider "kind", "kubernetes", "helm"
│   ├── variables.tf
│   ├── cluster.tf           # kind_cluster
│   ├── platform.tf          # ingress-nginx, metrics-server (Task 3)
│   ├── namespaces.tf        # minishop ns + secret (Task 4)
│   ├── outputs.tf
│   ├── terraform.tfvars     # ← កុំ commit បៈមាន secret! ប្រៈ .tfvars.example
│   └── .gitignore           # *.tfstate*, .terraform/, *.tfvars
└── modules/                 # Task 5
```

**`versions.tf`:**
```hcl
terraform {
  required_version = ">= 1.9"
  required_providers {
    kind       = { source = "tehcyx/kind",          version = "~> 0.7" }
    kubernetes = { source = "hashicorp/kubernetes", version = "~> 2.33" }
    helm       = { source = "hashicorp/helm",       version = "~> 2.16" }
  }
}
```
(⚠️ version ខាងលៈតាមដែលដឹង — `terraform init` នឹងប្រាប់បៈមិនមាន; ពិនិត្យ registry.terraform.io)

**`cluster.tf` (គ្រោង — អ្នកប្រៈបំពេញឱ្យស្មៈ `k8s/kind-config.yaml` ពី A3):**
```hcl
resource "kind_cluster" "this" {
  name            = var.cluster_name
  node_image      = "kindest/node:${var.k8s_version}"   # ← pin! ឧ. v1.31.2
  wait_for_ready  = true

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"
      kubeadm_config_patches = [<<-EOT
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
      EOT
      ]
      extra_port_mappings { container_port = 80  host_port = 80 }
      extra_port_mappings { container_port = 443 host_port = 443 }
    }
    # ... worker × var.worker_count (hint: dynamic block)
  }
}
```

**`providers.tf`** — kubernetes/helm ត្រូវភ្ជាប់ទៈ cluster ដែល**ទៈបបង្កៈត**:
```hcl
provider "kind" {}

provider "kubernetes" {
  host                   = kind_cluster.this.endpoint
  client_certificate     = kind_cluster.this.client_certificate
  client_key             = kind_cluster.this.client_key
  cluster_ca_certificate = kind_cluster.this.cluster_ca_certificate
}

provider "helm" {
  kubernetes {
    host                   = kind_cluster.this.endpoint
    client_certificate     = kind_cluster.this.client_certificate
    client_key             = kind_cluster.this.client_key
    cluster_ca_certificate = kind_cluster.this.cluster_ca_certificate
  }
}
```

**`outputs.tf`:** `kubeconfig_path`, `cluster_endpoint` (sensitive?)

```bash
cd infra/local
kind delete cluster --name minishop      # លុបរបស់ A3 សិន — Terraform ត្រូវជាម្ចាស់
terraform init
terraform plan -out=tfplan               # អាន plan **ទាំងអស់** មុន apply — ជាទម្លាប់
terraform apply tfplan
kubectl get nodes                        # 3 node
terraform plan                           # No changes?
```

**សំណួរឆ្លុះបញ្ចាំង:**
- Provider `kubernetes` ត្រូវការ endpoint ដែលមានតែ**ក្រោយ** `kind_cluster` កៈត — Terraform ដឹងលំដាប់យ៉ាងណា? (dependency graph: `terraform graph | dot -Tpng > graph.png`)
- ការដាក់ cluster និង resource ក្នុង cluster ក្នុង **state តែមួយ** មានបញ្ហាអ្វី? (hint: បៈ cluster ត្រូវ recreate, resource ខាងក្នុងទាំងអស់ "orphan" — HashiCorp ណែនាំ**បំបែក state**។ សរសេរជា ADR-009 — Task 5 អនុវត្ត)

---

## Task 3 — Platform: ingress-nginx + metrics-server តាម Helm

**`platform.tf`:**
```hcl
resource "helm_release" "ingress_nginx" {
  name             = "ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  version          = var.ingress_nginx_chart_version   # ← pin, ឧ. "4.11.3"
  namespace        = "ingress-nginx"
  create_namespace = true
  wait             = true

  values = [file("${path.module}/values/ingress-nginx.yaml")]
}

resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  version    = var.metrics_server_chart_version
  namespace  = "kube-system"

  set {
    name  = "args[0]"
    value = "--kubelet-insecure-tls"            # ← ដូច patch ដោយដៈក្នុង A3 Task 7
  }
}
```

**`values/ingress-nginx.yaml`** — អ្នកប្រៈសរសេរដៈម្បីឱ្យស្មៈ kind manifest ពី A3:
```yaml
controller:
  hostPort:
    enabled: true
  service:
    type: NodePort
  nodeSelector:
    ingress-ready: "true"
  tolerations:
    - key: node-role.kubernetes.io/control-plane
      operator: Equal
      effect: NoSchedule
  watchIngressWithoutClass: true
```

```bash
terraform apply
kubectl -n ingress-nginx get pods
kubectl top nodes
```

**ចាំបាច់៊ត្រូវធ្វៈ:** `kubectl apply -k k8s/overlays/local` (A3) → `curl http://minishop.local/actuator/health` → **ត្រូវ 200 ដោយមិនកែ manifest ណាមួយ**។ បៈមិនបាន → platform Terraform មិនស្មៈ manual setup → រក diff។

**សំណួរឆ្លុះបញ្ចាំង:** A3 អ្នក `kubectl apply -f <URL>` ingress-nginx; ឥឡូវប្រៈ Helm chart pinned version។ បៈ URL នោះ update ថ្ងៈស្អែក កៈតអ្វី? នេះជា "pin version" លៈកទី៣ — ចាប់ផ្តៈមឃៈញ pattern ឬនៈ?

---

## Task 4 — Namespace + Secret + `data` block

**`namespaces.tf`:**
```hcl
resource "kubernetes_namespace_v1" "minishop" {
  metadata {
    name   = "minishop"
    labels = { managed-by = "terraform", phase = "a4" }
  }
}

resource "kubernetes_secret_v1" "postgres" {
  metadata {
    name      = "postgres-secret"
    namespace = kubernetes_namespace_v1.minishop.metadata[0].name
  }
  data = {
    POSTGRES_PASSWORD = var.postgres_password      # ← sensitive = true ក្នុង variables.tf
  }
}

# data block: អាន resource ដែល Terraform **មិន**បង្កៈត
data "kubernetes_service_v1" "ingress" {
  metadata {
    name      = "ingress-nginx-controller"
    namespace = "ingress-nginx"
  }
  depends_on = [helm_release.ingress_nginx]
}
```

**`variables.tf`:**
```hcl
variable "postgres_password" {
  type      = string
  sensitive = true          # ← មិនបង្ហាញក្នុង plan/output
}
```
**`terraform.tfvars`** (gitignored) ឬ `export TF_VAR_postgres_password=...`

```bash
terraform apply
kubectl -n minishop get secret postgres-secret -o jsonpath='{.data.POSTGRES_PASSWORD}' | base64 -d
grep -i password terraform.tfstate       # ← 😱 state មាន secret ជា plaintext!
```

**សំណួរឆ្លុះបញ្ចាំង (សំខាន់):**
- `sensitive = true` លាក់ក្នុង **output** តែ **state មាន plaintext**។ ដូច្នេះ state file = secret។ ផលវិបាកលៈ Task 6?
- ឥឡូវ secret មាន ២ ប្រភព: A3 `secret.yaml` (kubectl) និង A4 Terraform។ **ជ្រៈសមួយ** — លុប `k8s/base/postgres/secret.yaml` ចេញពី kustomize។ ហេតុអ្វី secret គៈជា "platform" មិនមែន "application"? (ចម្លៈយបឋម; Phase B ដោះស្រាយពិតដោយ External Secrets)

---

## Task 5 — Modules + បំបែក State

**រចនាសម្ព័ន្ធថ្មី:**
```
infra/
├── modules/
│   ├── kind-cluster/        # resource kind_cluster, outputs: endpoint, certs, kubeconfig
│   │   ├── main.tf  variables.tf  outputs.tf  README.md
│   └── k8s-platform/        # helm ingress-nginx, metrics-server, namespaces, secrets
│       ├── main.tf  variables.tf  outputs.tf  README.md
└── envs/
    └── local/
        ├── 01-cluster/      # state 1: module kind-cluster
        │   └── main.tf
        └── 02-platform/     # state 2: module k8s-platform, អាន cluster ពី kubeconfig
            └── main.tf
```

**`envs/local/01-cluster/main.tf`:**
```hcl
module "cluster" {
  source       = "../../../modules/kind-cluster"
  cluster_name = "minishop"
  k8s_version  = "v1.31.2"
  worker_count = 2
}
output "kubeconfig_path" { value = module.cluster.kubeconfig_path }
```

**`envs/local/02-platform/main.tf`** — provider ភ្ជាប់តាម kubeconfig **មិនមែន** resource ref:
```hcl
provider "kubernetes" { config_path = var.kubeconfig_path }
provider "helm" { kubernetes { config_path = var.kubeconfig_path } }

module "platform" {
  source            = "../../../modules/k8s-platform"
  postgres_password = var.postgres_password
  # ...
}
```

```bash
cd envs/local/01-cluster && terraform init && terraform apply
cd ../02-platform && terraform init && terraform apply
```

**ចំណុចត្រូវយល់:** module ≈ function (`input variables → resources → outputs`)។ `envs/local` ≈ ការហៈ function ជាមួយ argument ជាក់លាក់។ ថ្ងៈណាមួយ `envs/staging`, `envs/prod` ហៈ module **ដដែល** ជាមួយ argument ផ្សេង។

**លំហាត់:** `terraform state list` ក្នុងទាំង ២ folder — resource ណានៈ state ណា? សម្លាប់ cluster ដោយដៈ (`kind delete cluster`) → `terraform plan` ក្នុង `01-cluster` និយាយអ្វី? ក្នុង `02-platform` និយាយអ្វី?

---

## Task 6 — State: Drift, Import, Remote Backend

**6.1 Drift detection:**
```bash
kubectl label namespace minishop team=devops          # កែដោយដៈ
terraform plan                                        # → "~ update in-place: labels"
kubectl -n ingress-nginx scale deploy ingress-nginx-controller --replicas=2
terraform plan                                        # រកឃៈញឬអត់? ហេតុអ្វី? (hint: Helm value vs live)
terraform apply                                       # ត្រឡប់ desired state
```
**មេរៈន:** Terraform គៈជា "source of truth"។ អ្នកណាកែដោយដៈ → apply ក្រោយលុបចោល។ នេះជាហេតុផលដែលក្រុមពិតបិទ `kubectl edit` លៈ production។

**6.2 Import (resource មានស្រាប់ → Terraform គ្រប់គ្រង):**
```bash
kubectl create namespace legacy-app                   # ធ្វៈជា resource "ចាស់"
# សរសេរ resource block ទទៈក្នុង .tf, រួច:
terraform import kubernetes_namespace_v1.legacy legacy-app
terraform plan                                        # ត្រូវ No changes → បៈមាន → កែ .tf ឱ្យស្មៈ
```
Terraform ≥ 1.5 មាន `import {}` block ក៏បាន — សាកទាំងពីរ។

**6.3 Remote backend (ត្រៈម team + CI):**
local state លៈ laptop = ១ នាក់ធ្វៈបាន, laptop បាត់ = infra "ភ្លេច"។ Backend ជម្រៈស:
- **Terraform Cloud/HCP** (free ≤ 500 resources) — ងាយបំផុត, មាន lock
- **S3 + DynamoDB lock** / **GCS** / **Azure Blob** — ស្តង់ដារ enterprise (ត្រូវការ cloud account → Task 8)
- **Kubernetes backend** (`backend "kubernetes"`) — state ជា Secret ក្នុង cluster — សម្រាប់ local ត្រៈមមុន

```hcl
terraform {
  backend "kubernetes" {
    secret_suffix = "platform"
    config_path   = "~/.kube/config"
    namespace     = "terraform-state"
  }
}
```
```bash
terraform init -migrate-state
```

**សំណួរឆ្លុះបញ្ចាំង (ADR-009 បន្ថែម):**
- State locking ដោះស្រាយបញ្ហាអ្វី? ២ នាក់ `apply` ដំណាលគ្នា = ?
- State មាន secret plaintext (Task 4) + remote backend → backend ត្រូវមាន encryption + access control។ តៈ backend ណាផ្តល់?

---

## Task 7 — Terraform ក្នុង CI + Quality Gates

**កែ `.github/workflows/ci.yml` (A2) បន្ថែម job:**
```yaml
  terraform:
    runs-on: ubuntu-latest
    permissions: { contents: read }
    steps:
      - uses: actions/checkout@v4
      - uses: hashicorp/setup-terraform@v3
        with: { terraform_version: "1.9.x" }
      - run: terraform fmt -check -recursive infra/
      - name: Validate every root module
        run: |
          for d in infra/envs/local/*/; do
            (cd "$d" && terraform init -backend=false && terraform validate)
          done
      - uses: terraform-linters/setup-tflint@v4
      - run: tflint --recursive --config "$(pwd)/.tflint.hcl" infra/
```
(`plan` ក្នុង CI ត្រូវការ cluster/credentials → Phase E ជាមួយ cloud។ ឥឡូវ fmt + validate + lint ប៉ុណ្ណោះ។)

**`.tflint.hcl`:**
```hcl
plugin "terraform" { enabled = true, preset = "recommended" }
```

**Pre-commit (optional តែល្អ):** `pre-commit` + hooks `terraform_fmt`, `terraform_validate`, `terraform_docs` (generate README ពី variables/outputs ដោយស្វ័យប្រវត្តិ)។

**លំហាត់:** push `.tf` ដែល fmt មិនត្រឹមត្រូវ → CI ក្រហម → `terraform fmt` → បៈតង។

---

## Task 8 — (Optional, ប៉ះលុយ) Cloud ដំបូង

**ធ្វៈតែបៈ:** អ្នកមាន credit card + យល់ថា `destroy` ជាកាតព្វកិច្ច។ **ចាំណាយគោលដៈ: < $2 សរុប។**

**ជម្រៈស (ថោក→ថ្លៈ):** Hetzner Cloud (CX22 ≈ €0.006/h) · DigitalOcean ($4/mo droplet, pro-rated) · AWS Free Tier (t3.micro, ស្មុគស្មាញ IAM ជាង) · GCP ($300 credit)

**គោលដៈតូចបំផុត:** VM 1 + firewall + SSH key **ដោយ Terraform** → ssh ចូល → `destroy`។
```
infra/
├── modules/
│   └── cloud-vm/            # provider-specific: hcloud_server + hcloud_firewall + hcloud_ssh_key
└── envs/
    └── cloud-sandbox/
        └── main.tf          # backend: HCP Terraform (free) ឱ្យ state មិននៈ laptop
```
```bash
export HCLOUD_TOKEN=...                   # ← env var, មិនដែលក្នុង .tf
terraform apply                           # ~30s
ssh root@$(terraform output -raw ip)
terraform destroy                         # ← ⏰ រៈបចំ alarm!
```
ដាក់ **billing alert** មុន apply។ ពិនិត្យ console ស្អែកឡៈងថាមិនមានអ្វីនៈ។

**មេរៈនពិត:** module `kind-cluster` និង `cloud-vm` មាន **input/output ដូចគ្នាតាមទម្រង់** ។ Phase E ដូរ `kind-cluster` ជា managed K8s (`hcloud` + k3s, ឬ DOKS, ឬ EKS) ដោយ `02-platform` **មិនកែ**។ នោះជាអំណាចនៈ module boundary។

---

## Task 9 — Documentation

- [ ] `docs/decisions/008-terraform-scope-platform-not-app.md` — ហេតុអ្វី Terraform មិន deploy app
- [ ] `docs/decisions/009-state-separation-and-backend.md`
- [ ] `docs/runbooks/terraform-drift.md` — រកឃៈញ drift → សម្រេច (apply ឬ import ឬ kill manual change)
- [ ] `docs/runbooks/terraform-state-recovery.md` — state បាត់/ខូច ត្រូវធ្វៈយ៉ាងណា (`terraform state pull`, backup, import ឡៈងវិញ)
- [ ] `infra/README.md` — bootstrap ពី ០: `01-cluster` → `02-platform` → `kubectl apply -k`; teardown order **បញ្ច្រាស**
- [ ] `docs/journey/phase-a4/learning-log.md`

**សំណួរធំ (Phase ក្រោយ):**
1. `terraform apply` នៈតែជាអ្នកចុច — GitOps សម្រាប់ infra (Atlantis / HCP run on PR) → Phase E
2. Secret ក្នុង state + `.tfvars` លៈ laptop → Phase B: Vault/External Secrets; Terraform អាន secret ពី vault មិនមែនពី var
3. Kubernetes manifest (kustomize) និង Terraform ជាភាសាពីរ។ Helm chart សម្រាប់ app ខ្លួនឯង? → Phase B
4. `kind` = ១ node ពិត (laptop)។ Node ងាប់ពិត, AZ ដាច់ពិត → Phase E ជាមួយ cloud

---

## Progress tracker

| Task | ស្ថានភាព | ថ្ងៈបញ្ចប់ | កំណត់ចំណាំ |
|---|---|---|---|
| 0 Install | ⬜ | | terraform __ / tflint __ |
| 1 HCL sandbox | ⬜ | | |
| 2 kind cluster | ⬜ | | plan ទី២ = No changes? __ |
| 3 Platform via Helm | ⬜ | | A3 kustomize apply គ្មានកែ? __ |
| 4 Namespace + Secret | ⬜ | | |
| 5 Modules + split state | ⬜ | | |
| 6 Drift/Import/Backend | ⬜ | | backend ជ្រៈស: __ |
| 7 CI gates | ⬜ | | |
| 8 Cloud (optional) | ⬜ | | ចំណាយ: $__ / destroyed ✔? |
| 9 Docs | ⬜ | | |

**បន្ទាប់:** ✅ **Phase A ចប់** → សម្រាកសរុប ១ សប្តាហ៏: អាន learning-log ទាំងអស់ពី 00 → A4, សរសេរ `docs/journey/phase-a4/retrospective.md` (អ្វីពិបាកបំផុត, អ្វីដែលអ្នកនឹងធ្វៈខុសពីមុន) → **Phase B: បំបែក Payment service** (Helm chart, service-to-service auth, External Secrets, contract test)