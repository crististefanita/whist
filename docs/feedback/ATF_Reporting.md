# Calificarea publicării și a predării rezultatelor

## Pe scurt

Consumatorul deține înregistrările jocului, selecția, oracolul și indexarea externă. Defectele generice de API sau randare urmează [rutarea către responsabilul bibliotecilor](README.md).

## RPT-OBSERVATION-CONTRACT — validare după capabilitățile declarate

**P2 · integrarea adaptorului.** Profilurile explicite acoperă deja rapoarte neutre fără HTTP. Asocierea unei capturi OBSERVE cu acțiunea și ownerul declarați urmează contractul comun; feedback-ul local păstrează calificarea adaptorului și a datelor consumate.

- [ ] După livrarea capabilității comune, calificați adaptorul real și raportul produs cu biblioteca efectiv consumată. Verificați proprietarul capturii, inventarul expected/actual și refuzul la eliminarea capturii declarate.


## Validarea timpurie a checkpoint-ului

| Prioritate | Lacună și sursă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | [AndroidUiFlowTrace.checkpoint](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java) păstrează aserțiuni pe care [publicatorul](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) le confruntă ulterior cu politica bibliotecii; sumarul prea lung poate fi refuzat abia la publicare. | Componentă: validați prin aceeași politică efectivă înaintea următoarei capturi, cu refuz localizat și fără trunchierea valorilor integrale din observații și XML. |

Consumatorul deține momentul validării și datele transmise. Dacă API-ul public nu expune politica necesară, contractul reutilizabil se solicită responsabilului bibliotecii; limita nu se recopiază ca o regulă locală independentă.

<a id="bounded-component-inputs"></a>

## Costul intrărilor consumatorului

[Ghidul componentelor Gradle](../testing/ATF_GradleComponents.md#bounded-matrix-inputs) definește agregarea intrărilor matricilor, populațiile independente și contextul primei abateri. Optimizările ulterioare păstrează aceleași oracole și măsoară separat corpurile cazurilor, resursele comune și imaginile observațiilor E2E.

| Prioritate | Responsabil și calificare rămasă | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Consumatorul raportării: costul selecției și compoziției la populații mari ori predominant cu eșec. | Componente cu populații controlate și mediu identificat: cost, număr de comparații și corp opțional, cu toate eșecurile, valorile obligatorii și inventarul omisiunilor păstrate. |

## RPT-BOUNDED-REPORT-HEAP — finalizare sigură pentru suite mari

**P1 · responsabil biblioteci: publicarea și verificarea HTML; consumatorul: dimensiunea intrărilor.** O suită poate avea teste JUnit verzi și totuși să nu producă raportul canonic dacă injecția navigării ori verificarea DOM epuizează heap-ul. Eroarea de publicare nu trebuie să ascundă rezultatul testelor sau să lase un raport parțial prezentat ca acceptat.

- [ ] Măsurați separat memoria de vârf pentru randarea inițială, injectarea navigării și verificarea raportului, în funcție de numărul comparațiilor, corpurilor și capturilor inline.
- [ ] Evitați copiile integrale repetate ale documentului în etapele de publicare și verificare; fiecare etapă trebuie să poată procesa un raport mare valid sub limita de heap declarată.
- [ ] Calificați limitele cu rapoarte sintetice mici, medii și mari, inclusiv payload-uri repetate și imagini inline. Verificați identitățile JUnit–HTML, toate capturile și octeții obligatorii după publicare.
- [ ] La depășirea unei limite explicite, emiteți un diagnostic localizat care identifică etapa și păstrează rezultatul JUnit și dovezile disponibile; nu lăsați o excepție de heap să se transforme într-o lipsă ambiguă de fișier.

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
