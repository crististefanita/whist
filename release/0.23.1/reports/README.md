# Auditul calificării Whist 0.23.1

## Rezultat

Planul bazat pe diff-ul sursei a selectat un singur grup de componente și guvernanță și a refolosit nouă grupuri neschimbate din snapshot-uri legate de sursă. Grupul selectat a trecut cu 563/563. Selecția auditată completă cuprinde 1529 de invocări, fără eșecuri, erori sau teste omise.

## Identitatea APK-ului

| Câmp | Valoare |
|---|---|
| APK | `WhistLab-0.23.1.apk` |
| Application ID | `ro.whistlab.app` |
| Cod versiune | 48 |
| Commit sursă | `9cbd9241a8fe1ee29573ce7c79e3c437961d8ac9` |
| Dimensiune APK | 1033649 bytes |
| SHA-256 APK | `3db4700170713369a35f3fb225c720d0412195c73b9aaca94d5e9a8183fb5752` |
| CRC-32 APK | `6288ee71` |
| SHA-256 certificat de semnare Android | `b80c45a9b52ef0c83dcb929ba4aeb8775c836e5d587324d7ec725c079fb4c3` |
| SDK Android | min 24, țintă 36 |

## Suite de componente și guvernanță

| Grup | Decizie | Teste | Extent |
|---|---:|---:|---|
| atf-validation | REUSE | 19 | [Raport](component/atf-validation/component-report.html) · [Receipt](component/atf-validation/execution.json) |
| atf-test-android | REUSE | 277 | [Raport](component/atf-test-android/component-report.html) · [Receipt](component/atf-test-android/execution.json) |
| atf-tests | RUN | 563 | [Raport](component/atf-tests/component-report.html) · [Receipt](component/atf-tests/execution.json) |
| whist-lab-app-bidding | REUSE | 255 | [Raport](component/whist-lab-app-bidding/component-report.html) · [Receipt](component/whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | REUSE | 123 | [Raport](component/whist-lab-app-commands/component-report.html) · [Receipt](component/whist-lab-app-commands/execution.json) |
| whist-lab-app-display | REUSE | 107 | [Raport](component/whist-lab-app-display/component-report.html) · [Receipt](component/whist-lab-app-display/execution.json) |
| whist-lab-app-history | REUSE | 61 | [Raport](component/whist-lab-app-history/component-report.html) · [Receipt](component/whist-lab-app-history/execution.json) |
| whist-lab-app-achievements | REUSE | 28 | [Raport](component/whist-lab-app-achievements/component-report.html) · [Receipt](component/whist-lab-app-achievements/execution.json) |
| whist-lab-app-atf-card-table-ui | REUSE | 56 | [Raport](component/whist-lab-app-atf-card-table-ui/component-report.html) · [Receipt](component/whist-lab-app-atf-card-table-ui/execution.json) |
| whist-lab-app-rules | REUSE | 40 | [Raport](component/whist-lab-app-rules/component-report.html) · [Receipt](component/whist-lab-app-rules/execution.json) |

## Scopul E2E

**NOT_RUN**, conform selecției explicite pe baza diff-ului. Modificările fixate afectează RAG-ul și guvernanța, documentația și metadatele versiunii Android; nu modifică regulile jocului, navigarea, aspectul, introducerea datelor sau contractele vizuale. Diff-ul evaluat nu cere niciun grup E2E. Rezultatele E2E mai vechi nu sunt prezentate drept dovezi pentru acest APK.

## Proveniență și dovezi

- [Selecția componentelor, reutilizare și contract Extent](component/README.md)
- [Receipt agregat al execuției componentelor](component/source-component-run.json)
- [Inventarul sursei APK-ului](apk-inputs.json)
- [Planul de calificare fixat](qualification-plan.json)
- Build-ul, semnarea și identitatea sunt documentate în [diagnostics](diagnostics/).

[Catalogul release-urilor](../../README.md) · [Strategia comună de testare](../../../docs/testing/README.md)
