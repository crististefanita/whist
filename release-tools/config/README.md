# Configurația release-ului

## Pe scurt

Acest director deține politica executabilă de release consumată de modulul Maven `release-tools`.

Politica guvernează domeniul RAG, prospețimea dovezilor, identitatea APK-ului și publicarea. Colecțiile comune de teste, regulile de guvernanță și strategia jocului rămân în directorul `config/` din rădăcină, deoarece sunt consumate de mai multe module.

- Fișierul politicii: `release-platform-rag-policy.json`.
- La schimbarea cititorului politicii sau a dependenței, actualizează inventarul dependențelor componentei și sigiliile cititorilor.
