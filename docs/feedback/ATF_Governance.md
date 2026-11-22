# Reutilizarea guvernanței și coeziunea codului

## Pe scurt

Proiectul deține perimetrul Android și adaptarea verificatoarelor. Contractul comun reutilizabil se solicită prin [responsabilitățile bibliotecilor](README.md), fără introducerea în consumator a implementărilor acestora.

## Costul inspecției sintactice

| Prioritate | Observație și sursă | Criteriu de închidere |
|---|---|---|
| P2 | [Parserul sintactic](../../atf-tests/src/test/java/atf/tests/governance/source/JavaTestDeclarations.java) dezactivează deja procesarea adnotărilor și folosește căi goale pentru clase și procesoare. Costul acestei configurări nu a fost măsurat pe același inventar. | Măsurați costul parsării pe un mediu și inventar identificate. Păstrați controale independente pentru adnotări compuse, importuri ambigue, erori de sintaxă și ciclul de viață al parserului; configurarea singură nu dovedește accelerarea. |


## Coeziunea și evoluția pachetelor

[Politica executabilă](../../config/atf-governance-policy.json) aplică limita fără derogări pentru structura curentă. [Responsabilitățile pachetelor](../governance/ATF_Packages.md) delimitează API-urile, suportul comun și selecțiile testelor.

[Guvernanța calității](../governance/README.md) · [Bune practici documentare](../governance/ATF_Documentation.md)

## Calificarea afirmațiilor și lansatoarelor

