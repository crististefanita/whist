# Semnarea ediției Android de test

## Pe scurt

Certificatul Whist public și amprenta sa SHA-256 identifică semnatarul ediției de test. Compară amprenta cu rezultatul `apksigner verify --print-certs` înainte de calificarea unei actualizări.

Keystore-ul privat rămâne local și ignorat de Git. Modulul de release îl furnizează explicit invocării Gradle izolate, astfel încât semnarea nu depinde de profilul Android al unui utilizator anume. [Ghidul de livrare Android](../../docs/platform/ATF_AndroidDelivery.md) explică identitatea semnării și a actualizărilor.

## Calificarea sursei

| Intrare | Regula de export |
|---|---|
| `.gitignore`, `README.md`, `whist-android-test.pem`, `whist-android-test.fingerprint` | Contract public de semnare: inventariat, hash-uit și copiat. |
| Alte intrări din `.atf-signing` | Material privat local: exclus înainte de citirea conținutului sau traversarea directoarelor. |
| Surse din afara `.atf-signing` | Se aplică regulile normale ale sursei; această graniță nu este un scanner general de secrete. |

- [Politica comună a surselor](../../scripts/AtfSigningSourcePolicy.ps1) ține cheile și copiile locale de siguranță în afara snapshot-urilor, rapoartelor și asset-urilor release.
- O cheie locală poate exista, lipsi sau fi rotită fără să schimbe identitatea sursei. Calificarea release-ului se oprește dacă cheia de semnare nu este disponibilă; cheia nu se regenerează și nu se înlocuiește. Modificarea metadatelor publice sau a altei surse schimbă identitatea.
- Copierea verifică întregul manifest înainte de scriere și refuză căile private, aliasurile canonice și căile indirecte. Semnarea folosește explicit cheia locală; calificarea nu o șterge și nu o modifică.
