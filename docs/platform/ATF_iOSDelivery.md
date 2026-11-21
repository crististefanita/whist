# Starea livrării iOS

## Pe scurt

APK-ul Whist este un pachet Android și nu se poate instala pe iPhone sau iPad.
Adaptorul iOS este o aplicație SwiftUI separată, care adaptează motorul comun de
joc Java prin J2ObjC. Motorul comun rămâne sursa regulilor, AI-ului, licitării,
punctajului și comportamentului salvărilor.

## Starea release-ului

- Livrarea iOS este pusă în așteptare; politica executabilă consemnează
  `platforms.ios.status = PARKED` și `activeReleaseGate = false`.
- Planurile release-urilor curente califică numai Android. Un APK Android nu este
  dovadă de suport iOS.
- Workflow-ul nativ păstrat și porțile iOS definite nu fac parte din calificarea
  release-urilor active.

## Criterii de reactivare

Reactivarea cere o modificare revizuită a politicii și porți native de livrare
calificate. Înainte de activarea platformei, verificați toolchain-ul Apple și
disponibilitatea runner-elor, restabiliți o cale completă de teste native, produceți
artefactul Apple declarat și validați separat semnarea și distribuția față de
Android. Politica trebuie apoi să declare explicit iOS ca platformă activă de
release; documentația sau un release Android reușit nu pot face acest lucru implicit.

Workflow-ul [iOS](../../.github/workflows/ios.yml) păstrat poate fi invocat
separat pentru verificări de inginerie. O astfel de rulare nu califică un release
cât timp politica marchează iOS drept pus în așteptare.

[Versiuni Apple](https://developer.apple.com/news/releases/) ·
[Cerințe de sistem Xcode](https://developer.apple.com/xcode/system-requirements/) ·
[Proiectul și limitele J2ObjC](https://github.com/google/j2objc)

## Limitele livrării

Când lucrul va fi reluat, construiți și testați aplicația nativă pe un commit
revizuit, păstrați rezultatul simulatorului, apoi calificați separat semnarea,
instalarea pe dispozitive fizice și canalul Apple de distribuție ales. Păstrați
`xcarchive` sau IPA-ul rezultat și dovezile lui distincte de APK-ul Android și
receipt-urile release-ului.

[Livrarea Android](ATF_AndroidDelivery.md) · [Adaptorul iOS](../../ios/README.md)
