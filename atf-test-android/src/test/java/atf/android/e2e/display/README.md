# Indexul verificărilor mesei

<!-- table-audit-contract: TABLE-PHASE-CONTINUITY -->
<!-- table-audit-levels: component,e2e -->
<!-- table-audit-steps: GIVEN,WHEN,THEN -->

## Pe scurt

Contract comun pentru componente și E2E. Protocolul platformei aparține adaptorului; matricea și aserțiunile aparțin sursei comune.

## Contract

[Matricea și responsabilitățile](../../../../../../../../docs/testing/ATF_TableContinuity.md)

## Componente

Componentele verifică toate combinațiile legale, actorii și câștigătorii, fără poze. Capturați precondițiile și comparațiile originale în Extent.

## E2E

Selectați puține parcursuri reprezentative, cu Given/When/Then, comparații înainte/după și poze inline. Nu repetați matricea de rezoluții în E2E.

[Planul pereche APK/Web](../../../../../../../../config/table-sanity-plan.json) conduce două mâini de o carte: manuală, apoi AutoGame, apoi revenire la licitația manuală. Patru checkpoint-uri planificate au imagini; ceilalți pași păstrează observații și aserțiuni. Jurnalul și publicatorul refuză rolurile lipsă, capturile suplimentare și dovezile reordonate sau atribuite greșit.

## Artefacte

[Catalogul și starea APK](../../../../../../../../release/README.md)

[Dosarul Web și componentele comune](../../../../../../../../release/WEB/README.md)

## Limite

Rezultatele de politică nu califică widgeturile ori pixelii APK-ului instalat. Dovezile native neexecutate nu se moștenesc de la browser.
