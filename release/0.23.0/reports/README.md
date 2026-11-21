# Whist 0.23.0 qualification audit

## Qualification status

The pinned source snapshot passed the selected component, governance and Android acceptance suites. The read-only RAG verdict is the final pre-publication check; it does not publish or promote this candidate.

## Identity

| Item | Value |
|---|---|
| APK | `WhistLab-0.23.0.apk` |
| versionCode | 47 |
| Source commit | ``8b8b9d3681b2cdc44ef89d74241dc86cce34669c`` |
| APK size | 1,033,649 bytes |
| APK SHA-256 | `f77a65d25f7c1249409d10de3379751592ca018b350394a3ef852bc73e32665a` |
| Android signing certificate SHA-256 | `b80c45a9b52ef0c83dcb929ba4aeb8775c836e5e6d587324d7ec725c079fb4c3` |
| Device profile | Android API 28; 1080×1920 at 440 dpi |

## Component and governance suites

| Selection | Invocations | Decision | Evidence |
|---|---:|---|---|
| atf-validation | 19 | REUSE | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | REUSE | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 555 | RUN | [Canonical Extent](component/atf-tests/component-report.html) |
| whist-lab-app-bidding | 255 | REUSE | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | REUSE | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | REUSE | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | REUSE | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-achievements | 28 | REUSE | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-atf-card-table-ui | 56 | REUSE | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | REUSE | [Extent](component/whist-lab-app-rules/component-report.html) |

Total: **1,521** component and governance invocations. Reused results retain their original source fingerprints and execution receipts. The one newly executed Maven selection passed 555/555.

## Android acceptance flows

All seven flows ran on the identified signed APK and passed with 1 test, 0 failures, 0 errors and 0 skips. Each suite has its own Extent report with inline Base64 screenshots, execution receipt and source provenance.

| Flow | Result |
|---|---|
| [Signed APK smoke](e2e/signed-release-smoke/report.html) | PASS 1/1 |
| [Completed hand history](e2e/history-release-sanity/report.html) | PASS 1/1 |
| [Responsive display](e2e/responsive-ui-sanity/report.html) | PASS 1/1 |
| [Achievements](e2e/signed-release-achievements/report.html) | PASS 1/1 |
| [AutoGame hand boundary](e2e/signed-release-auto-game/report.html) | PASS 1/1 |
| [Bidding recommendation and saved contract](e2e/signed-release-bidding/report.html) | PASS 1/1 |
| [Home, profile and language restore](e2e/signed-release-home-profile/report.html) | PASS 1/1 |

The achievements flow's first attempt ended with a transient UI Automator hierarchy-capture error before any assertion failure. That isolated flow alone was retried; the retry passed 1/1. Its final accepted report is the single canonical suite report above. Each run used a disposable Android user and removed it after execution; the original user 0 data was preserved.

## Provenance and diagnostics

- [Component execution and reuse receipt](component/source-component-run.json)
- [Component source snapshots](component/snapshots/)
- [Build, APK identity and signing diagnostics](diagnostics/)
- [RAG qualification plan](qualification-plan.json)

The APK checksum sidecar is included beside the binary. The post-publication download receipt is intentionally pending until the declared GitHub release has been published and downloaded for verification.
## Source and APK identity

The source snapshot was rebuilt at commit ``8b8b9d3681b2cdc44ef89d74241dc86cce34669c``. The signed output is byte-identical to the APK used for the seven accepted Android flows (SHA-256 `f77a65d25f7c1249409d10de3379751592ca018b350394a3ef852bc73e32665a`), so those native reports remain valid. Their original execution-source commit is retained in each receipt; the only intervening source change is the reviewed bilingual-documentation record.
