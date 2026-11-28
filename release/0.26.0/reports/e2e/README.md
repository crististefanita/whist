# Android end-to-end evidence

The seven required flows were not rerun in this RAG iteration. Their signed APK SHA-256 is byte-identical to the candidate, and the execution receipts retain each original source snapshot, device profile and result. Current RAG admission verifies the source delta and permits reuse only because it is limited to release verification and test orchestration.

## Candidate identity

- Candidate APK SHA-256: `ba4fad67af1fcd9758fffdd67bddad0ef5113120ed9c571bab636d2d876cb692`
- Candidate source commit: `6f36b0d6785acde9611209a8f1a09eee5879aa9f`
- Android API: 28
- Display profile: 1080×1920 at 440 dpi
- Evidence schema: inline Base64 Extent captures; no separate screenshot files.

## Flows

- [history-release-sanity — report](./history-release-sanity/report.html) · [execution receipt](./history-release-sanity/execution.json) · original source proof: [history-release-sanity/source-provenance.json](./history-release-sanity/source-provenance.json)
- [responsive-ui-sanity — report](./responsive-ui-sanity/report.html) · [execution receipt](./responsive-ui-sanity/execution.json) · original source proof: [responsive-ui-sanity/source-provenance.json](./responsive-ui-sanity/source-provenance.json)
- [signed-release-achievements — report](./signed-release-achievements/report.html) · [execution receipt](./signed-release-achievements/execution.json) · original source proof: [signed-release-achievements/source-provenance.json](./signed-release-achievements/source-provenance.json)
- [signed-release-auto-game — report](./signed-release-auto-game/report.html) · [execution receipt](./signed-release-auto-game/execution.json) · original source proof: [signed-release-auto-game/source-provenance.json](./signed-release-auto-game/source-provenance.json)
- [signed-release-bidding — report](./signed-release-bidding/report.html) · [execution receipt](./signed-release-bidding/execution.json) · original source proof: [signed-release-bidding/source-provenance.json](./signed-release-bidding/source-provenance.json)
- [signed-release-home-profile — report](./signed-release-home-profile/report.html) · [execution receipt](./signed-release-home-profile/execution.json) · original source proof: [signed-release-home-profile/source-provenance.json](./signed-release-home-profile/source-provenance.json)
- [signed-release-smoke — report](./signed-release-smoke/report.html) · [execution receipt](./signed-release-smoke/execution.json) · original source proof: [signed-release-smoke/source-provenance.json](./signed-release-smoke/source-provenance.json)
