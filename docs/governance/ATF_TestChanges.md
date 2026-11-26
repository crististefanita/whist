# Governance of test changes

## Overview

A change in the tests shall preserve the meaning of the contract and the reason for which an entry is admitted or refused. The change in implementation does not justify adjusting the expected result only to achieve success.

## Criteria

| Surface | Contract |
|---|---|
| Expected | Derived from requirement, invariant or independent oracle. |
| Control negative | The intended failure demonstrates that the test detects the defect concerned. |
| Identity | The case, sources, configuration and scope are explicit. |
| Verdict | Preserve the original cause through exception handling and report publication. |
| Population | Declare the selection and omissions; keep run counts out of generic guidance. |
| Results | Keep concrete runs and version comparisons in the external archive. |

## Revision of a change

1. Form general ownership and its borders.
2. Reproduce the defect in a small case with controlled data.
3. Check negative control failure and successful valid case.
4. Reconcile competing sources prior to qualification.
5. Update the collection, contract and documentation of the mechanism in the same revision.

[Governance of documentation](ATF_Documentation.md) · [Collections](../testing/ATF_Collections.md)

## Checking the Git diff

Run the [governance launcher](../../scripts/run-atf-governance.ps1) from the reviewed checkout. Select the shared commit before the change. The launcher checks tests on an isolated copy and reads the Git diff from the declared checkout.

```powershell
.\scripts\run-atf-governance.ps1 -TestChangeBaseRef $reviewBase
```

| Contract | Verification |
|---|---|
| Base | A local commit at or before `HEAD`. CI declares an explicit base that is an ancestor of `HEAD`. Without a local parameter, `HEAD` delimits only uncommitted changes. |
| Git inputs | Committed changes from the base, index, working tree and untracked files, including protected files ignored by Git. |
| Scope | Maven and Gradle tests and resources, POMs, Gradle configuration, suite selections, and executable governance or qualification inputs. |
| Authorization | Exactly one eligible record must match the exact path, base SHA and UTC review date. Expiry is inclusive. |
| Rejection | Missing or unrelated base, uncovered path, expired or ambiguous control, invalid metadata, or unsupported Git state. |
| Rename | A rename is evaluated as deletion of the old path plus addition of the new path. Deletion requires `allowDelete: true`. |

The launcher transmits the base through `atf.governance.testChanges.baseRef`. For component selection, set `ATF_TEST_CHANGE_BASE_REF` before launch. A branch review always uses a commit earlier than `HEAD`.

## Life cycle of control

| Step | Place and criterion |
|---|---|
| ACTIVE | The [feedback owner](../feedback/README.md) document keeps the reason, the order of the sample and the exact list of pathways. |
| VERIFIED | [Technical registry](ATF_TestChangesBaseline.md) keeps the same ID and sample selection. The current diff authorization requires verified proof, basis and expiration. |
| DRAFT AND CLOSED | The structured entry may record the proposal or closure. These states have zero eligibility. |
| Withdrawal | Compare the control against the review base. Remove it only when the targeted diff no longer needs it. |

- A YAML block may declare a `testChanges` list. Each item declares `id`, `status`, `base`, `expires`, `reason`, `proof` and `paths`. `allowDelete` is optional and defaults to `false`.
- Relative paths use `/`. IDs must be unique across the inspected documents.

The proof command declares the planned verification and runs separately. [Component tests](../../atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java) create local Git histories and qualify the mechanism without a device.

## Proof of verified control

<a id="editorial-continuation-of-original-proofs"></a>

### Editorial continuation of original proofs

An original proof remains bound to the commit, base, diff, selectors and Extent bytes recorded at execution. A later editorial commit does not become a new execution: the [continuation validator](../../atf-tests/src/main/java/atf/tests/testchange/EditorialProofContinuation.java) admits reuse while retaining that original identity. Audit readers display both the original and admitted commits and retain links to the original report.

