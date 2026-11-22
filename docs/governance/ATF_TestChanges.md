# Guvernanța schimbărilor în teste

## Pe scurt

O modificare a testelor păstrează sensul contractului și motivul pentru care o intrare este admisă sau refuzată. Schimbarea implementării nu justifică ajustarea rezultatului așteptat doar pentru a obține succes.

## Criterii

| Suprafață | Contract |
|---|---|
| Așteptat | Derivat din cerință, invariant sau oracol independent. |
| Control negativ | Eșecul intenționat demonstrează că testul detectează defectul vizat. |
| Identitate | Cazul, sursele, configurația și aria sunt explicite. |
| Verdict | Cauza originală este păstrată prin tratarea excepției și publicarea raportului. |
| Populație | Selecția și omisiunile sunt declarate; ghidul generic nu fixează numărători istorice. |
| Rezultate | Rulările concrete și comparațiile de versiuni se păstrează în arhiva externă. |

## Revizia unei schimbări

1. Formulați proprietatea generală și frontierele ei.
2. Reproduceți defectul într-un caz mic cu date controlate.
3. Verificați eșecul controlului negativ și succesul cazului valid.
4. Reconciliați sursele concurente înaintea calificării.
5. Actualizați colecția, contractul și documentația mecanismului în aceeași revizie.

[Guvernanța documentației](ATF_Documentation.md) · [Colecții](../testing/ATF_Collections.md)

## Verificarea diff-ului Git

Rulați [lansatorul de guvernanță](../../scripts/run-atf-governance.ps1) din checkout-ul revizuit. Selectați commitul comun de dinaintea schimbării. Lansatorul verifică testele într-o copie izolată și citește diff-ul Git din checkout-ul declarat.

```powershell
.\scripts\run-atf-governance.ps1 -TestChangeBaseRef $reviewBase
```

| Contract | Verificare |
|---|---|
| Bază | Un commit local cel mult egal cu `HEAD`. În CI, declarați explicit o bază strămoș a lui `HEAD`. Fără parametru local, `HEAD` delimitează doar schimbările necomise. |
| Intrări Git | Modificările comise față de bază, indexul, fișierele de lucru și fișierele neadăugate. Sunt incluse și intrările protejate, ignorate de Git. |
| Arie | Seturile Maven și Gradle de teste și resurse, POM-urile, configurarea Gradle, selecțiile de suite și intrările executabile de guvernanță sau calificare. |
| Autorizare | Trebuie să existe o singură înregistrare eligibilă pentru calea exactă, SHA-ul bazei și data UTC a revizuirii. Data expirării este inclusivă. |
| Refuz | Bază absentă sau fără legătură, cale neacoperită, control expirat ori ambiguu, metadate invalide sau stare Git neacceptată. |
| Redenumire | O redenumire este evaluată ca ștergerea căii vechi și adăugarea celei noi. Ștergerea cere `allowDelete: true`. |

Lansatorul transmite baza prin `atf.governance.testChanges.baseRef`. Pentru selecția componentelor, setați `ATF_TEST_CHANGE_BASE_REF` înainte de lansare. Revizuirea unei ramuri folosește întotdeauna un commit anterior lui `HEAD`.

## Ciclul de viață al controlului

| Etapă | Loc și criteriu |
|---|---|
| ACTIV | Documentul de [feedback responsabil](../feedback/README.md) păstrează motivul, comanda probei și lista exactă de căi. |
| VERIFICAT | [Registrul tehnic](ATF_TestChangesBaseline.md) păstrează același ID și aceeași selecție. Autorizarea diff-ului curent cere dovadă verificată, bază și expirare. |
| SCHIȚĂ și ÎNCHIS | Intrarea structurată poate consemna propunerea sau închiderea. Aceste stări nu sunt eligibile. |
| Retragere | Comparați controlul cu baza revizuirii. Eliminați intrarea doar când diff-ul vizat nu mai are nevoie de ea. |

- Un bloc YAML poate declara o listă `testChanges`. Fiecare element declară `id`, `status`, `base`, `expires`, `reason`, `proof` și `paths`. `allowDelete` este opțional și are implicit valoarea `false`.
- Căile relative folosesc `/`. ID-urile trebuie să fie unice în toate documentele inspectate.

