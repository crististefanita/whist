# Sanity ilustrat minimal

## Pe scurt

Un parcurs în browserul local, un singur viewport lizibil de 1664×916, două mâini de o carte. Prima mână este manuală; a doua folosește AutoGame. Selecția verifică legăturile și continuitatea mesei, nu fiecare rezoluție sau o partidă completă.

<!-- table-audit-contract: TABLE-PHASE-CONTINUITY -->

| Limită | Dovadă |
|---|---|
| Combinațiile comune pentru 3–6 locuri | [Oracol de componentă](../component/adapters/component-report.html), fără poze |
| Sanity desenat pentru patru locuri | [Extent](table-sanity/extent.html): 14 pași ordonați, 140 de aserțiuni și patru capturi inline |
| Execuție și selecție | [Dovada execuției](table-sanity/execution.json), server JVM local |
| APK instalat | Neexecutat; dovezile release-ului Android existent rămân separate |

Admiterea respinge planurile lungi înaintea pornirii browserului; publicarea reconciliază mâinile și capturile reale. [Indexul testelor Web](../../../atf-ui-browser/tests/README.md) și [indexul APK](../../../atf-test-android/src/test/java/atf/android/e2e/display/README.md) partajează rolurile de audit; adaptoarele semantice pereche și controlul capturilor la publicarea APK rămân feedback explicit.

[Înapoi la dosar](../README.md) · [Manifest](../index.json)
