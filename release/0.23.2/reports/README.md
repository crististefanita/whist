# Calificarea Whist Lab 0.23.2

## Pe scurt

Admiterea pregătirii Maven este separată de recuperarea snapshotului. APK-ul păstrează codul jocului și al afișării; metadatele versiunii sunt actualizate.

## Dovezile componentelor

- [atf-validation — 19 invocări](component/atf-validation/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței
- [atf-test-android — 277 invocări](component/atf-test-android/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței
- [atf-tests — 563 invocări](component/atf-tests/component-report.html): execuție nouă pe sursa fixată
- [whist-lab-app-bidding — 255 invocări](component/whist-lab-app-bidding/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței
- [whist-lab-app-commands — 123 invocări](component/whist-lab-app-commands/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței
- [whist-lab-app-display — 107 invocări](component/whist-lab-app-display/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței
- [whist-lab-app-history — 61 invocări](component/whist-lab-app-history/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței
- [whist-lab-app-achievements — 28 invocări](component/whist-lab-app-achievements/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței
- [whist-lab-app-atf-card-table-ui — 56 invocări](component/whist-lab-app-atf-card-table-ui/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței
- [whist-lab-app-rules — 40 invocări](component/whist-lab-app-rules/component-report.html): reutilizat după verificarea responsabilității sursei și a chitanței

## Domeniu și limite

Diff-ul selectează grupul componentelor ATF. Nouă grupuri neschimbate de joc și infrastructură păstrează rapoartele și proveniența originale.

E2E nativ are starea NOT_RUN deoarece nu s-a modificat niciun contract al fluxului de joc sau al afișării. Android este platforma activă; iOS nativ rămâne în așteptare.

## Integritate

Manifestul versiunii leagă APK-ul, semnatarul, inventarul surselor, chitanțele componentelor și rapoartele Extent canonice. Dovezile publicării și ale descărcării încheiate se înregistrează separat.

## Costul procesului

[Timpii etapelor înregistrați](component/timing.json) disting pregătirea snapshotului, copierea dependențelor, compilarea, reutilizarea dovezilor, execuția suitei și verificarea finală a surselor.

Aceste etape se pot suprapune cu build-ul separat al APK-ului; nu se deduce un câștig total de viteză. Grupul ATF larg încă include fixture-uri de lansator izolate care pregătesc spații de lucru suplimentare.

[Catalogul release-urilor](../../README.md) · [Strategia comună de testare](../../../docs/testing/README.md)

## Verificările livrării curente

Aceste verificări din etapa publicării nu măresc totalurile componentelor primare.

- [Descărcări încheiate](distribution-verification.json) înregistrează octeții primiți ai APK-ului și checksum-ului
- [Guvernanța catalogului curent](current-governance/component-report.html) verifică metadatele publicate
- [Comparația surselor runtime](runtime-source-parity.json) identifică schimbările ulterioare numai în metadatele livrării
