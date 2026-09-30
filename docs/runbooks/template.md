# Runbook: <ប្រព័ន្ធ> — <រោគសញ្ញា>

<!-- ចម្លងទៅ <area>-<symptom>.md (ឧ. k8s-pod-not-ready.md)។ រាល់ § ត្រូវមាន error line **ពិត** + ថ្ងៃជួប។ កុំសរសេរ symptom ដែលមិនដែលជួប។ -->

> **ប្រើពេល:** <alert / អ្វីដែលមនុស្សឃើញ> · **អ្នកអាន:** on-call · **Update:** YYYY-MM-DD

## ០. ៣០ វិនាទីដំបូង

1. Command ដំបូងដែលបង្ហាញស្ថានភាព
2. អានអ្វីក្នុង output → ទៅ § ណា
3. ច្បាប់: អាន output ពិតតាមលំដាប់ — **កុំទាយ**

## ១. <Symptom> (YYYY-MM-DD)

```
<error line ពិត ចម្លងពី log>
```

- **មូលហេតុ:** …
- **ធ្វើ:** ជំហាន + command
- **ផ្ទៀងផ្ទាត់:** command + លទ្ធផលរំពឹង
- **កុំ:** workaround ដែលលាក់បញ្ហា

## ២. …

## Command reproduce

```bash
…
```

## ក្រោយដោះស្រាយ

- Symptom ថ្មី → បន្ថែម § ជាមួយ error ពិត + ថ្ងៃ
- ជួសជុលមូលហេតុ (code/config) → PR; runbook សម្រាប់ពេលវាកើតម្តងទៀត
