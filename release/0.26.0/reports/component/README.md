# Component reports for the pinned source

## Overview

Each newly executed selection has its own Extent report and execution receipt. These results qualify the pinned source; APK qualification and native Android flows have separate evidence.

| Selection | Execution | Report | Metadata |
|---|---|---|---|
| atf-validation | REUSE | [Extent](atf-validation/component-report.html) | [Execution](atf-validation/execution.json) |
| atf-test-android | REUSE | [Extent](atf-test-android/component-report.html) | [Execution](atf-test-android/execution.json) |
| atf-tests | MATCH | [Extent](atf-tests/component-report.html) | [Execution](atf-tests/execution.json) |
| whist-lab-app-bidding | REUSE | [Extent](whist-lab-app-bidding/component-report.html) | [Execution](whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | REUSE | [Extent](whist-lab-app-commands/component-report.html) | [Execution](whist-lab-app-commands/execution.json) |
| whist-lab-app-display | REUSE | [Extent](whist-lab-app-display/component-report.html) | [Execution](whist-lab-app-display/execution.json) |
| whist-lab-app-history | REUSE | [Extent](whist-lab-app-history/component-report.html) | [Execution](whist-lab-app-history/execution.json) |
| whist-lab-app-achievements | REUSE | [Extent](whist-lab-app-achievements/component-report.html) | [Execution](whist-lab-app-achievements/execution.json) |
| whist-lab-app-atf-card-table-ui | REUSE | [Extent](whist-lab-app-atf-card-table-ui/component-report.html) | [Execution](whist-lab-app-atf-card-table-ui/execution.json) |
| whist-lab-app-rules | REUSE | [Extent](whist-lab-app-rules/component-report.html) | [Execution](whist-lab-app-rules/execution.json) |
| whist-lab-app-card-table-core | REUSE | [Extent](whist-lab-app-card-table-core/component-report.html) | [Execution](whist-lab-app-card-table-core/execution.json) |
| release-tools | REUSE | [Extent](release-tools/component-report.html) | [Execution](release-tools/execution.json) |
| atf-tests-documentation | MATCH | [Extent](atf-tests-documentation/component-report.html) | [Execution](atf-tests-documentation/execution.json) |
| atf-tests-provenance | RUN | [Extent](atf-tests-provenance/component-report.html) | [Execution](atf-tests-provenance/execution.json) |
| atf-tests-android | MATCH | [Extent](atf-tests-android/component-report.html) | [Execution](atf-tests-android/execution.json) |
| atf-tests-github | MATCH | [Extent](atf-tests-github/component-report.html) | [Execution](atf-tests-github/execution.json) |
| atf-tests-orchestration | MATCH | [Extent](atf-tests-orchestration/component-report.html) | [Execution](atf-tests-orchestration/execution.json) |
| atf-tests-reporting | REUSE | [Extent](atf-tests-reporting/component-report.html) | [Execution](atf-tests-reporting/execution.json) |

The source manifest, provenance and aggregate receipt bind each new execution to the pinned source fingerprint. The reuse plan records each selection decision and its original evidence directory. JUnit XML files remain temporary inputs to the audited report.
