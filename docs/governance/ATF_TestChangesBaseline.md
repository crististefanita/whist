# Register of verified test controls

## Overview

This register defines reviewed controls, their Git base and exact paths. The [test-change guide](ATF_TestChanges.md) explains eligibility and separates registered checks from evidence executed for the current diff.

## Verified controls

<details>
<summary>Checkpoint ownership and separated journal</summary>

```yaml
testChanges:
  - id: RPT-CHECKPOINT-TRANSACTION
    status: VERIFIED
    base: 4caf2294d3a2bf3abce7aaa8a624546a8620cabb
    expires: 2026-12-24
    reason: Validate the checkpoint before transferring actions, preserve the prior journal and original cause across refusal and retry without duplicate registration. Separate pure encoding from coordination and preserve format, timing, replay and standalone HTML.
    proof: 'mvn -o -pl atf-tests -am -Dtest=AndroidUiFlowCheckpointComponentTest,AndroidUiFlowTraceTest,AndroidUiFlowTimingComponentTest,CapturedAndroidFlowComponentTest,AndroidUiFlowReportPublisherTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.android.e2e.support.journal.AndroidUiFlowCheckpointComponentTest#originalDispatchAndFailureSurviveReplayAndDetachedHtml
        - atf.android.e2e.support.AndroidUiFlowTraceTest#assertionsKeepCheckpointOrderContextAndVerdicts
        - atf.android.e2e.support.AndroidUiFlowTimingComponentTest#individualPublicationUsesCapturedPhaseIntervalsAndPreservesFullPngs
        - atf.android.e2e.support.replay.CapturedAndroidFlowComponentTest#immutableReplayRetainsOriginalOwnersValuesVerdictsActionsAndTiming
        - atf.android.e2e.support.AndroidUiFlowReportPublisherTest#embedsCompleteSupportingTextSafelyFromTheFingerprintedBytes
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
      negative:
        - atf.android.e2e.support.journal.AndroidUiFlowCheckpointComponentTest#rejectedInputsPreservePendingActionsExistingPhasesAndOriginalManifest
        - atf.android.e2e.support.journal.AndroidUiFlowCheckpointComponentTest#failedJournalWriteRollsBackOnlyTheUncommittedCheckpointAndRetriesOnce
        - atf.android.e2e.support.journal.AndroidUiFlowCheckpointComponentTest#timingFailureAfterReplacementCannotRollBackCommittedEvidence
        - atf.android.e2e.support.AndroidUiFlowReportPublisherTest#refusesFalseDriverTitleWithIdenticalRowsComparisonsAndPng
    paths:
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java
      - atf-test-android/src/test/java/atf/android/e2e/support/journal/AndroidUiFlowJournal.java
      - atf-test-android/src/test/java/atf/android/e2e/support/journal/AndroidUiFlowJournalUnitTest.java
      - atf-test-android/src/test/java/atf/android/e2e/support/journal/AndroidUiFlowCheckpointComponentTest.java
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
```

</details>

<details>
<summary>Independent reader seals and qualified source/catalog context</summary>

```yaml
testChanges:
  - id: RAG-READER-SEAL-OWNERS
    status: VERIFIED
    base: 99ca70c73f007730dadd4b76f7aab283ddc0fc2d
    expires: 2026-12-24
    reason: Separate admitted topology from reviewed reader bodies; select real consumers and inventory observers while retaining complete qualification for topology, missing seals and unknown scopes. Preserve original reports and verify explicit source/catalog context at restart and rejection of foreign provenance.
    proof: 'mvn -o -pl atf-tests -am -Dtest=AtfCollectionOwnershipComponentTest,QualifiedCatalogContextComponentTest,SuiteOwnershipGovernanceTest,TestChangeReviewComponentTest,ComponentEvidenceScopeComponentTest,ReleaseRagProvenanceComponentTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#reviewedReaderSealsSelectOnlyTheirConsumersAndPreserveOriginalReports
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
        - atf.tests.governance.documentation.release.rag.scope.context.QualifiedCatalogContextComponentTest#sourceAndCatalogIdentitySurvivesRestartWithoutRelabelingHistoricalReports
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#documentationDriftReadmitsOnlyUnchangedOwnersAndPreservesRejectedEvidence
      negative:
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#topologyMissingSealsAndUnknownReaderScopesCannotNarrowQualification
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
        - atf.tests.governance.documentation.release.rag.scope.context.QualifiedCatalogContextComponentTest#missingForeignContextAndLiveCatalogCorruptionCannotQualify
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#readerSealsAndCatalogContextCannotBypassSourceChangeReview
        - atf.tests.governance.documentation.release.rag.ReleaseRagProvenanceComponentTest#componentReportsFromAnotherSourceInventoryCannotQualifyTheApk
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/context/QualifiedCatalogContextComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/suites/AtfCollectionOwnershipComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfProvenanceComponentSuite.java
      - atf-tests/tools/atf-component-dependencies.mjs
      - atf-tests/tools/atf-dependency-references.mjs
      - atf-tests/tools/atf-component-dependencies.test.mjs
      - atf-tests/tools/atf-component-ownership.test.mjs
      - atf-tests/tools/atf-dependency-cli.mjs
      - atf-tests/tools/atf-dependency-fixture.mjs
      - atf-tests/tools/atf-reader-seals.test.mjs
      - atf-tests/tools/component-qualification-context.mjs
      - atf-tests/tools/component-qualification-context.test.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/prepare-component-reuse.mjs
      - atf-tests/tools/release-test-scope.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - scripts/AtfComponentResume.ps1
      - scripts/AtfQualificationSnapshot.ps1
      - scripts/run-component-suites.ps1
```

</details>

<details>
<summary>Snapshot and compiler I/O without metadata shortcuts</summary>

```yaml
testChanges:
  - id: RAG-SNAPSHOT-NATIVE-IO
    status: VERIFIED
    base: 3b28996f3e818558d237eca5989f12df48502c0c
    expires: 2026-12-24
    reason: Preserve exact source/release manifests and compiler seals while using fresh native content reads, bounded verified copying and named snapshot phases. Keep complete admission before mutation and preserve drift, corruption, private-material, duplicate-path and link refusals. Keep primary ownership and compiler reuse independently governed.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SnapshotIoComponentTest,LauncherSourceInventoryComponentTest,SigningSourceIsolationComponentTest,ComponentEvidenceScopeComponentTest,SuiteOwnershipGovernanceTest,AtfCollectionOwnershipComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.io.SnapshotIoComponentTest#byteInventoriesAndNamedSnapshotPhasesRemainEquivalent
        - atf.tests.governance.support.LauncherSourceInventoryComponentTest#outputBoundariesKeepLegalPackagesAndDetectSourceChanges
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#editorialChangesReuseVerifiedMavenCompilationAndKeepFreshExecutionInputs
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#documentationDriftReadmitsOnlyUnchangedOwnersAndPreservesRejectedEvidence
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
      negative:
        - atf.tests.governance.documentation.release.rag.scope.io.SnapshotIoComponentTest#sameMetadataDriftCorruptionAndIndirectPathsCannotQualify
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#aPublicPathCannotFollowALinkToPrivateMaterial
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#aForgedManifestCannotCopyPrivateMaterialOrPartiallyExportPublicFiles
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#contentDigestsCannotBypassSourceChangeReview
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
    paths:
      - scripts/AtfFileDigest.ps1
      - scripts/Get-AtfSourceInputs.ps1
      - scripts/Copy-AtfSourceInputs.ps1
      - scripts/AtfQualificationSnapshot.ps1
      - scripts/AtfComponentMaven.ps1
      - scripts/run-component-suites.ps1
      - scripts/run-atf-governance.ps1
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/io/SnapshotIoComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfOrchestrationComponentSuite.java
      - atf-tests/tools/snapshot-io.test.mjs
      - atf-tests/tools/component-source-check.test.mjs
      - atf-tests/tools/release-test-scope.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Maven compilation ownership and fresh execution</summary>

```yaml
testChanges:
  - id: RAG-MAVEN-INPUT-OWNERSHIP
    status: VERIFIED
    base: 2ca72d50c35d15375b294cce8aa502dc18feb4fd
    expires: 2026-12-24
    reason: Bind literal Maven and build-helper recipes before excluding registered Gradle-only test Java from the compiler key. Preserve resource and unknown inputs, full execution snapshots and output seals. Exercise fresh controls on changed Gradle snapshots with a reused compiler; reject unsupported, foreign, indirect or corrupt recipes and outputs.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#literalMavenRootsKeepSharedProvidersAndRejectUnknownOrUnboundRecipes
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#gradleOnlyChangesReuseCompilationWithoutReusingControlVerdicts
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#editorialChangesReuseVerifiedMavenCompilationAndKeepFreshExecutionInputs
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#tamperedCompilerCannotQualifyProofGeneration
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
    paths:
      - scripts/AtfCompilerIdentity.ps1
      - scripts/AtfComponentMaven.ps1
      - scripts/run-component-suites.ps1
      - atf-tests/tools/component-compiler-inputs.mjs
      - atf-tests/tools/component-compiler-inputs.test.mjs
      - atf-tests/tools/component-compiler-scope.test.mjs
      - atf-tests/tools/component-proof-compiler-reuse.test.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Shared policy units and static browser admission</summary>

```yaml
testChanges:
  - id: WHIST-SHIFT-LEFT-HOST
    status: VERIFIED
    base: 7ea1ac3d32b5abf313db445217f36fcadd8a1d61
    expires: 2026-12-24
    reason: Move isolated viewport and trump-fit oracles into common JUnit units while preserving Whist collaboration matrices. Enforce source-set and reporting boundaries. Admit delivered browser modules and fonts before startup, reject invalid HTTP requests and retain bounded compiled-game sanity evidence.
    proof: 'mvn -o -pl atf-tests -am -Dtest=GradleAuditGovernanceTest,WhistSurfaceAdapterComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#everyGradleTestClassHasOneExplicitLayerAndAnAuditedBase
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#rawJunitUnitsAndAuditedComponentsRetainTheirDeclaredBoundaries
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#productTestsDoNotKeepRawJUnitAssertionsBesideTheAuditFacade
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#deliveredSiteModulesAndFontsAreAdmittedBeforeBrowserStartup
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#adaptiveWebMenusKeepTextIndependentOfTheUsableViewport
      negative:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#unitDeclarationsCannotHideAnAuditedComponentOrReportingDependency
    paths:
      - atf-tests/src/test/java/atf/tests/governance/source/gradle/GradleAuditGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-ui-browser/tests/BrowserUi.java
      - atf-ui-browser/tests/Run-BrowserUi.ps1
      - atf-ui-browser/tests/ro/cardtable/testing/browser/StaticSiteHost.java
      - atf-ui-browser/tests/static-host.test.mjs
      - config/gradle-audit-collections.properties
      - config/atf-component-dependencies.json
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TableViewportPolicyTest.java
      - whist-lab-app/card-table-core/src/unitTest/java/ro/cardtable/ui/table/TableViewportPolicyTest.java
      - whist-lab-app/card-table-core/src/unitTest/java/ro/cardtable/ui/table/TrumpTileTextFitTest.java
```

</details>

<details>
<summary>Per-owner readmission after concurrent editorial drift</summary>

```yaml
testChanges:
  - id: RAG-DOCUMENTATION-DRIFT-READMISSION
    status: VERIFIED
    base: f6bdc5cc432c167a72486b71b01d9b7d5e06c74b
    expires: 2026-12-21
    reason: Preserve rejected aggregates and original reports while readmitting passing owners from bound staged/live observations; rerun changed owners and reject missing, corrupt, non-editorial or failed evidence.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentEvidenceScopeComponentTest,ReleaseRagProvenanceComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#documentationDriftReadmitsOnlyUnchangedOwnersAndPreservesRejectedEvidence
        - atf.tests.governance.documentation.release.rag.ReleaseRagProvenanceComponentTest#documentationDriftReadmissionKeepsTheRejectedAggregateAndOriginalReports
      negative:
        - atf.tests.governance.documentation.release.rag.ReleaseRagProvenanceComponentTest#documentationDriftRequiresBoundObservationsInTheReleasePayload
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#markdownFixturesAndSelectionContractsRemainProtected
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/RagComponentEvidenceFixture.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/tools/atf-component-ownership.test.mjs
      - atf-tests/tools/component-source-check.mjs
      - atf-tests/tools/component-source-check.test.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/prepare-component-reuse.mjs
      - scripts/AtfQualificationSnapshot.ps1
      - scripts/run-component-suites.ps1
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Canonical test-change admission before source and Maven preparation</summary>

```yaml
testChanges:
  - id: RAG-EARLY-TEST-CHANGE-ADMISSION
    status: VERIFIED
    base: 65bb00dbcd555a9d6e9284d3088014e77be06795
    expires: 2026-12-20
    reason: Run the canonical Git and proof validators before source copying or Maven, recheck after staging, and separate proof generation from normal authorization without accepting foreign, incomplete or altered evidence.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeProofComponentTest,QualificationLauncherComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#admissionUsesTheSameProofVerdictAndDiagnosticsAsFinalGovernance
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#validTestChangeProofPrecedesSourceCopyAndMavenInBothLaunchers
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#nodeContractTestsKeepReuseClassificationInsideTheRealBuildBoundary
      negative:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#admissionRechecksChangedSourcesAndSeparatesProofGeneration
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#invalidTestChangeProofStopsBothLaunchersBeforeCopyOrMaven
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#sharedDependenciesAndUnknownInputsCannotReuseTheirConsumers
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeAdmissionCli.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeAdmissionFixture.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeProofComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
      - scripts/AtfTestChangeAdmission.ps1
      - scripts/AtfStandaloneJava.ps1
      - scripts/run-atf-governance.ps1
      - scripts/run-component-suites.ps1
      - config/atf-component-dependencies.json
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - .github/workflows/whist-components.yml
      - atf-tests/tools/workflow-test-change-admission.test.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
```

</details>

<details>
<summary>Executable dependency ownership and conservative reader admission</summary>

```yaml
testChanges:
  - id: RAG-REAL-COMPONENT-DEPENDENCIES
    status: VERIFIED
    base: 9a655cff56265ac9609628c4deea1c50965cbcd4
    expires: 2026-12-20
    reason: Select real transitive consumers without treating classifier data as imports; require complete source populations, reviewed readers and generated providers, preserving immutable original evidence and conservative fallback for unknown inputs.
    proof: 'mvn -o -pl atf-tests -am -Dtest=AtfCollectionOwnershipComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
      negative:
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/suites/AtfCollectionOwnershipComponentTest.java
      - atf-tests/tools/atf-component-dependencies.mjs
      - atf-tests/tools/atf-component-dependencies.test.mjs
      - atf-tests/tools/atf-dependency-references.mjs
      - atf-tests/tools/atf-script-references.mjs
      - atf-tests/tools/atf-dependency-cli.mjs
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/prepare-component-reuse.mjs
      - atf-tests/tools/release-test-scope.mjs
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Governance of the game address on GitHub Pages</summary>

```yaml
testChanges:
  - id: WHIST-PAGES-DOCUMENTATION
    status: VERIFIED
    base: 671a35e0b205069168779a8c6901adb939eab29c
    expires: 2026-12-16
    reason: The project Pages address is a publication destination; foreign project identifiers remain governed prose. Adapter sources belong to the JVM inventory.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationPolicyGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#ownPagesDestinationIsPublicationData
      negative:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#externalAndNonCanonicalPagesDestinationsRemainSubjectToProseRules
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - config/atf-governance-policy.json
```

</details>

<details>
<summary>Offline preparation and recovery after scratch cleanup</summary>

```yaml
testChanges:
  - id: RAG-OFFLINE-CLEAN-RESTART
    status: VERIFIED
    base: b179972ff1e9c16e6b7ae52dff0077993bb8623b
    expires: 2026-12-20
    reason: Separate temporary-root policy from its CLI and classpath admission from compiled-output verification, preserving cold fallback after cleanup and rejecting partial, indirect or foreign preparation without weakening original evidence checks.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#editorialChangesReuseVerifiedMavenCompilationAndKeepFreshExecutionInputs
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#componentResumeImportsOnlyVerifiedEvidenceFromTheInterruptedSnapshot
    paths:
      - scripts/AtfComponentMaven.ps1
      - atf-tests/tools/qualification-temp-root.mjs
      - atf-tests/tools/qualification-temp-root.test.mjs
      - atf-tests/tools/component-maven-reuse.test.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
```

</details>

<details>
<summary>Owned component reuse, patch versioning and portable qualification roots</summary>

```yaml
testChanges:
  - id: RAG-COMPONENT-OWNERSHIP
    status: VERIFIED
    base: 4b96da72e6bc8c97e1a01db4517a50a9f9d67574
    expires: 2026-12-20
    reason: Derive component ownership from the Gradle collection map, reuse only hash-bound unchanged suites, fail closed for shared or unknown inputs, enforce 0.x.y increments by change risk and resolve qualification roots from machine configuration.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentEvidenceScopeComponentTest,ComponentEvidencePortabilityComponentTest,QualificationLauncherComponentTest,ReleaseVersionComponentTest,ReleaseRagProvenanceComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#nodeContractTestsKeepReuseClassificationInsideTheRealBuildBoundary
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidencePortabilityComponentTest#evidenceAndSdkRootsResolveFromMachineConfiguration
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#theApplicationUsesTheDeclaredPatchReleaseConvention
        - atf.tests.governance.documentation.release.rag.ReleaseRagProvenanceComponentTest#aPinnedComponentCandidateProducesCitedHumanReviewWithoutChangingInputsOrCallingE2e
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#sharedDependenciesAndUnknownInputsCannotReuseTheirConsumers
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidencePortabilityComponentTest#androidSdkDoesNotSilentlyFallBackWhenNoMachinePathIsConfigured
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidencePortabilityComponentTest#relativeEvidenceRootIsRejectedInsteadOfResolvingAgainstTheCheckout
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#checkoutTmpStopsBothLaunchersBeforeStagingOrMaven
        - atf.tests.governance.documentation.release.rag.ReleaseRagProvenanceComponentTest#aMalformedReleaseVersionPatternBlocksPolicyWithoutCrashingTheEvaluator
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#aChangeRiskCannotClaimAnIncorrectVersionIncrement
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidencePortabilityComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/versioning/ReleaseVersionComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/android-user-ownership.test.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/prepare-component-reuse.mjs
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - atf-tests/tools/publish-github-release.test.mjs
      - atf-tests/tools/qualification-temp-root.mjs
      - atf-tests/tools/qualification-temp-root.test.mjs
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - atf-tests/tools/release-version.mjs
      - atf-tests/tools/release-version.test.mjs
      - config/release-platform-rag-policy.json
      - scripts/AtfLocalPaths.ps1
      - scripts/AtfQualificationSnapshot.ps1
      - scripts/Invoke-AtfAndroidDisposableUser.ps1
      - scripts/export-card-table-ui.ps1
      - scripts/run-atf-governance.ps1
      - scripts/run-component-suites.ps1
      - scripts/run-documentation-governance.ps1
      - scripts/run-jvm-inventory.ps1
      - whist-lab-app/app/build.gradle
```

</details>

<details>
<summary>Check: Native acceptance of actions and history</summary>

