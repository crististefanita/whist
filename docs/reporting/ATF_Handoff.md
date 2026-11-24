# Handoff and acceptance of Android results

## Overview

Handoff publishes results to an external store and separates execution, republication and acceptance of a declared scope. Every later publication retains the original execution identity.

## Publication flow

```mermaid
flowchart LR
    TRACE[Original trace] -->|link to the descriptor| DESC[Explicit descriptor]
    DESC -->|validates| CHECK[Integrity and scope]
    CHECK -->|records immutably| ARCH[External record]
    ARCH -->|updates atomically| POINTER[Pointer by scope]
```

<details>
<summary>Legend: publication and acceptance</summary>

A record may describe success, failure or refusal of execution. Acceptance requires the controls required for the declared scope; the mere existence of an HTML or passing JUnit result does not by default update the acceptance.

</details>

## The contract of the descriptor

| Group | Responsibility |
|---|---|
| Identity | Run ID, source of republishing, package, scenario and declared/actual scope. |
| Execution | Native verdict, captured beginning / end, temporal basis and limits. |
| Application and sources | Identity of the installed binary, sources and libraries used. |
| Content | HTML, manifest, checkpoints and associations of actions, observations and comparisons. |
| Review | The checks required for the purpose, related to the hash of the inspected content. |

Missing data remains explicitly marked. An estimated time, later reading identity or an interrupted transfer shall not become full original captures.

## Identity and time of measurement

The descriptor separates the data captured on execution from data checked during offline review. In [whist_ui_handoff.py](../../atf-test-android/tools/whist_ui_handoff.py), a JAR identified in the native classpath may receive its hash only during review; that hash does not retrospectively become an execution-time observation.

- Keep the moment and source of each identity, including when the original information is missing.
- Correlate the interactive revision with the exact hash of the verified HTML.
- Treat acceptance of the republication descriptor and a new Android execution as separate-purpose records.

## External name spaces

| Contract | Meaning |
|---|---|
| runs and metadata | Immutable records for each execution or republishing. |
| latest-attempt by scope | The latest recorded attempt, including failure. |
| latest-accepted by scope | The most recent acceptance that meets the scope criteria. |
| public-trace | Observations permitted to the player, separate from the oracle’s post-game data. |
| indexes | Navigation indexes, without authority to declare acceptance. |

## Operational invariants

- A record identity is immutable; importing identical content again is idempotent.
- Pointers are ordered by execution time, with one lock and atomic replacement.
- Republishing an older run does not create a new execution, regardless of the copy time.
- Scope and package define which results are comparable.
- The data reserved for the oracle does not reach the Player's API.
- The external root shall be transmitted explicitly, independently of the staging directory.
- Automatic indexing refuses checkout destinations and does not rewrite editorial READMEs.

## Compact handoff and exceptions

The target of handoff is an auditable HTML that includes mandatory information, full values and captures associated with each checkpoint. Do not keep duplicate final copies merely for navigation after their bytes are embedded. A directory or ZIP of required annexes does not make the HTML standalone.

| Content | Retention policy |
|---|---|
| Mandatory data of the verdict | Body accessible in HTML with identity and association; a hash or file name does not replace content. |
| Identical copy incorporated | Delete only after the byte-for-byte comparison and detached-report verification. |
| Diagnosis or auxiliary result for separate purposes | Preserved for specific reasons, consumer or diagnostic question and identity; cannot hide any information necessary for HTML audit. |
| Distribution metadata | The index and integrity manifest remain compact, with verifiable role for navigation or governance; do not substitute the input, comparison or comments accessible in HTML. |
| Evidence unit still external | Preserved until full publication; dependency remains explicit in feedback. |

[Android publisher](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) includes PNGs, manifests and text bodies provided or declared by checkpoints. [Migrated component suites](../testing/ATF_DisplayComponents.md#families-using-the-same-audit) includes inputs and comparisons captured; selections without adaptor keep the audit qualification distinct.

- **History:** older native reports retain unique annexes not yet qualified for removal; changing the publisher does not update historical reports.
- **Observation time:** the hash and body come from the same publication-time read, separately from Android execution and later review.
- **Admission:** [the audit contract](ATF_Audit.md) requires qualification of each producer's inventory, bytes and associations before unique evidence is removed.

## Execution and limits

| Implementation | Contract and limit |
|---|---|
| [whist_ui_handoff.py](../../atf-test-android/tools/whist_ui_handoff.py) | Implement offline republishing with external descriptor and root. Its record type cannot claim a new Android execution. |
| [WhistExtentFeedback](../../atf-test-android/src/main/java/atf/android/whist/evidence/WhistExtentFeedback.java) | Publish the index of an audited run in the external index directory. The Handoff root and index directory are configurable; the source directory is not a valid destination. |
| [Handoff components](../../atf-test-android/tools/test_whist_ui_handoff.py) | Check the refusal of corrupt data and the preservation of accepted pointers after failure or republication of an older run. The Java index identifies the last published audit; acceptance comes from the scope descriptor and pointer, even when the latest attempt failed. |

[Publication architecture](README.md) · [Android contract](ATF_Android.md) · [criteria for handoff](../feedback/ATF_Reporting.md)
