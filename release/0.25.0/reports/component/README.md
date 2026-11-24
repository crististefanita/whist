# Component suite audit

## Overview

1673 primary component invocations passed across 17 owners. The final documentation revision executes one affected owner and preserves the original evidence of sixteen unchanged owners. Components contain captured inputs and comparisons without product screenshots.

| Owner | Invocations | Execution | Report | Metadata |
|---|---:|---|---|---|
| atf-validation | 19 | Reused original evidence | [Extent](atf-validation/component-report.html) | [Receipt](atf-validation/execution.json) |
| atf-test-android | 300 | Reused original evidence | [Extent](atf-test-android/component-report.html) | [Receipt](atf-test-android/execution.json) |
| atf-tests | 108 | Reused original evidence | [Extent](atf-tests/component-report.html) | [Receipt](atf-tests/execution.json) |
| whist-lab-app-bidding | 255 | Reused original evidence | [Extent](whist-lab-app-bidding/component-report.html) | [Receipt](whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | 127 | Reused original evidence | [Extent](whist-lab-app-commands/component-report.html) | [Receipt](whist-lab-app-commands/execution.json) |
| whist-lab-app-display | 107 | Reused original evidence | [Extent](whist-lab-app-display/component-report.html) | [Receipt](whist-lab-app-display/execution.json) |
| whist-lab-app-history | 70 | Reused original evidence | [Extent](whist-lab-app-history/component-report.html) | [Receipt](whist-lab-app-history/execution.json) |
| whist-lab-app-achievements | 28 | Reused original evidence | [Extent](whist-lab-app-achievements/component-report.html) | [Receipt](whist-lab-app-achievements/execution.json) |
| whist-lab-app-atf-card-table-ui | 8 | Reused original evidence | [Extent](whist-lab-app-atf-card-table-ui/component-report.html) | [Receipt](whist-lab-app-atf-card-table-ui/execution.json) |
| whist-lab-app-rules | 40 | Reused original evidence | [Extent](whist-lab-app-rules/component-report.html) | [Receipt](whist-lab-app-rules/execution.json) |
| whist-lab-app-card-table-core | 50 | Reused original evidence | [Extent](whist-lab-app-card-table-core/component-report.html) | [Receipt](whist-lab-app-card-table-core/execution.json) |
| atf-tests-documentation | 167 | Fresh documentation check | [Extent](atf-tests-documentation/component-report.html) | [Receipt](atf-tests-documentation/execution.json) |
| atf-tests-provenance | 124 | Reused original evidence | [Extent](atf-tests-provenance/component-report.html) | [Receipt](atf-tests-provenance/execution.json) |
| atf-tests-android | 54 | Reused original evidence | [Extent](atf-tests-android/component-report.html) | [Receipt](atf-tests-android/execution.json) |
| atf-tests-github | 33 | Reused original evidence | [Extent](atf-tests-github/component-report.html) | [Receipt](atf-tests-github/execution.json) |
| atf-tests-orchestration | 108 | Reused original evidence | [Extent](atf-tests-orchestration/component-report.html) | [Receipt](atf-tests-orchestration/execution.json) |
| atf-tests-reporting | 75 | Reused original evidence | [Extent](atf-tests-reporting/component-report.html) | [Receipt](atf-tests-reporting/execution.json) |

## Provenance and recovery

[Current source receipt](source-provenance.json) · [Source inventory](source-inputs.json) · [Original catalog inventory](release-inputs.json) · [Reuse decisions](component-reuse.json) · [Root run](source-component-run.json)

The Android owner reuses 53 invocations and retries one; orchestration reuses 106 and retries two. Their final counts remain 54 and 108. Original failed attempts and retry reports remain in the respective [Android lineage](atf-tests-android/README.md) and [orchestration lineage](atf-tests-orchestration/README.md); retries are not added to the primary total.

Unit prerequisites run separately without Extent; matrix assertions remain in their owning unit/component level. [Common testing strategy](../../../../docs/testing/README.md)
