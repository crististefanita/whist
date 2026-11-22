# Whist 0.23.1 qualification audit

## Result

The source-diff plan selected one component/governance suite and reused nine unchanged groups from source-bound snapshots. The selected suite passed 563/563. The complete audited selection contains 1529 invocations, with zero failures, errors or skips.

## APK identity

| Field | Value |
|---|---|
| APK | `WhistLab-0.23.1.apk` |
| Application ID | `ro.whistlab.app` |
| Version code | 48 |
| Source commit | `9cbd9241a8fe1ee29573ce7c79e3c437961d8ac9` |
| APK size | 1033649 bytes |
| APK SHA-256 | `3db4700170713369a35f3fb225c720d0412195c73b9aaca94d5e9a8183fb5752` |
| APK CRC-32 | `6288ee71` |
| Signing certificate SHA-256 | `b80c45a9b52ef0c83dcb929ba4aeb8775c836e5e6d587324d7ec725c079fb4c3` |
| Android SDK | min 24, target 36 |

## Component and governance suites

| Group | Decision | Tests | Extent |
|---|---:|---:|---|
| atf-validation | REUSE | 19 | [Report](component/atf-validation/component-report.html) · [Receipt](component/atf-validation/execution.json) |
| atf-test-android | REUSE | 277 | [Report](component/atf-test-android/component-report.html) · [Receipt](component/atf-test-android/execution.json) |
| atf-tests | RUN | 563 | [Report](component/atf-tests/component-report.html) · [Receipt](component/atf-tests/execution.json) |
| whist-lab-app-bidding | REUSE | 255 | [Report](component/whist-lab-app-bidding/component-report.html) · [Receipt](component/whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | REUSE | 123 | [Report](component/whist-lab-app-commands/component-report.html) · [Receipt](component/whist-lab-app-commands/execution.json) |
| whist-lab-app-display | REUSE | 107 | [Report](component/whist-lab-app-display/component-report.html) · [Receipt](component/whist-lab-app-display/execution.json) |
| whist-lab-app-history | REUSE | 61 | [Report](component/whist-lab-app-history/component-report.html) · [Receipt](component/whist-lab-app-history/execution.json) |
| whist-lab-app-achievements | REUSE | 28 | [Report](component/whist-lab-app-achievements/component-report.html) · [Receipt](component/whist-lab-app-achievements/execution.json) |
| whist-lab-app-atf-card-table-ui | REUSE | 56 | [Report](component/whist-lab-app-atf-card-table-ui/component-report.html) · [Receipt](component/whist-lab-app-atf-card-table-ui/execution.json) |
| whist-lab-app-rules | REUSE | 40 | [Report](component/whist-lab-app-rules/component-report.html) · [Receipt](component/whist-lab-app-rules/execution.json) |

## E2E scope

**NOT_RUN**, by explicit diff-based selection. The pinned changes affect RAG/governance, documentation and Android version metadata; they do not change game behavior, navigation, layout, input or visual contracts. No E2E group is required by the evaluated diff. Earlier E2E results are not relabeled as evidence for this APK.

## Source and evidence

- [Component selection, reuse and Extent contract](component/README.md)
- [Aggregate component execution receipt](component/source-component-run.json)
- [APK source inventory](apk-inputs.json)
- [Pinned qualification plan](qualification-plan.json)
- Build, signer and identity receipts are in [diagnostics](diagnostics/).

[Release catalog](../../README.md) · [Shared testing strategy](../../../docs/testing/README.md)
