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

| ID | Remaining boundary | Closure criterion |
|---|---|---|
| GOV-06 | [BrowserUi](../../atf-ui-browser/tests/BrowserUi.java) combines loopback hosting, browser lifecycle, scenario drivers and result emission. | Extract these responsibilities behind small collaborators while preserving scenario IDs, source identity, owned-profile cleanup and standard English Extent output. Keep lifecycle/error components independent of gameplay flows and compare the selected case inventory before and after extraction; no coverage may be removed. |

## Qualification of claims and launchers

[RAG evaluator regressions](../testing/ATF_ReleaseQualification.md#evaluator-regression-tests) verify qualified-source attestations. [Release integrity governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java) separately verifies APK and payload bytes in the recorded dossiers.

| Priority | Owner and gap | Verification level and closure criterion |
|---|---|---|
| P1 | TEST-03: the legacy [BrowserUi](../../atf-ui-browser/tests/BrowserUi.java) host omits current imported modules and reference fonts; its desktop check also assumes the previous reference frame. | Derive the served-asset inventory and MIME types from the delivered site contract, retain rejection of unknown paths/methods, and verify real module/font requests with a controlled HTTP host before browser launch. Replace the old desktop expectation with the current adaptive contract and verify startup plus one move on the compiled site. Keep English scenario narration and the original application-language evidence; legacy results cannot qualify the current renderer until these checks pass. |
| P2 | Launcher coordination: entry points check preconditions inconsistently. | Controlled components for Java, Maven, Gradle, Node and Python must check required capabilities before costly operations and report missing capabilities with source identity. Distinguish the Python installation alias from an executable interpreter. For the [PowerShell ADB module](../../atf-test-android/tools/AndroidDeviceSetup.psm1), verify availability, refusal, timeout and process state separately from parsing and the Java client. |
| P2 | Qualification cost: source/release snapshot preparation still copies and hashes retained reports repeatedly. | Reduce repeated snapshot work without weakening input admission. The implemented [compiler and cleanup recovery contracts](../testing/ATF_Collections.md#maven-compiler-reuse) are regression boundaries, not open implementation tasks. Measure source/release inventory and copying separately from compilation and execution; reconcile recorded stage durations with wall time before claiming further savings. Time the post-compilation snapshot check before output sealing explicitly rather than leaving it between named phases. |
| P1 | Failed-test recovery is only selection-level: one failing JUnit invocation invalidates the complete owner-group receipt, so its already passing siblings cannot be reused safely. | Persist every JUnit unique ID, outcome and source/provenance binding. On restart rerun only failed or incomplete identities, then publish a reconciliation Extent linking the original and retry reports. Reject missing, duplicate, foreign or source-changed identities and any failed retry; never promote by editing the original report. |

[Inline-image retention governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) separately checks for duplicated PNG files beside HTML. It does not prove data autonomy, image readability or APK integrity; the [adapter criterion](ATF_Reporting.md#rpt-android-html-only---audit-delivery-without-auxiliary-copies) remains separate.

## Improving guidance guides

The [documentation editorial grid](../governance/ATF_Documentation.md#editorial-verification-and-evaluation) evaluates documents and their stated limits separately from code maturity. Scores and reviewed inventories belong to the external audit; this register retains only work that can improve comprehension or reproducibility.

| Priority | Owner and area | Closure criterion |
|---|---|---|
| P2 | Complementary English ATF guides still contain inherited game and qualification terminology that needs semantic review. | Review operational terms against public code and the Romanian edition. Require editorial acceptance of purpose, inputs, outcomes and limits for each affected guide before refreshing the bilingual receipt; structural and token checks alone cannot establish meaning. |

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

| Priority | Bounded gap | Closure criterion |
|---|---|---|
| P2 | No measured evidence yet shows that a graph coordinator improves this deterministic release workflow over its existing invocation checkpoints. | Build an isolated shadow pilot that calls existing commands only. Inject interruptions at each stage, then require exact selection, report/JUnit identity and verdict parity, no duplicate side effects, and no signing, publication or lifecycle authority. Measure startup, normal-run overhead and recovery across the same cases; adopt only with at least 20% lower median recovery time and no more than 5% overhead on uninterrupted runs. No model may set or weaken a release verdict. |

<a id="testchanges-admission-before-collection-preparation"></a>

## testChanges admission before collection preparation

The [early-admission contract](../governance/ATF_TestChanges.md#early-source-control-admission) owns canonical proof validation, generation-plan admission and rechecking after staging. Original proof reports, exact Git identities and final governance remain mandatory; selection or compiler reuse does not bypass admission.

| Priority | Separate remaining gap | Closure criterion |
|---|---|---|
| P2 | Qualification metadata still reaches some diagnostics only after snapshot preparation. | Admit the active dossier's E2E inventory, catalog links and editorial limits through a cheap metadata check before the long collection. Preserve failed evidence and prove that a focused recheck cannot qualify a failed primary group; this is separate from source-control proof admission. |


<a id="framework-selection-boundaries"></a>

## Framework selection boundaries

The [independent ATF owners](../testing/ATF_Collections.md#independent-atf-component-owners) separate editorial and payload checks from provenance, mocked Android, mocked GitHub, orchestration and reporting. Each primary collection has a positive class inventory, one canonical Extent and its own source-bound receipt.

The [dependency selection contract](../testing/ATF_Collections.md#executable-dependency-selection) derives consumers from current executable references and explicit repository-observer scopes. Reviewed reader bodies and generated providers bound what the analysis can admit. Unknown inputs, missing populations and changed reader contracts retain complete ATF qualification.

- Markdown-only changes select the documentation owner without replaying independent launcher fixtures. Shared helpers, dependencies and unknown inputs still invalidate their consumers conservatively.
- Reuse retains original JUnit identities, outcomes, comparisons and report hashes. Historical monolithic reports remain immutable and cannot qualify new partition identities.

| Priority | Bounded gap | Closure criterion |
|---|---|---|
| P2 | RAG-26: updating one reviewed reader seal invalidates all ATF collections through the global policy identity. | Separate the topology fingerprint from reviewed reader bodies and their consumer scopes. A bounded seal-only update must select its affected owners. Topology changes, unknown scopes, missing seals and unadmitted reader changes retain complete qualification. Cover these boundaries with positive and negative component cases; preserve original evidence identities and expose selection reasons and stage timings. |
| P1 | RAG-27: cold preparation and repeated filesystem validation can dominate the release before selected tests start. | Time snapshot subphases, post-compile validation and compiler sealing independently; batch filesystem inspection and hashing without weakening link, corruption or source-drift rejection, then compare cold and warm runs while preserving exact manifests and original report identities. |
| P1 | RAG-29: qualification context becomes implicit when audit or publication adds outputs to the catalog observed by the documentation component. | Persist and reconstruct qualified source and catalog identities before promotion. Resume against the same verified context and verify live catalog, links, download and retention separately after publication. Cover wrong or missing context, actual catalog drift and corrupt assets; do not hide real drift or label pre-publication evidence as current-catalog qualification. |
| P2 | RAG-28: the Maven compiler key includes Gradle-only test sources, so an Android test correction invalidates unchanged Maven bytecode. | Derive compilation inputs from declared Maven and build-helper roots while retaining POMs, resources, generated inputs, tools and unknown dependencies; keep the complete execution inventory and byte-seal rejection tests. |

Measure snapshot preparation, compilation and execution separately. A focused diagnostic remains distinct from qualification of a complete primary collection; failure recovery within an owner is covered by the open invocation-reconciliation item above.

## Reporting schema admission

The evaluator applies one reporting admission rule before readiness and preserves historical manifests. Accepted schema/report combinations are:

- Versions 1 and 2 may omit `qualification.reportSchema` only while every declared component and E2E report is HTML.
- Version 3 and later declare `ATF_EXTENT_HTML_INLINE_BASE64_V1`.
- XML-only, unknown, missing-modern or mixed-format declarations block readiness. JUnit XML remains a temporary oracle, never the canonical report.

The policy is in `config/release-platform-rag-policy.json`, admission is enforced by `atf-tests/tools/evaluate-release-rag.mjs`, and component coverage belongs to `ReleaseRagProvenanceComponentTest` and `ReleaseRagPlatformGovernanceTest`.
