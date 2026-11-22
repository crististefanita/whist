# Calificarea adaptorului Web

## Pe scurt

[Documentația adaptorului](../../atf-ui-browser/README.md) explică parcursul APK, JAR și Pages. Registrul urmărește comparația pe același dispozitiv Android.

## Calificarea pe același dispozitiv

| ID | Probă rămasă | Criteriu de închidere |
|---|---|---|
| WEB-07 | O partidă românească 1–8–1 cu patru locuri, de la același checkpoint în APK și Chrome pe Android. | Compară mutările legale, actorul, ultima carte și colectarea, scorul și istoricul cu ajustări. Verifică setările, Realizările, Joacă din nou și reluarea după redeschidere în RO/EN, cu text normal și mărit. |
| WEB-11 | Mâna maximă în Chrome Android la 360 × 800 și 414 × 896, cu text mărit. | Confirmă accesul la toate cele 17 cărți, inclusiv la A, K, Q, J și `10`. Compară glifele în mână, pe masă și la colectare, inclusiv în browser fără `font-variant-emoji`. |

Folosiți același profil util și documentați densitatea, scara textului, limba, versiunea browserului și hash-ul APK-ului. Comparați geometria după zonele rezervate sistemului. Înregistrați separat rotunjirea marginilor, măsurarea fonturilor și zonele interactive. [Matricea de afișaj](../testing/ATF_Display.md)

Deciziile AI, seriile și Jocul la 0 se extind prin componente cu oracol de domeniu. Capturile și interacțiunile fizice rămân în selecția vizuală. [Strategia de testare](../testing/README.md)

- Atașați capturile native și Web inline în Extent, împreună cu distribuirea, comenzile și comparațiile.
- Selectați tranzițiile pentru proba pe dispozitiv pe baza verificărilor deja disponibile. [Componente și Playwright](../../atf-ui-browser/README.md#verificare)

## Adoptarea pachetului comun

| ID | Lucrare rămasă | Criteriu de închidere |
|---|---|---|
| WEB-15 | Finalizarea adoptării contractelor adaptive de prezentare în consumatorii rămași. | Reîmpachetați execuția și testele comune după calificarea sursei; verificați aceiași octeți în consumatori fără modificarea artefactului Android. Pachetul calificat curent rămâne imuabil. |

## WEB-16 Suprafața de joc responsivă și tipografia meniurilor

**P1 · 88/100 · Implementarea încadrării și contractele portabile sunt disponibile; acceptanța APK/browser pe același dispozitiv rămâne deschisă.**

Whist partajează politicile native ale locurilor, cărților și animațiilor. Păstrați această graniță și calificați independent randările fizice.

- [Politica viewport-ului](../../atf-ui-browser/src/main/web/viewport.mjs) derivă o singură încadrare proporțională din înălțimea, lățimea și conținutul logic necesar. [Adaptorul DOM](../../atf-ui-browser/src/main/web/surface-fit.mjs) încadrează meniurile și dialogurile, paginează conținutul lung și urmărește deplasările tastaturii. Pachetele publicate rămân imuabile.
- Rolurile tipografice explicite și [componentele native/Web](../../atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java) guvernează chenarele, cărțile, comenzile și banda rezervată etichetei acțiunii. Suprapunerile colectării și schimbările AutoGame nu trebuie să declanșeze callback-uri de încadrare.

**Direcția necesară**

1. Păstrați o singură încadrare măsurată după înălțime prin adaptor. Folosiți geometria logică și rolurile tipografice înaintea încadrării proporționale; evitați constantele independente pe rezoluții sau decuparea conținutului pentru a ascunde depășirile.
2. Licitarea, jocul, meniurile și fiecare dialog fără istoric încap în înălțimea utilă fără derulare verticală. Regulile, grupele de realizări și clasamentele folosesc pagini limitate; istoricul scorului este singura excepție cu derulare verticală internă. Păstrați textul complet, câmpurile, navigarea și alegerile manuale.
3. Păstrați chenarele fixe între licitare, joc și colectare. Etichetele localizate ale turei stau deasupra chenarelor în benzi rezervate ambelor faze. Oh Hell, Reverse Oh Hell și variantele descrescătoare la trei jucători nu trebuie să intersecteze etichete, butoane de licitație, alte chenare sau mâna umană.
4. Păstrați gestul orizontal nativ pentru mâinile dense. Verificați accesul la prima/ultima carte, fără a forța toate cele 17 cărți într-un viewport îngust prin reducerea indicilor.

**Dovezi de închidere**

- [Componentele](../../atf-ui-browser/tests/responsive-surface.test.mjs) acoperă meniuri/dialoguri cu înălțime fixă, paginare, izolarea callback-urilor, etichete localizate și text 1.0×/1.3× printr-o graniță HTTP controlată. [Verificările cu motor real](../../atf-ui-browser/tests/responsive-game.mjs) păstrează aceeași mână la redimensionare și schimbarea limbii; niciuna dintre suite nu pretinde acceptanță nativă APK.
- Înregistrați intervalele de derulare și toate limitele vizibile ale paginii/panoului. Includeți profiluri compacte și desktop, etichete la trei jucători și mâini dense. Barele ascunse sau o singură imagine cu o carte nu stabilesc acest contract.
- Randați același checkpoint și meniurile principal/configurare în APK și Chrome pe același Android. Comparați un profil compact, RO/EN și text normal/mărit; adăugați un profil browser desktop fără repetarea partidelor complete la fiecare dimensiune.
- Extent include viewport-ul, densitatea, identitatea browserului/APK-ului, fonturile, limitele, verdictele derulării și faptele vizibile ale jocului, cu capturi inline. Păstrați acceptanța nativă deschisă până trece comparația fizică.

Folosiți tipografia explicită și limitele derivate din viewport din Chess Web ca referințe de proiectare, păstrând motorul și responsabilitatea adaptoarelor Whist. WEB-07 și WEB-11 rămân criterii distincte de flux și glife.
