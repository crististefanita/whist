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

## Suprafața de joc responsivă și tipografia meniurilor

| ID | Lacună rămasă | Criteriu de închidere |
|---|---|---|
| WEB-16 | Browserul partajează geometria portabilă a locurilor și a mâinii, dar viewport-ul exterior pornește încă de la un cadru fix de 414 × 896 și scalează uniform întregul cadru pe desktop. Tipografia meniurilor suprapune un `--whist-text-scale: 1.5` fix peste fontul rădăcină, iar JavaScript determină separat scara textului pentru geometria mesei. Separarea lasă implicit contractul dintre meniuri/font și geometrie: ferestrele desktop scunde pot micșora împreună meniul și jocul, iar textul mărit ocupă spațiu fără o decizie corespunzătoare de așezare responsivă. | Adoptați abordarea viewport-ului condusă de conținut din Chess Web fără importarea interfeței specifice șahului: măsurați viewport-ul util și zonele sigure, lăsați masa să folosească lățimea disponibilă până la un plafon lizibil și păstrați ancorele locurilor stabile. Definiți roluri tipografice comune pentru titlul meniului, subtitlu, acțiuni primare/secundare și text ajutător; mapați-le la sp Android și rem Web astfel încât zoom-ul textului în browser să modifice textul independent de dimensiunea mesei. Comparați aceeași partidă salvată și meniurile principal/configurare pe telefon compact, telefon de referință și desktop, în RO/EN cu text normal și 1.3×. Confirmați aceeași ierarhie și proporții lizibile pentru nume, licitații, levate câștigate, atu, cărți jucate, mână, actor și scor. Înregistrați fonturile meniurilor, limitele mesei, decuparea/depășirile și derularea lângă capturile APK/Web în Extent. Folosiți componente pentru politicile de dimensionare și tipografie; păstrați numai verificări randate reprezentative pe clasă de viewport. Nu pretindeți identitate în pixeli între fonturile native și cele din browser. |

Baza din cod este `viewport.mjs` (`REFERENCE` 414 × 896 și `scale` desktop), `table.mjs` (transformarea întregului cadru) și `table.css` (`--whist-text-scale` și regulile meniurilor). Chess Web oferă referința responsivă prin tipografia fluidă `rem`, spațierea `clamp()` și limita tablei derivată din `100dvh`; păstrați motorul, semantica mesei, culorile și responsabilitatea adaptoarelor Whist.
