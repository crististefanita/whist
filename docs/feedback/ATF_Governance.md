# Re-use of governance and Code Cohesion

## Overview

The project owns Android-boundary and adapter checks. Reusable contracts are requested through the [library ownership register](README.md), without copying producer implementations into consumers.

<a id="source-language-governance"></a>

## Source language governance

The [implementation-language contract](../governance/ATF_SourceLanguage.md) owns the source inventory, reviewed lexical checks, localized-data boundaries and mandatory semantic review.

## Cohesion and package development

[Executable policy](../../config/atf-governance-policy.json) enforces the package limit without exceptions. [Package responsibilities](../governance/ATF_Packages.md) separates public APIs, shared support and test selections.

[Quality governance](../governance/README.md) · [Good documentation practices](../governance/ATF_Documentation.md)

The [browser test index](../../atf-ui-browser/tests/README.md#diagnostic-runner-boundaries) owns the selection, lifecycle, scenario and result boundaries. Mocked components preserve case identities, resource ownership and failure causes; the short rendered journey remains separate.

## Qualification of claims and launchers

[RAG evaluator regressions](../testing/ATF_ReleaseQualification.md#evaluator-regression-tests) verify qualified-source attestations. [Release integrity governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java) separately verifies APK and payload bytes in the recorded dossiers.

| Priority | Owner and gap | Verification level and closure criterion |
|---|---|---|
| P2 | Launcher coordination: entry points check preconditions inconsistently. | Controlled components for Java, Maven, Gradle, Node and Python must check required capabilities before costly operations and report missing capabilities with source identity. Distinguish the Python installation alias from an executable interpreter. For the [PowerShell ADB module](../../atf-test-android/tools/AndroidDeviceSetup.psm1), verify availability, refusal, timeout and process state separately from parsing and the Java client. |
| P2 | Snapshot cost: retained reports are copied and hashed with source staging. | Separate retained-report admission from source staging; retain exact byte seals, source/catalog identity and conservative handling of unknown readers. Measure inventory, copying, compilation and execution separately, and reconcile named stage durations with total time. |
| P2 | Compiler preparation: output sealing and broad recipe invalidation remain costly. | Use the [compiler cost partitions](../testing/ATF_Collections.md#compiler-cost-partitions) and [source-bound digest](../testing/ATF_Collections.md#source-bound-native-digest) to compare identified cold/warm populations; retain missing-class and tamper rejection. Bind complete Maven recipes before narrowing keys or excluding unrelated Gradle metadata. Mixed build changes remain conservative. The implemented [compiler and cleanup recovery contracts](../testing/ATF_Collections.md#maven-compiler-reuse) remain regression boundaries. |
| P2 | Admission cost: full-source inventory and repeated Git review remain costly. | Measure Git enumeration and lexical scanning separately. Keep the bounded inventory budget and source identity visible on retry. Profile repeated review reads and serialization separately from the selected test bodies; retain conservative rejection of unknown ownership. |
| P2 | Proof recovery: static controls still share a complete protected Git diff identity. A failed multi-control attempt cannot retain unaffected passing controls after one test driver changes. | Qualify per-control production, test, runtime and shared dependency closures before reusing such outcomes. Keep original source/report identities; reject changed shared or unknown inputs and changed selectors. Components must contrast an isolated driver correction with a shared dependency change and reconcile both JUnit population and Extent hashes. Until that admission exists, requalify the focused control bundle conservatively. |

[Independent proof domains](../governance/ATF_TestChanges.md#independent-proof-domains) owns path-based selection, mixed-diff union and separate evidence. Keep those regression boundaries active; excluding unchanged domains does not close the remaining snapshot, compilation or admission-cost criteria.

[Inline-image retention governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) separately checks for duplicated PNG files beside HTML. It does not prove data autonomy, image readability or APK integrity; the [adapter criterion](../reporting/ATF_Audit.md) remains separate.

- The [journal failure contract](../testing/ATF_ReleaseQualification.md#qualification-journal-failures) preserves original command and publication causes separately.
- Controlled writer failures and actual filesystem refusal remain component regression boundaries; they do not qualify every capability or host-resource condition.

## Improving guidance guides

The [documentation editorial grid](../governance/ATF_Documentation.md#editorial-verification-and-evaluation) evaluates documents and their stated limits separately from code maturity. Scores and reviewed inventories belong to the external audit; this register retains only work that can improve comprehension or reproducibility.

Operational terminology follows the [editorial contract](../governance/ATF_Documentation.md#operational-terminology). Review meaning against code and the Romanian edition; automated diagnostics enforce reviewed errors but do not replace semantic review.

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

The [early metadata contract](../testing/ATF_ReleaseQualification.md#early-release-metadata) checks the active E2E declarations, catalog destinations and release-index editorial rules before inventory, copying or Maven. Its source-bound snapshot recheck remains separate from artifact bytes, primary-suite results and full publication qualification.


<a id="framework-selection-boundaries"></a>

## Framework selection boundaries

The [independent ATF owners](../testing/ATF_Collections.md#independent-atf-component-owners) separate editorial and payload checks from provenance, mocked Android, mocked GitHub, orchestration and reporting. Each primary collection has a positive class inventory, one canonical Extent and its own source-bound receipt.

The [dependency selection contract](../testing/ATF_Collections.md#executable-dependency-selection) derives consumers from current executable references and explicit repository-observer scopes. Reviewed reader bodies and generated providers bound what the analysis can admit. Unknown inputs, missing populations and unadmitted reader changes retain complete ATF qualification.

- Markdown-only changes select the documentation owner without replaying independent launcher fixtures. Shared helpers, dependencies and unknown inputs still invalidate their consumers conservatively.
- Reuse retains original JUnit identities, outcomes, comparisons and report hashes. Historical monolithic reports remain immutable and cannot qualify new partition identities.

The [launcher-fixture contract](../testing/ATF_Collections.md#launcher-fixtures) owns the resource profile, diagnostic retention before cleanup, independent invocation timings and the separation of pure matrices from real admission components.

Measure snapshot preparation, compilation and execution separately. A focused diagnostic remains distinct from qualification of a complete primary collection; Maven/Jupiter recovery within an owner follows the [invocation-recovery contract](../testing/ATF_Collections.md#recovery-within-a-maven-selection).

## Reporting schema admission

The evaluator applies one reporting admission rule before readiness and preserves historical manifests. Accepted schema/report combinations are:

- Versions 1 and 2 may omit `qualification.reportSchema` only while every declared component and E2E report is HTML.
- Version 3 and later declare `ATF_EXTENT_HTML_INLINE_BASE64_V1`.
- XML-only, unknown, missing-modern or mixed-format declarations block readiness. JUnit XML remains a temporary oracle, never the canonical report.

The policy is in `config/release-platform-rag-policy.json`, admission is enforced by `atf-tests/tools/evaluate-release-rag.mjs`, and component coverage belongs to `ReleaseRagProvenanceComponentTest` and `ReleaseRagPlatformGovernanceTest`.
