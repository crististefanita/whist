# Parcursuri ilustrate ale mesei

## Pe scurt

Puține parcursuri la un singur viewport lizibil: patru locuri refolosite și două parcursuri curente la cinci/șase locuri. Componentele comune dețin combinațiile de rezoluție, text, distribuire și actori.

## Contractul comun al auditului

<!-- table-audit-contract: TABLE-PHASE-CONTINUITY -->

| Locuri | Componente APK/Web | E2E Web | E2E APK instalat |
|---|---|---|---|
| 3 | [Contractul comun](../component/phases/component-report.html) | Neselectat în această iterație | Neexecutat |
| 4 | [Contractul comun](../component/phases/component-report.html) | [Parcurs refolosit](oh-hell/extent.html) | Neexecutat |
| 5 | [Contractul comun](../component/phases/component-report.html) | [Parcurs curent](table-handoffs/extent.html) · 10/9 cărți | Neexecutat |
| 6 | [Contractul comun](../component/phases/component-report.html) | [Parcurs curent](table-handoffs/extent.html) · 8/7 cărți | Neexecutat |

Fiecare depunere și colectare observată păstrează pașii Given/When/Then, datele originale înainte/după și aserțiunile individuale. Pozele inline aparțin momentelor capturate; datele geometrice nu sunt prezentate drept imagini reconstruite.

## Indexuri și limite

- [Index Web](../../../atf-ui-browser/tests/README.md) și [index APK](../../../atf-test-android/src/test/java/atf/android/e2e/display/README.md) urmează aceleași niveluri și roluri de audit
- [Integrarea JVM Android](../component/apk-policy/component-report.html) execută contractul aplicației fără emulator sau capturi native
- [Catalog Android](../../README.md) păstrează dovezile release-urilor; comparația fizică APK/Chrome rămâne deschisă
- [Înapoi la dosar](../README.md) · [Manifest și proveniență](../index.json)
