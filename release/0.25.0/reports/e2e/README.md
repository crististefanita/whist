# Auditul fluxurilor native

## Pe scurt

Șapte scenarii focalizate pe APK-ul semnat au trecut pe Android 9 / API 28 la 1080 × 1920, 440 dpi. Fiecare raport conține acțiuni ordonate, valori observate, aserțiuni individuale și capturi PNG inline. Nu sunt păstrate imagini separate sau XML JUnit duplicat.

| Flux | Invocări | Dovezi | Identitate |
|---|---:|---|---|
| history-release-sanity | 1 | [Extent](history-release-sanity/report.html) | [Receipt](history-release-sanity/execution.json) |
| responsive-ui-sanity | 1 | [Extent](responsive-ui-sanity/report.html) | [Receipt](responsive-ui-sanity/execution.json) |
| signed-release-achievements | 1 | [Extent](signed-release-achievements/report.html) | [Receipt](signed-release-achievements/execution.json) |
| signed-release-auto-game | 1 | [Extent](signed-release-auto-game/report.html) | [Receipt](signed-release-auto-game/execution.json) |
| signed-release-bidding | 1 | [Extent](signed-release-bidding/report.html) | [Receipt](signed-release-bidding/execution.json) |
| signed-release-home-profile | 1 | [Extent](signed-release-home-profile/report.html) | [Receipt](signed-release-home-profile/execution.json) |
| signed-release-smoke | 1 | [Extent](signed-release-smoke/report.html) | [Receipt](signed-release-smoke/execution.json) |

## Granițele execuției

Fiecare flux a folosit un utilizator Android temporar. Receipt-urile de pregătire și curățare păstrează rezervarea și starea utilizatorului/pachetului original; rezervarea emulatorului este eliberată. Sursa execuției native originale este păstrată chiar dacă revizia documentară finală este mai nouă. [Reconcilierea build-ului cu octeți identici](../diagnostics/build-reconciliation.json) explică această graniță.

Prima captură smoke conține un banner temporar de configurare Android Messages; checkpoint-ul final al meniului principal este neacoperit. Paritatea Android/Web extinsă, matricile cu text mărit și calificarea dispozitivelor consumatorilor rămân în [feedback-ul activ](../../../../docs/feedback/README.md).

[Strategia comună de testare](../../../../docs/testing/README.md)
