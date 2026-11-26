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
| P2 | Proof recovery: static controls still share a complete protected Git diff identity. | Selective passing-control reuse remains gated on per-control production, test, runtime and shared dependency closures. Keep original source/report identities; reject changed shared or unknown inputs and changed selectors. The batch now rejects empty or malformed receipts, foreign control IDs and a receipt whose Extent digest does not match its report. The component regression also qualifies the conservative fallback: after one driver fails, the corrected attempt executes the entire selected bundle in a fresh run; original mocked JUnit and Extent identities remain unchanged, and shared-input drift prevents promotion. This does not authorize partial proof reuse. |

The bounded [batch component](../../atf-tests/src/test/java/atf/tests/execution/components/TestChangeBatchComponentTest.java) compares a corrected full-bundle run with shared-source and control-output negative cases. It binds each mocked receipt to its control ID and exact Extent bytes. The synthetic per-control JUnit population and report hashes are fixtures; the generated component report records the actual execution.

```yaml
testChanges:
  - id: RAG-36-LAB-01-RECOVERY
    status: ACTIVE
    base: 3340c8124e109659bcf975e2b6ec9abbf63b10b8
    expires: 2026-12-26
    reason: Qualify conservative whole-bundle recovery after an isolated proof-driver failure and identity-checked recovery after a hard-stopped corpus publisher.
    proof: "mvn -o -pl atf-tests -am '-Dtest=TestChangeBatchComponentTest#isolatedDriverCorrectionRerunsTheBundleAndSharedInputDriftRejectsPromotion,TestChangeBatchComponentTest#invalidProofReceiptOrReportIdentityPreventsBatchPublication,CorpusPublicationComponentTest#hardStoppedPublisherExcludesLiveWritersAndRecoversOnlyTheOriginalClaim,CorpusPublicationComponentTest#curatorTraversesAndPublishesGeneratedArchiveWithMeasuredResources' '-Dsurefire.failIfNoSpecifiedTests=false' test"
    checks:
      positive:
        - atf.tests.execution.components.TestChangeBatchComponentTest#isolatedDriverCorrectionRerunsTheBundleAndSharedInputDriftRejectsPromotion
        - atf.tests.learning.publication.CorpusPublicationComponentTest#hardStoppedPublisherExcludesLiveWritersAndRecoversOnlyTheOriginalClaim
      negative:
        - atf.tests.execution.components.TestChangeBatchComponentTest#changedSourcePreventsPublicationOfPassingControls
        - atf.tests.execution.components.TestChangeBatchComponentTest#invalidProofReceiptOrReportIdentityPreventsBatchPublication
        - atf.tests.learning.publication.CorpusPublicationComponentTest#interruptedConcurrentAndFailedInputsCannotPublishOrEraseOriginals
    paths:
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeBatchComponentTest.java
      - atf-tests/src/test/java/atf/tests/learning/publication/CorpusPublicationComponentTest.java
      - whist-lab-app/ai-lab/tools/qualification/atomic-directory-hard-stop-child.mjs
      - whist-lab-app/ai-lab/tools/testing/corpus-publication.test.mjs
```

- Component selection runs against the source catalogs before a run directory, source stage or Maven preparation is created; unknown and duplicate groups fail closed.
- Groups that do not own release snapshots omit release payload bytes while retaining the complete release inventory, source/catalog identity, live pre/post checks and artifact preflight.
- A selector hash binds the plan to the staged catalogs. Resume refuses a workspace whose omitted-payload mode cannot satisfy the selected owner.
- The compiler boundary keeps its complete byte seal. Cold sealing and warm receipt admission are measured separately; warm admission requires the same output fingerprint and preserves all missing-class, tamper, foreign-receipt and path refusals.

- The shared launcher capability check resolves every required executable before probing versions, records its source hash and verifies Java 17+, Maven, Node and the HTTPS/SHA-256-pinned Gradle wrapper inputs before workspace or Maven preparation.
- Android release preflight remains transport-specific. Mocked ADB cases distinguish command refusal, timeout, bounded termination and cleanup failure from device readiness and display parsing.

- Test-change proof generation performs one protected-source review, shares one exact protected-diff digest across eligible controls, then checks clean checkout and HEAD.
- Component evidence records actual phase counts and elapsed time. Existing qualification timing separates inventory, copied bytes and compiler-output sealing.
- These measurements establish reduced review work; they do not claim a general Maven or end-to-end release speedup. Full byte seals, unknown-reader rejection and original proof provenance remain unchanged.

- [Independent proof domains](../governance/ATF_TestChanges.md#independent-proof-domains) owns path-based selection, mixed-diff union and separate evidence. Keep those regression boundaries active; excluding unchanged domains does not close the remaining snapshot, compilation or admission-cost criteria.

- [Inline-image retention governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) checks for duplicated PNG files beside HTML. It does not prove data autonomy, image readability or APK integrity; the [adapter criterion](../reporting/ATF_Audit.md) remains separate.

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

- Maven component classes have one positive primary owner, and each inventory matches its suite descriptor exactly. The governance suite rejects omitted or duplicate owners and unlisted suite classes.
- Gradle test classes have one declared layer in `config/gradle-audit-collections.properties`. `unit` is limited to the isolated unit source set and cannot depend on component reporting; the inventory gate rejects missing or obsolete owners and unknown layers.

The [dependency selection contract](../testing/ATF_Collections.md#executable-dependency-selection) derives consumers from current executable references and explicit repository-observer scopes. Reviewed reader bodies and generated providers bound what the analysis can admit. Unknown inputs, missing populations and unadmitted reader changes retain complete ATF qualification.

- Markdown-only changes select the documentation owner without replaying independent launcher fixtures. Shared helpers, dependencies and unknown inputs still invalidate their consumers conservatively.
- Reuse retains original JUnit identities, outcomes, comparisons and report hashes. Historical monolithic reports remain immutable and cannot qualify new partition identities.
- A reviewed reader-seal update is validated against the complete current reader inventory and unchanged dependency topology. It selects consumers of changed reader bodies only; inventory observers do not broaden that selection. Multiple changed readers select the union of their consumers, while unexplained seal changes or unknown reader scopes retain complete qualification.

The [launcher-fixture contract](../testing/ATF_Collections.md#launcher-fixtures) owns the resource profile, diagnostic retention before cleanup, independent invocation timings and the separation of pure matrices from real admission components.

Measure snapshot preparation, compilation and execution separately. A focused diagnostic remains distinct from qualification of a complete primary collection; Maven/Jupiter recovery within an owner follows the [invocation-recovery contract](../testing/ATF_Collections.md#recovery-within-a-maven-selection).

## Reporting schema admission

The evaluator applies one reporting admission rule before readiness and preserves historical manifests. Accepted schema/report combinations are:

- Versions 1 and 2 may omit `qualification.reportSchema` only while every declared component and E2E report is HTML.
- Version 3 and later declare `ATF_EXTENT_HTML_INLINE_BASE64_V1`.
- XML-only, unknown, missing-modern or mixed-format declarations block readiness. JUnit XML remains a temporary oracle, never the canonical report.

The policy is in `config/release-platform-rag-policy.json`, admission is enforced by `atf-tests/tools/evaluate-release-rag.mjs`, and component coverage belongs to `ReleaseRagProvenanceComponentTest` and `ReleaseRagPlatformGovernanceTest`.