Comanda de probă declară verificarea planificată și se execută separat. [Testele de componentă](../../atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java) creează istorii Git locale și califică mecanismul fără dispozitiv.

## Dovada controlului verificat

### Admiterea timpurie a controlului sursei

Ambele lansatoare de calificare apelează [adaptorul de admitere](../../scripts/AtfTestChangeAdmission.ps1) înaintea copierii sau restaurării snapshot-ului sursei, copierii cache-urilor de dependențe ori pornirii Maven. Un compilator autonom restrâns construiește [CLI-ul](../../atf-tests/src/main/java/atf/tests/testchange/TestChangeAdmissionCli.java) din sursele curente ale uneltelor și citește JAR-urile fixate ale parserelor din `-MavenCache`. CLI-ul apelează aceiași validatori `TestChangeReview` și `TestChangeProof` ca guvernanța finală.

| Granița selectorului | Admitere și autoritate |
|---|---|
| Eligibilitatea sursei | [Validatorul](../../atf-tests/src/main/java/atf/tests/testchange/selectors/StaticProofSelectors.java) parsează rădăcinile Maven literale cu JDK, fără compilarea aplicației sau încărcarea claselor ei. Fiecare `Class#method` pozitiv/negativ identifică o singură metodă de instanță `void @Test` declarată în perimetrul componentelor. Adnotările locale compuse, perimetrul moștenit și containerele imbricate nestatice se rezolvă din surse; injectarea parametrilor este permisă, multiplicitatea invocărilor nu. |
| Refuz timpuriu | Fabricile/șabloanele dinamice, testele repetate sau parametrizate, supraîncărcările, tipurile duplicate, compunerea nerezolvată/ciclică a adnotărilor și importurile ambigue produc `TEST-CHANGE-SELECTOR-*`. Clasele din sursele de producție și metodele numai moștenite nu pot autoriza selectori. Verificarea normală și admiterea generării probelor reevaluează contractul după pregătire. |
| Autoritate finală | Eligibilitatea sursei este conservatoare și nu înlocuiește descoperirea JUnit a exact unei invocări pe selector sau rezultatele originale trecute. Execuția dezactivată, abandonată, lipsă sau eșuată nu poate produce o probă verificată. [Cazurile de componentă](../../atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeSelectorComponentTest.java) acoperă granița statică; [controalele lansatoarelor](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java) dovedesc refuzul înaintea copierii surselor/cache-urilor și înainte de Maven. |

| Mod | Contractul admiterii |
|---|---|
| Guvernanță sau componente normale | Verifică eligibilitatea controalelor și toate receipt-urile originale necesare, inclusiv ID-ul, baza Git, commitul, diff-ul brut, selectorii pozitivi/negativi și SHA-256 al HTML-ului. |
| Generarea probelor | Validează diff-ul comis și curat, controalele actuale și selectorii pozitivi/negativi declarați, fără să ceară receipt-uri încă negenerate. Numai opțiunile explicite pentru generarea controalelor selectează acest mod. |
| După snapshot sau reluare | Reverifică același contract canonic înaintea selectării sau pregătirii execuțiilor; sursele sau octeții probelor schimbați nu pot împrumuta admiterea anterioară. |
| Refuz | Publică diagnostice și o etapă de admitere eșuată, fără copierea sursei ori a cache-ului de dependențe și fără pornirea Maven. |

```mermaid
flowchart LR
    DIFF[Baza Git și diff] -->|inspectează| GATE[Admitere canonică]
    GATE -->|respinge| STOP[Receipt de admitere eșuată]
    GATE -->|admite| COPY[Snapshot sau reluare]
    COPY -->|reverifică| VERIFY[Admitere canonică]
    VERIFY -->|acceptă| EXEC[Reutilizează sau execută colecțiile afectate]
    VERIFY -->|respinge| STOP
```

<details>
<summary>Legendă: generarea probelor și autorizarea normală rămân distincte</summary>