```yaml
testChanges:
  - id: PORTRAIT-ACTION-REGIONS-AND-HISTORY
    status: VERIFIED
    base: 5ae98e8cc1863c7513f0d7b8396087f19c0bc6cb
    expires: 2026-12-05
    reason: Measure distance from the player frame and its observed action, rejecting overlap; cover exact dimensions, localization, legally prepared actors, and access to every row and column.
    proof: 'gradlew.bat :app:testDebugUnitTest -Pwhist.displayComponents=true; mvn -Dtest=AndroidDisplayComponentSuite,RecordsRunContractComponentTest,RecordsFixturesComponentTest,AndroidUiFlowReportPublisherTest test; mvn -Dtest=WhistRecordsE2eTest#historyPresentationAcrossDeclaredPortraitProfiles -Datf.android.enabled=true test'
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TableViewportPolicyTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/TableViewportObservation.java
      - atf-test-android/src/test/java/atf/android/e2e/display/component/SyntheticTableHierarchy.java
      - atf-test-android/src/test/java/atf/android/e2e/display/component/TableViewportObservationComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveMenuChecks.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveEvidence.java
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java
      - atf-test-android/src/test/java/atf/android/e2e/records/WhistRecordsE2eTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsFixtures.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsFixturesComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsContext.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsRunContract.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsRunContractComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/NameHistoryScenarios.java
```

