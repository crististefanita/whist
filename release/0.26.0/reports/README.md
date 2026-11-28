# Release qualification 0.26.0

## Result summary

- Candidate APK: WhistLab-0.26.0.apk
- APK SHA-256: `ba4fad67af1fcd9758fffdd67bddad0ef5113120ed9c571bab636d2d876cb692`
- Component collections: 18; 17 reused and 1 recovered after a bounded timeout.
- Unique component invocations: 1746; zero failures, errors or skips.
- Android E2E: 7/7 PASS, reused against byte-identical signed APK SHA-256. No emulator rerun was needed.
- Extent screenshots are inline Base64; no loose screenshot directory is included.

## Component reports

- [atf-test-android](./component/atf-test-android/component-report.html) · [execution receipt](./component/atf-test-android/execution.json)
- [atf-tests](./component/atf-tests/component-report.html) · [execution receipt](./component/atf-tests/execution.json)
- [atf-tests-android](./component/atf-tests-android/component-report.html) · [execution receipt](./component/atf-tests-android/execution.json)
- [atf-tests-documentation](./component/atf-tests-documentation/component-report.html) · [execution receipt](./component/atf-tests-documentation/execution.json)
- [atf-tests-github](./component/atf-tests-github/component-report.html) · [execution receipt](./component/atf-tests-github/execution.json)
- [atf-tests-orchestration](./component/atf-tests-orchestration/component-report.html) · [execution receipt](./component/atf-tests-orchestration/execution.json)
- [atf-tests-provenance](./component/atf-tests-provenance/component-report.html) · [execution receipt](./component/atf-tests-provenance/execution.json)
- [atf-tests-reporting](./component/atf-tests-reporting/component-report.html) · [execution receipt](./component/atf-tests-reporting/execution.json)
- [atf-validation](./component/atf-validation/component-report.html) · [execution receipt](./component/atf-validation/execution.json)
- [release-tools](./component/release-tools/component-report.html) · [execution receipt](./component/release-tools/execution.json)
- [whist-lab-app-achievements](./component/whist-lab-app-achievements/component-report.html) · [execution receipt](./component/whist-lab-app-achievements/execution.json)
- [whist-lab-app-atf-card-table-ui](./component/whist-lab-app-atf-card-table-ui/component-report.html) · [execution receipt](./component/whist-lab-app-atf-card-table-ui/execution.json)
- [whist-lab-app-bidding](./component/whist-lab-app-bidding/component-report.html) · [execution receipt](./component/whist-lab-app-bidding/execution.json)
- [whist-lab-app-card-table-core](./component/whist-lab-app-card-table-core/component-report.html) · [execution receipt](./component/whist-lab-app-card-table-core/execution.json)
- [whist-lab-app-commands](./component/whist-lab-app-commands/component-report.html) · [execution receipt](./component/whist-lab-app-commands/execution.json)
- [whist-lab-app-display](./component/whist-lab-app-display/component-report.html) · [execution receipt](./component/whist-lab-app-display/execution.json)
- [whist-lab-app-history](./component/whist-lab-app-history/component-report.html) · [execution receipt](./component/whist-lab-app-history/execution.json)
- [whist-lab-app-rules](./component/whist-lab-app-rules/component-report.html) · [execution receipt](./component/whist-lab-app-rules/execution.json)

## Android E2E reports

- [history-release-sanity](./e2e/history-release-sanity/report.html) · [execution receipt](./e2e/history-release-sanity/execution.json)
- [responsive-ui-sanity](./e2e/responsive-ui-sanity/report.html) · [execution receipt](./e2e/responsive-ui-sanity/execution.json)
- [signed-release-achievements](./e2e/signed-release-achievements/report.html) · [execution receipt](./e2e/signed-release-achievements/execution.json)
- [signed-release-auto-game](./e2e/signed-release-auto-game/report.html) · [execution receipt](./e2e/signed-release-auto-game/execution.json)
- [signed-release-bidding](./e2e/signed-release-bidding/report.html) · [execution receipt](./e2e/signed-release-bidding/execution.json)
- [signed-release-home-profile](./e2e/signed-release-home-profile/report.html) · [execution receipt](./e2e/signed-release-home-profile/execution.json)
- [signed-release-smoke](./e2e/signed-release-smoke/report.html) · [execution receipt](./e2e/signed-release-smoke/execution.json)

## Audit trail

- [RAG plan](./qualification-plan.json) binds source commit, diff selection, component selections and required E2E flows.
- [Source input manifest](./apk-inputs.json) pins 1619 files at commit `6f36b0d6785acde9611209a8f1a09eee5879aa9f`.
- [Component reuse map](./component/component-reuse.json) and per-origin snapshots preserve evidence lineage. The recovered provenance suite includes original and retry attempts.
- [Build and signer diagnostics](./diagnostics/) bind the signed APK identity.
- Canonical Extent: [ATF component report](./component/atf-tests/component-report.html).
