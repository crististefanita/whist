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

**P1 · 88/100 · Deschis; constatare din cod, paritatea randată nu este încă calificată.**

J4 partajează deja calculele portabile ale locurilor și mâinii. Păstrați această graniță; lacuna aparține învelișului browserului, nu justifică duplicarea geometriei mesei sau importarea interfeței specifice Chess în Whist.

- Pe telefoane, `viewportGeometry` folosește viewport-ul CSS primit. Pe desktop pornește de la referința 414 × 896 și aplică o singură scară întregului cadru. Astfel, ecranele late pot rămâne insuficient utilizate, iar într-o fereastră scundă meniul, etichetele, masa și controalele se pot micșora împreună. Vezi [politica viewport-ului](../../atf-ui-browser/src/main/web/viewport.mjs) și [dimensionarea cadrului](../../atf-ui-browser/src/main/web/table.mjs).
- Suprafața Whist fixează `--whist-text-scale: 1.5`, iar `table.mjs` citește separat fontul rădăcină pentru `/api/layout`. Browserul nu are, așadar, un contract explicit unic între rolurile meniurilor, zoom-ul textului și măsurătorile geometriei. Factorul 1.5 poate corespunde unui profil APK ales, dar nu înlocuiește testarea textului normal și mărit în browser. Vezi [tipografia meniurilor și geometria](../../atf-ui-browser/src/main/web/table.css).

**Direcția necesară**

1. Înlocuiți zoom-ul întregului cadru desktop cu dimensionarea condusă de conținut, bazată pe viewport-ul util și zonele sigure. Folosiți lățimea disponibilă pentru suprafața de joc până la un maxim lizibil; păstrați ancorele locurilor și proporțiile mesei stabile când se schimbă lățimea și înălțimea. Nu reparați depășirile micșorând fiecare element sau adăugând ajustări izolate la praguri de rezoluție.
2. Definiți roluri tipografice explicite pentru titlul meniului, subtitluri, acțiuni primare/secundare și text ajutător. Mapați rolurile la `sp` Android și `rem` Web; zoom-ul textului în browser trebuie să afecteze textul fără scalarea mesei sau a țintelor tactile. Verificați formulările reale RO și EN din meniuri, nu numai etichetele englezești.
3. Păstrați ierarhia informației între adaptoare: numele, licitațiile, levatele câștigate, jucătorul activ, atuul, cărțile jucate, mâna umană și scorul trebuie să rămână ușor de parcurs, cu aceeași proeminență relativă. Controalele rămân accesibile, fără decupare sau derulare inutilă a paginii pe profilurile acceptate.

**Dovezi de închidere**

- Componentele acoperă dimensionarea viewport-ului, rolurile tipografice, ferestrele scunde/înalte, etichetele localizate lungi și textul 1.0×/1.3×, fără emulator.
- Același checkpoint de partidă salvată și meniurile principal/configurare sunt randate pe același Android în APK și Chrome la un profil compact; un profil browser desktop verifică utilizarea lățimii și depășirile. Includeți RO/EN și text 1.0×/1.3× în comparația focalizată, fără o partidă completă la fiecare rezoluție.
- Capturile Extent includ dimensiunile viewport-ului, identitatea browserului și APK-ului, fonturile, limitele elementelor principale, verdictele de depășire/derulare și faptele vizibile ale jocului. Păstrați problema deschisă până trece comparația randată; geometria de componentă singură nu este paritate vizuală.

Folosiți ca referință responsivă tipografia fluidă `rem`, `clamp()` și limita tablei derivată din viewport din Chess Web. Păstrați motorul, semantica mesei, culorile și responsabilitatea adaptoarelor Whist. Criteriul completează WEB-07 și WEB-11; verificările fluxurilor și glifelor cărților nu închid tipografia responsivă a meniurilor sau dimensionarea pe desktop.
