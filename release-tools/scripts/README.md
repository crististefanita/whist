# Adaptoare pentru operațiile de release

## Pe scurt

Aceste scripturi oferă intrări portabile pentru operațiile de release. Rezolvă checkoutul după propria locație sau prin `-ProjectRoot` / `ATF_PROJECT_ROOT`, apoi apelează proprietarul existent al fiecărei operații. Ele sunt comenzile fluxului de release; comenzile comune de calificare și lansare a suitelor rămân în directorul rădăcină [scripts](../../scripts/README.md).

| Script | Proprietar delegat | Regula de ieșire |
|---|---|---|
| `Invoke-ReleaseRag.ps1` | `atf-tests/tools/evaluate-release-rag.mjs` și `RagAdvisoryGate` | Verdict în afara tuturor intrărilor; doar revizie umană. |
| `Invoke-ReleaseToolsJavaGate.ps1` | Adaptor Maven comun pentru porțile de dovezi, RAG și certificat. |
| `Build-SignedReleaseApk.ps1` | Modulul aplicației Android Gradle | Construiește dintr-un director de sursă fixat; APK-ul și checksum-ul rămân în afara checkoutului. |
| `Publish-GithubRelease.ps1` | `atf-tests/tools/publish-github-release.mjs` | Jurnal în afara proiectului și candidatului; credențialele rămân în mediul apelantului. |
| `Verify-GithubReleaseDownload.ps1` | `atf-tests/tools/collect-github-release-retention.mjs` | Fișier nou de observație în afara proiectului. |
| `Sync-LocalRelease.ps1` | `atf-tests/tools/verify-release-git-snapshot.mjs` | Chitanță în afara depozitului; se proiectează doar octeții publicați și verificați. |

RAG doar citește intrări sigilate, scrie o recomandare auditabilă și cere revizie umană. Nu apelează operații de build, semnare, Git, GitHub sau sincronizare locală. Fiecare adaptor este o acțiune ulterioară explicită și își verifică intrările, granița de ieșire și codul de terminare.

## Ordinea calificării

1. Lansatorul comun din rădăcină validează checkoutul, rădăcinile temp/dovezi, proprietarii afectați și intrările sursei fixate înaintea probelor de capabilitate, creării directorului de execuție sau stagingului.
2. Colecțiile de componente și guvernanță selectate rulează prin proprietarii existenți și produc dovezi Extent/JUnit legate de sursă în dosarul extern.
3. `Invoke-ReleaseRag.ps1` validează dovezile sigilate și planul, apelează evaluatorul comun read-only, apoi verifică verdictul prin poarta Java a modulului.
4. Revizia umană autorizează separat construirea/semnarea și publicarea. Verificarea descărcării și proiecția locală se fac după publicare.

- `scripts/` din rădăcină deține lansatoarele comune de calificare, Android și laborator de joc.
- `config/` din rădăcină deține politicile și inventarele executabile consumate de mai multe module.
- `atf-tests/tools/` deține motoarele reutilizabile de selecție a suitelor și de dovezi, folosite și de alți proprietari ATF.
- `release-tools/` depinde de aceste interfețe și nu le dublează.
