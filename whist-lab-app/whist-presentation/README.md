# Prezentarea Whist

## Pe scurt

Proiecții portabile ale jocului, consumate de Android și Web. Modulul păstrează etichetele Whist, configurarea, conținutul public al mesei, realizările, rândurile de punctaj și codec-urile checkpointurilor în afara rendererelor de platformă.

## Limitele dependențelor

Modulul depinde de `game-core` și `card-table-core`. Nu depinde de Android, DOM sau raportare. Numele istorice ale pachetelor Java rămân stabile pentru compatibilitatea stării salvate și a consumatorilor; proprietatea lor fizică nu mai implică o dependență de aplicația Android.

API-ul JSON este disponibil numai la compilare: Android oferă API-ul sistemului, iar launcherul browser oferă biblioteca JVM fixată. Catalogul realizărilor rămâne generat din același YAML Whist și validat față de aceleași enum-uri de execuție.

## Verificare

Cazurile de componentă Whist existente își păstrează identitățile și exercită modulul prin adaptoarele aplicației și browserului. Matricea UI comună aparține core; programele jocului, eligibilitatea și punctajul rămân la joc.

[Matrice modul × nivel](../../docs/testing/ATF_TestingGuide.md) · [Componente Whist](../../docs/testing/ATF_DisplayComponents.md) · [Adaptor browser](../../atf-ui-browser/README.md)
