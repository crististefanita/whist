# Qualification of local testing infrastructure

## Overview

The consumer owns API validation, process checks and local adoption; the [ownership register](README.md#common-test-support) routes generic Kit primitives. Module READMEs define implemented contracts; binary-library and native Android acceptance remain separate.

## Test kit concurrency

[Shared support](README.md#common-test-support) owns generic primitives and controlled tests. The consumer retains Android-adapter acceptance; transferring code does not qualify concurrency or close that criterion.

## Cost of validating API

The [validation contract](../../atf-validation/README.md#cost-and-limits) owns structural limits and the shared prevalidation/evaluation work budget. Unit tests cover exact boundaries; public-contract components retain paths, policy and evaluation isolation. Parser allocation and Java regex wall-clock limits remain explicitly outside that budget.

## Stability of process verification

### Disposable Android profile readiness

Durable ownership and recovery now belong to the [release qualification contract](../testing/ATF_ReleaseQualification.md#disposable-user-recovery). Controlled components qualify interruption and identity checks; native profile acceptance remains separate.

The [mocked ADB component](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidReleasePreflightComponentTest.java) separates fixture readiness from transport verdicts. A controlled production process port verifies order and original causes without sleeps; the bounded launcher probe requires observed fixture entry before attributing a timeout to ADB.

[Test kit](../../lib/README.TestKit.md#purpose-and-integration) · [Validation API](../../atf-validation/README.md) · [Governance](ATF_Governance.md)

## Launcher composition

The [Android action contract](../../atf-test-android/src/test/README.md#action-dispatch-and-ownership) separates hierarchy selection, command execution and caller-owned phases. Controlled components qualify order, durations, cancellation and original causes; native application acceptance remains with the Android register.

## RAG cost rating

| Step | Remaining criterion |
|---|---|
| Android | Measure lease waiting, profile preparation, transport and scenario execution separately. Correlate the active window to attribute interference from another game correctly. [Android acceptance](ATF_Android.md) |
| Reports | Measure evidence collection and handoff separately from execution on the same invocation inventory. [Reporting register](ATF_Reporting.md) |

The [RAG resume contract](../testing/ATF_ReleaseQualification.md#resume-validated-steps) defines checkpoints and snapshot reuse. Concrete execution timelines belong to the external run dossier.
