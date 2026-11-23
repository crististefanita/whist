# Testele de componentă ale prezentării portabile

## Pe scurt

Componentele exercită implementarea portabilă reală cu conținut, dimensiuni și colaboratori controlați explicit. Fiecare componentă înregistrează intrările și aserțiunile evaluate așteptat/observat în Extent. Matricele de continuitate și rezoluție nu conțin poze.

## Organizare

Identitățile Java originale ale testelor rămân neschimbate după extragere. Pachetele urmează responsabilitatea de producție: cărți, mâini, meniuri, punctaj, locuri, masă și ritmul turelor. Componentele de graniță resping cuplarea între module și observațiile invalide.

Setul separat `unitTest` verifică normalizarea izolată și proprietatea imuabilă numai prin JUnit. Cazurile sale sunt declarate unitare în același inventar și nu devin invocări de componentă Extent.

## Contractele adaptoarelor

- Fixture-urile core acceptă regiuni identificate, imuabile, în coordonate logice; un singur oracol compară identitățile, ordinea și limitele.
- Măsurătorile Android elimină densitatea și insets; măsurătorile browser elimină originea și scara scenei.
- Programele și cazurile legale specifice jocului rămân la furnizorul jocului.

[Strategia de testare](../../../../docs/testing/README.md) · [Matrice detaliată](../../../../docs/testing/ATF_TestingGuide.md) · [Contractul comun al fazelor](../../../../docs/testing/ATF_TableContinuity.md)