Legarea dovezilor de sursa calificată este verificată de [regresiile evaluatorului RAG](../testing/ATF_ReleaseQualification.md#regresiile-evaluatorului). Integritatea APK-ului și a payload-ului din dosarele înregistrate rămâne verificată de [guvernanța integrității release-ului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java).

| Prioritate | Responsabil și lacună | Nivel și criteriu de închidere |
|---|---|---|
| P2 | Coordonarea: lansatoarele verifică precondițiile în moduri diferite. | Componente controlate pentru Java, Maven, Gradle, Node și Python: capabilitățile cerute intrării sunt verificate înaintea operației costisitoare, iar lipsa lor produce diagnostic și identitate de sursă explicite. Pentru Python, detectați aliasul de instalare separat de un interpretor executabil; pentru [modulul PowerShell ADB](../../atf-test-android/tools/AndroidDeviceSetup.psm1), verificați disponibilitatea, refuzul, timeout-ul și starea transportului cu proces controlat, separat de parsare și de clientul Java. |
| P2 | Costul calificării: pregătirea snapshot-urilor sursei și release-ului încă recopiază și recalculează hash-uri pentru rapoartele păstrate. | Reduceți munca repetată a snapshot-ului fără slăbirea admiterii intrărilor. [Contractele implementate de compilare și recuperare după curățare](../testing/ATF_Collections.md#reutilizarea-compilării-maven) sunt limite de regresie, nu sarcini de implementare deschise. Măsurați inventarul sursei/release-ului și copierea separat de compilare și execuție; confruntați duratele etapelor cu timpul total înainte de alte afirmații de accelerare. |
| P1 | Reluarea testelor eșuate este doar la nivel de selecție: o singură invocare JUnit eșuată invalidează receipt-ul întregului grup responsabil, deci testele-surori deja trecute nu pot fi reutilizate în siguranță. | Păstrați fiecare ID unic JUnit, rezultat și legătura la sursă/proveniență. La reluare executați doar identitățile eșuate sau incomplete, apoi publicați o reconciliere Extent care leagă raportul inițial de cel al reluării. Respingeți identități lipsă, duplicate, străine sau cu sursă schimbată și orice reluare eșuată; nu promovați prin editarea raportului original. |

[Retenția copiilor inline](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) verifică separat lipsa PNG-urilor identice lângă HTML. Această gardă nu dovedește autonomia datelor, lizibilitatea imaginilor sau integritatea APK-ului; [criteriul adaptorului](ATF_Reporting.md#rpt-android-html-only--livrabilul-de-audit-fără-copii-auxiliare) rămâne distinct.

## Îmbunătățirea ghidurilor de orientare

Grila editorială din [contractul documentației](../governance/ATF_Documentation.md#verificare-și-evaluare-editorială) evaluează documentul și limita lui, separat de maturitatea codului. Notele și inventarul evaluat rămân în auditul extern; aici păstrăm numai lucrările care pot îmbunătăți lectura sau reproducerea.

| Prioritate | Responsabil și suprafață | Criteriu de închidere |
|---|---|---|
| P2 | Ghidurile bibliotecilor distribuite: reutilizarea probei la producător | [Acceptanța locală a fragmentelor](../platform/ATF_Integration.md#verificarea-exemplelor-din-ghiduri) separă compilarea, execuția și verdictul. Promovați un contract generic al acestor probe în registrul responsabililor comuni și sincronizați clarificările ghidurilor la consumatori. Regresia producătorului și acceptanța fiecărui consumator rămân distincte. |

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

Calea de producție rămâne deterministă: inventarul Git selectează responsabilii, lansatorul PowerShell pregătește copiile și execută numai suitele lipsă sau invalidate, checkpoint-urile Maven/Gradle leagă probele acceptate de sursă, iar evaluatorul Node citește doar dosarul. Testele de componentă acoperă deja întreruperile, intrările schimbate, checkpoint-urile parțiale și respingerea probelor. [Calificarea release-ului](../testing/ATF_ReleaseQualification.md#orchestrare-și-poarta-de-adoptare-langgraph) definește contractele stabile de reluare și selecție pe platformă.

LangGraph oferă noduri explicite, stare partajată și limite de checkpoint, dar nu scurtează compilarea. Un nod eșuat poate fi reluat de la început, așadar pilotul trebuie să demonstreze idempotența efectelor secundare și să lege fiecare reluare de sursă, comandă și receipt-urile inițiale. Vezi [îndrumarul oficial LangGraph pentru fluxuri](https://docs.langchain.com/oss/javascript/langgraph/thinking-in-langgraph).

| Prioritate | Lacună delimitată | Criteriu de închidere |
|---|---|---|
| P2 | Încă nu există măsurători care să arate că un coordonator grafic îmbunătățește acest proces determinist de release față de checkpoint-urile actuale ale invocărilor. | Construiți un pilot shadow izolat care apelează numai comenzile existente. Injectați întreruperi în fiecare etapă și cereți paritate exactă pentru selecție, identitățile rapoartelor/JUnit și verdict, fără efecte secundare duplicate și fără autoritate de semnare, publicare sau ciclu de viață. Măsurați pornirea, costul unei rulări normale și recuperarea pe aceleași cazuri; adoptați numai dacă timpul median de recuperare scade cu cel puțin 20%, iar costul unei rulări neîntrerupte crește cu cel mult 5%. Niciun model nu poate stabili sau slăbi verdictul unui release. |

## Admiterea testChanges înainte de pregătirea colecției

Respingeți probele de control al sursei lipsă sau expirate înainte de copierea cache-urilor, compilare ori lansarea unei colecții lungi. Eșecul târziu de guvernanță protejează corectitudinea, dar consumă inutil pregătirea validată și execuțiile fără legătură.

- Descoperiți ID-urile controalelor necesare din diff-ul Git revizuit și validați explicit rădăcina externă a probelor.
- Folosiți același validator pentru admitere și guvernanța finală: ID-ul, baza, commitul, diff-ul, selectorii pozitivi și negativi și hash-ul raportului original trebuie să corespundă.
- Generați controalele lipsă folosind compilarea existentă înaintea colecției primare; transmiteți explicit rădăcina rezultată procesului suitei.
- Acoperiți configurația absentă, alt commit, controlul negativ lipsă, raportul modificat și schimbarea sursei între admitere și execuție. Probele lipsă trebuie să oprească fluxul înainte de Maven; sursele schimbate trebuie să invalideze în continuare admiterea.
- Măsurați separat pregătirea respinsă și execuția acceptată. Nu înlocuiți raportul original eșuat cu un caz vecin trecut și nu editați un receipt de execuție.

- Faceți baza de revizie un parametru explicit în lansările locale și izolate. Verificați planul controalelor înaintea dependențelor; o bază istorică incompatibilă trebuie respinsă cu același diagnostic înaintea compilării.

Fluxul izolat primește acum explicit baza reviziei. Admiterea probelor de control al sursei lipsă înaintea pregătirii dependențelor rămâne deschisă; alegerea bazei corecte nu închide singură această limită.

- Verificați sintaxa comenzilor de control, inventarul E2E explicit al dosarului activ, linkurile la catalog și limitele editoriale printr-o admitere ieftină înaintea colecției lungi. Păstrați raportul eșuat; reverificarea focalizată nu califică un grup primar eșuat.

## Limitele selecției ATF

[Responsabilii ATF independenți](../testing/ATF_Collections.md#responsabili-independenți-pentru-componentele-atf) separă verificările editoriale și ale payload-ului de proveniență, Android simulat, GitHub simulat, orchestrare și raportare. Fiecare colecție primară are un inventar pozitiv de clase, un singur Extent canonic și propriul receipt legat de surse.

- Schimbările numai în Markdown selectează responsabilul documentației fără reluarea fixture-urilor independente ale lansatoarelor. Helper-ele comune, dependențele și intrările necunoscute invalidează în continuare consumatorii în mod conservator.
- Reutilizarea păstrează identitățile și rezultatele JUnit originale, comparațiile și hash-urile rapoartelor. Rapoartele monolitice istorice rămân imuabile și nu pot califica noile identități ale partițiilor.

Măsurați separat pregătirea snapshot-ului, compilarea și execuția. Diagnosticul focalizat rămâne distinct de calificarea unei colecții primare complete; recuperarea eșecurilor dintr-un responsabil aparține punctului deschis pentru reconcilierea invocărilor de mai sus.

## Admiterea schemei de raportare

Evaluatorul aplică acum aceeași regulă de admitere a raportării înainte de verdict și păstrează manifestele istorice neschimbate. Combinațiile de scheme și rapoarte acceptate sunt:

- Versiunile 1 și 2 pot omite `qualification.reportSchema` numai dacă toate rapoartele declarate pentru componente și E2E sunt HTML.
- Versiunea 3 și cele ulterioare declară `ATF_EXTENT_HTML_INLINE_BASE64_V1`.
- Modul XML-only, valorile necunoscute, declarația modernă absentă sau formatele amestecate blochează verdictul. JUnit XML rămâne o dovadă temporară, nu raportul canonic.

Politica se află în `config/release-platform-rag-policy.json`, admiterea este aplicată de `atf-tests/tools/evaluate-release-rag.mjs`, iar testele de componentă aparțin claselor `ReleaseRagProvenanceComponentTest` și `ReleaseRagPlatformGovernanceTest`.