| Boundary | Required check |
|---|---|
| History | The original commit exists locally and is an ancestor through at most 256 consecutive single-parent editorial commits. Merges or unrelated histories require new proofs. |
| Executable inputs | Every intervening commit changes only non-build Markdown or `config/documentation-editions.json`. Source code, resources, packaged game-rule documents and other configuration remain unchanged, including changes later reverted. |
| Protected diff | Recompute the original full raw Git diff from its recorded base and verify its hash. The protected raw diff, including Git blob identities and modes, must equal the current protected diff from that same base. |
| Controls | `testChanges` metadata in each changed document remains semantically unchanged, including reasons, commands, paths and positive/negative selectors. Current authorization and expiry still apply. |
| Files and outcomes | Documents are regular non-executable Git blobs. Original cases must still be exactly the declared passing population, and the original HTML hash must match. A dirty checkout remains blocked. |
| Provenance | The admission receipt exposes `continuations`: original commit, admitted commit, protected-diff hash and editorial paths. Neither the original receipt nor Extent is rewritten. |

```mermaid
flowchart LR
    Proof[Original proof] -->|hash and cases| Identity[Original identity]
    Head[Current commit] -->|Git history| Scope[Editorial boundary]
    Identity -->|same protected diff| Reuse[Admit original proof]
    Scope -->|unchanged executable inputs| Reuse
    Scope -->|source or control changed| Regenerate[Generate new proof]
    Reuse -->|current documents| Docs[Fresh documentation governance]
```

<details>
<summary>Legend — retained execution and current documentation</summary>

- Proof reuse authorizes the unchanged source-control contract; it does not qualify changed documentation or add executed test cases.
- Keep `ATF_TEST_CHANGE_PROOF_ROOT` pointed at the original bundle and use the normal collection launcher with `-ReuseFrom`. Unchanged Maven outputs remain independently admissible; do not invoke `-AllTestChangeControls` when all required original proofs are admitted.
- A new control, executable input, unsupported history or failed original outcome requires proof generation. [Component cases](../../atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeEditorialProofComponentTest.java) exercise real Git histories and explicitly mocked proof-boundary inputs.

</details>

### Early source-control admission

Both qualification launchers call the [admission adapter](../../scripts/AtfTestChangeAdmission.ps1) before copying or restoring a source snapshot, copying dependency caches, or starting Maven. A small standalone compiler builds the [CLI](../../atf-tests/src/main/java/atf/tests/testchange/TestChangeAdmissionCli.java) from the current tool sources and reads the pinned parser JARs from `-MavenCache`. The CLI calls the same `TestChangeReview` and `TestChangeProof` validators as final governance.

| Selector boundary | Admission and authority |
|---|---|
| Source eligibility | The [validator](../../atf-tests/src/main/java/atf/tests/testchange/selectors/StaticProofSelectors.java) parses literal Maven roots with the JDK, without application compilation or class loading. Each positive/negative `Class#method` identifies one declared instance `void @Test` method in component scope. Local composed annotations, inherited scope and non-static nested containers resolve from source; parameter injection is allowed, invocation multiplicity is not. |
| Early rejection | Dynamic factories/templates, repeated or parameterized tests, overloads, duplicate types, unresolved/cyclic annotation composition and ambiguous imports produce `TEST-CHANGE-SELECTOR-*`. Main-source classes and inherited-only methods cannot authorize selectors. Normal verification and proof-generation admission re-evaluate this contract after staging. |
| Final authority | Source eligibility is conservative and does not replace JUnit discovery of exactly one invocation per selector or the original passing outcomes. Disabled, aborted, missing or failing execution cannot produce a verified proof. [Component cases](../../atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeSelectorComponentTest.java) cover the static boundary; [launcher checks](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java) prove rejection before copying sources/caches and before Maven. |

| Mode | Admission contract |
|---|---|
| Normal governance or components | Verify eligible controls and all required original receipts, including ID, Git base, commit, raw diff, positive/negative selectors and HTML SHA-256. |
| Proof generation | Validate the clean committed diff, current controls and declared positive/negative selectors without requiring the not-yet-generated receipts. Only the explicit control-generation options select this mode. |
| After snapshot or resume | Recheck the same canonical contract before selecting or preparing executions; changed sources or proof bytes cannot borrow an earlier admission. |
| Rejection | Publish diagnostics and a failed admission timing phase without copying the source or dependency cache and without starting Maven. |

