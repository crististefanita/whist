# Calificarea publicării și a predării rezultatelor

## Pe scurt

Consumatorul deține înregistrările jocului, selecția, oracolul și indexarea externă. Defectele generice de API sau randare urmează [rutarea către responsabilul bibliotecilor](README.md).

## RPT-OBSERVATION-CONTRACT — validare după capabilitățile declarate

**P2 · integrarea adaptorului.** Profilurile explicite acoperă deja rapoarte neutre fără HTTP. Asocierea unei capturi OBSERVE cu acțiunea și ownerul declarați urmează contractul comun; feedback-ul local păstrează calificarea adaptorului și a datelor consumate.

- [ ] După livrarea capabilității comune, calificați adaptorul real și raportul produs cu biblioteca efectiv consumată. Verificați proprietarul capturii, inventarul expected/actual și refuzul la eliminarea capturii declarate.


<a id="bounded-component-inputs"></a>

## Costul intrărilor consumatorului

[Ghidul componentelor Gradle](../testing/ATF_GradleComponents.md#bounded-matrix-inputs) definește agregarea intrărilor matricilor, populațiile independente și contextul primei abateri. Optimizările ulterioare păstrează aceleași oracole și măsoară separat corpurile cazurilor, resursele comune și imaginile observațiilor E2E.

| Prioritate | Responsabil și calificare rămasă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Costul consumatorului raportării în afara [profilului delimitat al selectorului](../reporting/ATF_Presentation.md#selecție-și-compoziție-cu-resurse-delimitate), în special populații mari cu eșecuri randate și corpuri obligatorii lungi. | Variați independent comparațiile randate, mărimea corpurilor și densitatea eșecurilor; măsurați alocarea reală și etapele compoziției, păstrând fiecare eșec selectat, valoare obligatorie și omisiune. Componentele cu o singură copie per înregistrare și o compoziție HTML reală mică nu califică acest domeniu mai larg al randării. |

## RPT-BOUNDED-REPORT-HEAP — finalizare sigură pentru suite mari

**P1 · responsabil biblioteci: publicarea și verificarea HTML; consumatorul: dimensiunea intrărilor.** O suită poate avea teste JUnit verzi și totuși să nu producă raportul canonic dacă injecția navigării ori verificarea DOM epuizează heap-ul. Eroarea de publicare nu trebuie să ascundă rezultatul testelor sau să lase un raport parțial prezentat ca acceptat.

- [ ] Expuneți în bibliotecă măsurători independente ale vârfurilor pentru randare și navigație. Chitanțele consumatorului observă acum granița lor comună; vârfurile cumulative din pool-uri nu substituie vârfurile izolate pe etapă.
- [ ] Reduceți copiile integrale repetate din randarea generică și verificarea DOM. Sigiliile incrementale ale consumatorului evită o copie locală suplimentară, dar nu stabilesc alocările bibliotecii.
- [ ] Extindeți profilul de resurse separat pentru volumul comparațiilor, corpurilor și capturilor. Profilul sintetic limitat verifică valorile complete și octeții PNG; nu califică orice raport aflat sub limita de dimensiune a artefactului.

Componenta consumatorului leagă profilurile limitate de receipt-ul bibliotecii de raportare fixate și verifică etapele denumite de randare/finalizare, verificare și promovare, duratele lor și limitele cumulative ale pool-urilor. Randarea/navigarea sunt raportate intenționat ca o singură etapă opacă a bibliotecii; acest contract al consumatorului nu satisface criteriile pentru vârfuri independente sau reducerea copiilor.

[Publicarea verificată a componentelor](../reporting/ATF_Audit.md#publicarea-verificată-a-componentelor) deține pregătirea privată, inspecția înainte de promovare, înlocuirea atomică și diagnosticele cu cauza originală. Păstrați aceste granițe de regresie active; criteriile rămase țin de resursele native și alocările bibliotecii.

- Protocolul de eșec al consumatorului folosește JVM-uri izolate și limitate pentru a verifica epuizarea reală a heap-ului și terminarea abruptă în timpul randării sau verificării. Jurnalul persistent păstrează ultima etapă începută; HTML-ul canonic și JUnit original rămân neschimbate.
- O nouă încercare trebuie calificată înainte de promovare. Operațiile controlate verifică această graniță de proces; nu măsoară alocările interne ale rendererului generic și nu dovedesc recuperarea după orice eșec al sistemului de operare.

Calificarea este comună producătorului și verificatorului: mărirea heap-ului unui singur proces nu închide criteriul și nu demonstrează că raportul rămâne publicabil în mediul standard.

## RPT-EXTENT-REPEATED-PAYLOAD — compactare fără pierderea auditului

**P2 · compactarea HTML-ului de componentă fără reducerea dovezilor.** Payload-urile identice pot crește dimensiunea și timpul de navigare al raportului. Profilarea exactă aparține dosarului extern al execuției; criteriul urmărește mecanismul de compactare și păstrarea auditabilității. Capabilitățile reutilizabile urmează [rutarea comună](README.md#responsabilitatea-schimbării).

- [ ] Profilul compact trebuie să păstreze fiecare owner, comparație, verdict și corp integral accesibil offline în același HTML. Nu eliminați valori și nu mutați conținutul în ZIP, JSON ori fișiere externe.
- [ ] Evaluați referințe interne content-addressed pentru corpuri byte-identice repetate. Fiecare invocare trebuie să afișeze conținutul integral corect; conținutul poate fi stocat o singură dată în HTML autonom.
- [ ] Păstrați capturile PNG inline Base64 și navigarea către checkpoint-ul original; optimizarea textului nu trebuie să altereze identitatea capturii sau asocierea ownerului.
- [ ] Expuneți un sumar de dimensiune pe categorie și o opțiune de prezentare configurabilă. Preview-ul se poate scurta, dar corpul integral și criteriul PASS/FAIL rămân verificabile.
- [ ] Calificați HTML-ul singur într-un director gol, cu payload-uri repetate, lungi, diferite și Base64. Confruntați byte-for-byte fiecare valoare vizibilă și verificați hash-ul raportului final.

Măsurarea justifică investigarea reducerii redundanței; nu dovedește că toate corpuri similare au același sens. Referințele se pot unifica numai pentru payload-uri identice, iar verdictul nu poate depinde de modul compact.

## Contractele responsabile

- [Predarea rezultatelor](../reporting/ATF_Handoff.md) deține proveniența republicării, idempotența înregistrării și pointerele distincte pentru încercare și acceptanță.
- [Auditul publicării](../reporting/ATF_Audit.md#calificarea-unui-producător) deține calificarea fiecărui producător, comparațiile originale, ultima valoare, capturile și controalele negative.
- [Contractul Android](../reporting/ATF_Android.md) deține asocierea acțiune–observație–comparație și distincția dintre rezultatul funcțional și publicare.

[Contractul Android](../reporting/ATF_Android.md) · [Auditul publicării](../reporting/ATF_Audit.md) · [Retenție](../learning/ATF_Retention.md)
