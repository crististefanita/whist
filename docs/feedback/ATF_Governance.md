# Reutilizarea guvernanței și coeziunea codului

## Pe scurt

Proiectul deține perimetrul Android și adaptarea verificatoarelor. Contractul comun reutilizabil se solicită prin [responsabilitățile bibliotecilor](README.md), fără introducerea în consumator a implementărilor acestora.

<a id="source-language-governance"></a>

## Guvernanța limbii surselor

[Contractul limbii implementării](../governance/ATF_SourceLanguage.md) deține inventarul surselor, controalele lexicale revizuite, limitele datelor localizate și revizia semantică obligatorie.

## Coeziunea și evoluția pachetelor

[Politica executabilă](../../config/atf-governance-policy.json) aplică limita fără derogări pentru structura curentă. [Responsabilitățile pachetelor](../governance/ATF_Packages.md) delimitează API-urile, suportul comun și selecțiile testelor.

[Guvernanța calității](../governance/README.md) · [Bune practici documentare](../governance/ATF_Documentation.md)

[Indexul testelor browser](../../atf-ui-browser/tests/README.md#granițele-runnerului-de-diagnostic) deține granițele selecției, ciclului de viață, scenariilor și rezultatelor. Componentele cu mock păstrează identitățile cazurilor, proprietatea resurselor și cauzele erorilor; parcursul scurt randat rămâne separat.

## Calificarea afirmațiilor și lansatoarelor

Legarea dovezilor de sursa calificată este verificată de [regresiile evaluatorului RAG](../testing/ATF_ReleaseQualification.md#regresiile-evaluatorului). Integritatea APK-ului și a payload-ului din dosarele înregistrate rămâne verificată de [guvernanța integrității release-ului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java).

| Prioritate | Responsabil și lacună | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Recuperarea probelor: controalele statice încă împart identitatea întregului diff Git protejat. | Reutilizarea selectivă a controalelor reușite rămâne condiționată de închiderea dependențelor de producție, teste, runtime și suport comun per control. Păstrați identitățile originale ale sursei/raportului; refuzați intrările comune sau necunoscute schimbate și selectorii modificați. Înainte de promovare, lotul validează ID-ul controlului, baza, commit-ul, digestul diff-ului protejat, listele distincte și nevidate de selectori pozitivi/negativi cu verdict PASS și digestul Extent exact. Regresia de componentă califică și varianta conservatoare: după eșecul unui driver, încercarea corectată execută întregul lot selectat într-o rulare nouă; identitățile originale JUnit și Extent din fixture rămân nemodificate, iar deriva intrării comune împiedică promovarea. Aceasta nu autorizează reutilizarea parțială a probelor. |

- Componenta delimitată [pentru loturi](../../atf-tests/src/test/java/atf/tests/execution/components/TestChangeBatchComponentTest.java) compară o rulare completă după corectarea driverului cu cazuri negative pentru sursa comună și ieșirea controlului. Fiecare recipisă simulată este legată de ID-ul controlului și de octeții exacți ai raportului Extent.
- Populația JUnit per control și hash-urile rapoartelor sunt fixture-uri simulate; raportul generat al componentei înregistrează execuția reală. Controlul schimbării sursei și selectorii pozitivi/negativi exacți apar în [baseline-ul `testChanges` verificat](../governance/ATF_TestChangesBaseline.md#rag-36-and-lab-02-receipt-admission-and-curator-recovery).


- Selecția componentelor verifică aceste cataloage înainte de crearea dosarului de rulare, staging-ului sursei sau pregătirii Maven; grupurile necunoscute și duplicate sunt refuzate.
- Grupurile fără proprietate asupra snapshot-ului release omit octeții payload-ului, dar păstrează inventarul complet, identitatea sursă/catalog, controalele live înainte și după, precum și preflight-ul artefactelor.
- Hash-ul selectorilor leagă planul de cataloagele din staging. Reluarea refuză un mod fără payload dacă proprietarul selectat are nevoie de acel conținut.
- Sigiliul compilatorului rămâne complet. Sigilarea rece și admiterea caldă a recipisei se măsoară separat; admiterea caldă păstrează verificările pentru clase lipsă, alterări, recipise străine și căi indirecte.
- Preflight-ul comun al lansatoarelor rezolvă toate executabilele necesare înaintea probelor de versiune, înregistrează hash-ul sursei și verifică Java 17+, versiunile Maven și Node și intrările wrapper-ului Gradle fixate prin HTTPS/SHA-256 înaintea pregătirii workspace-ului ori Maven.
- Preflight-ul Android rămâne specific transportului. Cazurile ADB cu mock separă refuzul comenzii, timeout-ul, oprirea limitată și eșecul curățării de disponibilitatea dispozitivului și interpretarea ecranului.
- Generarea probelor testChanges face o singură revizie a surselor protejate și partajează un singur digest exact al diff-ului protejat între controalele eligibile, apoi verifică starea curată și HEAD.
- Dovada de componentă înregistrează numărul real de faze și timpul scurs. Măsurătorile calificării separă inventarul, octeții copiați și sigilarea rezultatelor compilării.
- Aceste măsurători arată reducerea muncii de revizie; nu pretind o accelerare generală a Maven ori a procesului complet de release. Sigiliile integrale ale octeților, refuzul cititorilor necunoscuți și proveniența originală a probei rămân intacte.
- [Domeniile independente ale probelor](../governance/ATF_TestChanges.md#domenii-independente-ale-probelor) dețin selecția după căi, reuniunea diff-urilor mixte și dovezile separate. Excluderea domeniilor neschimbate nu închide criteriile rămase pentru costul snapshot-ului, compilării sau admiterii.
- [Retenția imaginilor inline](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) verifică duplicarea PNG-urilor lângă HTML; nu dovedește autonomia datelor, lizibilitatea imaginilor sau integritatea APK-ului. [Criteriul adaptorului](../reporting/ATF_Audit.md) rămâne distinct.

## Proprietatea asset-urilor RAG și semnarea locală

| Prioritate | Graniță actuală | Criteriu de închidere |
|---|---|---|
| P2 | Politica de release, cataloagele de teste și lansatoarele au proprietari diferiți în `config/` și `scripts/`; `.atf-signing/` conține contractul public de semnare alături de material local ignorat. Mutarea directoarelor întregi ar muta împreună lansatoare comune, politica jocului și contractele de căi. | Inventariați mai întâi proprietarii și cititorii. Luați în calcul `config/rag/` și `scripts/rag/` numai pentru fișiere exclusive RAG; păstrați lansatoarele comune și configurația jocului în zonele comune actuale. Țineți cheile private în afara Git și a snapshot-urilor RAG, în stocare locală protejată (de exemplu, `.imp/local/signing`). Nu mutați nimic până când testele de componentă și guvernanță nu dovedesc reconstruirea sursei cu și fără cheie locală, rezolvarea suprascrierilor de cale, evaluarea release-ului și absența octeților privați din manifeste, surse copiate și rapoarte. |


- [Contractul eșecurilor jurnalului](../testing/ATF_ReleaseQualification.md#qualification-journal-failures) păstrează separat cauzele originale ale comenzii și publicării.
- Erorile controlate de scriere și refuzul real pe disc rămân granițe de regresie în componente; nu califică toate capabilitățile sau condițiile de resurse ale gazdei.

## Îmbunătățirea ghidurilor de orientare

Grila editorială din [contractul documentației](../governance/ATF_Documentation.md#verificare-și-evaluare-editorială) evaluează documentul și limita lui, separat de maturitatea codului. Notele și inventarul evaluat rămân în auditul extern; aici păstrăm numai lucrările care pot îmbunătăți lectura sau reproducerea.

Terminologia operațională urmează [contractul editorial](../governance/ATF_Documentation.md#terminologia-operațională). Revizuiți sensul după cod și ediția română; diagnosticele automate impun erorile deja identificate, fără să înlocuiască revizia semantică.

## Descărcarea prin browser

[Contractul distribuției](../platform/ATF_AndroidDelivery.md#verificări-automate-ale-distribuției) separă proba API, descărcarea efectivă din browser și accesul anonim. Verificarea browserului și hash-ul fișierului primit se consemnează în [dosarul versiunii active](../../release/README.md).

| Prioritate | Lacună delimitată | Criteriu de închidere |
|---|---|---|
| P2 | Proba din browserul autentificat depinde de operator și de sesiunea GitHub disponibilă. | După publicare, verificați URL-ul versiunii, fișierul primit și SHA-256, apoi deschideți auditul de pe commitul din note. Înregistrați separat răspunsurile anonime și eventualele refuzuri locale ale browserului. |

Proba de transfer prin Android DownloadManager și verificarea certificatului aparțin dosarului versiunii efectiv testate. [Contractul transferului Android](../platform/ATF_AndroidDelivery.md#separarea-accesului-de-transferul-android) descrie datele necesare, iar [accesul anonim pe ruta publică](../platform/ATF_AndroidDelivery.md#accesul-anonim-și-ruta-publică) are propriul criteriu de închidere.

## Acceptanța nativă a distribuției

| Prioritate | Lacună delimitată | Criteriu de închidere |
|---|---|---|
| P2 | Proba nativă este păstrată cu sursele în Extent, dar lansarea ei este încă o operație TMP a operatorului. | Adaptor opt-in versionat: citește identitatea candidatului, folosește permisiuni Android proprii și separă accesul de transfer. Componentele injectează 404, fișier scurt, hash/CRC greșit, certificat absent ori diferit și timeout; nu salvează redirectul semnat. Raportul păstrează comparațiile, eroarea și durata originale, iar cleanup-ul elimină doar resursele deținute. Un singur transfer Android califică livrarea; nu se multiplică pe rezoluțiile UI. |

## Limitele calificării release-ului

Separarea sursei native de validator aparține [contractului de calificare](../testing/ATF_ReleaseQualification.md#proveniența-execuției-native-și-a-validatorului). Reutilizarea cere în continuare motor, scenariu, APK și dovezi originale neschimbate.

## Pilot LangGraph în umbră

Calea de producție rămâne un lansator PowerShell determinist, cu checkpoint-uri Maven/Gradle legate de sursă și un evaluator Node cu acces doar la citire.

[Recuperarea probelor statice](../../atf-tests/src/test/java/atf/tests/execution/proofs/StaticProofRecoveryComponentTest.java) și [recuperarea invocărilor](../../atf-tests/src/test/java/atf/tests/execution/recovery/InvocationRecoveryComponentTest.java) verifică întreruperea, reluarea legată de sursă, populațiile exacte ale invocărilor și paritatea raport/JUnit pentru acest lansator. Aceste rezultate nu dovedesc un pilot LangGraph: acest checkout nu conține un coordonator executabil bazat pe grafuri sau o rulare măsurată a grafului.

[Calificarea release-ului](../testing/ATF_ReleaseQualification.md#orchestrare-și-poarta-de-adoptare-langgraph) definește contractele stabile de reluare și selecție pe platformă.

LangGraph oferă noduri explicite, stare partajată și limite de checkpoint, dar nu scurtează compilarea. Un nod eșuat poate fi reluat de la început, așadar pilotul trebuie să demonstreze idempotența efectelor secundare și să lege fiecare reluare de sursă, comandă și receipt-urile inițiale. Vezi [îndrumarul oficial LangGraph pentru fluxuri](https://docs.langchain.com/oss/javascript/langgraph/thinking-in-langgraph).

| Prioritate | Lacună delimitată | Criteriu de închidere |
|---|---|---|
| P2 | În acest checkout nu există un pilot executabil de coordonare prin graf sau o comparație măsurată. Componentele existente pentru reluare legată de sursă, populația invocărilor și paritatea raport/JUnit acoperă doar lansatorul determinist. | Construiți un pilot shadow izolat care apelează numai comenzile existente. Injectați întreruperi în fiecare etapă și cereți paritate exactă pentru selecție, identitățile rapoartelor/JUnit și verdict, fără efecte secundare duplicate și fără autoritate de semnare, publicare sau ciclu de viață. Măsurați pornirea, costul unei rulări normale și recuperarea pe aceleași cazuri; adoptați numai dacă timpul median de recuperare scade cu cel puțin 20%, iar costul unei rulări neîntrerupte crește cu cel mult 5%. Niciun model nu poate stabili sau slăbi verdictul unui release. |

<a id="testchanges-admission-before-collection-preparation"></a>

## Admiterea testChanges înainte de pregătirea colecției

[Contractul admiterii timpurii](../governance/ATF_TestChanges.md#admiterea-timpurie-a-controlului-sursei) deține validarea canonică a probelor, admiterea planului de generare și reverificarea după pregătire. Rapoartele originale ale probelor, identitățile Git exacte și guvernanța finală rămân obligatorii; selecția sau reutilizarea compilării nu ocolesc admiterea.

[Contractul metadatelor preliminare](../testing/ATF_ReleaseQualification.md#early-release-metadata) verifică declarațiile E2E active, destinațiile catalogului și regulile editoriale ale indexului release-ului înainte de inventar, copiere sau Maven. Reverificarea snapshot-ului rămâne legată de sursă și separată de octeții artefactelor, rezultatele suitelor primare și calificarea completă a publicării.


<a id="framework-selection-boundaries"></a>

## Limitele selecției ATF

[Responsabilii ATF independenți](../testing/ATF_Collections.md#responsabili-independenți-pentru-componentele-atf) separă verificările editoriale și ale payload-ului de proveniență, Android simulat, GitHub simulat, orchestrare și raportare. Fiecare colecție primară are un inventar pozitiv de clase, un singur Extent canonic și propriul receipt legat de surse.

- Clasele de componente Maven au un singur proprietar primar declarat pozitiv, iar inventarul corespunde exact descriptorului suitei. Suita de guvernanță respinge proprietarii omiși sau duplicați și clasele de suită nelistate.
- Clasele de test Gradle au un singur strat declarat în `config/gradle-audit-collections.properties`. `unit` este limitat la setul de surse izolat pentru unități și nu poate depinde de raportarea componentelor; poarta inventarului respinge proprietarii lipsă ori învechiți și straturile necunoscute.

[Contractul selecției după dependențe](../testing/ATF_Collections.md#selecția-după-dependențe-executabile) determină consumatorii din referințele executabile actuale și domeniile explicite de observare a repository-ului. Corpurile revizuite ale cititorilor și furnizorii generați delimitează ce poate admite analiza. Intrările necunoscute, populațiile lipsă și modificările neadmise ale cititorilor păstrează calificarea ATF completă.

- Schimbările numai în Markdown selectează responsabilul documentației fără reluarea fixture-urilor independente ale lansatoarelor. Helper-ele comune, dependențele și intrările necunoscute invalidează în continuare consumatorii în mod conservator.
- Reutilizarea păstrează identitățile și rezultatele JUnit originale, comparațiile și hash-urile rapoartelor. Rapoartele monolitice istorice rămân imuabile și nu pot califica noile identități ale partițiilor.
- O actualizare revizuită a sigiliilor cititorilor este validată față de inventarul complet actual și topologia neschimbată a dependențelor. Sunt selectați doar consumatorii corpurilor de cititor schimbate; observatorii metadatelor nu lărgesc selecția. Mai mulți cititori schimbați selectează reuniunea consumatorilor lor, iar sigiliile neexplicate sau domeniile necunoscute ale cititorilor păstrează calificarea completă.

[Contractul fixture-urilor de lansare](../testing/ATF_Collections.md#launcher-fixtures) deține profilul resurselor, păstrarea diagnosticelor înaintea curățării, duratele independente ale invocărilor și separarea matricilor pure de componentele reale de admitere.

Măsurați separat pregătirea snapshot-ului, compilarea și execuția. Diagnosticul focalizat rămâne distinct de calificarea unei colecții primare complete; recuperarea Maven/Jupiter în interiorul unui responsabil urmează [contractul reluării invocărilor](../testing/ATF_Collections.md#reluarea-în-interiorul-unei-selecții-maven).

## Admiterea schemei de raportare

Evaluatorul aplică acum aceeași regulă de admitere a raportării înainte de verdict și păstrează manifestele istorice neschimbate. Combinațiile de scheme și rapoarte acceptate sunt:

- Versiunile 1 și 2 pot omite `qualification.reportSchema` numai dacă toate rapoartele declarate pentru componente și E2E sunt HTML.
- Versiunea 3 și cele ulterioare declară `ATF_EXTENT_HTML_INLINE_BASE64_V1`.
- Modul XML-only, valorile necunoscute, declarația modernă absentă sau formatele amestecate blochează verdictul. JUnit XML rămâne o dovadă temporară, nu raportul canonic.

Politica se află în `config/release-platform-rag-policy.json`, admiterea este aplicată de `atf-tests/tools/evaluate-release-rag.mjs`, iar testele de componentă aparțin claselor `ReleaseRagProvenanceComponentTest` și `ReleaseRagPlatformGovernanceTest`.