```mermaid
flowchart LR
    DIFF[Git base and diff] -->|inspects| GATE[Canonical admission]
    GATE -->|rejects| STOP[Failed admission receipt]
    GATE -->|admits| COPY[Snapshot or resume]
    COPY -->|rechecks| VERIFY[Canonical admission]
    VERIFY -->|accepts| EXEC[Reuse or execute affected collections]
    VERIFY -->|rejects| STOP
```

<details>
<summary>Legend: proof generation and normal authorization remain separate</summary>

The bootstrap writes only temporary utility classes under the configured external temporary root and removes them after each check. `test-change-admission.json`, `test-change-admission-recheck.json` and `timing.json` describe admission; test execution and its canonical Extent reports keep their own receipts. Generation-plan admission does not authorize a normal run without the required verified proofs.

</details>

Java 21 and the locally cached pinned JSON/YAML parser dependencies are required even when Maven compilation is reused. Source immutability and final governance still run after early admission; a receipt never disables their checks.

The hosted workflow prepares only the pinned JSON/YAML parser inputs from Maven Central over HTTPS, then admits the generation plan before preparing the Maven reactor. Failed parser preparation or plan admission prevents Maven; ordinary qualification still requires the subsequently generated original proofs.

### Generate and consume proof

Keep the control and its test selection in the reviewed commit. Write proof to a new directory outside the checkout. The launcher prepares an isolated copy and publishes evidence only after confirming that the inputs did not change.

```powershell
.\scripts\run-component-suites.ps1 -AllTestChangeControls -TestChangeBaseRef $reviewBase -RunRoot $runRoot
$env:ATF_TEST_CHANGE_PROOF_ROOT = Join-Path $runRoot 'proofs'
.\scripts\run-atf-governance.ps1 -TestChangeBaseRef $reviewBase
```

| Element | Contract |
|---|---|
| Selectors | Declare distinct `checks.positive` and `checks.negative` component methods in `package.Class#method` form. |
| Negative control | The test injects the defect and asserts the expected rejection. Both the valid case and the rejection case must pass at the JUnit level. |
| Identity | The receipt binds the control ID, base SHA, reviewed commit SHA and raw Git diff SHA-256, including blobs and file modes. |
| Diff batch | One compilation feeds all required `VERIFIED` controls. Each control has its own JVM, log and HTML report. Publish `proofs` only after every control passes and the source and selection are reconfirmed. |
| Execution | The launcher runs exactly the declared selectors and reconciles invocations with Extent. The receipt and HTML are stored together under the control ID. |
| Verification | A changed executable input, protected diff or control, missing selectors, or an HTML hash mismatch blocks `VERIFIED` authorization. Only an admitted editorial descendant can reuse an original commit's proof. |
| History | Keep prior controls in the register. A `VERIFIED` control applies only when its selection and proof match the reviewed diff. |

On failure or source change, inspect the per-control logs and the `proofs.pending` diagnostic directory. For a focused run, `-TestChangeControl` stores the receipt under `<RunRoot>/<ID>`. Reuse that receipt and report for the original commit and diff, or for a verified editorial continuation that preserves their identity.

### Independent proof domains

