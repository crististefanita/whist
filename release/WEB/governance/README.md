# Dovezi de guvernanță

## Pe scurt

Păstrați identitatea originală a sursei și populația fiecărui raport. Un raport califică instantaneul declarat; copierea octeților și hash-urile corecte nu califică singure surse schimbate ulterior.

[Înapoi la index](../README.md)

| Raport | Verdict original | Proveniență |
|---|---|---|
| [Referința de guvernanță ATF](atf/component-report.html) | `PASS` · `REFERENCE` | `9cc19f7f` |

HTML-ul păstrează datele complete și capturile inline aferente. Inventarul și hash-urile sunt în [manifest](../index.json); verificarea lor nu reexecută testele.

- [Guvernanță curentă pentru responsabilități și sursă](current/component-report.html)

## Selecția documentației și a planului

[Suita Extent focalizată](documentation-plan/component-report.html) păstrează opt controale documentare trecute. [Selecția și refolosirea compilării](documentation-plan/execution.json) identifică exact clasele și commiturile; arhitectura în trei module rămâne planificată.

[Măsurarea intrării raportului](documentation-plan/footprint.json) separă payload-ul PNG din HTML-ul original. Observația justifică scurtarea selecției UI, fără modificarea JAR-ului Reporting și fără noi rulări E2E.