Inițializarea scrie numai clase temporare ale utilitarului în rădăcina temporară externă configurată și le elimină după fiecare verificare. `test-change-admission.json`, `test-change-admission-recheck.json` și `timing.json` descriu admiterea; execuția testelor și rapoartele sale Extent canonice păstrează propriile receipt-uri. Admiterea planului de generare nu autorizează o rulare normală fără probele verificate necesare.

</details>

Java 21 și dependențele JSON/YAML fixate din cache-ul local sunt necesare inclusiv când compilarea Maven este reutilizată. Imuabilitatea sursei și guvernanța finală rulează și după admiterea timpurie; un receipt nu le dezactivează verificările.

Workflow-ul găzduit pregătește numai intrările JSON/YAML fixate ale parserului din Maven Central prin HTTPS, apoi admite planul generării înaintea pregătirii reactorului Maven. Eșecul pregătirii parserului sau al admiterii planului împiedică Maven; calificarea normală cere în continuare probele originale generate ulterior.

### Generarea și folosirea probei

Păstrați controlul și selecția de teste în commitul revizuit. Scrieți dovada într-un dosar nou, în afara checkout-ului. Lansatorul pregătește o copie izolată și publică dovada numai după ce confirmă că intrările nu s-au schimbat.

```powershell
.\scripts\run-component-suites.ps1 -AllTestChangeControls -TestChangeBaseRef $reviewBase -RunRoot $runRoot
$env:ATF_TEST_CHANGE_PROOF_ROOT = Join-Path $runRoot 'proofs'
.\scripts\run-atf-governance.ps1 -TestChangeBaseRef $reviewBase
```

| Element | Contract |
|---|---|
| Selectori | Declarați verificări `checks.positive` și `checks.negative` distincte, prin metode de componentă în forma `pachet.Clasă#metodă`. |
| Control negativ | Testul injectează defectul și verifică refuzul așteptat. Atât cazul valid, cât și cel de refuz trebuie să aibă verdict JUnit `PASS`. |
| Identitate | Chitanța leagă ID-ul controlului, SHA-ul bazei, SHA-ul commitului revizuit și SHA-256 al diff-ului Git brut, inclusiv bloburi și moduri de fișiere. |
| Lotul diff-ului | O singură compilare deservește toate controalele `VERIFIED` cerute. Fiecare control are JVM, jurnal și raport HTML proprii. Publicați `proofs` numai după trecerea tuturor controalelor și reconfirmarea sursei și a selecției. |
| Execuție | Lansatorul rulează exact selectorii declarați și reconciliază invocările cu Extent. Chitanța și HTML-ul se păstrează împreună, sub ID-ul controlului. |
| Verificare | Un alt commit, diff sau control, selectori lipsă ori hash HTML diferit blochează autorizarea `VERIFIED`. Un checkout schimbat cere un candidat nou. |
| Istoric | Păstrați controalele anterioare în registru. Un control `VERIFIED` se aplică numai dacă selecția și dovada corespund diff-ului revizuit. |

La eșec sau schimbarea sursei, consultați jurnalele fiecărui control și directorul de diagnostic `proofs.pending`. Pentru o probă punctuală, `-TestChangeControl` păstrează chitanța sub `<RunRoot>/<ID>`. Reutilizați chitanța și raportul numai pentru același commit și același diff.

## Mutarea proprietății unei verificări

Transferul unei implementări într-o bibliotecă păstrează toate contractele native la producător. Consumatorul păstrează acceptarea API-ului folosit, identitatea JAR-ului încărcat și verificările negative pentru metadate sau livrări inconsistente.

- Cererea de transfer identifică sursele afectate, noul proprietar, înlocuirea echivalentă și criteriile independente de acceptare.
- Leagă probele și capturile transferului de livrare. Registrul tehnic păstrează baza Git și căile afectate.
- Ștergerea unei copii locale nu elimină un caz negativ și nu transformă verificarea bibliotecii în acceptanță Android.
- Documentați limitele de proprietate fără liste istorice ale testelor șterse.

[Guvernanța calității](README.md) verifică perimetrul curent și interzice sursele paralele ale bibliotecilor binare.
