# Auditul suitelor de componentă

## Pe scurt

1673 de invocări primare de componentă au trecut în 17 responsabilități. Revizia documentară finală execută un singur responsabil afectat și păstrează dovezile originale ale celor șaisprezece responsabili neschimbați. Componentele conțin intrări și comparații capturate, fără poze ale produsului.

| Responsabil | Invocări | Execuție | Raport | Metadate |
|---|---:|---|---|---|
| atf-validation | 19 | Dovezi originale reutilizate | [Extent](atf-validation/component-report.html) | [Receipt](atf-validation/execution.json) |
| atf-test-android | 300 | Dovezi originale reutilizate | [Extent](atf-test-android/component-report.html) | [Receipt](atf-test-android/execution.json) |
| atf-tests | 108 | Dovezi originale reutilizate | [Extent](atf-tests/component-report.html) | [Receipt](atf-tests/execution.json) |
| whist-lab-app-bidding | 255 | Dovezi originale reutilizate | [Extent](whist-lab-app-bidding/component-report.html) | [Receipt](whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | 127 | Dovezi originale reutilizate | [Extent](whist-lab-app-commands/component-report.html) | [Receipt](whist-lab-app-commands/execution.json) |
| whist-lab-app-display | 107 | Dovezi originale reutilizate | [Extent](whist-lab-app-display/component-report.html) | [Receipt](whist-lab-app-display/execution.json) |
| whist-lab-app-history | 70 | Dovezi originale reutilizate | [Extent](whist-lab-app-history/component-report.html) | [Receipt](whist-lab-app-history/execution.json) |
| whist-lab-app-achievements | 28 | Dovezi originale reutilizate | [Extent](whist-lab-app-achievements/component-report.html) | [Receipt](whist-lab-app-achievements/execution.json) |
| whist-lab-app-atf-card-table-ui | 8 | Dovezi originale reutilizate | [Extent](whist-lab-app-atf-card-table-ui/component-report.html) | [Receipt](whist-lab-app-atf-card-table-ui/execution.json) |
| whist-lab-app-rules | 40 | Dovezi originale reutilizate | [Extent](whist-lab-app-rules/component-report.html) | [Receipt](whist-lab-app-rules/execution.json) |
| whist-lab-app-card-table-core | 50 | Dovezi originale reutilizate | [Extent](whist-lab-app-card-table-core/component-report.html) | [Receipt](whist-lab-app-card-table-core/execution.json) |
| atf-tests-documentation | 167 | Verificare documentară nouă | [Extent](atf-tests-documentation/component-report.html) | [Receipt](atf-tests-documentation/execution.json) |
| atf-tests-provenance | 124 | Dovezi originale reutilizate | [Extent](atf-tests-provenance/component-report.html) | [Receipt](atf-tests-provenance/execution.json) |
| atf-tests-android | 54 | Dovezi originale reutilizate | [Extent](atf-tests-android/component-report.html) | [Receipt](atf-tests-android/execution.json) |
| atf-tests-github | 33 | Dovezi originale reutilizate | [Extent](atf-tests-github/component-report.html) | [Receipt](atf-tests-github/execution.json) |
| atf-tests-orchestration | 108 | Dovezi originale reutilizate | [Extent](atf-tests-orchestration/component-report.html) | [Receipt](atf-tests-orchestration/execution.json) |
| atf-tests-reporting | 75 | Dovezi originale reutilizate | [Extent](atf-tests-reporting/component-report.html) | [Receipt](atf-tests-reporting/execution.json) |

## Proveniență și recuperare

[Receipt-ul sursei curente](source-provenance.json) · [Inventarul surselor](source-inputs.json) · [Inventarul catalogului original](release-inputs.json) · [Deciziile reutilizării](component-reuse.json) · [Rularea rădăcină](source-component-run.json)

Responsabilul Android reutilizează 53 de invocări și reia una; orchestrarea reutilizează 106 și reia două. Totalurile finale rămân 54 și 108. Încercările originale eșuate și rapoartele reluării rămân în [filiația Android](atf-tests-android/README.ro.md) și [filiația orchestrării](atf-tests-orchestration/README.ro.md); reluările nu se adaugă totalului primar.

Precondițiile unitare rulează separat fără Extent; aserțiunile matricilor rămân în nivelul unitar/de componentă responsabil. [Strategia comună de testare](../../../../docs/testing/README.md)
