# Calificarea adaptorului Web

## Pe scurt

[Documentația adaptorului](../../atf-ui-browser/README.md) explică parcursul APK, JAR și Pages. Registrul urmărește comparația pe același dispozitiv Android.

[Planul comun în trei module](ATF_CardUi_Extraction.md#ui-three-module-plan) deține extragerea și matricea principală de afișaj. [Contractul sanity pereche](../testing/ATF_TableContinuity.md#rolurile-sanity-pereche) guvernează tranzițiile esențiale, rolurile ordonate și bugetele capturilor; acceptanța afișajului pe același dispozitiv rămâne separată.

## Calificarea pe același dispozitiv

Limita pentru înălțimi fracționare și sanity-ul public de două mâini trec. [Contractul de afișare](../testing/ATF_DisplayComponents.md) deține normalizarea întregilor și coordonatele viewport-ului vizibil.

Acceptanța în Chrome Android pe același dispozitiv rămâne deschisă; emularea mobilă pe desktop nu închide această limită.

| ID | Probă rămasă | Criteriu de închidere |
|---|---|---|
| WEB-07 | Cel mult două mâini românești 1–8–1 cu patru locuri, de la același checkpoint legal în APK și Chrome pe Android. | Compară mutările legale, actorul, ultima carte și colectarea, scorul și istoricul cu ajustări. Verifică setările, Realizările, Joacă din nou și reluarea după redeschidere în RO/EN, cu text normal și mărit. |
| WEB-11 | Mâna maximă în Chrome Android la 360 × 800 și 414 × 896, cu text mărit. | Confirmă accesul la toate cele 17 cărți, inclusiv la A, K, Q, J și `10`. Compară glifele în mână, pe masă și la colectare, inclusiv în browser fără `font-variant-emoji`. |

Folosiți același profil util și documentați densitatea, scara textului, limba, versiunea browserului și hash-ul APK-ului. Comparați geometria după zonele rezervate sistemului. Înregistrați separat rotunjirea marginilor, măsurarea fonturilor și zonele interactive. [Matricea de afișaj](../testing/ATF_Display.md)

Deciziile AI, seriile și Jocul la 0 se extind prin componente cu oracol de domeniu. Capturile și interacțiunile fizice rămân în selecția vizuală. [Strategia de testare](../testing/README.md)

- Atașați capturile native și Web inline în Extent, împreună cu distribuirea, comenzile și comparațiile.
- Selectați tranzițiile pentru proba pe dispozitiv pe baza verificărilor deja disponibile. [Componente și Playwright](../../atf-ui-browser/README.md#verificare)

## Adoptarea pachetului comun

| ID | Lucrare rămasă | Criteriu de închidere |
|---|---|---|
| WEB-15 | Distribuirea rămâne amânată până la acceptanța completă Whist Web. | Închideți lacunele afișajului Whist Web înaintea împachetării ori distribuirii către Rentz/Hold’em. Cereți activarea explicită a chenarelor măsurate și controale negative prin care împrumutarea temei păstrează geometria anterioară. Apoi calificați aceiași octeți de execuție/teste în consumatori fără modificarea artefactelor Android; păstrați imuabile pachetele admise. |

## WEB-16 Suprafața de joc responsivă și tipografia meniurilor

**P1 · 88/100 · Încadrarea proporțională calibrată după APK și contractele portabile sunt disponibile; acceptanța APK/browser pe același dispozitiv rămâne deschisă.**

Whist partajează politicile native ale locurilor, cărților și animațiilor. Păstrați această graniță și calificați independent randările fizice.

- [Politica viewport-ului](../../atf-ui-browser/src/main/web/viewport.mjs) derivă o singură încadrare proporțională din înălțimea, lățimea și conținutul logic necesar. [Adaptorul DOM](../../atf-ui-browser/src/main/web/surface-fit.mjs) încadrează meniurile și dialogurile, paginează conținutul lung și urmărește deplasările tastaturii. Pachetele publicate rămân imuabile.
- Rolurile tipografice explicite și [componentele native/Web](../../atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java) guvernează chenarele, cărțile, comenzile și banda rezervată etichetei acțiunii. Suprapunerile colectării și schimbările AutoGame nu trebuie să declanșeze callback-uri de încadrare.

**Direcția necesară**

1. Păstrați proporțiile APK-ului printr-o singură încadrare măsurată a scenei complete, inclusiv textul din chenare, cărțile, antetul, ajutorul, listele și tabelele de scor. Normalizați pixelii nativi prin densitate și zonele rezervate sistemului. Telefoanele folosesc dimensiunile CSS utile reale; desktopul mărește o referință calibrată după APK. Evitați măriri independente ale fonturilor, constante separate pe rezoluții sau decuparea conținutului pentru a ascunde depășirile.
2. Licitarea, jocul, meniurile și fiecare dialog fără istoric încap în înălțimea utilă fără derulare verticală. Regulile, grupele de realizări și clasamentele folosesc pagini limitate; istoricul scorului este singura excepție cu derulare verticală internă. Păstrați textul complet, câmpurile, navigarea și alegerile manuale.
3. Păstrați chenarele fixe între licitare, joc, colectare și mâini consecutive cu numere diferite de cărți. Verificați chenarele de sus la patru locuri și pe cele de jos la cinci, inclusiv la valori necunoscute și cu două cifre. Textul încadrat trebuie să păstreze înălțimea intrinsecă precum în APK; CSS-ul vechi pentru mai multe rânduri nu trebuie să ajungă la textul încadrat imbricat. Etichetele localizate stau deasupra chenarelor în benzi rezervate ambelor faze. Variantele la trei jucători nu trebuie să intersecteze etichete, licitații, chenare sau mâna umană.
4. Păstrați gestul orizontal nativ pentru mâinile dense. Verificați accesul la prima/ultima carte, fără a forța toate cele 17 cărți într-un viewport îngust prin reducerea indicilor.
5. Selectați politica nativă din înălțimea utilă a viewport-ului, apoi încadrați cerința intrinsecă a scenei. Separați rezervarea minimă a tablei de spațiul alocat și limitele vechi ale copiilor absoluți. Măsurați mâini consecutive, nu doar câteva levate din aceeași mână; păstrați urmele originale eșuate când se corectează un validator.

[Contractul comun al fazelor](../testing/ATF_TableContinuity.md) deține combinațiile legale pentru 3–6 jucători, fiecare actor și fiecare câștigător, prin adaptoare native/Web. Păstrați matricea fără poze în componente și puține parcursuri E2E ilustrate; acceptanța fizică nativă rămâne separată.

**Dovezi de închidere**

- [Componentele](../../atf-ui-browser/tests/responsive-surface.test.mjs) acoperă meniuri/dialoguri cu înălțime fixă, paginare, izolarea callback-urilor, etichete localizate și text 1.0×/1.3× printr-o graniță HTTP controlată. [Verificările cu motor real](../../atf-ui-browser/tests/responsive-game.mjs) păstrează aceeași mână la redimensionare și schimbarea limbii; niciuna dintre suite nu pretinde acceptanță nativă APK.
- Înregistrați intervalele de derulare și toate limitele vizibile ale paginii/panoului. Includeți profiluri compacte și desktop, etichete la trei jucători și mâini dense. Barele ascunse sau o singură imagine cu o carte nu stabilesc acest contract.
- Păstrați un singur raport E2E ilustrat primar cu puține parcursuri reprezentative la o rezoluție lizibilă și înregistrați comparațiile înainte/după fiecare depunere, inclusiv trecerile AI–om. Componentele dețin combinațiile de rezoluție, scară a textului și ancore fără poze. [Regresia măsurării intrinseci](../../atf-ui-browser/tests/seat-handoff.test.mjs) acoperă chenarele pregătite/montate și ancorele inferioare moștenite; păstrați acceptanța fizică deschisă și aserțiunile individuale pentru chenare, informații și fețe depuse în [dosarul Web](../../release/WEB/README.md).
- Comparați măsurătorile intrinseci ale chenarelor și rezervele native ale titlului/chenarului, nu doar dreptunghiurile obținute din înălțimi identice injectate. Verificați moștenirea rolului selecției de către opțiuni și păstrarea licitației, levatelor, diferenței mâinii și totalului cumulat în coloane cu tipografie nativă echilibrată. Încadrarea dialogurilor nu trebuie să aplice scara textului de două ori.
- Verificați contrastul etichetelor de paginare și contururile compacte în Realizări, clasamente și ajutor, în ambele limbi și stări active/inactive. Separați Înapoi/Înainte de Închide; verificați închiderea reală, nu doar etichetele. Păstrați antetele, textul atuului, rândurile și textul încadrat compatibile la trecerile AI/jucător, împreună cu nodurile mâinii, derularea și fețele depuse neschimbate. [Responsabilitatea mâinii](../../atf-ui-browser/tests/hand-lifecycle.test.mjs) și [verificările geometriei întârziate](../../atf-ui-browser/tests/layout-delivery.test.mjs) izolează callback-urile vechi, inclusiv terminarea fonturilor înaintea geometriei. Dreptunghiurile exterioare stabile nu dovedesc singure continuitatea desenării. Confirmați mișcarea vizuală raportată la trei până la șase locuri înaintea închiderii acceptanței fizice.
- Randați același checkpoint și meniurile principal/configurare în APK și Chrome pe același Android. Comparați un profil compact, RO/EN și text normal/mărit; adăugați un profil browser desktop fără repetarea partidelor complete la fiecare dimensiune.
- Extent include viewport-ul, densitatea, identitatea browserului/APK-ului, fonturile, limitele, verdictele derulării și faptele vizibile ale jocului, cu capturi inline. Păstrați acceptanța nativă deschisă până trece comparația fizică.
- Considerați incompletă o rulare parțială a instrumentării native când orice widget raportează eroare, chiar dacă numărul aserțiunilor eșuate este zero. Identificați artefactul APK/test înaintea atribuirii rezultatelor sursei curente.

Folosiți tipografia explicită și limitele derivate din viewport din Chess Web ca referințe de proiectare, păstrând motorul și responsabilitatea adaptoarelor Whist. WEB-07 și WEB-11 rămân criterii distincte de flux și glife.
