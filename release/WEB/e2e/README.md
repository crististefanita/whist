# Illustrated table journeys

## Overview

Few journeys at one readable viewport: a reused four-seat flow and two current five/six-seat journeys. Shared components own the combinations of resolution, text, legal deal and actors.

## Common audit contract

<!-- table-audit-contract: TABLE-PHASE-CONTINUITY -->

| Seats | APK/Web components | Web E2E | Installed APK E2E |
|---|---|---|---|
| 3 | [Shared contract](../component/phases/component-report.html) | Not selected this iteration | Not executed |
| 4 | [Shared contract](../component/phases/component-report.html) | [Reused flow](oh-hell/extent.html) | Not executed |
| 5 | [Shared contract](../component/phases/component-report.html) | [Current flow](table-handoffs/extent.html) · 10/9 cards | Not executed |
| 6 | [Shared contract](../component/phases/component-report.html) | [Current flow](table-handoffs/extent.html) · 8/7 cards | Not executed |

Every observed deposit and collection retains Given/When/Then steps, original before/after data and individual assertions. Inline pictures belong to captured instants; geometric data is not presented as reconstructed imagery.

## Indexes and limits

- [Web index](../../../atf-ui-browser/tests/README.md) and [APK index](../../../atf-test-android/src/test/java/atf/android/e2e/display/README.md) follow the same levels and audit roles
- [Android JVM integration](../component/apk-policy/component-report.html) executes the application contract without an emulator or native captures
- [Android catalog](../../README.md) retains release evidence; physical APK/Chrome comparison remains open
- [Back to the dossier](../README.md) · [Manifest and provenance](../index.json)
