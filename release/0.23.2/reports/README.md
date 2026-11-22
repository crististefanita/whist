# Whist Lab 0.23.2 qualification

## Overview

Maven preparation admission is separated from snapshot recovery. The APK keeps the same game and presentation code; the version metadata is updated.

## Component evidence

- [atf-validation — 19 invocations](component/atf-validation/component-report.html): reused after source ownership and receipt verification
- [atf-test-android — 277 invocations](component/atf-test-android/component-report.html): reused after source ownership and receipt verification
- [atf-tests — 563 invocations](component/atf-tests/component-report.html): new execution on the pinned source
- [whist-lab-app-bidding — 255 invocations](component/whist-lab-app-bidding/component-report.html): reused after source ownership and receipt verification
- [whist-lab-app-commands — 123 invocations](component/whist-lab-app-commands/component-report.html): reused after source ownership and receipt verification
- [whist-lab-app-display — 107 invocations](component/whist-lab-app-display/component-report.html): reused after source ownership and receipt verification
- [whist-lab-app-history — 61 invocations](component/whist-lab-app-history/component-report.html): reused after source ownership and receipt verification
- [whist-lab-app-achievements — 28 invocations](component/whist-lab-app-achievements/component-report.html): reused after source ownership and receipt verification
- [whist-lab-app-atf-card-table-ui — 56 invocations](component/whist-lab-app-atf-card-table-ui/component-report.html): reused after source ownership and receipt verification
- [whist-lab-app-rules — 40 invocations](component/whist-lab-app-rules/component-report.html): reused after source ownership and receipt verification

## Scope and limits

The diff selects the ATF component group. Nine unchanged game and infrastructure groups retain their original reports and provenance.

Native E2E is NOT_RUN because no game-flow or visual contract changed. Android is the active platform; native iOS remains parked.

## Integrity

The version manifest binds the APK, signer, source inventory, component receipts and canonical Extent reports. Publication and completed download evidence are recorded separately.

## Process cost

[Recorded stage timings](component/timing.json) distinguish snapshot preparation, dependency copying, compilation, evidence reuse, suite execution and final source verification.

These stages may overlap with the separate APK build; no total speedup is inferred. The broad ATF owner still includes isolated launcher fixtures that prepare additional workspaces.

[Release catalog](../../README.md) · [Shared testing strategy](../../../docs/testing/README.md)
