# Release Tools

## Overview

`release-tools` is the Maven-owned boundary for release policy, release gates and delivery adapters. It is the single operational entry point for RAG review, signed APK preparation, publication, download verification and local release projection. It calls shared ATF engines through their owner interfaces; it does not copy or absorb the test runner, Android device infrastructure, game modules or reporting library.

```mermaid
flowchart LR
    SOURCE["Pinned source and candidate"] -->|qualifies| RUNNER["Shared ATF qualification runner"]
    RUNNER -->|seals evidence| EVIDENCE["Test and release evidence"]
    EVIDENCE -->|assesses| ENGINE["Shared RAG evidence engine"]
    ENGINE -->|cites| VERDICT["Advisory verdict"]
    VERDICT -->|checks| JAVA["release-tools Java gates"]
    JAVA -->|presents| HUMAN["Human review"]
    HUMAN -->|authorizes| SIGN["APK build and signer check"]
    HUMAN -->|authorizes| PUBLISH["GitHub publication adapter"]
    PUBLISH -->|exposes| DOWNLOAD["Download verification"]
    DOWNLOAD -->|projects| SYNC["Verified local release projection"]
```

<details>
<summary>Legend — responsibility boundaries</summary>

- The existing Node evaluator owns diff-based selection, evidence citations and aggregate read-only assessment.
- The Java evidence gate verifies the declared manifest inventory before the RAG evaluator runs. The Java RAG gate checks candidate, source and plan bindings. It only admits `READY_FOR_HUMAN_REVIEW` or `BLOCKED`; it has no signing, promotion or publication authority.
- The evidence gate verifies every declared path, byte count and SHA-256, and refuses traversal, links and private signing material.
- PowerShell entry points invoke the Java admission gates and the separately owned build, publication, download and local-sync engines. They do not change a RAG verdict.
- Game algorithms and Android scenarios stay in their owning game or device modules.

</details>

## Module map

| Area | Owner |
|---|---|
| Maven implementation | `src/main/java/atf/release/tools` |
| Component and rejection tests | `src/test/java/atf/release/tools` |
| Release operation adapters | [scripts](scripts/README.md) |
| Release RAG policy | [config](config/README.md) |
| Public signing contract and ignored local identity | [.atf-signing](.atf-signing/README.md) |
| Shared suite, source and governance ownership | [config](../config) at project root |
| Shared diff selector, evidence engines and test/report tooling | [atf-tests/tools](../atf-tests/tools/README.md) |
| Cross-module suite, governance and Android launchers | [root scripts](../scripts/README.md) |
| Delivered APK and release evidence | [release](../release/README.md) |

- The release policy has one owner under this module; Android preflight and qualification read that same file.
- `release-tools/` owns release-specific decisions and operator commands. Root `scripts/` owns runners used across modules, `atf-tests/tools/` owns reusable test/evidence engines, and root `config/` owns shared suite and source governance.
- These dependencies are referenced once, not copied into a second implementation. Game, Android-device, shared-UI and reporting modules keep their existing owners.

## Ownership boundary

| Concern | Owner | What stays outside `release-tools/` |
|---|---|---|
| Release policy, advisory gates, signing continuity and operator actions | `release-tools/` | No duplicate policy or private-key implementation. |
| Suite selection, external staging, resume/checkpoint and shared launcher preflight | Root `scripts/` and root `config/` | These coordinate all ATF domains, not only release work. |
| Reusable diff selection, evidence parsing, provenance, publication protocol and test utilities | `atf-tests/tools/` | The release adapters invoke the tested shared engines. |
| Android app/device flows, game logic and Extent rendering | Their existing ATF modules | No release-specific fork or duplicated suite. |

The checkout remains the complete runnable project. `release-tools/` is the release workflow owner, not a detached copy of ATF infrastructure.

## Entry points

| Operation | Adapter | Boundary |
|---|---|---|
| RAG evaluation and independent verdict admission | `scripts/Invoke-ReleaseRag.ps1` → `scripts/Invoke-ReleaseToolsJavaGate.ps1` | Read-only inputs; output outside source and candidate; human review required. |
| APK build and certificate continuity | `scripts/Build-SignedReleaseApk.ps1` → `scripts/Invoke-ReleaseToolsJavaGate.ps1` | Pinned external source snapshot, preserved identity, external output. |
| GitHub release publication | `scripts/Publish-GithubRelease.ps1` | Qualified candidate and RAG verdict; resumable journal outside the checkout. |
| Remote release and asset downloads | `scripts/Verify-GithubReleaseDownload.ps1` | Authenticated observation of the two catalogued releases and exact downloaded bytes. |
| Local release synchronization | `scripts/Sync-LocalRelease.ps1` | Published Git payload, post-publication verdict and verified release projection. |

Use `-ProjectRoot` or `ATF_PROJECT_ROOT` when the checkout is not the script's detected root. The selected root must contain the reactor POM, active release policy and release catalog. Relative paths are resolved from the selected project, never from a developer-specific drive.

For qualification, use the shared root runner first; it resolves affected suite owners, validates the external temp/evidence roots, stages the pinned source and produces the source-bound receipts. Invoke `Invoke-ReleaseRag.ps1` only after that evidence and the plan exist. This ordering is part of the workflow contract, not an implicit side effect of the RAG adapter.

The component test source root has its own short catalogue at [src/test](src/test/README.md), linked to the shared testing strategy.

## Signing identity

- The existing private test-edition keystore was moved byte-for-byte into `release-tools/.atf-signing/`; it was not regenerated.
- Only its public certificate, fingerprint, guide and ignore file enter Git or source inventories. The local keystore and backups remain ignored.
- The APK adapter verifies the local signing bytes and the produced APK against the pinned public fingerprint. A missing or changed identity and linked signing paths fail closed; the adapter never creates or substitutes a key.
- The test certificate is not a production-managed key. Release manifests preserve the declared test-distribution channel and certificate identity.

## Tests and qualification

`ReleaseToolsComponentSuite` is the primary component selection. It covers portable root resolution, exact-byte evidence admission, RAG binding, negative paths and the no-side-effect review boundary. Changes in this module select its Maven component group and release-tools governance; they do not by themselves select game-flow E2E.

```powershell
pwsh -NoProfile -File .\scripts\run-component-suites.ps1 -Groups release-tools
```

The group publishes one source-bound Extent report and JUnit reconciliation through the configured reporting owner. Component comparisons and rejection reasons are captured in Extent.
