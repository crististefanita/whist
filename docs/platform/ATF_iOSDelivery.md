# Compatibilitate și livrare iOS

## Pe scurt

APK-ul Whist Lab este un pachet Android și nu se instalează pe iPhone sau iPad.
Proiectul iOS este o aplicație separată cu interfață SwiftUI și folosește motorul
Java comun prin traducerea J2ObjC. Motorul original continuă să fie sursa regulilor
de joc, AI, licitare, scor și salvări; adaptorul iOS nu implementează o a doua
variantă a acestor reguli.

Proiectul iOS este pus în așteptare și nu intră în calificarea release-urilor active. Valorile
din fișierele proiectului și workflow sunt setări de implementare, nu o matrice curentă
de suport sau o declarație de compatibilitate. Verificați versiunile Xcode și simulator
susținute de Apple înainte de reluarea platformei; APK-ul Android nu dovedește suportul iOS.
[Versiunile Apple](https://developer.apple.com/news/releases/) ·
[Matricea Xcode și SDK](https://developer.apple.com/xcode/system-requirements/)

## Calificarea pe GitHub Actions

Un runner macOS găzduit poate asigura build și teste la reluarea lucrului iOS. Această
cale separată nu este o condiție pentru release-ul Android activ.

- Pentru repository-uri publice, runner-ele standard sunt gratuite și nelimitate.
- Pentru repository-uri private, consumul folosește minutele incluse, apoi se taxează la tariful planului. Verificați vizibilitatea și facturarea înainte de a estima costul.
- Workflow-ul fixează toolchain-ul în loc să aleagă `latest`; verificați disponibilitatea
  curentă a runner-ului și actualizați aceste versiuni înainte de reactivarea traseului.
[Runner-e și condiții GitHub](https://docs.github.com/en/actions/reference/runners/github-hosted-runners) ·
[Imaginea curentă macOS Xcode 27](https://github.com/actions/runner-images/blob/main/images/macos/xcode-27-arm64-Readme.md) ·
[Preview Xcode 27](https://github.blog/changelog/2026-09-10-xcode-27-runner-image-now-runs-on-macos-27/)

Workflow-ul [iOS](../../.github/workflows/ios.yml) păstrat poate verifica un commit când este folosit explicit:

1. testele JVM ale motorului și adaptorului nativ;
2. traducerea surselor Java cu J2ObjC;
3. build-ul aplicației pentru simulator;
4. smoke test-ul Swift în simulatorul iOS 27.2.

J2ObjC 3.1 este fixat în workflow și construit din sursă, deoarece distribuția
precompilată nu mai este publicată de proiect. J2ObjC este beta și traduce logica,
nu interfața; SwiftUI rămâne stratul UI nativ. Build-ul din runner califică
compatibilitatea compilării și a testului automat pentru iOS 27.2, nu fiecare
versiune intermediară și nici experiența pe dispozitive fizice.
[J2ObjC — proiect și limite](https://github.com/google/j2objc) ·
[Construirea J2ObjC](https://developers.google.com/j2objc/guides/building-j2objc)

## Pașii de livrare

1. La reluarea lucrului iOS, rulați workflow-ul `iOS simulator qualification` pe commitul sursă revizuit.
2. Verificați rezultatele și păstrați `xcresult`-ul atașat acelui run.
3. Pentru TestFlight sau App Store, arhivați cu Xcode, semnați cu identitatea
   organizației și validați instalarea pe dispozitive iPhone și iPad.
4. Publicați separat aplicația iOS prin fluxul Apple. APK-ul și hash-ul Android nu
   servesc drept asset ori dovadă de instalare iOS.

Runner-ul simulator construiește aplicația fără semnare Apple. Crearea unui IPA
distribuibil și încărcarea lui în TestFlight/App Store cer un Apple Developer
Team, certificate, profil de provisioning și pași de release Apple; aceste
acreditări nu sunt stocate în repository sau în workflow-ul public.

[Livrarea Android](ATF_AndroidDelivery.md) · [Proiectul iOS](../../ios/README.md)
