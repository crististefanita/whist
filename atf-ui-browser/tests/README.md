# Indexul verificării mesei

<!-- table-audit-contract: TABLE-PHASE-CONTINUITY -->
<!-- table-audit-levels: component,e2e -->
<!-- table-audit-steps: GIVEN,WHEN,THEN -->

## Pe scurt

Contract comun pentru componente și E2E. Adaptorul deține protocolul platformei; sursa comună deține matricea și aserțiunile.

## Contract

[Matrice și proprietate](../../docs/testing/ATF_TableContinuity.md)

## Componente

Core deține observațiile neutre și oracolul de continuitate; Whist oferă programele legale și ambele adaptoare. Componentele verifică toate combinațiile legale, actorii și câștigătorii fără poze, capturând precondițiile și comparațiile originale în Extent. [Matricea executabilă modul × nivel](../../docs/testing/ATF_TestingGuide.md)

## E2E

Selectați un parcurs scurt explicit, cu Given/When/Then, comparații înainte/după și imagini inline. Profilul implicit `four-sanity` folosește un singur viewport 1664 × 916, o mână manuală și o mână AutoGame, cu cel mult patru capturi normale.

Runnerul respinge `all` sau mai mult de două mâini înainte să pornească browserul; publicatorul reconciliază independent rundele reale și referințele imaginilor. Matricele de rezoluție rămân în componente.

## Artefacte

[Catalogul APK și starea](../../release/README.md)

[Dosarul Web și componentele comune](../../release/WEB/README.md)

## Limite

Rezultatele politicilor nu califică widgeturile sau pixelii APK-ului instalat. Dovezile native neexecutate nu sunt moștenite din browser.