The [reviewed domain declarations](ATF_TestChangesBaseline.md#current-domain-controls) are the selection inventory. Each control owns exact paths and its positive and negative selectors; the existing Git reviewer selects the union of owners touched by the diff.

| Domain | Owns | Does not schedule by itself |
|---|---|---|
| Inventory validator | Source-language policy, complete inventory and its controlled rejection cases | Native geometry or report rendering |
| Native surface | Measured content, shared phase geometry and displaced-seat rejection | Full-source language inventory |
| Reporting | Publication, producer ownership, complete values and bounded resource fixtures | Native surface observations |
| AI evidence | Transactional sample admission, model frequencies and independent utility oracles | Native rendering or playing-strength evaluation |
| Catalog | Primary suites, class inventories, reader dependencies and seals | Unchanged UI or resource-test bodies |
| Selection | Domain routing, proof batching, failures and source/plan drift | Game or native-renderer execution |

```mermaid
flowchart LR
    DIFF["Real Git diff"] -->|identifies| OWNER["Exact reviewed path owners"]
    OWNER -->|assigns| VALIDATOR["Inventory validator"]
    OWNER -->|assigns| UI["Native surface"]
    OWNER -->|assigns| REPORT["Reporting"]
    OWNER -->|assigns| AI["AI evidence"]
    OWNER -->|assigns| CATALOG["Catalog and selection"]
    VALIDATOR -->|contributes| UNION["Required control union"]
    UI -->|contributes| UNION
    REPORT -->|contributes| UNION
    AI -->|contributes| UNION
    CATALOG -->|contributes| UNION
    UNION -->|uses| COMPILE["One sealed compilation"]
    COMPILE -->|produces| PROOFS["Separate original Extent and proof per control"]
```

<details>
<summary>Legend: omission and reuse have different meanings</summary>

An unchanged domain is absent from the execution plan; its original evidence keeps its original commit and scope. Exclusion does not turn that evidence into a proof for a new control or source. A mixed diff restores every affected owner, and a missing or ambiguous owner blocks qualification.

</details>

- Use cohesive `RAG-DOMAIN-` controls and a new reviewed ID/base for a new review; keep qualified historical declarations immutable. Expired controls and an incorrect Git base remain refusals.
- Shared selection metadata has its own independent suite-ownership checks. A change to UI source additionally selects the native domain; catalog ownership cannot hide it.
- [Domain components](../../atf-tests/src/test/java/atf/tests/governance/source/changes/domains/TestChangeDomainComponentTest.java) read the live declarations, retain the original checks, reject overlapping ownership and resolve actual committed fixture diffs through the production reviewer. Their component symbols are nonexecuted stubs, not UI acceptance.
- One batch still shares compilation and classpath. A failed control, changed source or changed plan prevents final proof publication; domain separation does not relax those boundaries. After an isolated driver correction, rerun the complete selected bundle in a fresh attempt until selective per-control dependency closure admission exists. Preserve original JUnit/report identities and reject shared-input drift. The [batch component](../../atf-tests/src/test/java/atf/tests/execution/components/TestChangeBatchComponentTest.java) qualifies this conservative fallback with mocked population/report hashes; it does not enable partial proof reuse.

### Resume one static proof control

Use a new external run directory and the original run as `-ResumeFrom`, together with the same `-TestChangeControl` and Git base. The launcher first admits the complete source/release snapshot and verified compilation. Fresh batch generation remains separate; `-AllTestChangeControls` does not accept resume.

```powershell
.\scripts\run-component-suites.ps1 -TestChangeControl $control -TestChangeBaseRef $reviewBase -ResumeFrom $previousRun -RunRoot $newRun
```

| Boundary | Recovery contract |
|---|---|
| Checkpoint | The [static proof executor](../../atf-tests/src/test/java/atf/tests/execution/proofs/StaticProofRun.java) records registration and outcomes through the shared neutral-capture ports. |
| Identity | Require the same control ID, Git base, commit, raw diff, positive/negative selectors, sealed compiled output, classpath and JVM/tool identity. |
| Discovery | Current compiled discovery must match the original static invocation population exactly. |
| Selection | Execute only failed or missing bodies. |
| Original evidence | Republish original passing comparisons and phase timing with their original checkpoint/report hashes; a failed retry remains failed. |
| Rejection | Altered captures, foreign or missing identities and failed owner containers cannot authorize a proof. |
| Publication | Require complete Extent reconciliation, source/diff rechecking and the launcher's pending-proof promotion. |

[Controlled recovery components](../../atf-tests/src/test/java/atf/tests/execution/proofs/StaticProofRecoveryComponentTest.java) count actual child-JVM body executions, inject process interruption and tamper with evidence. These are offline components without screenshots; their fixtures are not application E2E acceptance.

## Transferring ownership of a check

The transfer of an implementation to a library shall keep all native contracts to the producer. The consumer shall retain the acceptance of the API used, the identity of the loaded JARs and the refusal of metadata or inconsistent deliveries.

- A transfer request identifies affected sources, the new owner, the equivalent replacement and independent acceptance criteria.
- Link the transfer and captures to delivery. Keep the Git base and affected paths in the technical register.
- Deleting a local copy does not remove a negative case or turn a library check into Android acceptance.
- Document ownership limits without retaining historical lists of deleted tests.

[Quality governance](README.md) checks the current perimeter and prohibits parallel sources of binary libraries.
