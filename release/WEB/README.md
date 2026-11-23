# Dosarul curent de testare Web

## Pe scurt

Index portabil pentru artefactele Whist Web. Dovezile originale rămân autonome; toate legăturile către rapoarte sunt relative și funcționează după copierea întregului dosar.

**Stare: calificat în browser.** Revizia publicată 0.11 corespunde resurselor executate, conform regulilor explicite de comparare din confirmarea livrării. Comparația fizică APK/Chrome rămâne deschisă.

## Cum se citește

```mermaid
flowchart LR
    SOURCE["Sursă identificată"] -->|verifică prin| COMPONENT["Componente fără poze"]
    COMPONENT -->|completează cu| FLOW["Un flux E2E ilustrat"]
    FLOW -->|indexează în| DOSSIER["Dosar Web portabil"]
    REJECTED["Verdict respins original"] -->|explică regresia din| COMPONENT
```

<details>
<summary>Legendă: niveluri și limite</summary>

Componentele dețin combinațiile de dimensiuni, text, ancore și ordinea evenimentelor. E2E-ul păstrează puține parcursuri relevante, cu pași înainte/după și poze; un raport de referință nu devine calificare curentă prin copiere.

</details>

## Rapoarte și proveniență

| Raport | Verdict original | Rol | Revizia validatorului |
|---|---|---|---|
| [Treceri ilustrate la cinci/șase jucători](e2e/table-handoffs/extent.html) | `PASS` | `PRIMARY` | `9b57a75b` |
| [Componente APK/Web comune pentru fiecare loc](component/phases/component-report.html) | `PASS` | `PRIMARY` | `9b57a75b` |
| [Guvernanță curentă pentru responsabilități și sursă](governance/current/component-report.html) | `PASS` | `PRIMARY` | `9b57a75b` |
| [Integrarea JVM Android a contractului](component/apk-policy/component-report.html) | `PASS` | `REFERENCE` | `9b57a75b` |
| [Trecerile curente ilustrate](e2e/oh-hell/extent.html) | `PASS` | `REUSED` | `91716314` |
| [Componente curente pentru treceri și dosar](component/handoff/component-report.html) | `PASS` | `REUSED` | `9a5b49b8` |
| [Contractele intrărilor raportului și urmelor](component/reporting/component-report.html) | `PASS` | `REUSED` | `91716314` |
| [Geometria comună și matricile controlate](component/native-ports/component-report.html) | `PASS` | `REUSED` | `9ec0b560` |
| [Referința de guvernanță ATF](governance/atf/component-report.html) | `PASS` | `REFERENCE` | `9cc19f7f` |
| [Referința parcursului românesc anterior](reference/romanian/extent.html) | `PASS` | `REFERENCE` | `91716314` |
| [Deplasarea originală la trecerea AI–om](diagnostics/anchor-before/extent.html) | `FAIL` | `REJECTED` | `91716314` |

## Categorii

- [Componente](component/README.md): precondiții, date controlate, comparații și cazuri negative, fără poze ale produsului.
- [E2E](e2e/README.md): acțiuni și observații ordonate, aserțiuni individuale și capturi inline.
- [Guvernanță](governance/README.md): contracte de sursă, documentație și inventar.
- [Referințe](reference/README.md): acoperire anterioară cu limite explicite.
- [Diagnostice](diagnostics/README.md): probe respinse păstrate integral.

## Integritate și întreținere

- [Verificarea livrării](e2e/oh-hell/delivery.json) identifică sursa publicată și resursele deja testate; nu reprezintă o nouă rulare E2E.

[Manifestul](index.json) păstrează mărimea, SHA-256, rolul și proveniența fiecărui raport. Nu păstrăm PNG-uri duplicate, XML-uri JUnit ori build-uri în acest dosar; rapoartele conțin imaginile inline.

```powershell
node .\atf-tests\tools\web-release-dossier.mjs $ProjectRoot
```

Actualizați dosarul după calificarea și verificarea publicării Web; păstrați octeții și verdictele originale când mutați un raport. Documentația urmează limba ramurii, iar HTML-urile originale rămân identice. [Catalog Android](../README.md) · [Strategia de testare](../../docs/testing/README.md)
