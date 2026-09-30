# ឯកសារ mini-shop

> **គោលបំណង:** ចំណុចចាប់ផ្តើមនៃឯកសារទាំងអស់ · **អ្នកអាន:** អ្នកណាក៏បាន · **Update:** 2026-09-30

ឯកសារបែងចែកជា ២ ក្រុម:
- **Product docs** — ការពិតអំពី system: វាជាអ្វី, run យ៉ាងណា, ខូចធ្វើអ្វី។ ត្រូវតែត្រឹមត្រូវជានិច្ច។
- **Journey docs** (`journey/`) — ដំណើររៀន DevOps ជា Phase: ផែនការ, កំណត់ហេតុ, ការឆ្លុះបញ្ចាំង។ ជាប្រវត្តិ មិនមែន reference។

## អ្នកជានរណា → អានអ្វី

| ខ្ញុំចង់… | អាន |
|---|---|
| យល់ថា system នេះជាអ្វី ធ្វើការយ៉ាងណា | [architecture.md](architecture.md) |
| Run លើ laptop (Java ឬ Docker Compose) | [guides/local-development.md](guides/local-development.md) |
| Run លើ Kubernetes (kind) | [guides/run-on-kubernetes.md](guides/run-on-kubernetes.md) |
| Release version ថ្មី / rollback | [guides/release.md](guides/release.md) |
| ប្តូរ database schema | [guides/database-migration.md](guides/database-migration.md) |
| ដឹង env var / secret / config មួយនៅឯណា | [reference/configuration.md](reference/configuration.md) |
| ដឹង API endpoint និង status code | [reference/api.md](reference/api.md) |
| ដឹង CI job នីមួយៗធ្វើអ្វី | [reference/ci-pipeline.md](reference/ci-pipeline.md) |
| ដឹង version tool (source of truth) | [reference/tech-stack.md](reference/tech-stack.md) |
| ដឹងថាហេតុអ្វីរចនាបែបនេះ | [decisions/](decisions/) (ADR) |
| ជួសជុលអ្វីដែលខូច | [runbooks/](runbooks/) |
| តាមដានដំណើររៀន / ស្ថានភាព Phase | [journey/](journey/README.md) |

## រចនាសម្ព័ន្ធ

```
docs/
  README.md            ← ទីនេះ
  architecture.md      explanation: component, request flow, data model, ព្រំដែន domain
  guides/              how-to: ជំហាន copy-paste + លទ្ធផលរំពឹង
  reference/           ការពិតសុទ្ធ ជាតារាង (គ្មាន "ហេតុអ្វី" — ហេតុផលនៅ ADR)
  decisions/           ADR NNN-slug.md (template.md)
  runbooks/            symptom → មូលហេតុ → ជួសជុល (template.md)
  journey/phase-*/     plan.md · worklog.md · learning-log.md
```

## ច្បាប់សរសេរ

1. Header រាល់ file: **គោលបំណង · អ្នកអាន · Update** (ថ្ងៃ YYYY-MM-DD)។
2. Prose ជា**ខ្មែរ**; identifier (path, command, class, YAML key) ជា**អង់គ្លេស** (CLAUDE.md)។
3. Guide: command ដែល copy-paste run បាន + **លទ្ធផលរំពឹង** ក្រោម command នីមួយៗ។
4. Reference: តារាង, គ្មានការពន្យល់វែង — link ទៅ ADR សម្រាប់ "ហេតុអ្វី"។
5. **Source តែមួយ** សម្រាប់ការពិតនីមួយៗ: version → `reference/tech-stack.md`; ស្ថានភាព Phase → `journey/phase-*/plan.md` tracker; ទីកន្លែងផ្សេង **link** មិនចម្លង។
6. ប្តូរ code ដែលប៉ះ config/API/pipeline → update reference ក្នុង PR ដដែល។
