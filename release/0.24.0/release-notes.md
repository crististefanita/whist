# Whist 0.24.0

Whist 0.24.0 separă contractele portabile de prezentare de adaptoarele Android și Web și corectează parsarea viewport-ului mobil fracționar.

## Schimbări
- Păstrează geometria comună, rolurile textului și continuitatea fazelor în nucleul portabil de prezentare.
- Normalizează înălțimea CSS mobilă înaintea limitei geometrice întregi, păstrând erorile intrărilor invalide.
- Reutilizează colecțiile neschimbate și păstrează proveniența execuțiilor native originale.

## Validare
17 selecții de componentă admise acoperă 1.583 de invocări; șapte fluxuri sanity pe APK instalat au trecut pe Android API 28. Sanity-ul Web public a parcurs două mâini cu 16 pași, 142 de aserțiuni și patru capturi inline.

## Audit
[Auditul calificării Extent](https://github.com/crististefanita/whist/blob/305ae4b74a4dacc6a00863b5f40d0cc822ba9888/release/0.24.0/reports/README.md)
