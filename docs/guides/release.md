# Release និង rollback

> **គោលបំណង:** យក code ពី branch ទៅ image ទៅ cluster, និងថយក្រោយពេលខូច · **អ្នកអាន:** អ្នក release · **Update:** 2026-09-30
> ស្ថានភាព: build/scan/push **ស្វ័យប្រវត្តិ**; deploy ទៅ cluster **ដោយដៃ** (Phase E: GitOps)។ Zero-downtime បញ្ជាក់រួច (A3 Task 5, ADR-007)។

## ១. ពី code ទៅ image

```bash
git switch develop && git pull
# … commit …
git push                                  # CI: unit-test → integration-test
gh pr create --base main --head develop   # check ២ ត្រូវបៃតង (branch protection)
gh pr merge <n> --merge                   # ឬចុច Merge លើ GitHub
```

Run លើ `main` → job `docker` → build → **Trivy CRITICAL gate** → push។ យក tag:

```bash
gh run list --workflow ci.yml --branch main --limit 1
gh run view <run-id> --log | grep "pushing manifest for" | head -1
# ghcr.io/vuthin-devops-ecommerce/mini-shop:<short-sha>@sha256:…
```

Tag = **short git sha** (7 តួ) របស់ merge commit — `git log --oneline -1 origin/main`។ Trivy ក្រហម → image **មិន**ត្រូវ push; ជួសជុលតាម [runbooks/ci-failure.md §3b](../runbooks/ci-failure.md)។

## ២. Deploy ទៅ cluster

```bash
NEW=ghcr.io/vuthin-devops-ecommerce/mini-shop:<short-sha>
kubectl -n minishop set image deployment/minishop-app app=$NEW
kubectl -n minishop rollout status deployment/minishop-app
```

រំពឹង: pod ថ្មីឡើងម្តងមួយ (`maxSurge: 1`), pod ចាស់ចុះតែពេលថ្មី Ready (`maxUnavailable: 0`)។ Pod ថ្មីមិន Ready (readiness fail) → rollout ជាប់, **pod ចាស់នៅបម្រើ traffic**។ វាស់ 2026-09-30: 0 error / 69 request ក្រោម load ([ADR-007](../decisions/007-rolling-update-strategy.md)) — ទាមទារ `preStop sleep 10` + graceful shutdown; `rollout status` ត្រឡប់មុន pod ចាស់បញ្ចប់ termination (~10s)។

បន្ទាប់: update `image:` ក្នុង `k8s/app/deployment.yaml` ឱ្យត្រូវ → PR (manifest = ការពិត; `set image` តែឯង = drift)។ Phase A3 Task 6 (Kustomize) ផ្លាស់វាទៅ `images.newTag` កន្លែងតែមួយ។

**កុំ deploy `:latest`** — rollback មិនបាន និង pod អាច run code ខុសគ្នា។

## ៣. Rollback

```bash
kubectl -n minishop rollout history deployment/minishop-app
kubectl -n minishop rollout undo deployment/minishop-app                 # ទៅ revision មុន
kubectl -n minishop rollout undo deployment/minishop-app --to-revision=<n>
kubectl -n minishop rollout status deployment/minishop-app
```

ឬ `set image` ទៅ sha ចាស់ដែលដឹងថាល្អ។ ក្រោយ rollback: revert commit ឬ update manifest ឱ្យត្រូវ cluster។

## ៤. Checklist

- [ ] CI run លើ `main` បៃតង, Trivy CRITICAL = 0
- [ ] Tag ជា sha (មិនមែន `latest`)
- [ ] Schema ប្តូរ? migration ត្រូវ backward-compatible ជាមួយ version ចាស់ (pod ចាស់ + ថ្មី run ព្រមគ្នា ពេល rolling) — [guides/database-migration.md](database-migration.md)
- [ ] `rollout status` ជោគជ័យ, `curl …/actuator/health/readiness` UP
- [ ] `k8s/app/deployment.yaml` image update ក្នុង git
