# Android table adapter component tests

## Overview

The Android project owns renderer-specific host tests and source-boundary checks. Portable geometry, content, text roles and the bulk of display combinations are owned by [core components](../../../card-table-core/src/test/README.md).

| Family | Native adapter responsibility |
|---|---|
| Achievements | Catalog order, restoration identities and deferred detail evaluation. |
| Profile input | Android input ranges, Unicode limits and preservation of unaffected text. |
| Boundaries | Native and core inventories remain free of consumer game dependencies; production carries no reporting or storage owner. |

## Execution

- `:card-table-android:testDebugUnitTest` runs the native adapter's host component selection.
- `-Pcardui.only=component` selects Android, core and audit support; `-Pcardui.only=core` admits portable checks without an Android SDK.
- Core's `test` task requires its isolated `unitTest` prerequisite. Unit diagnostics have no Extent or reporting dependency.

## Acceptance

Real widget geometry, fonts, focus and touch need separate native instrumentation. A host component or an equal policy plan does not qualify painted pixels. Preserve case identities when moving assertions between owners.

[Common strategy](../../../../docs/testing/README.md) · [Module × level matrix](../../../../docs/testing/ATF_TestingGuide.md) · [Native instrumentation](../androidTest/README.md)
