#!/usr/bin/env bash
# check-env.sh — ពិនិត្យ tool + version លើម៉ាស៊ីន ធៀបនឹង docs/00-tech-stack.md
# ប្រើ: bash scripts/check-env.sh        (Git Bash លើ Windows ក៏បាន)
# ► ហេតុអ្វី: "run លើម៉ាស៊ីនខ្ញុំបាន" ភាគច្រើន = version ខុសគ្នា។ script នេះឆ្លើយក្នុង 5 វិនាទី។
# ► exit 1 បើ tool ចាំបាច់ណាមួយបាត់ — ប្រើក្នុង onboarding និងមុន debug អ្វីផ្សេង

set -u
ok=0; bad=0
pass() { printf '  \033[32m✔\033[0m %-14s %s\n' "$1" "$2"; ok=$((ok+1)); }
fail() { printf '  \033[31m✘\033[0m %-14s %s\n' "$1" "$2"; bad=$((bad+1)); }
warn() { printf '  \033[33m!\033[0m %-14s %s\n' "$1" "$2"; }

have() { command -v "$1" >/dev/null 2>&1; }

echo "== Stage 0 / Phase A (app) =="
if have java; then
  v=$(java -version 2>&1 | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
  case "$v" in 21*) pass java "$v (ត្រូវ 21)";; *) fail java "$v — ត្រូវការ 21 (Temurin)";; esac
else fail java "មិនមាន"; fi

if [ -f mini-shop/mvnw ]; then
  mv_v=$(grep -o 'apache-maven-[0-9.]*' mini-shop/.mvn/wrapper/maven-wrapper.properties | head -1)
  pass mvnw "wrapper → $mv_v (pin ក្នុង .mvn/wrapper)"
else warn mvnw "run ពី root repo ដើម្បីពិនិត្យ mini-shop/mvnw"; fi
if have mvn; then pass mvn "$(mvn -v 2>/dev/null | head -1 | awk '{print $3}') (fallback ពេល ./mvnw download មិនបាន — network TLS)"; fi

if have docker; then
  if docker info >/dev/null 2>&1; then pass docker "$(docker version --format '{{.Server.Version}}' 2>/dev/null) (daemon running)"
  else fail docker "CLI មាន តែ daemon មិន run — បើក Docker Desktop"; fi
else fail docker "មិនមាន"; fi
have git && pass git "$(git --version | awk '{print $3}')" || fail git "មិនមាន"

echo "== Phase A2 (CI) =="
have gh && pass gh "$(gh --version | head -1 | awk '{print $3}') ($(gh auth status 2>&1 | grep -o 'Logged in to [^ ]* account [^ ]*' | head -1))" || warn gh "optional — មើល run/PR ពី terminal"
if have trivy; then pass trivy "$(trivy --version 2>/dev/null | head -1 | awk '{print $2}')"
elif [ -x "$HOME/tools/trivy/trivy.exe" ]; then pass trivy "$("$HOME/tools/trivy/trivy.exe" --version | head -1 | awk '{print $2}') (~/tools/trivy)"
else warn trivy "optional លើ laptop — CI run វា; Windows: ~/tools/trivy"; fi

echo "== Phase A3 (Kubernetes) =="
if have kubectl; then pass kubectl "$(kubectl version --client 2>/dev/null | head -1 | awk '{print $3}') (skew ±1 minor ពី server)"
else fail kubectl "មិនមាន — Docker Desktop ភ្ជាប់មកជាមួយ ឬ dl.k8s.io"; fi
if have kind; then pass kind "$(kind version | awk '{print $2}') (ត្រូវ v0.33.0 — node kindest/node:v1.37.0)"
elif [ -x "$HOME/tools/kind/kind.exe" ]; then pass kind "$("$HOME/tools/kind/kind.exe" version | awk '{print $2}') (~/tools/kind — បើក terminal ថ្មីឱ្យ PATH ចូល)"
else fail kind "មិនមាន — https://github.com/kubernetes-sigs/kind/releases v0.33.0"; fi
have k9s && pass k9s "$(k9s version -s 2>/dev/null | head -1)" || warn k9s "optional (TUI)"
if have kind || [ -x "$HOME/tools/kind/kind.exe" ]; then
  kb=$(have kind && echo kind || echo "$HOME/tools/kind/kind.exe")
  c=$("$kb" get clusters 2>/dev/null | tr '\n' ' ')
  [ -n "$c" ] && pass cluster "kind: $c" || warn cluster "គ្មាន kind cluster — kind create cluster --name minishop --config k8s/kind-config.yaml"
fi

echo
echo "$ok ok · $bad missing"
[ "$bad" -eq 0 ]
