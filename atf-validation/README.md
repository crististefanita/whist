# Validare semantică API

## Pe scurt

Modulul evaluează contracte de răspuns și produce comparații și decizii independente de prezentare. Transportul furnizează răspunsul; regulile stabilesc așteptările, iar politica rezultatului interpretează comparațiile evaluate.

Valorile integrale decid verdictul. Proiecția pentru publicare și limitarea previzualizării au loc după evaluare, cu păstrarea cauzei și a valorilor comparate.

## Responsabilități

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".edgeLabel, .edgeLabel p { font-size: 12px !important; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":22,"padding":6},"sequence":{"mirrorActors":false,"messageMargin":24,"wrap":true}}}%%
flowchart LR
    BODY["Răspuns API"] -->|alimentează| CONTEXT["Context evaluare"]
    RULES["Contract"] -->|declară reguli| EVAL["Evaluator"]
    CONTEXT -->|furnizează valori| EVAL
    EVAL -->|produce| RESULT["Comparații și<br/>verdict"]
```

<details>
<summary>Legendă: context local și evaluare unică</summary>

[ContractEvaluation](src/main/java/atf/validation/contract/ContractEvaluation.java) parsează corpul JSON la prima regulă care îl cere și reutilizează documentul numai în evaluarea curentă. Regulile de antet pot funcționa separat; publicarea consumă rezultatul capturat, fără a repeta evaluarea după scurtarea textului.

</details>

## Contractele JSON

- Calea invalidă, absența și valoarea prezentă sunt distincte; `null` explicit este prezent.
- Căile simple și JSON Pointer au un subset explicit; selecțiile generale, filtrele și căutarea recursivă sunt refuzate.
- Schema este verificată preliminar, inclusiv în ramuri neatinse de instanță; cuvintele-cheie necunoscute sunt refuzate.
- Fiecare evaluare are propriul contor de lucru și propriile expresii regulate compilate; aceeași expresie se reutilizează pentru valorile repetitive, fără cache comun între evaluări sau după modificarea fișierului.
- Politica strictă produce eșec, iar politica de avertizare păstrează comparațiile negative și statutul `WARNING`.

<details>
<summary>Subsetul schemei și sursele contractului</summary>

| Suprafață | Semnificație |
|---|---|
| Schemă | Obiect sau boolean; prevalidare independentă de instanță. |
| Tipuri și valori | `type`, `enum`, egalitate de valori și comparații numerice exacte; `integer` acceptă o valoare numerică integrală. |
| Obiecte | `required`, `properties` și `additionalProperties` boolean sau schemă. |
| Tablouri | `items` boolean sau schemă, `minItems` și `maxItems`. |
| Șiruri | Limite în puncte de cod Unicode și expresii regulate Java valide. |
| Numere | `minimum` și `maximum` incluzive, prin `BigDecimal`. |
| Metadate | `$schema` acceptă identificatorul draft 2020-12; celelalte metadate permise sunt adnotări. |
| Refuzuri | Cuvinte-cheie și forme nesuportate, inclusiv `const`, `$ref` și `format`. |

[JsonSchemaCapabilities](src/main/java/atf/validation/contract/schema/JsonSchemaCapabilities.java) definește vocabularul și formele acceptate, iar [JsonSchemaValidator](src/main/java/atf/validation/contract/JsonSchemaValidator.java) evaluează instanța. URI-ul acceptat este `https://json-schema.org/draft/2020-12/schema`; modulul implementează subsetul declarat, cu refuz explicit în afara lui.

[JsonPathSupport](src/main/java/atf/validation/contract/JsonPathSupport.java) deține rezolvarea căilor. Regulile scalare refuză obiectele și tablourile ca valori scalare, în loc să le transforme într-un șir gol.

</details>

## Tipul conținutului și parserul comun

`contentTypeJson()` delegă clasificarea tipului concret către `MediaTypes.isJson` din Core. Clasificarea antetului și validitatea sintactică a corpului se verifică separat.

- Tipul și subtipul ignoră diferențele de majuscule, iar subtipurile cu sufix `+json` folosesc vocabularul comun.
- Tipul absent, selecțiile generale și listele de tipuri sunt refuzate; parametrii nu transformă `text/plain` în JSON.
- `contentTypeContains` păstrează comparația literală de compatibilitate.
- [JsonDocument](src/main/java/atf/validation/contract/json/JsonDocument.java) folosește parserul complet din Core, cu refuzul cheilor duplicate și al conținutului ulterior rădăcinii.

## Niveluri și verificare

Dintr-o copie temporară proprie, cu POM-ul părinte, bibliotecile și un depozit Maven propriu:

```powershell
mvn -B -pl atf-validation -am test
```

| Contract | Verificare |
|---|---|
| Prezență și scalar | [JsonPresenceContractTest](src/test/java/atf/validation/JsonPresenceContractTest.java). |
| Subsetul schemei și refuzuri | [JsonSchemaContractTest](src/test/java/atf/validation/JsonSchemaContractTest.java). |
| Clasificarea antetului | [JsonMediaTypeContractTest](src/test/java/atf/validation/media/JsonMediaTypeContractTest.java). |

[Arhitectura testării](../docs/testing/ATF_Architecture.md) separă regulile unitare de componentele evaluării contractelor și de colaborarea prin API-ul public. [POM-ul](pom.xml) definește dependențele și limita modulului.

## Cost și limite

Subsetul acceptat refuză construcțiile nesuportate și intrările care depășesc bugetul. [JsonSchemaBudget](src/main/java/atf/validation/contract/schema/JsonSchemaBudget.java) verifică separat schema și instanța complete înaintea evaluării recursive, inclusiv adnotările și ramurile neatinse de instanță.

| Resursă | Limită inclusivă și contabilizare |
|---|---|
| Adâncimea arborelui | 64 de muchii de la rădăcină; rădăcina are adâncimea zero. |
| Nodurile arborelui | 16.384 de valori pentru fiecare schemă sau instanță, inclusiv containerele și adnotările. |
| Text | 1.048.576 de unități de cod UTF-16 pentru fiecare arbore, inclusiv numele proprietăților și valorile text. |
| Lucrul evaluării | 200.000 de pași comuni prevalidării schemei și evaluării instanței: vizite de schemă/valoare, parcurgeri de câmpuri, verificări required și comparații enum recursive. |

Depășirea unei limite produce o singură comparație explicită de buget și elimină abaterile parțiale. Politica strictă întoarce `FAIL`; avertizarea întoarce `WARNING` cu o comparație negativă. Evaluarea următoare începe cu un buget nou; comparațiile admise își păstrează căile și ordinea.

- Acestea sunt limite deterministe de evaluare, nu garanții de durată sau memorie a parserului. Parsarea JSON precedă admiterea arborelui.
- Contorul de lucru nu poate întrerupe o potrivire regex Java. Folosiți scheme de încredere; termenul procesului sau transportului rămâne o frontieră separată.

[Feedback-ul validării](../docs/feedback/ATF_TestInfrastructure.md#costul-validării-api) rutează lucrările rămase ale infrastructurii. Păstrați măsurătorile și identitatea intrărilor în afara ghidurilor proiectului; acceptați optimizările prin paritate semantică, fără scurtarea valorilor comparate.

## Catalogul testelor

[Intrările și responsabilitățile testelor](src/test/README.md) explică selecțiile, precondițiile și interpretarea rezultatului, prin aceeași strategie comună pentru toate livrările.
