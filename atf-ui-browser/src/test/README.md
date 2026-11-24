# Suportul acceptanței în browser

## Pe scurt

Această rădăcină de surse deține suportul reutilizabil al acceptanței în browser: ciclul sesiunii, un server static izolat, scenarii Whist și rezultatele verificărilor capturate. [Strategia comună de testare](../../../docs/testing/README.md) definește nivelurile; [catalogul testelor de browser](../../tests/README.md) selectează verificările executabile.

## Responsabilități

| Responsabil | Contract |
|---|---|
| [BrowserAcceptanceSession](java/ro/cardtable/testing/browser/BrowserAcceptanceSession.java) și [StaticSiteHost](java/ro/cardtable/testing/browser/StaticSiteHost.java) | Creează și închid resursele proprii de browser/server, cu granițe explicite pentru origine și stocare. |
| [BrowserCheckResults](java/ro/cardtable/testing/browser/BrowserCheckResults.java) | Păstrează identitatea verificării, valorile evaluate, eșecurile și dovezile fără să transforme o verificare absentă în succes. |
| [BrowserCheckSupport](java/ro/cardtable/testing/browser/BrowserCheckSupport.java) | Oferă suport comun pentru observații și aserțiuni, păstrând cauza originală a eșecului. |
| Clasele scenariilor Whist în browser | Descriu parcursurile cărților, mesei și așezării prin adaptor; execuția și publicarea rămân responsabilități separate. |

## Niveluri de testare

| Nivel | Responsabilitate | Dovezi |
|---|---|---|
| Unitar | Selecție pură, bugete și proiecția valorilor în modulele responsabile | Rezultate native ale testelor fără Extent. |
| Componentă | Ciclu de viață controlat, fidelitatea rezultatelor și contractele adaptorului comun | Extent auditabil cu precondiții declarate și comparații originale, fără poze. |
| Sanity în browser | Tranziții scurte între meniuri și joc pe un site și checkpoint identificate | Un flux ilustrat restrâns, cu capturi inline și aserțiuni. |

## Precondiții și curățare

- Identificați site-ul, viewport-ul, limba, scala fontului și fixture-ul de joc/stocare înainte de observarea scenei.
- Păstrați combinațiile exhaustive de geometrie în contractele comune de componentă; selectați tranziții reprezentative pentru parcursul în browser.
- Izolați stocarea browserului și dețineți ciclul sesiunii și serverului; curățarea păstrează eroarea originală când eșuează și execuția, și închiderea.
- Păstrați rezultatele generate în afara surselor și publicați numai dosarul calificat curent.

[Modulul browser](../../README.md) · [Ghidul testării](../../../docs/testing/ATF_TestingGuide.md)
