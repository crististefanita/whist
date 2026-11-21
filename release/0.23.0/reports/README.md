# Auditul de calificare Whist 0.23.0

## Starea calificării

Snapshotul sursă fixat a trecut suitele selectate de componente, guvernanță și acceptanță Android. Verdictul RAG doar pentru citire este ultima verificare înainte de publicare; nu publică și nu promovează acest candidat.

## Identitate

| Element | Valoare |
|---|---|
| APK | `WhistLab-0.23.0.apk` |
| Cod versiune | 47 |
| Commit sursă | ``8b8b9d3681b2cdc44ef89d74241dc86cce34669c`` |
| Dimensiune APK | 1,033,649 bytes |
| SHA-256 APK | `f77a65d25f7c1249409d10de3379751592ca018b350394a3ef852bc73e32665a` |
| SHA-256 certificat de semnare Android | `b80c45a9b52ef0c83dcb929ba4aeb8775c836e5e6d587324d7ec725c079fb4c3` |
| Profil dispozitiv | Android API 28; 1080×1920 la 440 dpi |

## Suite de componente și guvernanță

| Selecție | Invocări | Decizie | Dovezi |
|---|---:|---|---|
| atf-validation | 19 | REUSE | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | REUSE | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 555 | RUN | [Extent canonic](component/atf-tests/component-report.html) |
| whist-lab-app-bidding | 255 | REUSE | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | REUSE | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | REUSE | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | REUSE | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-achievements | 28 | REUSE | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-atf-card-table-ui | 56 | REUSE | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | REUSE | [Extent](component/whist-lab-app-rules/component-report.html) |

Total: **1,521** de invocări de componente și guvernanță. Rezultatele reutilizate păstrează amprentele surselor și dovezile execuției inițiale. Singura selecție Maven rulată acum a trecut cu 555/555.

## Fluxuri de acceptanță Android

Toate cele șapte fluxuri au rulat pe APK-ul semnat identificat și au trecut cu 1 test, 0 eșecuri, 0 erori și 0 ignorări. Fiecare suită are propriul raport Extent cu capturi inline Base64, dovada execuției și proveniența sursei.

| Flux | Rezultat |
|---|---|
| [Verificare de bază APK semnat](e2e/signed-release-smoke/report.html) | PASS 1/1 |
| [Istoric de joc încheiat](e2e/history-release-sanity/report.html) | PASS 1/1 |
| [Afișare adaptivă](e2e/responsive-ui-sanity/report.html) | PASS 1/1 |
| [Realizări](e2e/signed-release-achievements/report.html) | PASS 1/1 |
| [Limită de mână AutoGame](e2e/signed-release-auto-game/report.html) | PASS 1/1 |
| [Recomandare de licitare și contract salvat](e2e/signed-release-bidding/report.html) | PASS 1/1 |
| [Ecran inițial, profil și restaurarea limbii](e2e/signed-release-home-profile/report.html) | PASS 1/1 |

Prima încercare a fluxului Realizări s-a încheiat cu o eroare tranzitorie la capturarea ierarhiei UI Automator, înaintea oricărei aserțiuni eșuate. Doar acel flux a fost reluat; repetarea a trecut cu 1/1. Raportul final acceptat este unicul raport canonic al suitei de mai sus. Fiecare execuție a folosit un utilizator Android temporar, eliminat la final; datele utilizatorului original 0 au fost păstrate.

## Proveniență și diagnosticare

- [Dovada execuției componentelor și a reutilizării](component/source-component-run.json)
- [Snapshoturi ale surselor componentelor](component/snapshots/)
- [Diagnosticare build, identitate APK și semnare](diagnostics/)
- [Planul de calificare RAG](qualification-plan.json)

Fișierul checksum al APK-ului este inclus lângă binar. Dovada descărcării după publicare rămâne în așteptare până când release-ul GitHub declarat este publicat și descărcat pentru verificare.

## Sursa și identitatea APK-ului

Snapshotul sursă a fost reconstruit la commitul ``8b8b9d3681b2cdc44ef89d74241dc86cce34669c``. Binarul semnat este identic octet cu octet cu APK-ul folosit pentru cele șapte fluxuri Android acceptate (SHA-256 `f77a65d25f7c1249409d10de3379751592ca018b350394a3ef852bc73e32665a`), deci rapoartele native rămân valabile. Commitul sursei fiecărei execuții inițiale este păstrat în dovada sa; singura schimbare intermediară de sursă este înregistrarea reviziei bilingve a documentației.
