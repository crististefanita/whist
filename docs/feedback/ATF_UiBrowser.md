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

**P1 · 88/100 · Implementarea sursei calificată offline; acceptanța APK/browser pe același dispozitiv rămâne deschisă.**

Whist partajează deja calculele portabile ale locurilor și mâinii. Păstrați această graniță; lacuna aparține învelișului browserului, nu justifică duplicarea geometriei mesei sau importarea interfeței specifice Chess în Whist.

- Whist adoptă acum dimensionarea după viewport-ul util, fără transformarea întregii scene, cu lățime maximă lizibilă centrată și derulare locală a meniurilor în ferestre scunde. Pachetul publicat imuabil și consumatorii existenți păstrează adaptorul cadrului de referință până la o livrare calificată separat. Vezi [politica viewport-ului](../../atf-ui-browser/src/main/web/viewport.mjs) și [dimensionarea cadrului](../../atf-ui-browser/src/main/web/table.mjs).
- Rolurile tipografice explicite ale meniurilor înlocuiesc multiplicatorul global implicit. Schimbarea textului rădăcină declanșează măsurarea geometriei prin același adaptor; masa primește dimensiunile sale utile reale. [Tipografia meniurilor și geometria](../../atf-ui-browser/src/main/web/table.css) păstrează ierarhia implicită aprobată fără scalarea întregii scene.

**Direcția necesară**

1. Păstrați dimensionarea condusă de conținut, zonele sigure și geometria comună a locurilor/cărților. Nu reintroduceți micșorarea întregii scene ori ajustări izolate la praguri de rezoluție pentru a ascunde depășirile.
2. Păstrați rolurile tipografice explicite și mărirea independentă a textului. Politicile native `sp` ale APK-ului rămân autoritatea comparației deschise; componentele browserului singure nu dovedesc echivalența fonturilor native.
3. Păstrați ierarhia informației între adaptoare: numele, licitațiile, levatele câștigate, jucătorul activ, atuul, cărțile jucate, mâna umană și scorul trebuie să rămână ușor de parcurs, cu aceeași proeminență relativă. Controalele rămân accesibile, fără decupare sau derulare inutilă a paginii pe profilurile acceptate.

**Dovezi de închidere**

- [Componentele](../../atf-ui-browser/tests/responsive-surface.test.mjs) acoperă dimensionarea viewport-ului, rolurile tipografice, ferestrele scunde/înalte, etichetele localizate lungi și textul 1.0×/1.3× fără emulator. Granița HTTP este un fixture controlat. [Verificările browserului cu motor real](../../atf-ui-browser/tests/responsive-game.mjs) păstrează aceeași mână Whist la redimensionare și schimbarea limbii; niciuna dintre suite nu pretinde acceptanță nativă APK.
- Același checkpoint de partidă salvată și meniurile principal/configurare sunt randate pe același Android în APK și Chrome la un profil compact; un profil browser desktop verifică utilizarea lățimii și depășirile. Includeți RO/EN și text 1.0×/1.3× în comparația focalizată, fără o partidă completă la fiecare rezoluție.
- Capturile Extent includ dimensiunile viewport-ului, identitatea browserului și APK-ului, fonturile, limitele elementelor principale, verdictele de depășire/derulare și faptele vizibile ale jocului. Păstrați problema deschisă până trece comparația randată; geometria de componentă singură nu este paritate vizuală.

Folosiți ca referință responsivă tipografia fluidă `rem`, `clamp()` și limita tablei derivată din viewport din Chess Web. Păstrați motorul, semantica mesei, culorile și responsabilitatea adaptoarelor Whist. Criteriul completează WEB-07 și WEB-11; verificările fluxurilor și glifelor cărților nu închid tipografia responsivă a meniurilor sau dimensionarea pe desktop.