A simulated landmark shows how the action ignore the comparison of distances on the same coordinates. Negative controls keep text, actor, size and identity errors of the row; native running requires installation metadata, reservation and private archive defined in the [Android contract](../testing/ATF_Android.md#profile-history-leaderboards-and-series).

</details>

<details>
<summary>Check: table proportions and action above the name</summary>

```yaml
testChanges:
  - id: DISPLAY-PROPORTIONS-ACTION-AND-HISTORY
    status: VERIFIED
    base: 8933374e33ab6eee9cb4217ed1a6112aebacc9b3
    expires: 2026-12-05
    reason: Reduce played-card height and trump width, enlarge player frames, and separate actions from names; retain non-overlapping geometry and chronological score identity.
    proof: 'gradlew.bat :app:testDebugUnitTest -Pwhist.displayComponents=true --offline --no-daemon; pwsh -NoProfile -File scripts/run-whist-responsive-sanity.ps1 -ComponentsOnly'
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TrickCardMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TableViewportPolicyTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/localization/WhistUiTextTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/seat/PlayerSeatMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryGridMetricsComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/profile/ResponsiveDisplayProfile.java
      - atf-test-android/src/test/java/atf/android/e2e/display/profile/ResponsiveDisplayProfileComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/TableViewportObservation.java
      - atf-test-android/src/test/java/atf/android/e2e/display/component/SyntheticTableHierarchy.java
      - atf-test-android/src/test/java/atf/android/e2e/display/component/TableViewportObservationComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveTableState.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveMenuChecks.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SixPlayerTableScenario.java
```

The oracle of dimensions follows the requirement for presentation and negative controls refuse the exceeded height, the missing action, the abbreviated text and the overlap over the name. The native verification remains distinct from the simulated observation and JVM mechanism.

</details>

<details>
<summary>Check control: historical measurement and log invariants</summary>

```yaml
testChanges:
  - id: HISTORY-MEASUREMENT-AND-LEDGER-INVARIANTS
    status: VERIFIED
    base: 180ea08cb639fd09291d191278e18fa93ae6db96
    expires: 2026-12-05
    reason: Extract measurement aggregation from the Android view and reject invalid seats, lost tricks, and impossible dimensions; retain unknown suffixes, immutability, and measurement of the final cell.
    proof: 'gradlew.bat :game-core:test --tests ro.whistlab.core.session.ScoreHistoryInvariantTest --offline; gradlew.bat :app:testDebugUnitTest --tests ro.whistlab.app.score.ScoreHistory*Test --offline'
    paths:
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/session/ScoreHistoryInvariantTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryMeasureComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryGridMetricsComponentTest.java
```

Regression is faced with the previous source prior to the correction. Font providers are injected into components; the qualification of the raster and Android font remains separate. A diagnosis of the classSPath is not reclassified as a defect of the product.

</details>

<details>
<summary>Checked control: compact history and explicit outcome of contract</summary>

```yaml
testChanges:
  - id: COMPACT-HISTORY-EXPLICIT-CONTRACT-OUTCOME
    status: VERIFIED
    base: 6e8f3f38e94f0513597b197bc8fa60ce73ee371b
    expires: 2026-12-05
    reason: Apply the agreed compact geometry and explicit trick schema; retain unknown prefixes, independent score comparisons, trick conservation, and complete cells in both languages.
    proof: 'gradlew.bat :app:testDebugUnitTest :app:assembleDebug -Pwhist.displayComponents=true --offline --no-daemon; mvn -Dtest=RecordsFixturesComponentTest,RecordsRunContractComponentTest,AndroidUiFlowReportPublisherTest test; mvn -Dtest=WhistRecordsE2eTest#compactHistoryAndSlashedContractsRemainReadable -Datf.android.enabled=true test'
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryGridMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryTableComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryOutcomeComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsFixtures.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsFixturesComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/NameHistoryScenarios.java
      - atf-test-android/src/test/java/atf/android/e2e/records/WhistRecordsE2eTest.java
```

Zero, failure with positive points, unknown data and an unfinished hand have distinct results. Negative controls refuse fractional, negative, unserviced or contradictory levatates, and injected measurements do not replace native rendering.

</details>

<details>
<summary>Check control: common asset indicator and discrete deposit</summary>

```yaml
testChanges:
  - id: UNIFORM-TRUMP-AND-COMPACT-BID-SUBMISSION
    status: VERIFIED
    base: e183df449ad54d4281925f905ce3a65cf4b6cdf3
    expires: 2026-12-05
    reason: Remove the redundant unturned-card row and use one two-region frame in every mode; retain the visible value, accessible identity, and touch target. Synchronize hand dimensions and seat indexes with concurrent display-policy changes.
    proof: 'gradlew.bat :app:testDebugUnitTest :app:assembleDebug -Pwhist.displayComponents=true --offline --no-daemon; pwsh -NoProfile -File scripts/run-whist-responsive-sanity.ps1 -ComponentsOnly'
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/localization/WhistUiTextTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TrumpIndicatorComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/GameTableLayoutComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/ThirteenCardBiddingCheck.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SixPlayerBiddingCheck.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/fan/FanSetupPhase.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveGameFlow.java
```

The controls shall keep the card or colour, legibility, location and accessible description; the absence of redundant text shall not replace the presence of value. Injected font measurements qualify for the common policy, and native verification compares the same geometry in states with and without trump.

</details>

<details>
<summary><strong>Library guides - short index and guides per librarian </strong></summary>

```yaml
testChanges:
  - id: LIBRARY-USAGE-GUIDES
    status: VERIFIED
    base: 52270cdbff4a7234c2cc6581c910d3431105f76e
    expires: 2026-11-30
    reason: Keep the exact flat binary inventory and require only the index plus four named usage guides; retain independent missing, directory and extra-file negatives, physical CodeSource and source/feedback ownership.
    proof: 'mvn -B -ntp -Dtest=PlatformLibraryContractTest,PlatformLibraryIntegrityTest,LibraryDocumentationExampleTest,DocumentationPolicyGovernanceTest#libraryMaintenanceFooterDoesNotExemptBodyOtherOwnersOrOtherFiles+libraryUsageGuidesAreGovernedAndKeepSourceOwnerOnlyInMaintenanceFooter test'
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/GovernanceProject.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryContractTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryIntegrityTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibrarySurface.java
```

Examples are extracted from READMEs and checked on physical JARs. Change keeps published pairs and existing oracles.

</details>

<details>
<summary>Check: technical address of public discharge</summary>

```yaml
testChanges:
  - id: DOCUMENTATION-PUBLIC-RELEASE-URI
    status: VERIFIED
    base: cd4e62e4fd89bb5134f874aee1912aa086a5bda2
    expires: 2026-12-05
    reason: Distinguish the technical destination of the project repository link from visible prose; continue rejecting identifiers in labels, other projects, source paths, and modified URLs.
    proof: 'mvn -B -pl atf-tests -am -Dtest=DocumentationPolicyGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - config/atf-governance-policy.json
```

Reproduction is refusal of the public link valid as editorial reference; negative controls keep labels, other projects and non-anonymous variants within the checked perimeter. The contract [documentation](ATF_Documentation.md#stable-content-and-variable-data) defines the limit without general exceptions for URLs.

</details>

<details>
<summary>Check: page of the declared version</summary>

```yaml
testChanges:
  - id: DOCUMENTATION-DECLARED-RELEASE-PAGE
    status: VERIFIED
    base: a5eebec1805b760c876f03d2439da8b73d8e3cec
    expires: 2026-12-06
    reason: Recognize only the declared release tag page, APK, and checksum; continue checking visible prose, other versions, and modified addresses.
    proof: 'mvn -B -pl atf-tests -am -Dtest=DocumentationPolicyGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/main/java/atf/tests/distribution/ApkDownloadTarget.java
```

[Delivery identity](../platform/ATF_AndroidDelivery.md) sets out the version; [Documentary policy](ATF_Documentation.md#stable-content-and-variable-data) classifies only canonical destinations. The samples shall refuse the undeclared page, another version, another repository, image and URLs with changes; the visible label shall be exempt.

</details>

<details>
<summary>Check: library ownership and documentation</summary>

```yaml
testChanges:
  - id: LIBRARY-OWNERSHIP-README
    status: VERIFIED
    base: d46948264e7b35347ad812be1e67e9fba30eef05
    expires: 2026-11-30
    reason: Require exactly four flat JAR/POM pairs plus the dedicated README and common producer ownership of library source and feedback; preserve physical binary identity, independent negative oracles and consumer acceptance.
    proof: 'mvn -B -ntp -pl atf-tests -am -Dtest=PlatformLibraryContractTest,PlatformLibraryIntegrityTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryContractTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryIntegrityTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibrarySurface.java
```

Negative controls reject the missing README, additional documents, a director instead of README- and the assignment of the code or feedback to a consumer. The distributed libraries keep their coordinates and hashes.

The inventory at Git base declared is that of the checked increment. The current pairs and guides are defined in [library contract](../../lib/README.md).

</details>

<details>
<summary>Control check: test authorization on the Git diff</summary>

```yaml
testChanges:
  - id: GOVERNANCE-GIT-TEST-CHANGES
    status: VERIFIED
    base: 08706c3a1a3fe0172b706da2c69e9f767fde54bd
    expires: 2026-12-19
    reason: Bind authorization to the reviewed commit, exact paths, and expiry, with negative checks on real Git repositories.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeReviewComponentTest,TestChangeGovernanceTest,DocumentationPolicyGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/main/java/atf/tests/testchange/GitTestChangeSnapshot.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeControls.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeGitFixture.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - scripts/run-atf-governance.ps1
```

</details>

<details>
<summary>Checked for traceability of the change sample</summary>

```yaml
testChanges:
  - id: GOVERNANCE-TEST-CHANGE-PROOF
    status: VERIFIED
    base: 7a28a0978e6b50c1f9dd85cbad40544aecaea06d
    expires: 2026-12-19
    reason: Bind offline evidence to its control, reviewed base, commit, and diff content, and require an executed negative control.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeProofComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofMatchesExactGitReviewAndDeclaredCases
      negative:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofFromAnotherCommitCannotBeReused
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofWithoutPassingNegativeControlIsRefused
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofRequiresTheSameIdentityAndReportBytes
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#verifiedControlNeedsProofAndCleanCommittedInputs
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/GitTestChangeSnapshot.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeControls.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeProofChecks.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeProof.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeProofComponentTest.java
      - scripts/run-component-suites.ps1
```

</details>

<details>
<summary>Checked control for session facade and restoration</summary>

```yaml
testChanges:
  - id: GAME-SESSION-BOUNDARY
    status: VERIFIED
    base: 48a94740184e39bc23de975c3d133bd132620492
    expires: 2026-12-19
    reason: Compare moves, scoring, and restoration through the public facade against an independent JVM oracle; verify rejection without mutation.
    proof: 'mvn -o -pl atf-tests -am -Dtest=GameSessionBoundaryComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.game.GameSessionBoundaryComponentTest#resumesBiddingPlayingAndScoreAndAdvancesTheSameMatch
      negative:
        - atf.tests.governance.source.game.GameSessionBoundaryComponentTest#illegalCommandsAndCorruptCheckpointsAreRefused
    paths:
      - atf-tests/src/test/java/atf/tests/governance/source/game/GameSessionBoundaryComponentTest.java
      - atf-tests/src/test/resources/session/GameSessionProbe.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/session/CheckpointHistoryAuditTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/session/MatchSummaryTest.java
```

</details>

<details>
<summary>Checked control for Gradle lots budget</summary>

```yaml
testChanges:
  - id: CORE-GRADLE-BATCH-BUDGET
    status: VERIFIED
    base: 2b16b7c6b5c0909b59d924189a938683345087a1
    expires: 2026-12-19
    reason: Fix the shared memory budget and separate strategy batches; reject heap overrides and stop at the first failed case.
    proof: 'mvn -o -pl atf-tests -am -Dtest=GradleAuditGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#coreBatchesHaveOneDeclaredBudgetAndSeparatePolicyExperiments
      negative:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#coreBatchPolicyDetectsTheOldHeapOverrideAndEarlyExit
    paths:
      - config/test-collections.json
      - atf-tests/src/test/java/atf/tests/governance/source/gradle/GradleAuditGovernanceTest.java
      - whist-lab-app/game-core/build.gradle
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/match/OfflineMockMatchTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/match/OfflinePerspectiveMatchTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/match/OfflineStrategyProfileMatchTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/rules/RomanianWhistModelTest.java
      - whist-lab-app/gradle/component-audit.gradle
      - whist-lab-app/test-audit/src/test/java/atf/gradle/audit/GradleAuditBridgeComponentTest.java
      - whist-lab-app/test-audit/src/testFixtures/java/atf/gradle/audit/GradleReport.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/rules/RomanianWhistExhaustiveTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/rules/RomanianWhistRulesTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/testing/OriginalVariantModelTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/coach/IndependentPlayerModelTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/scoring/SymmetricSeriesComponentTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/rules/OriginalVariantContractTest.java
      - whist-lab-app/test-audit/src/testFixtures/java/atf/gradle/audit/verification/GradleReportVerifier.java
```

</details>

<details>
<summary>Checked control for the execution of the evidence and movement of the verification of the dialogues</summary>

```yaml
testChanges:
  - id: OFFLINE-QUALITY-BOUNDARIES
    status: VERIFIED
    base: f2c1b58d50e57922bbbb9ac44c977149328d7c0c
    expires: 2026-12-19
    reason: Verify concurrent draining of Node output and preserve manual selection under overlapping dialogs without repeating the legend in the bidding flow.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#verboseNodeRunDrainsBothStreamsFromTheSelectedDirectory
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#nonZeroNodeExitKeepsTheChildDiagnostic
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/bidding/HumanBidResumeComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/release/ReleaseBidScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/release/README.md
```

</details>

<details>
<summary>Checked control for temporary root of qualification</summary>

```yaml
testChanges:
  - id: RAG-09-TEMP-ROOT
    status: VERIFIED
    base: f2c1b58d50e57922bbbb9ac44c977149328d7c0c
    expires: 2026-12-19
    reason: Qualification uses an external temporary directory and rejects paths inside a checkout before copying or Maven execution.
    proof: 'mvn -o -pl atf-tests -am -Dtest=QualificationLauncherComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#governancePassesWithAnIsolatedRootAndThePinnedInputManifest
      negative:
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#checkoutTmpStopsBothLaunchersBeforeStagingOrMaven
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#aJunctionInTempCannotHideAGitWorktree
    paths:
      - scripts/AtfQualificationSnapshot.ps1
      - scripts/run-atf-governance.ps1
      - scripts/run-component-suites.ps1
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
```

</details>

<details>
<summary>Checks verified for selection and publication of the consignment</summary>

```yaml
testChanges:
  - id: GOV-06-PLAN-CONTROLS
    status: VERIFIED
    base: 6f36177aae63557abfeb18160b6ec1101dcf509c
    expires: 2026-12-19
    reason: Discover VERIFIED controls in the real diff and preserve commit identity.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeProofComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#requiredProofsIncludeEveryVerifiedControlOfTheCommittedDiff
      negative:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofFromAnotherCommitCannotBeReused
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeProofComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
  - id: GOV-06-PUBLISH-BATCH
    status: VERIFIED
    base: 6f36177aae63557abfeb18160b6ec1101dcf509c
    expires: 2026-12-19
    reason: Publish the complete batch after checking source identity; retain individual reports when publication is rejected.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeBatchComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.execution.components.TestChangeBatchComponentTest#publishesBothControlsAfterCheckingTheSnapshot
      negative:
        - atf.tests.execution.components.TestChangeBatchComponentTest#failedControlDoesNotStopTheRemainingControlsOrPublishTheirProofs
        - atf.tests.execution.components.TestChangeBatchComponentTest#changedSourcePreventsPublicationOfPassingControls
        - atf.tests.execution.components.TestChangeBatchComponentTest#changedReviewPreventsPublicationOfPassingControls
    paths:
      - scripts/AtfTestChangeBatch.ps1
      - scripts/run-component-suites.ps1
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeBatchComponentTest.java
```

</details>

<details>
<summary>Verified controls for document orientation in both editions</summary>

```yaml
testChanges:
  - id: DOC-ORIENTATION-EN
    status: VERIFIED
    base: 14c16ebf3b1dea52a15849f506ed81032bb55337
    expires: 2026-12-17
    reason: Enforce the configured document orientation and reject the other edition without weakening structure or content checks.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationReadmeStructureGovernanceTest,DocumentationPolicyGovernanceTest,DocumentationGovernanceCliComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationReadmeStructureGovernanceTest#ownerDocumentsKeepAReadableStableShape
      negative:
        - atf.tests.governance.documentation.DocumentationReadmeStructureGovernanceTest#theOtherEditionHeadingIsRejectedByTheStructuralRule
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationReadmeStructureRules.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationReadmeStructureGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
  - id: DOC-ORIENTATION-RO
    status: VERIFIED
    base: 007e3f660c4a2bf7dfa16cc1cb2c1996190c8020
    expires: 2026-12-17
    reason: Enforce the configured document orientation and reject the other edition without weakening structure or content checks.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationReadmeStructureGovernanceTest,DocumentationPolicyGovernanceTest,DocumentationGovernanceCliComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationReadmeStructureGovernanceTest#ownerDocumentsKeepAReadableStableShape
      negative:
        - atf.tests.governance.documentation.DocumentationReadmeStructureGovernanceTest#theOtherEditionHeadingIsRejectedByTheStructuralRule
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationReadmeStructureRules.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationReadmeStructureGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
```

</details>

<details>
<summary>Branch language and original publication provenance</summary>

```yaml
testChanges:
  - id: WHIST-DOCUMENTATION-LANGUAGE
    status: VERIFIED
    base: 129a629bb65cfedf6032b503c1b826fba63d4a42
    expires: 2026-12-18
    reason: Govern every Markdown path in the configured branch language, including tables, diagram captions and YAML explanations. Keep API data and original publication bytes intact; verify both editions against build declarations and reject mixed prose, changed links, commands and source hashes.
    proof: 'mvn -pl atf-tests -am -Dtest=DocumentationLanguageComponentTest,DocumentationGovernanceCliComponentTest,DocumentedModuleMapGovernanceTest,ReleasePresentationGovernanceTest,DocumentationArchiveGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#codeAndLinkTargetsRemainTechnicalDataWhileVisibleLabelsAreChecked
        - atf.tests.governance.documentation.cli.DocumentationGovernanceCliComponentTest#acceptsSeveralExplicitFilesAndExcludesAnUnselectedDefect
        - atf.tests.governance.documentation.factual.DocumentedModuleMapGovernanceTest#rootMapMatchesTheBuildDeclarations
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#everyHandoffKeepsSchematicNotesBoundToItsActualStateAndAsset
        - atf.tests.governance.documentation.DocumentationArchiveGovernanceTest#translatedReleaseGuidesRetainTheOriginalDocumentsAtTheirPinnedGitCommit
      negative:
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#englishBranchRejectsShortForeignLabelsAndDiagramCaptions
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#romanianBranchRejectsAnEnglishGuideOrOwnerHeading
        - atf.tests.governance.documentation.factual.DocumentedModuleMapGovernanceTest#aMissingModuleAChangedScopeAndAnInvertedEdgeAreRejected
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#translatedPresentationKeepsTheReleaseStructureAssetsAndOriginalPointCount
    paths:
      - atf-test-android/src/test/README.md
      - atf-test-android/src/test/java/atf/android/e2e/release/README.md
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationInventory.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationLanguageRules.java
      - atf-tests/src/test/README.md
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationAnchorGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationArchiveGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/factual/DocumentedModuleMap.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/factual/DocumentedModuleMapGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationLanguageComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/handoff/ReleaseCommitEvidence.java
      - atf-tests/tools/documentation-editions.mjs
      - atf-tests/tools/documentation-editions.test.mjs
      - atf-tests/tools/release-documentation-provenance.mjs
      - atf-tests/tools/release-documentation-provenance.test.mjs
      - atf-validation/src/test/README.md
      - config/atf-governance-policy.json
      - whist-lab-app/ai-lab/src/test/README.md
      - whist-lab-app/app/src/androidTest/README.md
      - whist-lab-app/app/src/main/java/ro/whistlab/app/README.md
      - whist-lab-app/app/src/rasterTest/README.md
      - whist-lab-app/app/src/test/README.md
      - whist-lab-app/atf-card-table-ui/src/androidTest/README.md
      - whist-lab-app/atf-card-table-ui/src/test/README.md
      - whist-lab-app/game-core/src/test/README.md
      - whist-lab-app/ios-bridge/src/test/README.md
      - whist-lab-app/test-audit/src/test/README.md
      - whist-lab-app/test-audit/src/testFixtures/README.md
```

</details>

<details>
<summary>Suite-local JVM reporting profile</summary>

```yaml
testChanges:
  - id: WHIST-COMPONENT-JVM-MAIN
    status: VERIFIED
    base: e36b62a1e53d3bf593533f6a14c52687be8a3596
    expires: 2026-12-18
    reason: Scope reporting options to the suite JVM and reject inherited option variables before preparation; preserve child-process oracles.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentJvmLauncherComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.execution.jvm.ComponentJvmLauncherComponentTest#scopeBelongsToTheSuiteAndChildBytesStreamsAndExitRemainOriginal
      negative:
        - atf.tests.execution.jvm.ComponentJvmLauncherComponentTest#whitespaceGlobalOptionsAreStillRejected
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theComponentJvmProfileCannotChangeWithoutAReviewedControl
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/execution/components/ComponentSuiteExecution.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/execution/jvm/ComponentJvmFixture.java
      - atf-tests/src/test/java/atf/tests/execution/jvm/ComponentJvmLauncherComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - scripts/run-component-suites.ps1
      - scripts/AtfComponentJvm.ps1
  - id: WHIST-COMPONENT-JVM-RO
    status: VERIFIED
    base: 080da13d8f4edb48637043a0f6b1083ce58bcce5
    expires: 2026-12-18
    reason: Scope reporting options to the suite JVM and reject inherited option variables before preparation; preserve child-process oracles.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentJvmLauncherComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.execution.jvm.ComponentJvmLauncherComponentTest#scopeBelongsToTheSuiteAndChildBytesStreamsAndExitRemainOriginal
      negative:
        - atf.tests.execution.jvm.ComponentJvmLauncherComponentTest#whitespaceGlobalOptionsAreStillRejected
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theComponentJvmProfileCannotChangeWithoutAReviewedControl
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/execution/components/ComponentSuiteExecution.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/execution/jvm/ComponentJvmFixture.java
      - atf-tests/src/test/java/atf/tests/execution/jvm/ComponentJvmLauncherComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - scripts/run-component-suites.ps1
      - scripts/AtfComponentJvm.ps1
```

</details>

<details>
<summary>Private signing boundary for source qualification</summary>

```yaml
testChanges:
  - id: WHIST-SIGNING-SOURCE-MAIN
    status: VERIFIED
    base: 23de3299a3ddd40a739fc5a444e80b48bb928aef
    expires: 2026-12-18
    reason: Exclude private local signing inputs from source identity and copying; keep public identity and refuse forged or indirect source paths.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SigningSourceIsolationComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#gitReconstructionMatchesACheckoutContainingALocalKey
      negative:
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#aForgedManifestCannotCopyPrivateMaterialOrPartiallyExportPublicFiles
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theSigningExportPolicyRequiresAnExplicitReview
    paths:
      - scripts/AtfSigningSourcePolicy.ps1
      - scripts/Get-AtfSourceInputs.ps1
      - scripts/Copy-AtfSourceInputs.ps1
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/LauncherSourceInventoryComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/SourceInputFixture.java
      - atf-tests/src/test/java/atf/tests/governance/support/SigningSourceIsolationComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
  - id: WHIST-SIGNING-SOURCE-RO
    status: VERIFIED
    base: e25226e090d2a2f55a265d1bfbb8993342d2c126
    expires: 2026-12-18
    reason: Exclude private local signing inputs from source identity and copying; keep public identity and refuse forged or indirect source paths.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SigningSourceIsolationComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#gitReconstructionMatchesACheckoutContainingALocalKey
      negative:
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#aForgedManifestCannotCopyPrivateMaterialOrPartiallyExportPublicFiles
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theSigningExportPolicyRequiresAnExplicitReview
    paths:
      - scripts/AtfSigningSourcePolicy.ps1
      - scripts/Get-AtfSourceInputs.ps1
      - scripts/Copy-AtfSourceInputs.ps1
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/LauncherSourceInventoryComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/SourceInputFixture.java
      - atf-tests/src/test/java/atf/tests/governance/support/SigningSourceIsolationComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
```

</details>

<details>
<summary>Seal publication inputs before observing GitHub</summary>

```yaml
testChanges:
  - id: WHIST-PUBLICATION-SEAL-MAIN
    status: VERIFIED
    base: 8032a380bd1e683ac47950216e22a7679ca0b4d4
    expires: 2026-12-18
    reason: Seal final catalog and manifests against published Git before inventory, recheck them after observation, and qualify local tags before reconstructing sources; retain projection and transport oracles.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleasePublicationSealComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ReleasePublicationSealComponentTest#publicationSealsTheManifestBeforeObservingGithub
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ReleasePublicationSealComponentTest#unqualifiedLocalTagsStopBeforeSourceReconstruction
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#publicationSealAndReconstructionRequireReviewedControls
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ReleasePublicationSealComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/collect-github-release-retention.mjs
      - atf-tests/tools/github-release-retention.test.mjs
      - atf-tests/tools/github-retention-fixture.mjs
      - atf-tests/tools/github-retention-seal.mjs
      - atf-tests/tools/github-retention-seal.test.mjs
      - atf-tests/tools/release-git-snapshot.test.mjs
      - atf-tests/tools/verify-release-git-snapshot.mjs
  - id: WHIST-PUBLICATION-SEAL-RO
    status: VERIFIED
    base: fbbe8127088a6015e8288f3290eb09bb430995c5
    expires: 2026-12-18
    reason: Seal final catalog and manifests against published Git before inventory, recheck them after observation, and qualify local tags before reconstructing sources; retain projection and transport oracles.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleasePublicationSealComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ReleasePublicationSealComponentTest#publicationSealsTheManifestBeforeObservingGithub
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ReleasePublicationSealComponentTest#unqualifiedLocalTagsStopBeforeSourceReconstruction
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#publicationSealAndReconstructionRequireReviewedControls
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ReleasePublicationSealComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/collect-github-release-retention.mjs
      - atf-tests/tools/github-release-retention.test.mjs
      - atf-tests/tools/github-retention-fixture.mjs
      - atf-tests/tools/github-retention-seal.mjs
      - atf-tests/tools/github-retention-seal.test.mjs
      - atf-tests/tools/release-git-snapshot.test.mjs
      - atf-tests/tools/verify-release-git-snapshot.mjs
```

</details>

<details>
<summary>Visual contract proof and mandatory native flow union</summary>

```yaml
testChanges:
  - id: WHIST-VISUAL-SCOPE-MAIN
    status: VERIFIED
    base: f7d876687c476c5684d0a3a7a27fc8e8b6956ddb
    expires: 2026-12-18
    reason: Narrow only verified numeric presentation contracts; keep mandatory component owners and union every interaction, persistence, scoring and unknown-runtime flow with native sanity.
    proof: 'mvn -o -pl atf-tests -am -Dtest=VisualContractScopeComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#provenTypographyKeepsNativeSanityAndItsComponentOwners
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#theRealPlannerBindsPinnedBytesAndKeepsUnavailableProofsConservative
      negative:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#mixedBehaviorAndBinaryChangesCannotLoseMandatoryFlows
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#unprovenContractsKeepEveryNativeFlow
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#visualScopeProofsCannotChangeWithoutReview
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/VisualContractScopeComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/visual-change-contracts.mjs
      - atf-tests/tools/visual-contract-sources.mjs
      - atf-tests/tools/visual-scope-fixture.mjs
      - atf-tests/tools/visual-contracts.test.mjs
  - id: WHIST-VISUAL-SCOPE-RO
    status: VERIFIED
    base: a04c0bda3823620774ee4a431b5c13662f6f7ff8
    expires: 2026-12-18
    reason: Narrow only verified numeric presentation contracts; keep mandatory component owners and union every interaction, persistence, scoring and unknown-runtime flow with native sanity.
    proof: 'mvn -o -pl atf-tests -am -Dtest=VisualContractScopeComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#provenTypographyKeepsNativeSanityAndItsComponentOwners
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#theRealPlannerBindsPinnedBytesAndKeepsUnavailableProofsConservative
      negative:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#mixedBehaviorAndBinaryChangesCannotLoseMandatoryFlows
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#unprovenContractsKeepEveryNativeFlow
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#visualScopeProofsCannotChangeWithoutReview
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/VisualContractScopeComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/visual-change-contracts.mjs
      - atf-tests/tools/visual-contract-sources.mjs
      - atf-tests/tools/visual-scope-fixture.mjs
      - atf-tests/tools/visual-contracts.test.mjs
```

</details>

<details>
<summary>English operational terminology and durable temporary-user ownership</summary>

```yaml
testChanges:
  - id: WHIST-DOC-RECOVERY-MAIN
    status: VERIFIED
    base: 05fde6ecb286184c044b89b161a6036a24357de0
    expires: 2026-12-18
    reason: Check English guide terminology against live code and preserve original users through durable ownership, interrupted preparation and lease-bound recovery with mock transport.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationTerminologyComponentTest,DocumentationGovernanceCliComponentTest,AndroidUserOwnershipComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#technicalLiteralsAndRomanianAreOutsideTheEnglishProseRule
        - atf.tests.governance.documentation.cli.DocumentationGovernanceCliComponentTest#translationArtifactsHaveTheSameFullAndSelectedDiagnostics
        - atf.tests.governance.documentation.release.rag.scope.AndroidUserOwnershipComponentTest#durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport
      negative:
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#linkLabelsAndTableCellsRemainVisible
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#userRecoveryAndEnglishVocabularyRequireReviewedControls
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationTerminologyRules.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidUserOwnershipComponentTest.java
      - atf-tests/tools/android-user-ownership.test.mjs
      - scripts/AtfAndroidUserOwnership.ps1
      - atf-test-android/src/test/java/atf/android/e2e/release/README.md
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.mjs
      - scripts/Invoke-AtfAndroidDisposableUser.ps1
  - id: WHIST-DOC-RECOVERY-RO
    status: VERIFIED
    base: 6ce614b643e76d62b068899871658040f90c7186
    expires: 2026-12-18
    reason: Check English guide terminology against live code and preserve original users through durable ownership, interrupted preparation and lease-bound recovery with mock transport.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationTerminologyComponentTest,DocumentationGovernanceCliComponentTest,AndroidUserOwnershipComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#technicalLiteralsAndRomanianAreOutsideTheEnglishProseRule
        - atf.tests.governance.documentation.cli.DocumentationGovernanceCliComponentTest#translationArtifactsHaveTheSameFullAndSelectedDiagnostics
        - atf.tests.governance.documentation.release.rag.scope.AndroidUserOwnershipComponentTest#durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport
      negative:
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#linkLabelsAndTableCellsRemainVisible
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#userRecoveryAndEnglishVocabularyRequireReviewedControls
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationTerminologyRules.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidUserOwnershipComponentTest.java
      - atf-tests/tools/android-user-ownership.test.mjs
      - scripts/AtfAndroidUserOwnership.ps1
      - atf-test-android/src/test/java/atf/android/e2e/release/README.md
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.mjs
      - scripts/Invoke-AtfAndroidDisposableUser.ps1
```

</details>

<details>
<summary>Native runtime provenance and portable Web/native adapter contracts</summary>

```yaml
testChanges:
  - id: WHIST-NATIVE-ADAPTER-MAIN
    status: VERIFIED
    base: 6016fe842dc7b7110fdf7812a4a54004bdb321b1
    expires: 2026-12-19
    reason: Separate native runtime and scenario identity from the running validator; preserve original receipts and verify real Web/native portable adapters with controlled measurements.
    proof: 'mvn -o -pl atf-tests -am -Dtest=NativeSourceProvenanceComponentTest,WhistSurfaceAdapterComponentTest,ReleaseRagAndroidComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.ReleaseRagAndroidComponentTest#nativeRuntimeAndValidatorRevisionsRemainIndependentInTheRealEvaluator
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#nativeSourceAdmissionAndAdapterOraclesCannotBypassReview
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/NativeEvidenceProbe.java
      - atf-tests/tools/android-e2e-provenance.mjs
      - atf-tests/tools/android-e2e-provenance.test.mjs
      - atf-tests/tools/source-input-identity.mjs
      - atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagAndroidComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/ReleaseSuiteReportingGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/android-e2e-evidence.mjs
      - atf-tests/tools/apply-android-e2e.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/prepare-android-e2e.mjs
  - id: WHIST-NATIVE-ADAPTER-RO
    status: VERIFIED
    base: 2ea1ccb42cb1d7b34f481438411bbe49b6d61c99
    expires: 2026-12-19
    reason: Separate native runtime and scenario identity from the running validator; preserve original receipts and verify real Web/native portable adapters with controlled measurements.
    proof: 'mvn -o -pl atf-tests -am -Dtest=NativeSourceProvenanceComponentTest,WhistSurfaceAdapterComponentTest,ReleaseRagAndroidComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.ReleaseRagAndroidComponentTest#nativeRuntimeAndValidatorRevisionsRemainIndependentInTheRealEvaluator
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#nativeSourceAdmissionAndAdapterOraclesCannotBypassReview
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/NativeEvidenceProbe.java
      - atf-tests/tools/android-e2e-provenance.mjs
      - atf-tests/tools/android-e2e-provenance.test.mjs
      - atf-tests/tools/source-input-identity.mjs
      - atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagAndroidComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/ReleaseSuiteReportingGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/android-e2e-evidence.mjs
      - atf-tests/tools/apply-android-e2e.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/prepare-android-e2e.mjs
```

</details>

<details>
<summary>Shared score readability and AI/human trick continuity</summary>

```yaml
testChanges:
  - id: SHARED-SCORE-MAIN
    status: VERIFIED
    base: 06badb5679bd9deb2ec87bae0a587128fbcec571
    expires: 2026-12-19
    reason: Review shared producer synchronization, native/Web portable adapters, local audit ownership and actual Web score typography; reject premature collection clearing.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest,WhistSurfaceAdapterComponentTest,NativeSourceProvenanceComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#nativeSourceAdmissionAndAdapterOraclesCannotBypassReview
    paths:
      - atf-ui-browser/tests/fixtures.mjs
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/trick-playback.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportAudit.java
      - whist-lab-app/atf-card-table-ui/src/test/README.md
  - id: SHARED-SCORE-RO
    status: VERIFIED
    base: 19fbb34af6791e57e890f4856b3ca3bfbb2a9bf2
    expires: 2026-12-19
    reason: Review shared producer synchronization, native/Web portable adapters, local audit ownership and actual Web score typography; reject premature collection clearing.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest,WhistSurfaceAdapterComponentTest,NativeSourceProvenanceComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#nativeSourceAdmissionAndAdapterOraclesCannotBypassReview
    paths:
      - atf-ui-browser/tests/fixtures.mjs
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/trick-playback.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryGridComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreContentComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportAudit.java
      - config/gradle-audit-collections.properties
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/test/README.md
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/boundary/UiLibraryBoundaryComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/seat/PlayerSeatMetricsComponentTest.java
```

</details>

<details>
<summary>Native score typography and release-note preflight</summary>

```yaml
testChanges:
  - id: SCORE-TYPOGRAPHY-MAIN
    status: VERIFIED
    base: 1e078d06f4000d848675d87497302bad3052903d
    expires: 2026-12-19
    reason: Align score adapters and qualify Android API compatibility, complete launcher fixtures, transport line endings and independent component reuse without weakening artifact or unknown-input rejection.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest,ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.tests.governance.source.android.AndroidJavaApiGovernanceTest#androidProductionAvoidsTheJava16StreamTerminal
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#governancePassesWithAnIsolatedRootAndThePinnedInputManifest
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#githubPublicationResumesByIdAndVerifiesDownloadsBeforeRotation
        - atf.tests.governance.documentation.release.rag.scope.AndroidUserOwnershipComponentTest#durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#checkoutTmpStopsBothLaunchersBeforeStagingOrMaven
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#componentResumeImportsOnlyVerifiedEvidenceFromTheInterruptedSnapshot
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/tools/publish-github-release.test.mjs
      - atf-ui-browser/tests/fixtures.mjs
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/ScoreTypographyProbe.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/boundary/UiLibraryBoundaryComponentTest.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/app/build.gradle
      - config/gradle-audit-collections.properties
      - scripts/AtfAndroidUserOwnership.ps1
      - atf-tests/tools/android-user-ownership.test.mjs
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidUserOwnershipComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
  - id: SCORE-TYPOGRAPHY-RO
    status: VERIFIED
    base: 910439dab4bbcd72afe2d0a18c4fb9f583d339c6
    expires: 2026-12-19
    reason: Apply the same reviewed source and test boundaries to the Romanian edition.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest,ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.tests.governance.source.android.AndroidJavaApiGovernanceTest#androidProductionAvoidsTheJava16StreamTerminal
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#governancePassesWithAnIsolatedRootAndThePinnedInputManifest
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#githubPublicationResumesByIdAndVerifiesDownloadsBeforeRotation
        - atf.tests.governance.documentation.release.rag.scope.AndroidUserOwnershipComponentTest#durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#checkoutTmpStopsBothLaunchersBeforeStagingOrMaven
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#componentResumeImportsOnlyVerifiedEvidenceFromTheInterruptedSnapshot
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/tools/publish-github-release.test.mjs
      - atf-ui-browser/tests/fixtures.mjs
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/ScoreTypographyProbe.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/boundary/UiLibraryBoundaryComponentTest.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/app/build.gradle
      - config/gradle-audit-collections.properties
      - scripts/AtfAndroidUserOwnership.ps1
      - atf-tests/tools/android-user-ownership.test.mjs
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidUserOwnershipComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
```

</details>

<details>
<summary>Release note summaries</summary>

```yaml
testChanges:
  - id: NOTE-SUMMARY-MAIN
    status: VERIFIED
    base: 25ac9be5697dea9664268b9982e287fc05797b51
    expires: 2026-12-19
    reason: Accept one bounded opening paragraph while preserving canonical sections, Unicode limits and historical notes.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleasePresentationGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#translatedPresentationKeepsTheReleaseStructureAssetsAndOriginalPointCount
      negative:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#missingEvidenceNonBulletProseAndUnrecordedChangesAreRejected
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
  - id: NOTE-SUMMARY-RO
    status: VERIFIED
    base: 290590801afd29e201b03ebf43f10c6c4e8ad5a8
    expires: 2026-12-19
    reason: Accept one bounded opening paragraph while preserving canonical sections, Unicode limits and historical notes.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleasePresentationGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#translatedPresentationKeepsTheReleaseStructureAssetsAndOriginalPointCount
      negative:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#missingEvidenceNonBulletProseAndUnrecordedChangesAreRejected
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
```

</details>

<details>
<summary>Result action typography</summary>

```yaml
testChanges:
  - id: SCORE-ACTIONS-MAIN
    status: VERIFIED
    base: 30812f432d7e744e6f2acf711e30dbd1848fbacb
    expires: 2026-12-19
    reason: Enlarge only result actions through shared roles and preserve narrow-window access, other typography and callback order.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreActionsRejectStaleRolesClippingAndRepeatedScaling
    paths:
      - atf-ui-browser/Build-WhistPages.ps1
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/score-presentation.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
  - id: SCORE-ACTIONS-RO
    status: VERIFIED
    base: f680c257777611dc19d3cdf24a4295ed9b697029
    expires: 2026-12-19
    reason: Enlarge only result actions through shared roles and preserve narrow-window access, other typography and callback order.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreActionsRejectStaleRolesClippingAndRepeatedScaling
    paths:
      - atf-ui-browser/Build-WhistPages.ps1
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/score-presentation.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
```

</details>

<details>
<summary>Standard report interface</summary>

```yaml
testChanges:
  - id: RPT-OVERVIEW-MAIN
    status: VERIFIED
    base: a07e597fb9238e89f6614643442c344058c66362
    expires: 2026-12-20
    reason: Preserve the standard English report interface and original case-owned captures, comparisons and outcomes.
    proof: 'mvn -o -pl atf-tests -am -Dtest=BrowserExtentPublicationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures
      negative:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#missingCaptureIsRejectedBeforeReportCreation
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#externalCaptureAndInvalidPngCannotBePublished
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#invalidLayerAndImageShapeDoNotCreatePartialReports
    paths:
      - atf-ui-browser/tests/BrowserExtent.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserReportFixture.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportComponentSuite.java
  - id: RPT-OVERVIEW-RO
    status: VERIFIED
    base: 4a7b06cb78b5e98bcca3f45d62d1681073327a9a
    expires: 2026-12-20
    reason: Preserve the standard English report interface and original case-owned captures, comparisons and outcomes.
    proof: 'mvn -o -pl atf-tests -am -Dtest=BrowserExtentPublicationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures
      negative:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#missingCaptureIsRejectedBeforeReportCreation
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#externalCaptureAndInvalidPngCannotBePublished
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#invalidLayerAndImageShapeDoNotCreatePartialReports
    paths:
      - atf-ui-browser/tests/BrowserExtent.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserReportFixture.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportComponentSuite.java
```

</details>

<details>
<summary>Shared menu and history adapters</summary>

```yaml
testChanges:
  - id: UI-MENU-HISTORY-MAIN
    status: VERIFIED
    base: 8812885a4ecba84f9dfca3980a2c0b4165e4b900
    expires: 2026-12-20
    reason: Bind native and browser menu intentions and history appearance through shared models, preserving legacy behavior and immutable library coordinates.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedMenuHistoryComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#historyTypographyAndDividersFollowTheNativePolicy
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#menuAdaptersKeepNativeLabelsOrderAndDispatch
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#newLibraryCoordinatesUseTwoPartsAndLegacyCoordinatesAreReuseOnly
      negative:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#invalidStylesAreAtomicAndClearingRestoresTheBaseline
    paths:
      - config/gradle-audit-collections.properties
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/Start-CardTableBrowser.ps1
      - atf-ui-browser/pages/src/main/java/ro/cardtable/browser/PageJson.java
      - atf-ui-browser/src/main/java/ro/cardtable/browser/BrowserGame.java
      - atf-ui-browser/src/main/web/index.html
      - atf-ui-browser/src/main/web/records.mjs
      - atf-ui-browser/src/main/web/table-menu.mjs
      - atf-ui-browser/src/main/web/table.css
      - atf-ui-browser/src/main/web/table.mjs
      - scripts/export-card-table-ui.ps1
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/menu/TableMenuContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/ScoreHistoryView.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/menu/TableMenuContentComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/uiadapter/SharedMenuHistoryComponentTest.java
      - atf-ui-browser/tests/MenuHistoryProbe.java
      - atf-ui-browser/tests/menu-history-contract.mjs
      - atf-ui-browser/tests/menu-history.test.mjs
      - atf-ui-browser/tests/package-version.test.mjs
      - scripts/CardTableLibraryVersion.psm1
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryAppearance.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryAppearanceComponentTest.java
  - id: UI-MENU-HISTORY-RO
    status: VERIFIED
    base: 82d9dd9426c5a3ee7826f187bacf29879e30d1e3
    expires: 2026-12-20
    reason: Bind native and browser menu intentions and history appearance through shared models, preserving legacy behavior and immutable library coordinates.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedMenuHistoryComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#historyTypographyAndDividersFollowTheNativePolicy
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#menuAdaptersKeepNativeLabelsOrderAndDispatch
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#newLibraryCoordinatesUseTwoPartsAndLegacyCoordinatesAreReuseOnly
      negative:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#invalidStylesAreAtomicAndClearingRestoresTheBaseline
    paths:
      - config/gradle-audit-collections.properties
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/Start-CardTableBrowser.ps1
      - atf-ui-browser/pages/src/main/java/ro/cardtable/browser/PageJson.java
      - atf-ui-browser/src/main/java/ro/cardtable/browser/BrowserGame.java
      - atf-ui-browser/src/main/web/index.html
      - atf-ui-browser/src/main/web/records.mjs
      - atf-ui-browser/src/main/web/table-menu.mjs
      - atf-ui-browser/src/main/web/table.css
      - atf-ui-browser/src/main/web/table.mjs
      - scripts/export-card-table-ui.ps1
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/menu/TableMenuContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/ScoreHistoryView.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/menu/TableMenuContentComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/uiadapter/SharedMenuHistoryComponentTest.java
      - atf-ui-browser/tests/MenuHistoryProbe.java
      - atf-ui-browser/tests/menu-history-contract.mjs
      - atf-ui-browser/tests/menu-history.test.mjs
      - atf-ui-browser/tests/package-version.test.mjs
      - scripts/CardTableLibraryVersion.psm1
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryAppearance.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryAppearanceComponentTest.java
```

</details>

<details>
<summary>Android delivery identity</summary>

```yaml
testChanges:
  - id: RELEASE-02113-MAIN
    status: VERIFIED
    base: 569462291a317cb4e4d6a2077d3a08fd336f92ec
    expires: 2026-12-20
    reason: Increment the Android delivery identity without changing gameplay; bind the advertised APK to candidate metadata.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleaseArtifactLayoutGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.ReleaseArtifactLayoutGovernanceTest#theMainReadmeOmitsRawApkDigestWhileTheVersionManifestRetainsIt
      negative:
        - atf.tests.governance.documentation.release.ReleaseArtifactLayoutGovernanceTest#aPreparedAuthenticatedCandidateMayDeclareOnlyItsExactFutureGithubAsset
    paths:
      - whist-lab-app/app/build.gradle
  - id: RELEASE-02113-RO
    status: VERIFIED
    base: 552d4389703219c9ee07bfb46102be17c5d3e6f2
    expires: 2026-12-20
    reason: Increment the Android delivery identity without changing gameplay; bind the advertised APK to candidate metadata.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleaseArtifactLayoutGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.ReleaseArtifactLayoutGovernanceTest#theMainReadmeOmitsRawApkDigestWhileTheVersionManifestRetainsIt
      negative:
        - atf.tests.governance.documentation.release.ReleaseArtifactLayoutGovernanceTest#aPreparedAuthenticatedCandidateMayDeclareOnlyItsExactFutureGithubAsset
    paths:
      - whist-lab-app/app/build.gradle
```

</details>

<details>
<summary>Native evidence admission after Web-only changes</summary>

```yaml
testChanges:
  - id: NATIVE-WEB-ADMISSION-MAIN
    status: VERIFIED
    base: 569462291a317cb4e4d6a2077d3a08fd336f92ec
    expires: 2026-12-20
    reason: Preserve original evidence for the same APK after browser-only changes, rejecting changed Android runtime, scenarios or provenance.
    proof: 'mvn -o -pl atf-tests -am -Dtest=NativeSourceProvenanceComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards
    paths:
      - atf-tests/tools/android-e2e-provenance.mjs
      - atf-tests/tools/android-e2e-provenance-fixture.mjs
      - atf-tests/tools/android-e2e-runtime-scope.test.mjs
      - atf-tests/tools/android-e2e-provenance.test.mjs
  - id: NATIVE-WEB-ADMISSION-RO
    status: VERIFIED
    base: 552d4389703219c9ee07bfb46102be17c5d3e6f2
    expires: 2026-12-20
    reason: Preserve original evidence for the same APK after browser-only changes, rejecting changed Android runtime, scenarios or provenance.
    proof: 'mvn -o -pl atf-tests -am -Dtest=NativeSourceProvenanceComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards
    paths:
      - atf-tests/tools/android-e2e-provenance.mjs
      - atf-tests/tools/android-e2e-provenance-fixture.mjs
      - atf-tests/tools/android-e2e-runtime-scope.test.mjs
      - atf-tests/tools/android-e2e-provenance.test.mjs
  - id: NATIVE-WEB-ADMISSION-COLLECTOR
    status: VERIFIED
    base: 1e078d06f4000d848675d87497302bad3052903d
    expires: 2026-12-20
    reason: Preserve original evidence for the same APK after browser-only changes, rejecting changed Android runtime, scenarios or provenance.
    proof: 'mvn -o -pl atf-tests -am -Dtest=NativeSourceProvenanceComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards
    paths:
      - atf-tests/tools/android-e2e-provenance.mjs
      - atf-tests/tools/android-e2e-provenance-fixture.mjs
      - atf-tests/tools/android-e2e-runtime-scope.test.mjs
      - atf-tests/tools/android-e2e-provenance.test.mjs
```

</details>

<details>
<summary>Shared menus and audit on the collector review base</summary>

```yaml
testChanges:
  - id: SHARED-MENU-AUDIT-BASE-MAIN
    status: VERIFIED
    base: 1e078d06f4000d848675d87497302bad3052903d
    expires: 2026-12-20
    reason: Bind shared menus, history presentation, report publication and isolated collection to real positive and negative comparisons on the declared Git base.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedMenuHistoryComponentTest,BrowserExtentPublicationComponentTest,ReleasePresentationGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#historyTypographyAndDividersFollowTheNativePolicy
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#menuAdaptersKeepNativeLabelsOrderAndDispatch
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures
      negative:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#invalidStylesAreAtomicAndClearingRestoresTheBaseline
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#externalCaptureAndInvalidPngCannotBePublished
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#aPreparedCandidateCannotClaimPublicationOrInventASourceCommit
    paths:
      - .github/workflows/whist-components.yml
      - atf-tests/src/test/java/atf/tests/governance/uiadapter/SharedMenuHistoryComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserReportFixture.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryAppearanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportComponentSuite.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/menu/TableMenuContentComponentTest.java
  - id: SHARED-MENU-AUDIT-BASE-RO
    status: VERIFIED
    base: 552d4389703219c9ee07bfb46102be17c5d3e6f2
    expires: 2026-12-20
    reason: Bind shared menus, history presentation, report publication and isolated collection to real positive and negative comparisons on the declared Git base.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedMenuHistoryComponentTest,BrowserExtentPublicationComponentTest,ReleasePresentationGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#historyTypographyAndDividersFollowTheNativePolicy
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#menuAdaptersKeepNativeLabelsOrderAndDispatch
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures
      negative:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#invalidStylesAreAtomicAndClearingRestoresTheBaseline
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#externalCaptureAndInvalidPngCannotBePublished
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#aPreparedCandidateCannotClaimPublicationOrInventASourceCommit
    paths:
      - .github/workflows/whist-components.yml
      - atf-tests/src/test/java/atf/tests/governance/uiadapter/SharedMenuHistoryComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserReportFixture.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryAppearanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportComponentSuite.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/menu/TableMenuContentComponentTest.java
```

</details>

<details>
<summary>Two-part release identity and source-bound audit admission</summary>

```yaml
testChanges:
  - id: TWO-PART-RELEASE-AND-AUDIT
    status: VERIFIED
    base: 0020521d3793c90c5da53d9efb53c8ab03e35680
    expires: 2026-12-20
    reason: Keep numeric two-part and historical ordering consistent across publication, reconstruction, download and the local projection; reject foreign audit source and report bytes before GitHub mutation.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleaseVersionComponentTest,ReleasePresentationGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#theApplicationUsesTheDeclaredTwoPartReleaseConvention
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#releaseAdaptersAcceptTwoPartNamesAndRejectForeignAuditCommits
      negative:
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#absentVersionCannotBecomeADownloadIdentity
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#aPreparedCandidateCannotClaimPublicationOrInventASourceCommit
    paths:
      - .github/workflows/whist-components.yml
      - atf-tests/src/main/java/atf/tests/distribution/ApkDownloadTarget.java
      - atf-tests/src/main/java/atf/tests/distribution/ReleaseVersion.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/handoff/ReleaseCommitEvidence.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactCatalog.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseCatalogPresentation.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseHandoffState.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/ReleaseSuiteReportingGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/versioning/ReleaseVersionComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/strategy/TestingStrategyRules.java
      - atf-tests/tools/check-release-artifacts.mjs
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/documentation-editions.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/github-release-journal.mjs
      - atf-tests/tools/github-release-retention.mjs
      - atf-tests/tools/local-release-sync.mjs
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - atf-tests/tools/publication-audit.mjs
      - atf-tests/tools/publication-audit.test.mjs
      - atf-tests/tools/publish-github-release.mjs
      - atf-tests/tools/record-release-integrity.mjs
      - atf-tests/tools/release-documentation-provenance.mjs
      - atf-tests/tools/release-version.mjs
      - atf-tests/tools/release-version.test.mjs
      - atf-tests/tools/verify-release-git-snapshot.mjs
      - config/release-platform-rag-policy.json
      - whist-lab-app/app/build.gradle
```

</details>

<details>
<summary>Language-aware product links</summary>

```yaml
testChanges:
  - id: BILINGUAL-GAME-SECTION
    status: VERIFIED
    base: df7d1954a0b4098f9678ed54acc911a57600adff
    expires: 2026-12-20
    reason: Read the configured documentation language before checking the product section; retain exact APK and online links and reject foreign publication URLs.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleaseArtifactLayoutGovernanceTest,ReleasePresentationGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.ReleaseArtifactLayoutGovernanceTest#theGameSectionHasOnlyTheDirectApkAndTheOnlineGame
      negative:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#rawRepositoryLinksAndAnotherVersionsAssetsAreRejected
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java
```

</details>


<details>
<summary>Maven preparation responsibility and recovery admission</summary>

```yaml
testChanges:
  - id: MAVEN-PREPARATION-BOUNDARY
    status: VERIFIED
    base: 08d0c425bba7ea9f58b5188612654d6b252a55ba
    expires: 2026-12-20
    reason: Separate Maven output admission from snapshot recovery while retaining rejection of altered classpaths, changed classes and foreign or incomplete preparation receipts; preserve offline ownership.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#nodeContractTestsKeepReuseClassificationInsideTheRealBuildBoundary
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#componentResumeImportsOnlyVerifiedEvidenceFromTheInterruptedSnapshot
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - scripts/AtfComponentResume.ps1
      - scripts/AtfComponentMaven.ps1
      - scripts/run-component-suites.ps1
      - whist-lab-app/app/build.gradle
```

</details>

<details>
<summary>Independent ATF collection ownership and immutable evidence reuse</summary>

```yaml
testChanges:
  - id: RAG-ATF-COLLECTION-OWNERS
    status: VERIFIED
    base: 2da37aa6d962db58ef9954c5da96f44e09154ab6
    expires: 2026-12-20
    reason: Partition positive primary inventories without changing leaf identities; isolate editorial and payload execution, preserve original receipts, and reject historical relabeling, shared or unknown changes and forged suites.
    proof: 'mvn -o -pl atf-tests -am -Dtest=AtfCollectionOwnershipComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#positiveInventoriesPartitionEveryAtfComponentWithoutChangingLeafNames
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#documentationChangesReuseIndependentOriginalReceipts
      negative:
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#sharedUnknownAndHistoricalInputsCannotHideAffectedCollections
    paths:
      - atf-tests/src/test/java/atf/tests/execution/components/ComponentSuiteExecution.java
      - atf-tests/src/test/java/atf/tests/governance/suites/AtfCollectionOwnershipComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/AtfComponentSuite.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfAndroidComponentSuite.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfDocumentationComponentSuite.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfGithubComponentSuite.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfOrchestrationComponentSuite.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfProvenanceComponentSuite.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfReportingComponentSuite.java
      - atf-tests/tools/atf-component-ownership.mjs
      - atf-tests/tools/atf-component-ownership.test.mjs
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/prepare-component-reuse.mjs
      - atf-tests/tools/release-test-scope.mjs
      - config/atf-component-collections.json
      - config/atf-test-suites.json
      - scripts/run-component-suites.ps1
```

</details>

<details>
<summary>Current catalog and publication contracts in governance fixtures</summary>

```yaml
testChanges:
  - id: RAG-CURRENT-GOVERNANCE-FIXTURES
    status: VERIFIED
    base: 2da37aa6d962db58ef9954c5da96f44e09154ab6
    expires: 2026-12-20
    reason: Resolve the current report index from the catalog, validate current notes with the publication validator, and copy the declared ownership configuration into isolated launcher fixtures without changing historical dossiers.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationPolicyGovernanceTest,ReleasePresentationGovernanceTest,QualificationLauncherComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#sealedReportIndexesStayOutsideOwnerProseAndLanguageRules
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#currentPublisherNotesUseTheCanonicalValidatorAndRetainTheAuditLink
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#governancePassesWithAnIsolatedRootAndThePinnedInputManifest
      negative:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#missingEvidenceNonBulletProseAndUnrecordedChangesAreRejected
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#checkoutTmpStopsBothLaunchersBeforeStagingOrMaven
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
```

</details>

<details>
<summary>Editorial source guides and protected fixture payloads</summary>

```yaml
testChanges:
  - id: RAG-NONBUILD-DOCUMENTATION
    status: VERIFIED
    base: 2da37aa6d962db58ef9954c5da96f44e09154ab6
    expires: 2026-12-20
    reason: Keep source-root Markdown guides editorial while resource payloads, executable tests and collection selection contracts retain their reviewed controls across real Git states.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#sourceReadmesRemainEditorialAcrossGitStates
      negative:
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#markdownFixturesAndSelectionContractsRemainProtected
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
```

</details>

<details>
<summary>Compiler identity and cross-run Maven output admission</summary>

```yaml
testChanges:
  - id: RAG-MAVEN-COMPILER-REUSE
    status: VERIFIED
    base: d77e2444643082ad481a9d90608e1d78daff9c13
    expires: 2026-12-20
    reason: Separate editorial execution data from compilation, validate the toolchain and every cached output under an exclusive lease, and reject altered sources, dependencies, paths and receipts without crediting reused output as executed tests.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#editorialChangesReuseVerifiedMavenCompilationAndKeepFreshExecutionInputs
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#compilerInventoriesKeepPayloadsAndRejectInvalidSourceIdentities
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/tools/component-compiler-inputs.mjs
      - atf-tests/tools/component-compiler-inputs.test.mjs
      - atf-tests/tools/component-maven-reuse.test.mjs
      - atf-tests/tools/release-test-scope.mjs
      - scripts/AtfCompilerIdentity.ps1
      - scripts/AtfComponentMaven.ps1
      - scripts/AtfComponentMavenReuse.ps1
      - scripts/run-component-suites.ps1
```

</details>

<details>
<summary>Canonical library-source navigation</summary>

```yaml
testChanges:
  - id: J4-LIBRARY-SOURCE-NOTE-EN-20261122
    status: VERIFIED
    base: 65bb00dbcd555a9d6e9284d3088014e77be06795
    expires: 2026-12-22
    reason: Allow only the requested canonical source note in direct library guides, retaining negative controls for unrelated projects, wrong routes, fenced examples and other documents.
    proof: mvn -B -ntp -pl atf-tests -am "-Dtest=DocumentationPolicyGovernanceTest,DocumentationAnchorGovernanceTest,DocumentationLinkGovernanceTest,DocumentationStyleGovernanceTest,DocumentationProseGovernanceTest,DocumentationReadmeStructureGovernanceTest" "-Datf.governance.testChanges.baseRef=65bb00dbcd555a9d6e9284d3088014e77be06795" "-Dsurefire.failIfNoSpecifiedTests=false" test
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#canonicalLibrarySourceNotesAreNavigationInBothLanguages
      negative:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#librarySourceNoteCannotHideWrongPathsFencesOrExtraProjectText
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
  - id: J4-LIBRARY-SOURCE-NOTE-RO-20261122
    status: VERIFIED
    base: be23ba5416fe090807e34b73bc8bf3ed8e4f61ca
    expires: 2026-12-22
    reason: Allow only the requested canonical source note in direct library guides, retaining negative controls for unrelated projects, wrong routes, fenced examples and other documents.
    proof: mvn -B -ntp -pl atf-tests -am "-Dtest=DocumentationPolicyGovernanceTest,DocumentationAnchorGovernanceTest,DocumentationLinkGovernanceTest,DocumentationStyleGovernanceTest,DocumentationProseGovernanceTest,DocumentationReadmeStructureGovernanceTest" "-Datf.governance.testChanges.baseRef=be23ba5416fe090807e34b73bc8bf3ed8e4f61ca" "-Dsurefire.failIfNoSpecifiedTests=false" test
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#canonicalLibrarySourceNotesAreNavigationInBothLanguages
      negative:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#librarySourceNoteCannotHideWrongPathsFencesOrExtraProjectText
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
```

</details>

<details>
<summary>Static proof selectors and source-owned testing matrix</summary>

```yaml
testChanges:
  - id: RAG-STATIC-SELECTOR-ADMISSION
    status: VERIFIED
    base: 79434786cdc484924cad429ef8356681be9e596c
    expires: 2026-12-22
    reason: Admit unambiguous static component declarations before copying sources or caches and Maven, retain authoritative JUnit discovery, and govern the module-level testing map against live declarations.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeSelectorComponentTest,TestLayerGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.changes.TestChangeSelectorComponentTest#staticSourceDeclarationsAdmitWithoutLoadingApplicationClasses
        - atf.tests.governance.source.TestLayerGovernanceTest#theGuideMapsEveryGovernedSourceModuleWithoutClaimingBinarySourceSuites
      negative:
        - atf.tests.governance.source.changes.TestChangeSelectorComponentTest#dynamicAndAmbiguousDeclarationsFailBeforeProofExecution
        - atf.tests.governance.source.TestLayerGovernanceTest#theGuideCannotHideMissingDuplicateOrIncorrectLevels
    paths:
      - atf-tests/src/main/java/atf/tests/jvm/JavaSourceParser.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/main/java/atf/tests/testchange/selectors/SelectorSources.java
      - atf-tests/src/main/java/atf/tests/testchange/selectors/StaticProofSelectors.java
      - atf-tests/src/test/java/atf/tests/governance/source/JavaTestDeclarations.java
      - atf-tests/src/test/java/atf/tests/governance/source/TestLayerGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeAdmissionFixture.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeGitFixture.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeSelectorComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/AtfComponentSuite.java
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Documentation language — anchor metadata and visible prose</summary>

```yaml
testChanges:
  - id: J4-DOC-ANCHORS-EN-20261122
    status: VERIFIED
    base: 79434786cdc484924cad429ef8356681be9e596c
    expires: 2026-12-22
    reason: Exclude HTML anchor attributes from language classification while retaining foreign visible labels, adjacent prose, escaped examples and original diagnostic lines.
    proof: mvn -B -ntp -pl atf-tests -am test "-Dtest=DocumentationLanguageComponentTest,DocumentationPolicyGovernanceTest,DocumentationReadmeStructureGovernanceTest" "-Datf.governance.testChanges.baseRef=79434786cdc484924cad429ef8356681be9e596c" "-Dsurefire.failIfNoSpecifiedTests=false"
    checks:
      positive:
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#anchorAttributesRemainTechnicalDataInBothEditions
      negative:
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#anchorMarkupCannotHideForeignVisibleTextOrItsLine
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationLanguageRules.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationLanguageComponentTest.java
  - id: J4-DOC-ANCHORS-RO-20261122
    status: VERIFIED
    base: b45ef6424a417375fe5cf92ed8e5da1935a392c2
    expires: 2026-12-22
    reason: Exclude HTML anchor attributes from language classification while retaining foreign visible labels, adjacent prose, escaped examples and original diagnostic lines.
    proof: mvn -B -ntp -pl atf-tests -am test "-Dtest=DocumentationLanguageComponentTest,DocumentationPolicyGovernanceTest,DocumentationReadmeStructureGovernanceTest" "-Datf.governance.testChanges.baseRef=b45ef6424a417375fe5cf92ed8e5da1935a392c2" "-Dsurefire.failIfNoSpecifiedTests=false"
    checks:
      positive:
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#anchorAttributesRemainTechnicalDataInBothEditions
      negative:
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#anchorMarkupCannotHideForeignVisibleTextOrItsLine
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationLanguageRules.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationLanguageComponentTest.java
```

</details>

<details>
<summary>Editorial proof continuation and adaptive browser admission</summary>

```yaml
testChanges:
  - id: RAG-EDITORIAL-PROOF-CONTINUATION
    status: VERIFIED
    base: eab1603b30fa40c026bcc86ce1220854d8b91761
    expires: 2026-12-22
    reason: Preserve original proof identity across verified linear editorial descendants, reject executable or control changes, and keep the adaptive browser component entry points under explicit source review.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeEditorialProofComponentTest,WhistSurfaceAdapterComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.changes.TestChangeEditorialProofComponentTest#editorialDescendantsPreserveOriginalProofBytesAndExposeProvenance
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#adaptiveWebMenusKeepTextIndependentOfTheUsableViewport
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#dropdownAndScoreTypographyPreserveNativeFactsAndCommandBindings
      negative:
        - atf.tests.governance.source.changes.TestChangeEditorialProofComponentTest#executableInputsControlsAndRevertedCodeCannotReuseEditorialProofs
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#surfaceLifecycleRejectsInvalidPagesAndCallbacksAfterDisposal
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/EditorialProofContinuation.java
      - atf-tests/src/main/java/atf/tests/testchange/GitTestChangeSnapshot.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeAdmissionCli.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeControls.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeProof.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeEditorialProofComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeAdmissionFixture.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/AtfComponentSuite.java
      - atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java
      - atf-ui-browser/tests/responsive-surface.test.mjs
      - atf-ui-browser/tests/responsive-fixture.mjs
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - atf-ui-browser/tests/score-typography.test.mjs
      - atf-ui-browser/tests/surface-lifecycle.test.mjs
      - atf-ui-browser/tests/responsive-game.mjs
      - atf-ui-browser/tests/packaged-sanity.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Readable paging and stable seat painting</summary>

```yaml
testChanges:
  - id: WEB-PAGING-PAINT
    status: VERIFIED
    base: e79c84cad19dd559bb6b70fe529196783f7bf318
    expires: 2026-12-23
    reason: Verify readable compact paging controls and preserve native-style seat rows, fitted ink, accessible facts and unchanged frame geometry across actor handoffs.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#adaptiveWebMenusKeepTextIndependentOfTheUsableViewport
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#surfaceLifecycleRejectsInvalidPagesAndCallbacksAfterDisposal
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-ui-browser/tests/responsive-surface.test.mjs
      - atf-ui-browser/tests/seat-paint.test.mjs
      - atf-ui-browser/tests/turn-continuity.mjs
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Mounted table continuity and shared browser qualification</summary>

```yaml
testChanges:
  - id: WEB-MOUNTED-PACKAGE
    status: VERIFIED
    base: c4e49a7e3ec248678c5a0fc93fc417fd2f9a869f
    expires: 2026-12-23
    reason: Verify mounted board continuity, pending AutoGame intent, ranking spacing and available-height fitting, and portable exports of the same shared components.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#adaptiveWebMenusKeepTextIndependentOfTheUsableViewport
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#surfaceLifecycleRejectsInvalidPagesAndCallbacksAfterDisposal
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/tests/automation-handoff.test.mjs
      - atf-ui-browser/tests/packaged-sanity.mjs
      - atf-ui-browser/tests/responsive-fixture.mjs
      - atf-ui-browser/tests/responsive-surface.test.mjs
      - atf-ui-browser/tests/seat-paint.test.mjs
      - atf-ui-browser/tests/surface-lifecycle.test.mjs
      - atf-ui-browser/tests/turn-continuity.mjs
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Stable table transitions and unambiguous reader dismissal</summary>

```yaml
testChanges:
  - id: WEB-TRANSITION-STABILITY
    status: VERIFIED
    base: bb3430825c04293d98ceaee30dfe4dd181c5faea
    expires: 2026-12-23
    reason: Verify retained headers and hand scroll, fresh menu callbacks, delayed layout isolation, explicit native-frame opt-in and Close distinct from Previous/Next.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#adaptiveWebMenusKeepTextIndependentOfTheUsableViewport
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#surfaceLifecycleRejectsInvalidPagesAndCallbacksAfterDisposal
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/tests/layout-delivery.test.mjs
      - atf-ui-browser/tests/responsive-fixture.mjs
      - atf-ui-browser/tests/responsive-surface.test.mjs
      - atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java
      - atf-ui-browser/tests/seat-paint.test.mjs
      - atf-ui-browser/tests/turn-continuity.mjs
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Mounted cards and input ownership across phase changes</summary>

```yaml
testChanges:
  - id: WEB-CARD-OWNERSHIP
    status: VERIFIED
    base: 06383795adfecdc8189cdcf000412d0403fbd207
    expires: 2026-12-23
    reason: Retain compatible hand and deposited faces, rebind exactly one delegated owner, invalidate font continuations before geometry and separate source-cache checks from native policy compilation.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#mountedCardsAndLayoutOwnersSurvivePhaseAndActorChanges
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#surfaceLifecycleRejectsInvalidPagesAndCallbacksAfterDisposal
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/tests/asset-revision.test.mjs
      - atf-ui-browser/tests/hand-lifecycle.test.mjs
      - atf-ui-browser/tests/layout-delivery.test.mjs
      - atf-ui-browser/tests/responsive-surface.test.mjs
      - atf-ui-browser/tests/seat-paint.test.mjs
      - atf-ui-browser/tests/turn-continuity.mjs
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Intrinsic table reservation and stable successive hands</summary>

```yaml
testChanges:
  - id: WEB-INTRINSIC-FOOTPRINT
    status: VERIFIED
    base: c07a11a46e2c48a25272ea81de5f63797794538b
    expires: 2026-12-23
    reason: Keep fitted seat rows single-line, separate native viewport policy and minimum reservation from allocated scene size, and verify deposited-card boundaries without masking intentional fan recentering.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#mountedCardsAndLayoutOwnersSurvivePhaseAndActorChanges
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#adaptiveWebMenusKeepTextIndependentOfTheUsableViewport
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#surfaceLifecycleRejectsInvalidPagesAndCallbacksAfterDisposal
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/tests/layout-delivery.test.mjs
      - atf-ui-browser/tests/match-continuity.mjs
      - atf-ui-browser/tests/responsive-surface.test.mjs
      - atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java
      - atf-ui-browser/tests/seat-measurements.test.mjs
      - atf-ui-browser/tests/table-continuity.mjs
      - atf-ui-browser/tests/table-continuity.test.mjs
      - atf-ui-browser/tests/table-height.test.mjs
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - config/atf-component-dependencies.json
```

</details>


<details>
<summary>Ordered browser deposits and evaluated report assertions</summary>

```yaml
testChanges:
  - id: WEB-ORDERED-CHECKPOINTS
    status: VERIFIED
    base: 6cd3958315261c95218de7f724fdc4a7ad907960
    expires: 2026-12-23
    reason: Preserve ordered before/after deposit evidence, individual assertion rows and action-versus-observation ownership, rejecting incomplete or contradictory input without rewriting historical reports.
    proof: 'mvn -o -pl atf-tests -am -Dtest=BrowserExtentPublicationComponentTest,WhistSurfaceAdapterComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#orderedStepsKeepEachActionBeforeAfterAndAssertionsInTheirOriginalOwner
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#individualDepositsKeepOrderedBeforeAfterComparisons
      negative:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#missingCaptureIsRejectedBeforeReportCreation
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#externalCaptureAndInvalidPngCannotBePublished
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#invalidLayerAndImageShapeDoNotCreatePartialReports
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#incompleteOrContradictoryStepEvidenceIsRejectedBeforePublication
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserReportFixture.java
      - atf-ui-browser/tests/BrowserExtent.java
      - atf-ui-browser/tests/match-continuity.mjs
      - atf-ui-browser/tests/table-checkpoints.mjs
      - atf-ui-browser/tests/table-checkpoints.test.mjs
      - atf-ui-browser/tests/table-continuity.mjs
      - config/atf-component-dependencies.json
```

</details>


<details>
<summary>Intrinsic handoff anchors and portable Web dossier</summary>

```yaml
testChanges:
  - id: WEB-HANDOFF-DOSSIER
    status: VERIFIED
    allowDelete: true
    base: 9cc19f7f16b1c09f6e5737ffee9a97e18a26e668
    expires: 2026-12-23
    reason: Isolate inherited bottom-anchor stretch in controlled component fixtures without screenshots, retain one illustrated browser journey, and reject corrupt or misclassified portable report inventories.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest,WebReleaseDossierComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#preparedAndLiveSeatsShareIntrinsicHeightWithoutBottomAnchorStretch
        - atf.tests.governance.documentation.release.web.WebReleaseDossierComponentTest#portableWebInventoryChecksHashesPathsAndEvidenceOwnership
        - atf.tests.governance.documentation.release.web.WebReleaseDossierComponentTest#currentWebDossierHasOneIllustratedFlowAndNoComponentCaptures
      negative:
        - atf.tests.governance.documentation.release.ReleaseArtifactLayoutGovernanceTest#onlyTheExplicitTypedWebFolderIsAcceptedBesideAndroidVersions
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactLayoutGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/web/WebReleaseDossierComponentTest.java
      - config/atf-component-dependencies.json
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseArtifactCatalog.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfDocumentationComponentSuite.java
      - atf-ui-browser/src/main/web/index.html
      - atf-ui-browser/src/main/web/seat-presentation.mjs
      - atf-ui-browser/src/main/web/surface-fit.mjs
      - atf-ui-browser/src/main/web/table.mjs
      - atf-ui-browser/tests/match-continuity.mjs
      - atf-ui-browser/tests/responsive-surface.test.mjs
      - config/atf-component-collections.json
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/WebReleaseDossierComponentTest.java
      - atf-tests/tools/web-release-dossier.mjs
      - atf-tests/tools/web-release-dossier.test.mjs
      - atf-ui-browser/tests/seat-handoff.test.mjs
```

</details>


<details>
<summary>Shared legal-deal and every-seat phase contract</summary>

```yaml
testChanges:
  - id: WEB-EVERY-SEAT-PHASES
    status: VERIFIED
    base: ae48475aaa6a6d52759f51597dcab73baa6ccbdd
    expires: 2026-12-23
    reason: Reuse one native and Web policy matrix for every legal deal, actor and collection winner, with anchor regressions, deliberate drift mutations and equivalent audit indexes without component screenshots.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest,WebReleaseDossierComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#everySeatAndLegalDealShareOneNativeWebPhaseContract
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#preparedAndLiveSeatsShareIntrinsicHeightWithoutBottomAnchorStretch
        - atf.tests.governance.documentation.release.web.WebReleaseDossierComponentTest#portableWebInventoryChecksHashesPathsAndEvidenceOwnership
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#sharedPhaseOracleRejectsEveryDisplacedOrMissingSeat
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/tools/web-release-dossier.test.mjs
      - atf-ui-browser/tests/match-continuity.mjs
      - atf-ui-browser/tests/seat-handoff.test.mjs
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/GameTablePhaseGeometryComponentTest.java
      - atf-ui-browser/tests/phase-contract.test.mjs
      - atf-ui-browser/tests/portable-contract.mjs
      - atf-ui-browser/tests/ro/cardtable/browser/TablePhaseAdapters.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/NativePhaseAdapter.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TablePhaseContract.java
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Portable presentation ownership and minimal flow admission</summary>

```yaml
testChanges:
  - id: UI-THREE-MODULES
    status: VERIFIED
    base: 79056231f4264770968f7876ce7ac1704d2c8de9
    expires: 2026-12-23
    allowDelete: true
    reason: Move unchanged portable classes and component identities to explicit owners; preserve adapter baselines and reject incomplete observations, long flows and unprotected unit or browser tests.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest,DocumentedModuleMapGovernanceTest,GradleAuditGovernanceTest,BrowserExtentPublicationComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#everySeatAndLegalDealShareOneNativeWebPhaseContract
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#portableMenuRolesPreserveBothAdapterBaselines
        - atf.tests.governance.documentation.factual.DocumentedModuleMapGovernanceTest#rootMapMatchesTheBuildDeclarations
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#everyGradleTestClassHasOneExplicitLayerAndAnAuditedBase
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#presentationOwnersSelectOnlyAffectedCollections
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#fullDiffSeesCommittedStagedUnstagedUntrackedAndIgnoredTestPaths
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#sharedPhaseOracleRejectsEveryDisplacedOrMissingSeat
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#minimalFlowsReconcileHandsAndRejectMatricesOrExcessiveCaptures
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#passingTableFlowsCannotUnderstateTheirHandOrCaptureBudget
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#markdownFixturesAndSelectionContractsRemainProtected
        - atf.tests.governance.documentation.factual.DocumentedModuleMapGovernanceTest#explicitModuleLocationsRejectDuplicateAndEscapingAliases
    paths:
      - .github/workflows/whist-pages.yml
      - atf-test-android/src/test/java/atf/android/e2e/display/profile/ResponsiveDisplayProfile.java
      - atf-test-android/src/test/java/atf/android/e2e/display/profile/ResponsiveDisplayProfileComponentTest.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/factual/BuildModuleDeclarations.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/factual/DocumentedModuleMapGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseRagPlatformGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/gradle/GradleAuditGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - atf-tests/tools/visual-change-contracts.mjs
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/Prepare-WhistCatalog.ps1
      - atf-ui-browser/Start-CardTableBrowser.ps1
      - atf-ui-browser/Start-WhistBrowser.ps1
      - atf-ui-browser/build.gradle
      - atf-ui-browser/lib/browser-build-0.2.14.json
      - atf-ui-browser/lib/browser-build-0.2.15.json
      - atf-ui-browser/lib/browser-build.json
      - atf-ui-browser/pages/verify-card-adapter.mjs
      - atf-ui-browser/src/main/java/ro/cardtable/browser/BrowserGame.java
      - atf-ui-browser/src/main/web/index.html
      - atf-ui-browser/src/main/web/surface-fit.mjs
      - atf-ui-browser/src/main/web/table.mjs
      - atf-ui-browser/src/main/web/viewport.mjs
      - atf-ui-browser/tests/BrowserExtent.java
      - atf-ui-browser/tests/flow-budget.mjs
      - atf-ui-browser/tests/flow-budget.test.mjs
      - atf-ui-browser/tests/match-continuity.mjs
      - atf-ui-browser/tests/menu-history.test.mjs
      - atf-ui-browser/tests/menu-roles.test.mjs
      - atf-ui-browser/tests/portable-contract.mjs
      - atf-ui-browser/tests/ro/cardtable/contract/MenuTypographyContract.java
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - config/atf-component-dependencies.json
      - config/atf-governance-policy.json
      - config/gradle-audit-collections.properties
      - config/release-platform-rag-policy.json
      - scripts/export-card-table-ui.ps1
      - scripts/run-component-suites.ps1
      - scripts/run-whist-responsive-sanity.ps1
      - whist-lab-app/app/achievement-catalog.gradle
      - whist-lab-app/app/build.gradle
      - whist-lab-app/app/src/main/achievement-rules/whist-achievements.yaml
      - whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistAchievementCatalog.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistAchievementCodec.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistAchievementRepository.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistAchievementState.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistHandFacts.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/WhistMatchFacts.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/presentation/WhistAchievementPresentation.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/animation/TrickCollectionRequest.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/BidGridMetrics.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/HumanBidSelection.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/cards/CardFaceMetrics.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/cards/WhistCardFace.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/hand/HandLayoutMetrics.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/highscores/HighScoreCodec.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/highscores/HighScorePresentation.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/highscores/HighScoreRepository.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/highscores/HighScores.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/localization/WhistUiText.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/GameCheckpointCodec.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/SavedNumbers.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/score/ScoreHistoryColumn.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/score/ScoreHistoryTable.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/score/ScoreRows.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/screens/MatchSetup.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/table/TableLayoutMetrics.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/table/TrickCardMetrics.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/table/content/WhistTableContent.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/theme/GameHeaderTypography.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/theme/TrumpTileStyle.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/GameTablePhaseGeometryComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TablePhaseContract.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/animation/CollectionTarget.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/animation/TrickCollectionMotion.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/animation/TrickCollectionRun.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/cards/CardFace.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/cards/CardFaceInk.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/cards/CardFaceMetrics.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/cards/CardHighlight.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/hand/HandCard.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/hand/HandInput.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/hand/HandLayoutMetrics.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/menu/HomeMenuContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/menu/TableMenuContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/profile/NameEdit.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryAppearance.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryGridMetrics.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryScrollSync.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/ScoreTableContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/ScoreTypography.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/screens/GameTitleLayout.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/screens/ScreenViews.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/seat/PlayerSeatMetrics.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/seat/SeatContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/seat/SeatFactLayout.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/seat/SeatTypography.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/PlayedCardContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/PlayedCardMetrics.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/PlayedCardStyle.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/ScoreGridStyle.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/TableGeometry.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/TableSeatLayout.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/TableSizing.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/TableViewportPolicy.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/trump/TrumpTileMetrics.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/theme/TableColors.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/turn/TurnPacing.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/boundary/UiLibraryBoundaryComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/cards/CardFaceInkComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/cards/CardHighlightComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/cards/CardTypographyComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/hand/HandGeometryComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/hand/HandInputTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/menu/HomeMenuContentComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/menu/TableMenuContentComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/profile/NameEditComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryAppearanceComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryGridComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreContentComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/screens/GameTitleLayoutComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/seat/PlayerSeatMetricsComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/seat/SeatContentComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/seat/SeatFactLayoutComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/table/TablePresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/turn/TurnPacingComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/resources/card-typography.tsv
      - whist-lab-app/atf-card-table-ui/src/test/resources/hand-feedback.tsv
      - whist-lab-app/card-table-core/build.gradle
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/animation/CollectionTarget.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/animation/TrickCollectionMotion.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/animation/TrickCollectionRun.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/cards/CardFace.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/cards/CardFaceInk.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/cards/CardFaceMetrics.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/cards/CardHighlight.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/hand/HandCard.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/hand/HandInput.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/hand/HandLayoutMetrics.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/menu/HomeMenuContent.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/menu/MenuTypography.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/menu/TableMenuContent.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/profile/NameEdit.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/score/HistoryAppearance.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/score/HistoryContent.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/score/HistoryGridMetrics.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/score/HistoryScrollSync.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/score/ScoreTableContent.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/score/ScoreTypography.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/screens/GameTitleLayout.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/seat/PlayerSeatMetrics.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/seat/SeatContent.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/seat/SeatFactLayout.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/seat/SeatTypography.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/PlayedCardContent.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/PlayedCardMetrics.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/PlayedCardStyle.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/ScoreGridStyle.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/TableGeometry.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/TableSeatLayout.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/TableSizing.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/TableViewportPolicy.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/table/trump/TrumpTileMetrics.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/theme/TableColors.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/turn/TurnPacing.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/boundary/PortableSurfaceComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/cards/CardFaceInkComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/cards/CardHighlightComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/cards/CardTypographyComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/hand/HandGeometryComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/hand/HandInputTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/menu/HomeMenuContentComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/menu/TableMenuContentComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/profile/NameEditComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/score/HistoryAppearanceComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/score/HistoryGridComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/score/ScoreContentComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/screens/GameTitleLayoutComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/seat/PlayerSeatMetricsComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/seat/SeatContentComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/seat/SeatFactLayoutComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/table/TablePresentationComponentTest.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/turn/TurnPacingComponentTest.java
      - whist-lab-app/card-table-core/src/test/resources/card-typography.tsv
      - whist-lab-app/card-table-core/src/test/resources/hand-feedback.tsv
      - whist-lab-app/card-table-core/src/testFixtures/java/ro/cardtable/testing/SurfaceContinuity.java
      - whist-lab-app/card-table-core/src/testFixtures/java/ro/cardtable/testing/SurfaceObservation.java
      - whist-lab-app/card-table-core/src/unitTest/java/ro/cardtable/testing/SurfaceObservationTest.java
      - whist-lab-app/gradle/component-audit.gradle
      - whist-lab-app/settings.gradle
      - whist-lab-app/whist-presentation/achievement-catalog.gradle
      - whist-lab-app/whist-presentation/build.gradle
      - whist-lab-app/whist-presentation/src/main/achievement-rules/whist-achievements.yaml
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistAchievementCatalog.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistAchievementCodec.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistAchievementRepository.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistAchievementState.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistHandFacts.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistMatchFacts.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/presentation/WhistAchievementPresentation.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/animation/TrickCollectionRequest.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/bidding/BidGridMetrics.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/bidding/HumanBidSelection.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/cards/CardFaceMetrics.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/cards/WhistCardFace.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/hand/HandLayoutMetrics.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/highscores/HighScoreCodec.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/highscores/HighScorePresentation.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/highscores/HighScoreRepository.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/highscores/HighScores.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/localization/WhistUiText.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/persistence/GameCheckpointCodec.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/persistence/SavedNumbers.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/score/ScoreHistoryColumn.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/score/ScoreHistoryTable.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/score/ScoreRows.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/screens/MatchSetup.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/table/TableLayoutMetrics.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/table/TrickCardMetrics.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/table/content/WhistTableContent.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/theme/GameHeaderTypography.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/theme/TrumpTileStyle.java
```

</details>

<details>
<summary>Android shared-presentation release version</summary>

```yaml
testChanges:
  - id: WHIST-024-DELIVERY-VERSION
    status: VERIFIED
    base: d99ab5071b7a8a285ca055fc5938485f19a03aa4
    expires: 2026-12-23
    reason: Increment the Android delivery identity for the extracted shared presentation modules without changing test selection, task filters or runtime rules.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleaseVersionComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#theApplicationUsesTheDeclaredPatchReleaseConvention
      negative:
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#absentVersionCannotBecomeADownloadIdentity
    paths:
      - whist-lab-app/app/build.gradle
```

</details>
<details>
<summary>Fractional mobile geometry and extracted-owner admission</summary>

```yaml
testChanges:
  - id: WHIST-024-MOBILE-GEOMETRY-ADMISSION
    status: VERIFIED
    base: b4da7a1f867f77b857a07ad17fd35c961f174aef
    expires: 2026-12-23
    reason: Normalize fractional mobile CSS height before integer layout, preserve invalid-input rejection, reconcile extracted source owners and historical visual aliases, and readmit only identical native builds after verification maintenance.
    proof: 'mvn -o -pl atf-tests -am -Dtest=WhistSurfaceAdapterComponentTest,ReleaseRagPlatformGovernanceTest,NativeSourceProvenanceComponentTest,VisualContractScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#preparedAndLiveSeatsShareIntrinsicHeightWithoutBottomAnchorStretch
        - atf.tests.governance.documentation.release.ReleaseRagPlatformGovernanceTest#everyDeclaredApplicationAndSharedUiCollectionIsIncludedInReleaseExtentAndRag
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#theRealPlannerBindsPinnedBytesAndKeepsUnavailableProofsConservative
      negative:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#unprovenContractsKeepEveryNativeFlow
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseRagPlatformGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidReleasePreflightComponentTest.java
      - atf-tests/tools/android-e2e-provenance.mjs
      - atf-tests/tools/android-e2e-runtime-scope.test.mjs
      - atf-tests/tools/visual-change-contracts.mjs
      - atf-tests/tools/visual-contracts.test.mjs
      - atf-ui-browser/src/main/web/index.html
      - atf-ui-browser/src/main/web/surface-fit.mjs
      - atf-ui-browser/src/main/web/table.mjs
      - atf-ui-browser/tests/layout-delivery.test.mjs
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Bounded Whist projection audit inputs</summary>

```yaml
testChanges:
  - id: WHIST-024-PROJECTION-AUDIT-INPUTS
    status: VERIFIED
    base: d43a8576d3a9c850302194769178411f1c4933c9
    expires: 2026-12-23
    reason: Refactor Whist projection matrix inputs while preserving all four invocation identities and exhaustive pairs; independently assert the population and retain exact first-failure context. Review layer ownership and conservative dependency admission; execute the changed Gradle cases separately. Also verify supplemental roles and optional reference indexes against the real dossier and negative fixtures.
    proof: 'mvn -o -pl atf-tests -am -Dtest=GradleAuditGovernanceTest,AtfCollectionOwnershipComponentTest,WebReleaseDossierComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#everyGradleTestClassHasOneExplicitLayerAndAnAuditedBase
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
        - atf.tests.governance.documentation.release.web.WebReleaseDossierComponentTest#portableWebInventoryChecksHashesPathsAndEvidenceOwnership
        - atf.tests.governance.documentation.release.web.WebReleaseDossierComponentTest#currentWebDossierHasOneIllustratedFlowAndNoComponentCaptures
      negative:
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
    paths:
      - atf-tests/tools/web-release-dossier.mjs
      - atf-tests/tools/web-release-dossier.test.mjs
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/content/WhistTableContentComponentTest.java
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Geometry and score-history matrix audit inputs</summary>

```yaml
testChanges:
  - id: WHIST-0241-MATRIX-INPUTS
    status: VERIFIED
    base: d7f3a384248ddfab0b1531072a4361f15f20eb40
    expires: 2026-12-23
    reason: Keep every geometry and history pair, conditional invariant, real hand and JSON restart while aggregating passing inputs with independent population checks and precise first-failure context; prepare the next maintenance APK identity.
    proof: 'mvn -o -pl atf-tests -am -Dtest=GradleAuditGovernanceTest,AtfCollectionOwnershipComponentTest,ReleaseVersionComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#everyGradleTestClassHasOneExplicitLayerAndAnAuditedBase
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#theApplicationUsesTheDeclaredPatchReleaseConvention
      negative:
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#absentVersionCannotBecomeADownloadIdentity
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryGridMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryTableComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/GameTableLayoutComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/GameTablePhaseGeometryComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TableViewportPolicyTest.java
      - whist-lab-app/app/build.gradle
      - config/atf-component-dependencies.json
```

</details>

<details>
<summary>Bounded matrices and fresh proof controls</summary>

```yaml
testChanges:
  - id: WHIST-0242-MATRIX-COMPILER
    status: VERIFIED
    base: 7db8d301596e32384f5d7940a41654ee2fadf552
    expires: 2026-12-23
    reason: Preserve matrix loops, original oracles and independent populations while bounding passing record, raster, fine-layout and animation inputs. Include raster tests in the history primary owner without duplicate reporting. Separate admitted Maven compiler reuse from fresh proof execution, including failure, incomplete output and changed bytecode. Prepare a maintenance APK with unchanged gameplay and presentation.
    proof: 'mvn -o -pl atf-tests -am -Dtest=GradleAuditGovernanceTest,ComponentEvidenceScopeComponentTest,AtfCollectionOwnershipComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#everyGradleTestClassHasOneExplicitLayerAndAnAuditedBase
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#borrowedCompilerDoesNotCacheControlExecution
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#tamperedCompilerCannotQualifyProofGeneration
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#failedControlsCannotPromoteAProof
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/gradle/GradleAuditGovernanceTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/component-proof-compiler-reuse.test.mjs
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - config/atf-component-dependencies.json
      - scripts/run-component-suites.ps1
      - whist-lab-app/app/build.gradle
      - whist-lab-app/app/src/rasterTest/java/ro/whistlab/app/score/raster/HistoryRaster.java
      - whist-lab-app/app/src/rasterTest/java/ro/whistlab/app/score/raster/HistoryViewportRaster.java
      - whist-lab-app/app/src/rasterTest/java/ro/whistlab/app/score/raster/HistoryRasterAudit.java
      - whist-lab-app/app/src/rasterTest/java/ro/whistlab/app/score/raster/ScoreHistoryCumulativeComponentTest.java
      - whist-lab-app/app/src/rasterTest/java/ro/whistlab/app/score/raster/ScoreHistoryPhoneRasterComponentTest.java
      - whist-lab-app/app/src/rasterTest/java/ro/whistlab/app/score/raster/ScoreHistoryRecoveryRasterComponentTest.java
      - whist-lab-app/app/src/rasterTest/java/ro/whistlab/app/score/raster/ScoreHistoryResponsiveRasterComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/animation/TrickCollectionMotionComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/animation/TrickCollectionRequestComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/animation/TrickCollectionResizeComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/animation/TrickCollectionRunComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/bidding/AutoGameBidActionComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/lifecycle/CompletedTrickOwnershipComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryCheckpointTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryGridMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistorySeriesComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryTableComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/award/SeriesPointsFrameComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/seat/PlayerSeatMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/GameTableLayoutComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TableSeatOrderComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TableViewportPolicyTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TrickCardMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TrumpIndicatorComponentTest.java
      - whist-lab-app/gradle/app-component-collections.gradle
```

</details>

<details>
<summary>Paired sanity and qualified report producers</summary>

```yaml
testChanges:
  - id: WHIST-PAIRED-PRODUCER-20261124
    status: VERIFIED
    base: 95d0bab317302507d12c2bab47450e168af20dd3
    expires: 2026-12-24
    reason: Use one APK/Web intent, preserve every semantic checkpoint with bounded captures, inventory all raw renderers, retain original failures and parameter identities, and refuse legacy count-only qualification.
    proof: 'mvn -o -pl atf-tests -am -Dtest=PairedSanityComponentTest,AndroidComponentReportContractTest,BrowserExtentPublicationComponentTest,ReportProducerInventoryComponentTest,AtfSourceQualityGovernanceTest,SuiteOwnershipGovernanceTest,PackageCohesionGovernanceTest,AtfCollectionOwnershipComponentTest,HtmlInvocationIntegrityComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.android.e2e.support.journal.PairedSanityComponentTest#sharedPlanExecutesTheSameRolesInBothAdaptersAndPreservesFailure
        - atf.android.e2e.support.journal.PairedSanityComponentTest#nativeSemanticStepsAndFourCapturesSurviveReplayAndDetachedPublication
        - atf.android.component.AndroidComponentReportContractTest#bothRenderersRetainOutcomesInvocationsAndOriginalComparisons
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#pairedSanityPublishesEverySemanticRoleAndItsOwnFourImages
        - atf.tests.governance.source.reporting.ReportProducerInventoryComponentTest#everyRendererBoundaryHasAnExplicitOwnerAndExistingQualification
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
      negative:
        - atf.android.e2e.support.journal.PairedSanityComponentTest#missingExtraOrReorderedEvidenceIsRejectedBeforeChangingTheJournal
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#passingTableFlowsCannotUnderstateTheirHandOrCaptureBudget
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#incompleteOrContradictoryStepEvidenceIsRejectedBeforePublication
        - atf.tests.governance.source.reporting.ReportProducerInventoryComponentTest#missingDuplicateOrMisclassifiedProducersAndLostContractsAreRejected
        - atf.tests.governance.source.reporting.ReportProducerInventoryComponentTest#legacyAggregateCannotQualifyMissingOriginalComparisons
        - atf.tests.governance.documentation.release.integrity.html.HtmlInvocationIntegrityComponentTest#missingComparisonsFailureSkipAndMissingFixtureCannotQualify
    paths:
      - atf-test-android/src/main/java/atf/android/evidence/flow/UiSanityPlan.java
      - atf-test-android/src/test/java/atf/android/component/AndroidComponentReportContractTest.java
      - atf-test-android/src/test/java/atf/android/e2e/release/autogame/AutoGameReleaseScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/release/ReleaseBidContext.java
      - atf-test-android/src/test/java/atf/android/e2e/release/WhistReleaseBidE2eTest.java
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java
      - atf-test-android/src/test/java/atf/android/e2e/support/flow/AndroidUiFlowEvidence.java
      - atf-test-android/src/test/java/atf/android/e2e/support/flow/WhistLabScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/support/journal/AndroidUiFlowJournal.java
      - atf-test-android/src/test/java/atf/android/e2e/support/journal/PairedSanityComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/support/replay/CapturedAndroidFlow.java
      - atf-test-android/src/test/java/atf/android/evidence/flow/UiSanityPlanUnitTest.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/reporting/acceptance/CombinedAcceptanceReport.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserReportFixture.java
      - atf-tests/src/test/java/atf/tests/governance/source/reporting/ReportProducerInventoryComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfReportingComponentSuite.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-ui-browser/tests/BrowserExtent.java
      - atf-ui-browser/tests/flow-budget.mjs
      - atf-ui-browser/tests/flow-budget.test.mjs
      - atf-ui-browser/tests/match-continuity.mjs
      - atf-ui-browser/tests/Publish-BrowserExtent.ps1
      - atf-ui-browser/tests/sanity-plan.mjs
      - atf-ui-browser/tests/sanity-plan.test.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - atf-tests/src/test/java/atf/tests/suites/AtfComponentSuite.java
      - config/report-producers.json
      - config/table-sanity-plan.json
```

</details>

<details>
<summary>Detached reporting and native storage recovery</summary>

```yaml
testChanges:
  - id: P1-DETACHED-STORAGE
    status: VERIFIED
    base: 57a161faf4414207629c6819d3c2a0154de3fe5c
    expires: 2026-12-24
    reason: Preserve original report bytes and comparisons without auxiliary dependencies; verify native storage phases, explicit corruption errors, profile isolation and package restoration.
    proof: 'mvn -o -pl atf-tests -am -Dtest=StandaloneReportContractComponentTest,DetachedReviewComponentTest,HtmlInvocationIntegrityComponentTest,NativeStoragePlanComponentTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest,GradleAuditGovernanceTest,AndroidJavaApiGovernanceTest,ReportProducerInventoryComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.reporting.autonomy.StandaloneReportContractComponentTest#detachedBytesRemainAuditableWithoutTheirFormerDirectory
        - atf.tests.reporting.autonomy.DetachedReviewComponentTest#producerInventoryAndOfflineObservationsRejectMissingOrForeignQualification
        - atf.tests.governance.documentation.release.integrity.html.HtmlInvocationIntegrityComponentTest#aPassingOwnedReceiptMatchesItsActualRowsAndCannotBeCountedTwice
        - atf.tests.governance.documentation.release.integrity.html.HtmlInvocationIntegrityComponentTest#currentAtfExtentReceiptsQualifyWithoutDuplicateJunitXml
        - atf.tests.governance.documentation.release.integrity.html.HtmlInvocationIntegrityComponentTest#modernQualificationRefusesMissingHtmlGroupsAndDuplicateXmlReporting
        - atf.android.component.storage.NativeStoragePlanComponentTest#orderedNativeReceiptPreservesFailuresAndRejectsLostForeignOrContradictoryCases
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
      negative:
        - atf.tests.reporting.autonomy.StandaloneReportContractComponentTest#alteredMissingAndUnsealedArtifactsCannotBeAdmitted
        - atf.tests.reporting.autonomy.StandaloneReportContractComponentTest#aFreshHashCannotAuthorizeExternalOrBrokenEvidence
        - atf.tests.governance.documentation.release.integrity.html.HtmlInvocationIntegrityComponentTest#anExternalEvidenceAttachmentCannotQualifyAnOtherwisePassingReceipt
    paths:
      - atf-test-android/src/test/java/atf/android/component/storage/NativeStoragePlanComponentTest.java
      - atf-test-android/tools/native-storage-contract.mjs
      - atf-test-android/tools/native-storage-contract.test.mjs
      - atf-tests/src/main/java/atf/tests/reporting/autonomy/StandaloneReportContract.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/integrity/html/HtmlInvocationIntegrity.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/integrity/html/HtmlInvocationIntegrityComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/autonomy/DetachedReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/autonomy/StandaloneReportContractComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfReportingComponentSuite.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/report-autonomy.mjs
      - atf-tests/tools/report-autonomy.test.mjs
      - atf-ui-browser/tests/review-detached-reports.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - config/gradle-audit-collections.properties
      - config/native-storage-components.json
      - scripts/run-native-storage-components.ps1
      - whist-lab-app/app/build.gradle
      - whist-lab-app/app/src/androidTest/java/ro/whistlab/app/persistence/nativecomponent/NativeStorageChecks.java
      - whist-lab-app/app/src/androidTest/java/ro/whistlab/app/persistence/nativecomponent/NativeStorageRecovery.java
      - whist-lab-app/app/src/androidTest/java/ro/whistlab/app/persistence/nativecomponent/StorageRecoveryInstrumentation.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/MainActivity.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/CheckpointPreferenceIntegrity.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/GameSessionStore.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/localization/WhistUiTextTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/persistence/CheckpointPreferenceIntegrityComponentTest.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/localization/WhistUiText.java
```

</details>

<details>
<summary>Qualified compiler borrowing and separated browser diagnostics</summary>

```yaml
testChanges:
  - id: RAG-COMPILER-BROWSER-COHESION
    status: VERIFIED
    allowDelete: true
    base: 19929344b76cfbeee3bf810193469ef4393949fa
    expires: 2026-12-24
    reason: Admit modern and legacy compiler pointers through one context-bound contract; preserve byte seals, leases, fresh controls and observable refusal. Separate browser selection, lifecycle, scenario drivers and results without removing cases or changing game behavior.
    proof: 'mvn -o -pl atf-tests -am -Dtest=BrowserRunnerComponentTest,WhistBrowserChecksComponentTest,ComponentEvidenceScopeComponentTest,QualifiedCatalogContextComponentTest,WhistSurfaceAdapterComponentTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.browser.runner.BrowserRunnerComponentTest#ownedLifecyclePreservesOrderingMutedHeadlessOptionsAndResultSchema
        - atf.tests.browser.runner.WhistBrowserChecksComponentTest#everyDiagnosticSelectionRetainsItsOrderedCaseIdentitiesAndViewports
        - atf.tests.browser.runner.WhistBrowserChecksComponentTest#offlineComponentsAndStandaloneDiagnosticsUseTheSamePinnedClientWithoutBundledBrowsers
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#modernCompilerPointersAndLegacyInputsPreserveSealsAndExplicitRefusals
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#changedCatalogKeepsCompilerIdentityAndExecutesControlsWithFreshContext
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#borrowedCompilerDoesNotCacheControlExecution
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#gradleOnlyChangesReuseCompilationWithoutReusingControlVerdicts
        - atf.tests.governance.documentation.release.rag.scope.context.QualifiedCatalogContextComponentTest#sourceAndCatalogIdentitySurvivesRestartWithoutRelabelingHistoricalReports
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#deliveredSiteModulesAndFontsAreAdmittedBeforeBrowserStartup
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.source.TestLayerGovernanceTest#theGuideMapsEveryGovernedSourceModuleWithoutClaimingBinarySourceSuites
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
      negative:
        - atf.tests.browser.runner.BrowserRunnerComponentTest#failedCaptureBodyAndCleanupCannotMaskTheOriginalOrHideTheNextCase
        - atf.tests.browser.runner.BrowserRunnerComponentTest#launchAndShutdownFailuresPreserveThePrimaryCauseAndCloseOnlyOwnedResources
        - atf.tests.browser.runner.BrowserRunnerComponentTest#earlyContextFailuresAndJavascriptErrorsCannotBecomePassingCases
        - atf.tests.browser.runner.WhistBrowserChecksComponentTest#invalidSelectionsDuplicateOwnersAndInterruptedFlowsCannotInventSuccessfulCases
        - atf.tests.browser.runner.WhistBrowserChecksComponentTest#aClosedWriterCannotCreditLaterRowsOrRewritePublishedEvidence
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#failedControlsCannotPromoteAProof
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#incompleteControlsCannotPromoteAProof
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#tamperedCompilerCannotQualifyProofGeneration
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theSharedCompilerWorkspaceContractRequiresAReviewedControl
    paths:
      - atf-tests/pom.xml
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/browser/runner/BrowserRunnerComponentTest.java
      - atf-tests/src/test/java/atf/tests/browser/runner/BrowserSessionFixture.java
      - atf-tests/src/test/java/atf/tests/browser/runner/WhistBrowserChecksComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfOrchestrationComponentSuite.java
      - atf-tests/tools/component-maven-reuse.test.mjs
      - atf-tests/tools/component-proof-compiler-reuse.test.mjs
      - atf-tests/tools/component-qualification-context.test.mjs
      - atf-ui-browser/src/test/java/ro/cardtable/testing/browser/BrowserAcceptanceSession.java
      - atf-ui-browser/src/test/java/ro/cardtable/testing/browser/BrowserCheckResults.java
      - atf-ui-browser/src/test/java/ro/cardtable/testing/browser/BrowserCheckSupport.java
      - atf-ui-browser/src/test/java/ro/cardtable/testing/browser/StaticSiteHost.java
      - atf-ui-browser/src/test/java/ro/cardtable/testing/browser/WhistBrowserCardScenarios.java
      - atf-ui-browser/src/test/java/ro/cardtable/testing/browser/WhistBrowserChecks.java
      - atf-ui-browser/src/test/java/ro/cardtable/testing/browser/WhistBrowserLayoutScenarios.java
      - atf-ui-browser/src/test/java/ro/cardtable/testing/browser/WhistBrowserTableScenarios.java
      - atf-ui-browser/tests/BrowserUi.java
      - atf-ui-browser/tests/ro/cardtable/testing/browser/StaticSiteHost.java
      - atf-ui-browser/tests/Run-BrowserUi.ps1
      - atf-ui-browser/tests/static-host.test.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - config/atf-governance-policy.json
      - scripts/AtfComponentMavenReuse.ps1
      - scripts/AtfComponentResume.ps1
      - scripts/AtfComponentWorkspace.ps1
      - scripts/run-component-suites.ps1
```

</details>

<details>
<summary>Source-bound invocation recovery and reviewed English terminology</summary>

```yaml
testChanges:
  - id: RAG-INVOCATION-RECOVERY-TERMINOLOGY
    status: VERIFIED
    base: a1dfdbf9dd1afe9adbe9659201c4863e73f12d10
    expires: 2026-12-24
    reason: Retain original passing comparisons through neutral capture files, admit current JUnit identities and rerun unresolved invocations only. Preserve failed retries, source and report seals, standalone Extent, relative attempt indexes and literal technical text.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleaseRagProvenanceComponentTest,InvocationRecoveryComponentTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest,DocumentationTerminologyComponentTest,DocumentationGovernanceCliComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#retryRunsOnlyFailedAndAbortedIdsAndRepublishesOriginalComparisons
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#interruptedStaticPopulationResumesTheMissingBodiesFromDurableCaptures
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#successfulOwnerReusePreservesItsOriginalSealedAttemptArtifacts
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.source.TestLayerGovernanceTest#theGuideMapsEveryGovernedSourceModuleWithoutClaimingBinarySourceSuites
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
      negative:
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#aFailedRetryStaysFailedUntilOnlyItsRemainingIdentitiesPass
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#changedSourcesUnknownPopulationsAndTamperedEvidenceAreRefused
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#currentSuiteDiscoveryRefusesMissingAndForeignOriginalIdentities
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#domainTermsRetainMeaningWithoutRejectingCodeOrExceptionTerminology
        - atf.tests.governance.documentation.cli.DocumentationGovernanceCliComponentTest#translationArtifactsHaveTheSameFullAndSelectedDiagnostics
        - atf.tests.governance.documentation.release.rag.ReleaseRagProvenanceComponentTest#recoveredComponentAttemptsMustRemainDeclaredSealedAndReadable
    paths:
      - atf-tests/tools/component-recovery-artifacts.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagProvenanceComponentTest.java
      - atf-test-android/src/main/java/atf/android/evidence/component/AndroidComponentRun.java
      - atf-test-android/src/main/java/atf/android/evidence/component/snapshot/CapturedInvocation.java
      - atf-test-android/src/main/java/atf/android/evidence/component/snapshot/ComponentCaptureJournal.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationTerminologyRules.java
      - atf-tests/src/test/java/atf/tests/execution/components/ComponentExecutionListener.java
      - atf-tests/src/test/java/atf/tests/execution/components/ComponentSuiteExecution.java
      - atf-tests/src/test/java/atf/tests/execution/recovery/InvocationCaptureUnitTest.java
      - atf-tests/src/test/java/atf/tests/execution/recovery/InvocationCheckpoint.java
      - atf-tests/src/test/java/atf/tests/execution/recovery/InvocationRecovery.java
      - atf-tests/src/test/java/atf/tests/execution/recovery/InvocationRecoveryComponentTest.java
      - atf-tests/src/test/java/atf/tests/execution/recovery/RecoveryFixture.java
      - atf-tests/src/test/java/atf/tests/execution/recovery/RecoveryPublication.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfOrchestrationComponentSuite.java
      - atf-tests/tools/prepare-component-reuse.mjs
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - scripts/run-component-suites.ps1
```

</details>

<details>
<summary>Bidding evidence ownership and executable one-card domain</summary>

```yaml
testChanges:
  - id: AI-BID-EVIDENCE-DOMAIN
    status: VERIFIED
    base: 3e071bba4d2123ad355d0e57b9e39c741d1af4de
    expires: 2026-12-24
    reason: Keep numerical update order and the public bid-sampling facade, reject invalid evidence, preserve frozen source decisions, seeds, work limits and cancellation. Compare the one-card YAML entry and conditioning domain with runtime predicates, including omitted and reversed fields.
    proof: "mvn -o -pl atf-tests -am -Dtest=BidEvidenceComponentTest,OneCardDomainComponentTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest,AtfCollectionOwnershipComponentTest -Dsurefire.failIfNoSpecifiedTests=false test"
    checks:
      positive:
        - atf.android.ai.BidEvidenceComponentTest#weightedEstimatesRetainIndependentArithmeticAndFacadeCompatibility
        - atf.android.ai.BidEvidenceComponentTest#stoppingKeepsTheMinimumEffectivePopulationAndPairedVariance
        - atf.android.ai.BidEvidenceComponentTest#frozenBidOutputsSeedsAndWorkOrderRemainByteIdentical
        - atf.android.ai.BidEvidenceComponentTest#independentDecisionsKeepCancellationAndWorkBudgetOwnership
        - atf.android.ai.OneCardDomainComponentTest#theManifestMatchesTheRuntimeDomainAndKeepsItsSetsImmutable
        - atf.android.ai.OneCardDomainComponentTest#everyModeTableSizeAndPublicShapeKeepsEntrySeparateFromConditioning
        - atf.android.ai.OneCardDomainComponentTest#unsupportedTablesModesAndMissingTrumpKeepTheOriginalFallback
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.source.TestLayerGovernanceTest#theGuideMapsEveryGovernedSourceModuleWithoutClaimingBinarySourceSuites
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
      negative:
        - atf.android.ai.BidEvidenceComponentTest#invalidWeightsAndUtilitiesCannotClaimCompletedEvidence
        - atf.android.ai.OneCardDomainComponentTest#missingOrReversedDomainFieldsFailInsteadOfSilentlyChangingTheContract
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
    paths:
      - atf-test-android/pom.xml
      - atf-test-android/src/test/java/atf/android/ai/AiBidFixtures.java
      - atf-test-android/src/test/java/atf/android/ai/BidEvidenceComponentTest.java
      - atf-test-android/src/test/java/atf/android/ai/OneCardDomainComponentTest.java
      - atf-test-android/src/test/resources/ai/bid-evidence-baseline.tsv
      - whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java
      - whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/OneCardBidBelief.java
      - whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/inference/BidSampleEvidence.java
      - config/whist-play-objectives.yaml
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
```

</details>

<details>
<summary>Semantic menu actions and early checkpoint policy</summary>

```yaml
testChanges:
  - id: UI-ACTION-CHECKPOINT-POLICY
    status: VERIFIED
    base: 47c07656cf806c6696467a8ba83c8fca4b4dbe1a
    expires: 2026-12-24
    reason: Share command and supporting action roles between menus, setup and score; preserve dispatch, complete labels and touch targets. Admit checkpoint summaries with the captured public Reporting policy before journal or action ownership changes; reject legacy overflow at publication and retain original Unicode values and retry semantics.
    proof: "mvn -o -pl atf-tests -am -Dtest=SharedMenuHistoryComponentTest,SharedPresentationComponentTest,AndroidUiFlowTraceTest,AndroidUiFlowReportPublisherTest,AndroidUiFlowCheckpointComponentTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest,AtfCollectionOwnershipComponentTest -Dsurefire.failIfNoSpecifiedTests=false test"
    checks:
      positive:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#semanticActionTypographyMatchesHomeSetupAndScoreWithoutChangingCommands
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.android.e2e.support.AndroidUiFlowTraceTest#assertionsKeepCheckpointOrderContextAndVerdicts
        - atf.android.e2e.support.AndroidUiFlowTraceTest#liveCaptureKeepsItsPublicPolicyWhileLegacyReplayUsesPublicationPolicy
        - atf.android.e2e.support.AndroidUiFlowReportPublisherTest#livePublicationUsesTheSamePublicPolicyAsCheckpointAdmission
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.source.TestLayerGovernanceTest#theGuideMapsEveryGovernedSourceModuleWithoutClaimingBinarySourceSuites
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
      negative:
        - atf.android.e2e.support.AndroidUiFlowTraceTest#overlongAssertionIsRejectedBeforeOwnershipAndJournalChange
        - atf.android.e2e.support.AndroidUiFlowReportPublisherTest#refusesAssertionValuesThatWouldBeTruncatedInExtent
        - atf.android.e2e.support.journal.AndroidUiFlowCheckpointComponentTest#failedJournalWriteRollsBackOnlyTheUncommittedCheckpointAndRetriesOnce
        - atf.android.e2e.support.journal.AndroidUiFlowCheckpointComponentTest#timingFailureAfterReplacementCannotRollBackCommittedEvidence
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreActionsRejectStaleRolesClippingAndRepeatedScaling
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
    paths:
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisherTest.java
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTraceTest.java
      - atf-test-android/src/test/java/atf/android/e2e/support/flow/AndroidUiFlowEvidence.java
      - atf-test-android/src/test/java/atf/android/e2e/support/flow/CheckpointAssertionPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/uiadapter/SharedMenuHistoryComponentTest.java
      - atf-ui-browser/src/main/web/table.css
      - atf-ui-browser/src/main/web/table.mjs
      - atf-ui-browser/tests/action-typography.test.mjs
      - atf-ui-browser/tests/fixtures.mjs
      - atf-ui-browser/tests/score-presentation.mjs
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - whist-lab-app/app/src/main/java/ro/whistlab/app/screens/ScoreScreens.java
      - whist-lab-app/app/src/main/java/ro/whistlab/app/screens/SetupScreen.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/screens/ScreenViews.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/menu/ActionTypography.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/menu/MenuTypography.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/score/ScoreTypography.java
      - whist-lab-app/card-table-core/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
```

</details>

<details>
<summary>Process deadline stability and paired syntax inspection cost</summary>

```yaml
testChanges:
  - id: GOV-PROCESS-PARSER-MEASUREMENT
    status: VERIFIED
    base: 0f63cef42900a5c0df278c966de206fe8adf1a62
    expires: 2026-12-24
    reason: Separate controlled process expiry from fixture readiness and watchdogs without changing public transport semantics. Measure production and JDK-default-path syntax parsing on one frozen source inventory with paired order and identical AST fingerprints; retain independent annotation, ambiguity, syntax and lifecycle controls without a speed threshold.
    proof: "mvn -o -pl atf-tests -am -Dtest=CommandProcessComponentTest,TestLayerSyntaxTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest,AtfCollectionOwnershipComponentTest -Dsurefire.failIfNoSpecifiedTests=false test"
    checks:
      positive:
        - atf.android.process.CommandProcessComponentTest#separatesBinaryOutputAndKeepsNonzeroExit
        - atf.android.process.CommandProcessComponentTest#drainsBothFullPipesWithoutDeadlock
        - atf.android.process.CommandProcessComponentTest#mergedOutputUsesTheSameDeadline
        - atf.android.process.CommandProcessComponentTest#completionBeforeTheDeadlineKeepsBothOutputsAndTheExitCode
        - atf.android.process.CommandProcessComponentTest#interruptionKillsTheOwnedProcessAndKeepsTheInterruptFlag
        - atf.tests.governance.source.TestLayerSyntaxTest#syntaxInspectionCostUsesOneFrozenInventoryAndPreservesAstAcrossOptions
        - atf.tests.governance.source.TestLayerSyntaxTest#everyTopLevelExecutableKeepsItsOwnLayer
        - atf.tests.governance.source.TestLayerSyntaxTest#qualifiedAndInlineMethodAnnotationsIdentifyAnExecutableClass
        - atf.tests.governance.source.TestLayerSyntaxTest#annotationsBetweenModifiersBelongToTheDeclaredType
        - atf.tests.governance.source.TestLayerSyntaxTest#fixtureMethodsCannotTurnAnEnclosingHelperIntoAnExecutable
        - atf.tests.governance.source.TestLayerSyntaxTest#anIdentifierContainingInterfaceIsStillAConcreteClass
        - atf.tests.governance.source.TestLayerSyntaxTest#annotationDiscoveryIgnoresExamplesAndKeepsTheRealDeclaration
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.source.TestLayerGovernanceTest#theGuideMapsEveryGovernedSourceModuleWithoutClaimingBinarySourceSuites
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#realDependencyClosuresSelectOnlyTheirConsumers
      negative:
        - atf.android.process.CommandProcessComponentTest#aLateProcessStartIsBoundedAndItsEventualProcessIsCleaned
        - atf.android.process.CommandProcessComponentTest#blockedDrainCannotTurnProcessExitIntoAnUnboundedJoin
        - atf.android.process.CommandProcessComponentTest#readFailureIsNotSilentlyReportedAsSuccessAndCleanupCannotMaskIt
        - atf.android.process.CommandProcessComponentTest#aBlockedStdoutDoesNotHideTheOriginalStderrFailure
        - atf.android.process.CommandProcessComponentTest#startFailureKeepsTheOriginalCauseWithoutAClockRace
        - atf.android.process.CommandProcessComponentTest#timedOutTreeKillsDescendantHoldingThePipeAfterParentExit
        - atf.tests.governance.source.TestLayerSyntaxTest#parserOwnershipSurvivesASyntaxErrorAndEndsAtClose
        - atf.tests.governance.source.TestLayerSyntaxTest#wildcardImportsResolveOnlyAnUnambiguousComposedAnnotation
        - atf.tests.governance.source.TestLayerSyntaxTest#syntaxErrorsCannotBecomeSuccessfulEmptyDiscovery
        - atf.tests.governance.source.TestLayerSyntaxTest#parserCostProbeRejectsAmbiguousInputsAndMalformedSources
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#missingOrUnreviewedDependenciesCannotAuthorizeReuse
    paths:
      - atf-test-android/src/main/java/atf/android/process/CommandProcess.java
      - atf-test-android/src/test/java/atf/android/process/CommandProcessComponentTest.java
      - atf-test-android/src/test/java/atf/android/process/ProcessFixture.java
      - atf-tests/src/test/java/atf/tests/governance/source/ParserCostProbe.java
      - atf-tests/src/test/java/atf/tests/governance/source/TestLayerSyntaxTest.java
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
```

</details>

<details>
<summary>Static proof invocation recovery on the identical qualified review</summary>

```yaml
testChanges:
  - id: RAG-STATIC-PROOF-RECOVERY
    status: VERIFIED
    base: ed9473ee05bd6780a6dce6da86e262533570ca79
    expires: 2026-12-24
    reason: Preserve original passing comparisons and retry only unresolved static invocations under the same Git review, selectors, source/catalog context, compiled byte seal and toolchain. Keep original failed attempts immutable, reject altered or foreign inputs, and retain independent primary-owner recovery and final Extent population checks.
    proof: "mvn -o -pl atf-tests -am -Dtest=StaticProofRecoveryComponentTest,InvocationRecoveryComponentTest,ComponentEvidenceScopeComponentTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest,AtfCollectionOwnershipComponentTest,TestChangeProofComponentTest -Dsurefire.failIfNoSpecifiedTests=false test"
    checks:
      positive:
        - atf.tests.execution.proofs.StaticProofRecoveryComponentTest#failedProofRetriesOnlyItsFailedBodyAndRetainsOriginalComparisons
        - atf.tests.execution.proofs.StaticProofRecoveryComponentTest#interruptionResumesOnlyMissingBodiesFromTheKnownStaticPopulation
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#staticProofResumeKeepsItsOriginalControlAndVerifiedCompilation
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#retryRunsOnlyFailedAndAbortedIdsAndRepublishesOriginalComparisons
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#interruptedStaticPopulationResumesTheMissingBodiesFromDurableCaptures
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#positiveInventoriesPartitionEveryAtfComponentWithoutChangingLeafNames
      negative:
        - atf.tests.execution.proofs.StaticProofRecoveryComponentTest#unresolvedRootCauseRemainsAFailedProof
        - atf.tests.execution.proofs.StaticProofRecoveryComponentTest#foreignReviewSelectionCompilerOrToolchainCannotReuseAProof
        - atf.tests.execution.proofs.StaticProofRecoveryComponentTest#alteredCapturesAndOriginalReportsCannotAuthorizeReuse
        - atf.tests.execution.recovery.InvocationRecoveryComponentTest#changedSourcesUnknownPopulationsAndTamperedEvidenceAreRefused
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofFromAnotherCommitCannotBeReused
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofWithoutPassingNegativeControlIsRefused
    paths:
      - atf-tests/src/test/java/atf/tests/execution/proofs/ProofRecoveryFixture.java
      - atf-tests/src/test/java/atf/tests/execution/proofs/StaticProofRecoveryComponentTest.java
      - atf-tests/src/test/java/atf/tests/execution/proofs/StaticProofRun.java
      - atf-tests/src/test/java/atf/tests/execution/proofs/StaticProofSelectors.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/execution/recovery/InvocationRecovery.java
      - atf-tests/src/test/java/atf/tests/execution/recovery/RecoveryPublication.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfOrchestrationComponentSuite.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/tools/component-proof-compiler-reuse.test.mjs
      - scripts/run-component-suites.ps1
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
```

</details>

<details>
<summary>Release identity for the qualified release-process improvement</summary>

```yaml
testChanges:
  - id: RELEASE-0-25-0-IDENTITY
    status: VERIFIED
    base: d8f48628e19533ea466a5a9001d5072aac2b7987
    expires: 2026-12-24
    reason: Advance Android version metadata after qualifying static proof recovery, without changing the application runtime. Preserve the independent risk-based increment checks and source-bound release identity.
    proof: "mvn -o -pl atf-tests -am -Dtest=ReleaseVersionComponentTest -Dsurefire.failIfNoSpecifiedTests=false test"
    checks:
      positive:
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#theApplicationUsesTheDeclaredPatchReleaseConvention
      negative:
        - atf.tests.governance.documentation.release.versioning.ReleaseVersionComponentTest#aChangeRiskCannotClaimAnIncorrectVersionIncrement
    paths:
      - whist-lab-app/app/build.gradle
```

</details>

<details>
<summary>English implementation and bounded launcher qualification</summary>

```yaml
testChanges:
  - id: GOV-EN-LAUNCHER-COST
    status: VERIFIED
    base: 9dc7455729b8ef111e7598f7787e911dd71d1100
    expires: 2026-12-24
    reason: Enforce reviewed English implementation language independently of localized data and documentation editions. Preserve original launcher rejection cases and causes, retain bounded child diagnostics before cleanup, remove duplicate compiler admission launches and keep pure matrices in the unit layer.
    proof: "mvn -o -pl atf-tests -am -Dtest=SourceLanguageComponentTest,SourceLanguageGovernanceTest,LauncherFixtureComponentTest,ComponentEvidenceScopeComponentTest,AtfSourceQualityGovernanceTest,PackageCohesionGovernanceTest,TestLayerGovernanceTest,SuiteOwnershipGovernanceTest,AtfCollectionOwnershipComponentTest,ReleaseRagProvenanceComponentTest -Dsurefire.failIfNoSpecifiedTests=false test"
    checks:
      positive:
        - atf.tests.governance.source.language.SourceLanguageComponentTest#acceptsLocalizedDataWithOneOwnedPopulation
        - atf.tests.governance.source.language.SourceLanguageGovernanceTest#implementationCommentsAndDiagnosticsUseEnglish
        - atf.tests.governance.documentation.release.rag.scope.launcher.LauncherFixtureComponentTest#completedChildrenRetainIndependentTimingsAndResourceIdentity
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#modernCompilerPointersAndLegacyInputsPreserveSealsAndExplicitRefusals
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#borrowedCompilerDoesNotCacheControlExecution
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#staticProofResumeKeepsItsOriginalControlAndVerifiedCompilation
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.PackageCohesionGovernanceTest#currentProjectUsesNoCohesionExceptions
        - atf.tests.governance.source.TestLayerGovernanceTest#executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#positiveInventoriesPartitionEveryAtfComponentWithoutChangingLeafNames
        - atf.tests.governance.documentation.release.rag.ReleaseRagProvenanceComponentTest#provenanceReceiptMustBindTheSameComponentManifestAndAggregateRun
      negative:
        - atf.tests.governance.source.language.SourceLanguageComponentTest#rejectsRomanianImplementationOnBothEditions
        - atf.tests.governance.documentation.release.rag.scope.launcher.LauncherFixtureComponentTest#interruptedChildPreservesBothStreamsBeforeOwnedFixtureCleanup
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#failedControlsCannotPromoteAProof
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#incompleteControlsCannotPromoteAProof
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#tamperedCompilerCannotQualifyProofGeneration
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#lexicalGuardFindsRealViolationsAndIgnoresExamples
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#newOmittedClassOverlapWrongLayerAndHiddenSelectionAreRejected
    paths:
      - .gitignore
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveDisplayActions.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveEvidence.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveGameFlow.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveMenuChecks.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveSanityRun.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveSettings.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveTableState.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/SimultaneousTableChecks.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/DisplayActions.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/GameplayDisplay.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/GameplayGeometry.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/ResponsiveGeometry.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/ScreenGeometry.java
      - atf-test-android/src/test/java/atf/android/e2e/driver/hierarchy/CardHierarchy.java
      - atf-test-android/src/test/java/atf/android/e2e/driver/hierarchy/CardLabelText.java
      - atf-test-android/src/test/java/atf/android/e2e/driver/hierarchy/HierarchyDocument.java
      - atf-test-android/src/test/java/atf/android/e2e/driver/UiHierarchy.java
      - atf-test-android/src/test/java/atf/android/e2e/driver/waiting/UiStateAwaiter.java
      - atf-test-android/src/test/java/atf/android/e2e/evidence/frames/PngUptimeMetadata.java
      - atf-test-android/src/test/java/atf/android/e2e/evidence/frames/TrickFrameAnalysis.java
      - atf-test-android/src/test/java/atf/android/e2e/evidence/frames/TrickFrameCapture.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/configuration/DefaultGameScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/persistence/SavedGamePreparationPhase.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/persistence/SavedGameRecoveryScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/persistence/SavedGameResumePhase.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/fan/FanPlayPhase.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/fan/FanSetupPhase.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/fan/ThirteenCardFanFlow.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SingleCardCenteringCheck.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SixPlayerBiddingCheck.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SixPlayerNavigationCheck.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SixPlayerSetupPhase.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SixPlayerTableScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/ThirteenCardBiddingCheck.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/ThirteenCardDisplayScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/ThirteenCardPlayScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/support/TrickUiState.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/tricks/CompletedTrickScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportAudit.java
      - atf-test-android/src/test/java/atf/android/e2e/support/flow/UiFlowCheckpoint.java
      - atf-test-android/src/test/java/atf/android/e2e/support/flow/WhistLabScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/support/flow/WhistLabScenarioArtifactsComponentTest.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/launcher/LauncherFixtureComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/launcher/LauncherFixtureUnitTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseCatalogPresentation.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseRagPlatformGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/language/SourceLanguageComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/language/SourceLanguageGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/language/SourceLanguageUnitTest.java
      - atf-tests/src/test/java/atf/tests/reporting/acceptance/CombinedAcceptanceReport.java
      - atf-tests/src/test/java/atf/tests/reporting/acceptance/NativeModuleResults.java
      - atf-tests/src/test/java/atf/tests/reporting/replay/ReplayEvidenceFiles.java
      - atf-tests/src/test/java/atf/tests/reporting/replay/UiFlowReplay.java
      - atf-tests/src/test/java/atf/tests/suites/AtfComponentSuite.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfOrchestrationComponentSuite.java
      - atf-tests/tools/android-e2e-evidence.mjs
      - atf-tests/tools/android-package-registration.test.mjs
      - atf-tests/tools/android-user-ownership.test.mjs
      - atf-tests/tools/android-user-readiness.test.mjs
      - atf-tests/tools/atf-component-dependencies.test.mjs
      - atf-tests/tools/atf-component-ownership.test.mjs
      - atf-tests/tools/component-compiler-scope.test.mjs
      - atf-tests/tools/component-maven-reuse.test.mjs
      - atf-tests/tools/component-proof-compiler-reuse.test.mjs
      - atf-tests/tools/component-qualification-context.test.mjs
      - atf-tests/tools/component-source-check.test.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/fixture-process.mjs
      - atf-tests/tools/fixture-process.test.mjs
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - atf-tests/tools/snapshot-io.test.mjs
      - atf-tests/tools/source-language-admission.test.mjs
      - atf-tests/tools/source-language-tokens.mjs
      - atf-tests/tools/source-language.mjs
      - atf-tests/tools/source-language.test.mjs
      - atf-tests/tools/workflow-test-change-admission.test.mjs
      - atf-ui-browser/pages/verify-card-adapter.mjs
      - atf-ui-browser/pages/verify-controls.mjs
      - atf-ui-browser/pages/verify-effects.mjs
      - atf-ui-browser/pages/verify-navigation.mjs
      - atf-ui-browser/pages/verify-records.mjs
      - atf-ui-browser/pages/verify.mjs
      - atf-ui-browser/pages/web/local-transport.mjs
      - atf-ui-browser/tests/run-components.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - config/gradle-app-collections.properties
      - config/launcher-fixture-profile.json
      - config/source-language-policy.json
      - scripts/Copy-AtfSourceInputs.ps1
      - scripts/export-card-table-ui.ps1
      - scripts/Get-AtfSourceInputs.ps1
      - scripts/run-atf-governance.ps1
      - scripts/run-component-suites.ps1
      - whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/AndroidWhistAchievementStorage.java
      - whist-lab-app/gradle/app-component-collections.gradle
      - whist-lab-app/whist-presentation/achievement-catalog.gradle
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistAchievementCatalog.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistAchievementCodec.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistAchievementRepository.java
      - whist-lab-app/whist-presentation/src/main/java/ro/whistlab/app/achievements/WhistMatchFacts.java
```

</details>

<details>
<summary>Java launcher timeout diagnostics</summary>

```yaml
testChanges:
  - id: RAG-JAVA-FIXTURE-TAIL
    status: VERIFIED
    base: 1a1b4c8cff2cfa0472a331b5800513ae3f94d5e8
    expires: 2026-12-24
    reason: Retain bounded combined child output in the Java timeout assertion before fixture cleanup. Preserve the original timeout identity when diagnostic capture fails and keep stream-size matrices in unit tests.
    proof: "mvn -o -pl atf-tests -am -Dtest=LauncherFixtureComponentTest,SourceLanguageGovernanceTest,AtfSourceQualityGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test"
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.launcher.LauncherFixtureComponentTest#javaTimeoutRetainsCombinedOutputBeforeFixtureCleanup
        - atf.tests.governance.source.language.SourceLanguageGovernanceTest#implementationCommentsAndDiagnosticsUseEnglish
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
      negative:
        - atf.tests.governance.documentation.release.rag.scope.launcher.LauncherFixtureComponentTest#missingJavaLogDoesNotMaskTheOriginalTimeout
    paths:
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/launcher/LauncherFailureLog.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/launcher/LauncherFixtureComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/launcher/LauncherFixtureUnitTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
```

</details>

<details>
<summary>Measured native content and verified report promotion</summary>

```yaml
testChanges:
  - id: UI-CONTENT-REPORT-PUBLICATION
    status: VERIFIED
    base: a51c9ab5889ea1d5f167ad4f60de44a76dc3d93d
    expires: 2026-12-24
    reason: Qualify actual Android measurement API adaptation and transactional bounded-heap report publication. Keep pure value matrices in units and preserve native raster and opaque library-stage limits.
    proof: "mvn -o -pl atf-tests -am -Dtest=AndroidComponentReportContractTest,ReportProducerInventoryComponentTest,WhistSurfaceAdapterComponentTest,ComponentPublicationComponentTest,ReportHeapComponentTest,AtfSourceQualityGovernanceTest,SourceLanguageGovernanceTest,SuiteOwnershipGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test"
    checks:
      positive:
        - atf.android.component.AndroidComponentReportContractTest#bothRenderersRetainOutcomesInvocationsAndOriginalComparisons
        - atf.tests.governance.source.reporting.ReportProducerInventoryComponentTest#everyRendererBoundaryHasAnExplicitOwnerAndExistingQualification
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#nativeContentMeasurementsRejectClippingAndLateOwners
        - atf.tests.reporting.acceptance.ComponentPublicationComponentTest#verificationMustPassBeforeCanonicalReplacement
        - atf.tests.reporting.acceptance.heap.ReportHeapComponentTest#boundedSmallMediumAndLargeReportsKeepEveryComparisonUnderTheDeclaredHeap
        - atf.tests.governance.source.AtfSourceQualityGovernanceTest#sourcesRespectPackagePathsImportsAndProductionBoundaries
        - atf.tests.governance.source.language.SourceLanguageGovernanceTest#implementationCommentsAndDiagnosticsUseEnglish
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared
        - atf.tests.governance.suites.AtfCollectionOwnershipComponentTest#positiveInventoriesPartitionEveryAtfComponentWithoutChangingLeafNames
      negative:
        - atf.tests.governance.source.reporting.ReportProducerInventoryComponentTest#missingDuplicateOrMisclassifiedProducersAndLostContractsAreRejected
        - atf.tests.reporting.acceptance.ComponentPublicationComponentTest#failedVerificationLimitsAndOriginalErrorsPreserveThePreviousReportAndJUnit
        - atf.tests.governance.suites.SuiteOwnershipGovernanceTest#newOmittedClassOverlapWrongLayerAndHiddenSelectionAreRejected
    paths:
      - config/report-producers.json
      - atf-test-android/src/main/java/atf/android/evidence/component/AndroidComponentRun.java
      - atf-test-android/src/main/java/atf/android/evidence/component/publication/ComponentReportPublication.java
      - atf-tests/src/test/java/atf/tests/reporting/acceptance/ComponentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/acceptance/heap/BoundedReportProbe.java
      - atf-tests/src/test/java/atf/tests/reporting/acceptance/heap/CapturedTextProbe.java
      - atf-tests/src/test/java/atf/tests/reporting/acceptance/heap/ReportHeapComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/acceptance/heap/ReportHeapUnitTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/suites/components/AtfReportingComponentSuite.java
      - atf-ui-browser/tests/native-observation-fixture.mjs
      - atf-ui-browser/tests/native-observation.test.mjs
      - config/atf-component-collections.json
      - config/atf-component-dependencies.json
      - config/atf-component-reader-seals.json
      - config/report-heap-profile.json
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/observation/RenderedSurface.java
      - whist-lab-app/card-table-core/src/main/java/ro/cardtable/ui/observation/SurfaceEpoch.java
      - whist-lab-app/card-table-core/src/testFixtures/java/ro/cardtable/testing/RenderedSurfaceAudit.java
      - whist-lab-app/card-table-core/src/unitTest/java/ro/cardtable/testing/RenderedSurfaceUnitTest.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/observation/AndroidSurfaceObservation.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/android/BoardLayout.java
```

</details>
