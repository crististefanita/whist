# Release operation adapters

## Overview

These scripts provide portable command entry points for release operations. They resolve the checkout from their own location or an explicit `-ProjectRoot` / `ATF_PROJECT_ROOT`, then delegate to the existing owner of each operation. They are the release workflow's commands; shared qualification and suite-launch commands remain in the root [scripts directory](../../scripts/README.md).

| Script | Delegated owner | Output rule |
|---|---|---|
| `Invoke-ReleaseRag.ps1` | `atf-tests/tools/evaluate-release-rag.mjs` and `RagAdvisoryGate` | Verdict outside all inputs; human review only. |
| `Invoke-ReleaseToolsJavaGate.ps1` | Shared Maven adapter for evidence, RAG and certificate gates. |
| `Build-SignedReleaseApk.ps1` | Android Gradle application module | Builds from a pinned source directory; APK and checksum outside the checkout. |
| `Publish-GithubRelease.ps1` | `atf-tests/tools/publish-github-release.mjs` | Journal outside project and candidate; credentials remain in the caller environment. |
| `Verify-GithubReleaseDownload.ps1` | `atf-tests/tools/collect-github-release-retention.mjs` | New observation file outside the project. |
| `Sync-LocalRelease.ps1` | `atf-tests/tools/verify-release-git-snapshot.mjs` | Receipt outside the repository; only verified published bytes are projected. |

RAG only reads sealed inputs, writes an auditable recommendation and asks for human review. It never calls build, signing, Git, GitHub or local-sync operations. Each adapter is an explicit later action and checks its own inputs, output boundary and exit code.

## Qualification sequence

1. The shared root runner validates the checkout, temp/evidence roots, changed owners and pinned source inputs before tool capability probes, run-directory creation or staging.
2. The selected component and governance collections run through their existing owners and produce source-bound Extent/JUnit evidence in the external run dossier.
3. `Invoke-ReleaseRag.ps1` validates the sealed evidence and plan, calls the shared read-only evaluator, then checks its verdict with the module's Java gate.
4. Human review authorizes separate build/signing and publication operations. Download verification and local projection happen after publication.

- Root `scripts/` owns the launchers shared by qualification, Android and the game laboratory.
- Root `config/` owns policy and executable inventories consumed by multiple modules.
- `atf-tests/tools/` owns reusable suite-selection and evidence engines used by other ATF owners.
- `release-tools/` depends on those interfaces and does not duplicate them.
