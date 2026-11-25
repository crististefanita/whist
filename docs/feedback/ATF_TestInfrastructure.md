# Calificarea infrastructurii locale de testare

## Pe scurt

Consumatorul deține validarea API, procesele și adopția locală; [registrul responsabilităților](README.md#suportul-comun-al-testelor) rutează primitivele Kit. Contractele implementate rămân în README-urile modulelor; biblioteca binară și acceptanța Android au verificări separate.

## Concurența Kitului

[Suportul comun](README.md#suportul-comun-al-testelor) deține primitivele și probele generice. Consumatorul păstrează acceptanța adaptorului Android; transferul nu califică paralelismul și nu închide criteriul.

## Costul validării API

[Contractul validării](../../atf-validation/README.md#cost-și-limite) deține limitele structurale și bugetul comun al prevalidării/evaluării. Unitățile acoperă limitele exacte; componentele contractului public păstrează căile, politica și izolarea evaluărilor. Alocarea parserului și durata regex Java rămân explicit în afara acestui buget.

## Stabilitatea verificării proceselor

### Pregătirea profilului Android temporar

Deținerea persistentă și recuperarea aparțin [contractului de calificare](../testing/ATF_ReleaseQualification.md#recuperarea-utilizatorului-temporar). Componentele controlate califică întreruperea și verificările de identitate; acceptanța profilului nativ rămâne separată.

[Componenta ADB simulată](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidReleasePreflightComponentTest.java) separă pregătirea fixture-ului de verdictul transportului. Un port controlat al procesului de producție verifică ordinea și cauzele originale fără așteptări artificiale; proba limitată a lansatorului cere intrarea observată în fixture înainte de a atribui un timeout transportului ADB.

[Kitul testelor](../../lib/README.TestKit.md#scope-și-integrare) · [Validarea API](../../atf-validation/README.md) · [Guvernanță](ATF_Governance.md)

## Compoziția acțiunilor driverului

[Contractul acțiunilor Android](../../atf-test-android/src/test/README.md#action-dispatch-and-ownership) separă selecția ierarhiei, execuția comenzilor și fazele deținute de apelant. Componentele controlate califică ordinea, duratele, anularea și cauzele originale; acceptanța nativă rămâne în registrul Android.

## Calificarea costului RAG

| Etapă | Criteriu rămas |
|---|---|
| Android | Măsurați separat așteptarea rezervării, pregătirea profilului, transportul și fluxul. Confruntați fereastra activă pentru a atribui corect întreruperile terțe. [Acceptanța Android](ATF_Android.md) |
| Rapoarte | Calificați costul colectării și predării separat de execuție, la același inventar de invocări. [Registrul raportării](ATF_Reporting.md) |

Contractul de [reluare RAG](../testing/ATF_ReleaseQualification.md#reluarea-etapelor-validate) deține checkpoint-urile și reutilizarea snapshot-ului. Cronologiile concrete aparțin dosarului extern al execuției.
