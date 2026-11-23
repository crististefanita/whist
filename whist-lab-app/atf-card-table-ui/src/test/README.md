# Testele de componentă ale adaptorului Android pentru masă

## Pe scurt

Proiectul Android deține testele de gazdă specifice rendererului și controalele granițelor surselor. Geometria portabilă, conținutul, rolurile textului și majoritatea combinațiilor de afișare aparțin [componentelor core](../../../card-table-core/src/test/README.md).

| Familie | Responsabilitatea adaptorului nativ |
|---|---|
| Realizări | Ordinea catalogului, identitățile de restaurare și evaluarea amânată a detaliilor. |
| Intrarea profilului | Intervalele Android de intrare, limitele Unicode și păstrarea textului neafectat. |
| Granițe | Inventarele native și core rămân independente de jocurile consumatoare; producția nu deține raportarea sau stocarea. |

## Execuție

- `:card-table-android:testDebugUnitTest` rulează selecția componentelor de gazdă ale adaptorului nativ.
- `-Pcardui.only=component` selectează Android, core și suportul de audit; `-Pcardui.only=core` admite verificările portabile fără Android SDK.
- Taskul `test` din core cere precondiția izolată `unitTest`. Diagnosticele unitare nu au Extent sau dependență de raportare.

## Acceptanță

Geometria reală a widgeturilor, fonturile, focusul și atingerea cer instrumentare nativă separată. O componentă de gazdă sau un plan de politică egal nu califică pixelii desenați. Păstrați identitățile cazurilor când mutați aserțiunile între responsabili.

[Strategia comună](../../../../docs/testing/README.md) · [Matrice modul × nivel](../../../../docs/testing/ATF_TestingGuide.md) · [Instrumentare nativă](../androidTest/README.md)
