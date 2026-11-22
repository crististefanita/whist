# Ghid de testare

## Pe scurt

Alege testul după granița care poate eșua, apoi selectează colecția responsabilă. Ghidul mapează modulele Whist la verificările executabile, inclusiv convențiile diferite folosite de Maven, Gradle și adaptorul browser. [Strategia de testare](README.md) rămâne proprietarul normativ al politicii de risc și acceptare; matricea oferă harta implementării.

Android și Web sunt active. Calificarea iOS nativă este suspendată. Un test JVM local al punții nu califică o aplicație iOS.

```mermaid
flowchart LR
    Change[Contract modificat] -->|identifică| Owner[Modul responsabil]
    Owner -->|alege granița| Layer[Nivel minim util]
    Layer -->|selectează| Collection[Colecție primară]
    Collection -->|observă| Evidence[Comparații și proveniență]
    Evidence -->|admite| RAG[Calificarea release-ului]
```

<details>
<summary>Legendă — responsabilitate înainte de execuție</summary>

- O modificare de sursă selectează consumatorii prin [contractul dependențelor](ATF_Collections.md#selecția-după-dependențe-executabile), nu printr-o listă dedusă din numele fișierelor.
- Dovezile reutilizate își păstrează identitățile invocărilor și octeții raportului original. O execuție nouă are un certificat nou legat de surse.
- Rezultatul unei componente dovedește granița exercitată. Fonturile, interacțiunea tactilă și callback-urile sistemului necesită acceptare randată separată când li se modifică contractele.

</details>

## Niveluri și medii

| Nivel | Graniță și date potrivite | Ce nu stabilește |
|---|---|---|
| Unitar | Un calcul sau o politică; exemple tipizate, timp controlat și așteptări derivate independent. | Colaborarea între module sau randarea platformei. |
| Componentă | Granița publică a unui modul sau adaptor; implementare locală reală cu colaboratori controlați, fixture-uri legale și cazuri negative. | O aplicație nou instalată sau un parcurs într-un browser real. |
| Integrare | Colaborare reală între module, binare fizice ori infrastructura de execuție și publicare. | Fiecare variantă a unui contract de componentă deja izolat. |
| E2E | O comandă a utilizatorului prin aplicația randată până la efectul observabil și persistența lui. | Acoperirea exhaustivă a regulilor, politicilor sau geometriei. |

Mediul este o dimensiune separată. O componentă poate lansa Git, Node sau un subproces controlat fără Android. O captură salvată, un răspuns ADB simulat sau un jurnal arhivat nu devine execuție E2E.

## Matrice detaliată modul × nivel de testare

### Declarații Maven

- `✅` înseamnă că clasele executabile live declară nivelul, direct sau printr-o adnotare compusă.
- `—` înseamnă că nu există o asemenea declarație executabilă. Nu solicită o suită artificială și nu implică acoperire lipsă.
- [Guvernanța matricei](../../atf-tests/src/test/java/atf/tests/governance/source/TestLayerGovernanceTest.java) compară rândurile cu inventarul surselor parsate prin JDK și respinge modulele lipsă, duplicate sau incorecte.

<!-- atf-maven-level-matrix -->

| Modul | Unitar | Componentă | Integrare | E2E | Responsabil și scop principal |
|---|---|---|---|---|---|
| `atf-validation` | ✅ | ✅ | — | — | [Validare](../../atf-validation/README.md): oracole pentru contractele publice ale regulilor și dovezilor; fără dependență de aplicație. |
| `atf-test-android` | ✅ | ✅ | ✅ | ✅ | [Verificare Android](../../atf-test-android/README.md): ceasuri, adaptoare ADB, observații, integrarea raportării și parcursuri în aplicația instalată. |
| `atf-tests` | ✅ | ✅ | ✅ | — | [Verificare ATF](../../atf-tests/README.md): consumatori de binare, contracte ale adaptoarelor, guvernanța surselor/documentației și calificarea release-ului. |

<!-- /atf-maven-level-matrix -->

Fiecare clasă Maven executabilă are exact un nivel. Clasele de suită descriu selecții; ajutoarele, fixture-urile și publicatorii nu sunt invocări suplimentare de test. [Contractul sintactic](../governance/README.md#declarațiile-testelor) definește inspectarea claselor imbricate și adnotărilor compuse fără executarea codului aplicației.

### Granițe Gradle și browser

Rândurile descriu granițele exercitate și responsabilii executabili. Testele JUnit Gradle și contractele Node nu declară adnotările ATF de nivel din Maven. Numele unui task sau al unui fișier JavaScript nu extinde, așadar, matricea adnotărilor de mai sus.

| Modul sau suprafață | Graniță unitară | Graniță de componentă | Graniță de integrare sau randare | Punct de intrare și limită |
|---|---|---|---|---|
| `whist-lab-app/game-core` | Cărți legale, punctaj, serii, limitele licitării și calculele politicilor. | Comenzi de sesiune, checkpoint-uri, realizări și observațiile permise AI-ului. | Partidele offline complete compară politici și invariante independente; fără acceptare UI. | [Testele motorului](../../whist-lab-app/game-core/src/test/README.md); regulile release-ului folosesc `:game-core:releaseRulesComponents`. Învățarea și calibrarea au populații separate. |
| `whist-lab-app/ai-lab` | Date de evaluare și ajutoare deterministe pentru cercetare. | Fixture-uri legale generate, corpus și contractele uneltelor de experimentare. | Comparațiile controlate între politici aparțin cercetării, nu calificării aplicației instalate. | [Laborator](../../whist-lab-app/ai-lab/README.md); `:ai-lab:check` include verificările locale JVM și Node. |
| `whist-lab-app/app` | Calcule de prezentare, etichete și valori persistate. | Controlere, preferințe, navigare, licitare și comportamentul istoricului scorului cu colaboratori Android controlați. | Interacțiunea nativă, imaginea raster și reluarea procesului au acceptare Android separată. | [Testele aplicației](../../whist-lab-app/app/src/test/README.md); responsabilii primari sunt achievements, bidding, commands, display și history. |
| `whist-lab-app/atf-card-table-ui` | Geometrie portabilă a mesei, tipografie și măsurătorile feței cărților. | Controale comune, meniuri și ciclul animațiilor. | Acceptarea fontului și randării native rămâne separată de acordul calculelor. | [Teste UI](../../whist-lab-app/atf-card-table-ui/src/test/README.md); `:atf-card-table-ui:testDebugUnitTest`. |
| `whist-lab-app/card-table-names` | Politica deterministă a numelor afișate. | Contractele consumatorilor exercită numele în motorul real. | Nu se revendică un parcurs nativ independent. | [Biblioteca de nume](../../whist-lab-app/card-table-names/README.md); compilare JVM și consumatorii motorului. |
| `whist-lab-app/test-audit` | Modelul auditului și calculele comparațiilor. | Fixture-urile comune de raportare validează comparațiile capturate și rezultatul publicat. | Suitele consumatorilor stabilesc integrarea cu binarul real de raportare. | [Fixture-uri de audit](../../whist-lab-app/test-audit/README.md); suportul de test nu este livrat în aplicație. |
| `whist-lab-app/ios-bridge` | Politici JVM ale punții. | Proiecția motorului și maparea comenzilor. | iOS nativ este suspendat; testele punții nu stabilesc acceptarea IPA sau a simulatorului. | [Perimetrul punții](../platform/ATF_iOSDelivery.md); exclusă din build-ul activ exclusiv JVM. |
| `atf-ui-browser` | Calcule portabile pentru viewport, meniuri, gesturi și contractele randării. | Contractele JAR-urilor exportate de execuție/test și cazurile comune ale adaptoarelor. | Fontul, zoom-ul, focalizarea și tastatura necesită observații în browser. | [Adaptor comun](../../atf-ui-browser/README.md); [runner de contracte Node](../../atf-ui-browser/tests/run-components.mjs). |
| `atf-ui-browser/whist` | Proiecția Whist și valorile expuse browserului. | Comenzile motorului, geometria și adaptarea interacțiunii. | Un parcurs real în browser verifică o mână randată și redeschiderea stocării. | [Adaptor Whist](../../atf-ui-browser/README.md); [responsabil de componentă al adaptorului](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java). |
| `atf-ui-browser/pages` | Codec-uri pentru stocarea locală și politici ale stării clientului. | Controale ale motorului compilat, navigare, istoric și efecte cu stocare sau timp controlat. | Scripturile Playwright exercită site-ul produs; starea simulată a browserului este identificată separat de acceptarea nativă. | [Pages](../../atf-ui-browser/README.md#verificare); `verify.mjs`, `verify-controls.mjs`, `verify-records.mjs`, `verify-navigation.mjs` și `verify-effects.mjs` au scopuri distincte. |

[Harta modulelor](../../README.md#harta-modulelor) deține apartenența la build și configurațiile dependențelor. [Catalogul colecțiilor](ATF_Collections.md) deține selecțiile executabile, bugetele și comenzile. Matricea mapează responsabilii; nu dublează definițiile taskurilor.

### Biblioteci binare consumate

| Bibliotecă | Verificare locală | Limita responsabilității |
|---|---|---|
| Core | API public și identitate binară prin consumatorii Maven. | Suitele unitare/de componentă ale producătorului sunt în afara reactorului local. |
| Reporting | Date capturate, comparații originale, structura Extent în engleză și acoperirea exactă a invocărilor. | Prezența HTML nu dovedește singură lizibilitatea interactivă sau un parcurs nou în aplicație. |
| Console | Integrarea consumatorilor și diagnostice. | Testele consumatorului nu înlocuiesc testele surselor producătorului. |
| Mock | Comportamentul consumatorului la granițe controlate deliberat. | Rezultatele simulate rămân intrări explicite, niciodată dovezi live fabricate. |
| Test Kit | Contracte de nivel, aserțiuni și suite prin consumatorii locali. | Perechea JAR/POM versionată definește dependența locală. |

[Indexul bibliotecilor binare](../../lib/README.md) deține versiunile și proveniența. Modulele sursă UI comune rămân în checkout; consumatorii AAR/JAR-urilor exportate verifică aceiași octeți livrați și arhiva de teste, în loc să copieze sursele producătorului.

## Contracte comune Android–Web

| Proprietate | Probă comună de componentă | Acceptare rămasă pe platformă |
|---|---|---|
| Așezarea mesei și scalarea | Politici comune de așezare cu regiuni disponibile explicit, scară de text și populații legale de jucători/cărți; verifică încadrarea și locurile fixe între faze. | Insets native, viewport browser adaptiv și măsurători ale fontului real. |
| Reacția cărții și colectarea | Aceeași secvență de gesturi, carte respinsă/recomandată și cazuri de anulare; timpul controlat verifică ordinea evenimentelor. | Ținte tactile, fețele cărților randate și cadrele intermediare ale animației. |
| Profilul și partida reluată | Codec real, checkpoint-uri legale ale motorului, anulare, intrări invalide și restaurare idempotentă. | Redeschiderea stocării și callback-uri de ciclu de viață prin controale vizibile. |
| Licitarea și istoricul scorului | Intrări comune ale regulilor, selecție manuală, totaluri, marcaje de serie și modele localizate. | Confirmarea vizibilă, ultimele rânduri lizibile și controale accesibile. |

Folosește [contractul adaptorului](ATF_DisplayComponents.md) pentru a identifica aceleași cazuri pe ambele platforme. Valorile așteptate comune fără exercitarea fiecărui adaptor sunt insuficiente; compararea unei capturi simulate cu alta nu stabilește paritatea randării.

## Selecții, reutilizare și admiterea schimbărilor de teste

1. Identifică sursele modificate și colecția responsabilă prin diff-ul Git real și modelul dependențelor.
2. Revizuiește modificările protejate ale testelor față de baza și căile exacte. Admite declarațiile sursă ale selectorilor pozitivi și negativi înainte de copierea surselor sau cache-urilor de dependențe și înainte de Maven.
3. Solicită o singură metodă de instanță `void @Test` declarată neambiguu în perimetrul componentelor. Fabricile/șabloanele dinamice, testele parametrizate/repetate, supraîncărcările și compunerea nerezolvată a adnotărilor nu sunt selectori statici de probă. Vezi [admiterea selectorilor](../governance/ATF_TestChanges.md#admiterea-timpurie-a-controlului-sursei).
4. Compilează o singură dată pentru controalele necesare, apoi reconciliază descoperirea și rezultatele JUnit reale cu fiecare selector declarat. Eligibilitatea sursei nu autorizează invocări lipsă, omise sau eșuate.
5. Reutilizează numai dovezi admise pentru responsabili nemodificați. Reevaluează deriva exclusiv documentară față de ambele snapshot-uri; păstrează agregatul eșuat și cauza originală.
   Probele originale ale controlului sursei pot continua după commituri editoriale admise fără schimbarea identității execuției; guvernanța documentației curente rulează în continuare. [Continuarea probelor](../governance/ATF_TestChanges.md#editorial-continuation-of-original-proofs)

6. Execută responsabilii lipsă sau invalidați, publică comparațiile și lasă evaluatorul RAG doar în citire să decidă dacă populația necesară este calificată.

Suitele primare dețin populații disjuncte. Selecțiile focalizate își păstrează părinții primari; rularea unui caz prin ambele puncte de intrare nu creează două cazuri unice. [Catalogul suitelor](../../config/atf-test-suites.json) și [inventarul responsabililor componentelor](../../config/atf-component-collections.json) sunt intrări executabile, nu liste istorice de rezultate.

## Execuție și audit

Configurează [rădăcinile externe pentru dovezi și fișiere temporare](../../scripts/README.md) înainte de lansare. Rezultatele build-ului, profilurile de test și snapshot-urile intermediare rămân în afara checkout-ului; folosește setări locale mașinii în locul căilor fixe. [Colecțiile și comenzile](ATF_Collections.md) enumeră lansatoarele acceptate și selecția pe responsabil.

- Porțile de componentă și E2E publică rapoarte Extent canonice separate, în engleză, prin biblioteca de raportare. Capturează intrările, comparațiile așteptat/obținut, cauzele originale și identitățile invocărilor în timpul execuției.
- Imaginile Base64 inline aparțin unei observații reale raster sau în browser. O verificare numerică de geometrie este raportată ca aserțiune de componentă, fără inventarea unei capturi.
- Rezultatele JUnit reconciliază populația executată. Republicarea sau reluarea unui jurnal arhivat nu califică o interacțiune nouă.
- Păstrează numerele concrete, hash-urile, timpii, eșecurile și deciziile de reutilizare în auditul extern. Ghidurile generice descriu mecanisme și rămân valabile pentru release-uri viitoare.

[Arhitectura testelor](ATF_Architecture.md) · [Contractul auditului](../reporting/ATF_Audit.md) · [Calificarea release-ului](ATF_ReleaseQualification.md) · [Guvernanța calității](../governance/README.md)
