# Re-use of governance and Code Cohesion

## Overview

The project owns Android-boundary and adapter checks. Reusable contracts are requested through the [library ownership register](README.md), without copying producer implementations into consumers.

## Cost of syntactic inspection

| Priority | Observation and source | Closure criterion |
|---|---|---|
| P2 | The [syntax parser](../../atf-tests/src/test/java/atf/tests/governance/source/JavaTestDeclarations.java) already disables annotation processing and uses empty class and processor paths. The cost of this configuration has not been measured against the same inventory. | Measure parsing cost with an identified environment and inventory. Preserve independent controls for composed annotations, ambiguous imports, syntax errors and parser lifecycle; do not claim a speedup from configuration alone. |


## Cohesion and package development

[Executable policy](../../config/atf-governance-policy.json) enforces the package limit without exceptions. [Package responsibilities](../governance/ATF_Packages.md) separates public APIs, shared support and test selections.

[Quality governance](../governance/README.md) · [Good documentation practices](../governance/ATF_Documentation.md)

## Qualification of claims and launchers

[RAG evaluator regressions](../testing/ATF_ReleaseQualification.md#evaluator-regression-tests) verify qualified-source attestations. [Release integrity governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java) separately verifies APK and payload bytes in the recorded dossiers.

| Priority | Owner and gap | Verification level and closure criterion |
|---|---|---|
| P2 | Launcher coordination: entry points check preconditions inconsistently. | Controlled components for Java, Maven, Gradle, Node and Python must check required capabilities before costly operations and report missing capabilities with source identity. Distinguish the Python installation alias from an executable interpreter. For the [PowerShell ADB module](../../atf-test-android/tools/AndroidDeviceSetup.psm1), verify availability, refusal, timeout and process state separately from parsing and the Java client. |
| P2 | Qualification cost: snapshot preparation still copies and hashes retained reports repeatedly. Nested Node and PowerShell checks must use the same validated external evidence root as the suite runner; falling back to an unrelated OS temp path makes isolated runs fragile. | The runner validates or creates the configured external root before collection, rejects paths inside the checkout, and passes the root to nested publication tests. The inventory-only path records the mandatory `DOCUMENTATION_EDITIONS` gate while preserving unrelated Maven evidence. Maven output admission now has its own helper, separate from snapshot recovery; controls reject altered classpaths, foreign identities, malformed or missing receipts and changed classes. Continue measuring source/release hashing, copying, JVM preparation and execution separately; narrow other owners only with immutable-input and negative-control proof. |
| P1 | Failed-test recovery is only selection-level: one failing JUnit invocation invalidates the complete owner-group receipt, so its already passing siblings cannot be reused safely. | Persist every JUnit unique ID, outcome and source/provenance binding. On restart rerun only failed or incomplete identities, then publish a reconciliation Extent linking the original and retry reports. Reject missing, duplicate, foreign or source-changed identities and any failed retry; never promote by editing the original report. |

[Inline-image retention governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) separately checks for duplicated PNG files beside HTML. It does not prove data autonomy, image readability or APK integrity; the [adapter criterion](ATF_Reporting.md#rpt-android-html-only---audit-delivery-without-auxiliary-copies) remains separate.

## Improving guidance guides

The [documentation editorial grid](../governance/ATF_Documentation.md#editorial-verification-and-evaluation) evaluates documents and their stated limits separately from code maturity. Scores and reviewed inventories belong to the external audit; this register retains only work that can improve comprehension or reproducibility.

| Priority | Owner and area | Closure criterion |
|---|---|---|
| P2 | Distributed library guides: producer-side sample reuse. | [Local example acceptance](../platform/ATF_Integration.md#verification-of-examples-in-guides) separates compilation, execution and verdict. Promote a generic evidence contract in the shared-ownership register and synchronize consumer guides. Producer regression and each consumer's acceptance remain separate. |

## Download via browser

[Distribution contract](../platform/ATF_AndroidDelivery.md#automatic-distribution-checks) distinguishes API checks, completed browser downloads and anonymous access. Record browser verification and the received file's hash in the [active release dossier](../../release/README.md).

| Priority | Bounded gap | Closure criterion |
|---|---|---|
| P2 | Authenticated browser verification depends on an operator and a working GitHub session. | After publication, verify the version URL, received file and SHA-256, then open the audit at the commit linked in the release notes. Record anonymous responses and local browser restrictions separately. |

Android Download Manager transfer and certificate checks belong to the tested release dossier. [Android transfer contract](../platform/ATF_AndroidDelivery.md#separating-access-from-android-transfer) defines the required data; [anonymous public access](../platform/ATF_AndroidDelivery.md#anonymous-access-and-public-route) has a separate closure criterion.

## Native acceptance of distribution

| Priority | Bounded gap | Closure criterion |
|---|---|---|
| P2 | Native transfer evidence includes sources in Extent, but launching it still requires an operator-managed temporary operation. | Version a native adapter that reads candidate identity, owns its Android permissions and separates access from transfer. Inject 404, truncation, wrong hash or CRC, absent or mismatched certificates and timeouts. Keep original comparisons, causes and durations, omit signed redirects, and remove only owned resources. One native transfer qualifies delivery; do not repeat it for every UI resolution. |

## Release qualification boundaries

Native source and validator separation belongs to the [qualification contract](../testing/ATF_ReleaseQualification.md#native-execution-and-validator-provenance). Reuse remains conditional on unchanged runtime, scenario, APK and original evidence.

## LangGraph shadow pilot

The production path remains a deterministic PowerShell stage runner with source-bound Maven/Gradle checkpoints and a read-only Node evaluator. Component tests already cover interruption, changed inputs, partial checkpoints and evidence rejection. [Release qualification](../testing/ATF_ReleaseQualification.md#orchestration-and-the-langgraph-adoption-gate) owns the stable contracts for resume and platform-aware selection.

LangGraph offers explicit nodes, shared state and checkpoint boundaries, but it does not shorten compilation. A failed node can run again from its beginning, so a pilot must prove side-effect idempotency and bind every resume to the original source, command and evidence receipts. See the [official LangGraph workflow guidance](https://docs.langchain.com/oss/javascript/langgraph/thinking-in-langgraph).

| Pilot check | Observed result | Decision |
|---|---|---|
| Process restart and selector | Persisted plan resumed with exact selector parity. | Pass |
| Changed source | Rejected before handoff. | Pass |
| Side effects and adoption gate | No commands ran; release-level report/JUnit parity and timing remain unmeasured. | Keep LangGraph out of production. |

| Priority | Bounded gap | Closure criterion |
|---|---|---|
| P2 | No measured evidence yet shows that a graph coordinator improves this deterministic release workflow over its existing invocation checkpoints. | Build an isolated shadow pilot that calls existing commands only. Inject interruptions at each stage, then require exact selection, report/JUnit identity and verdict parity, no duplicate side effects, and no signing, publication or lifecycle authority. Measure startup, normal-run overhead and recovery across the same cases; adopt only with at least 20% lower median recovery time and no more than 5% overhead on uninterrupted runs. No model may set or weaken a release verdict. |

## testChanges admission before collection preparation

Reject missing or stale source-control evidence before copying caches, compiling or launching a long collection. A late governance failure still protects correctness, but wastes the accepted preparation and unrelated test work.

- Discover the required control IDs from the reviewed Git diff and validate their external evidence root explicitly.
- Use the same proof validator for admission and final governance: control ID, base, commit, diff, positive and negative selectors, and original report hash must match.
- Generate unresolved controls with the existing compilation before starting the primary collection; pass the resulting root explicitly to the suite process.
- Cover absent configuration, another commit, a missing negative control, altered report bytes and a source change between admission and execution. Missing evidence must fail before Maven; changed sources must still invalidate admission.
- Measure rejected preparation separately from accepted execution. Do not replace a failed original report with a passing neighbour or edit an execution receipt.

- Make the review base an explicit input for local and isolated launches. Check the control plan before dependency preparation; an incompatible historical base must fail with the same diagnostic before compilation.

The isolated workflow now receives the review base explicitly. Admission of unresolved source-control evidence before dependency preparation remains open; selecting the right base alone does not close that boundary.

- Admit control command syntax, the active dossier's explicit E2E inventory, catalog links and editorial limits through a cheap check before the long collection. Preserve the failed report; a focused recheck does not qualify a failed primary group.

## Reporting schema admission

The evaluator applies one reporting admission rule before readiness and preserves historical manifests. Accepted schema/report combinations are:

- Versions 1 and 2 may omit `qualification.reportSchema` only while every declared component and E2E report is HTML.
- Version 3 and later declare `ATF_EXTENT_HTML_INLINE_BASE64_V1`.
- XML-only, unknown, missing-modern or mixed-format declarations block readiness. JUnit XML remains a temporary oracle, never the canonical report.

The policy is in `config/release-platform-rag-policy.json`, admission is enforced by `atf-tests/tools/evaluate-release-rag.mjs`, and component coverage belongs to `ReleaseRagProvenanceComponentTest` and `ReleaseRagPlatformGovernanceTest`.
