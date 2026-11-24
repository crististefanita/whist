# Browser acceptance support

## Overview

This source root owns reusable support for browser acceptance: session lifecycle, an isolated static host, Whist scenarios and captured check results. The [common testing strategy](../../../docs/testing/README.md) defines the levels; the [browser test catalog](../../tests/README.md) selects executable checks.

## Responsibilities

| Owner | Contract |
|---|---|
| [BrowserAcceptanceSession](java/ro/cardtable/testing/browser/BrowserAcceptanceSession.java) and [StaticSiteHost](java/ro/cardtable/testing/browser/StaticSiteHost.java) | Create and close owned browser/host resources, with explicit origin and storage boundaries. |
| [BrowserCheckResults](java/ro/cardtable/testing/browser/BrowserCheckResults.java) | Preserve check identity, evaluated values, failures and evidence without turning an absent check into a pass. |
| [BrowserCheckSupport](java/ro/cardtable/testing/browser/BrowserCheckSupport.java) | Provide shared observation and assertion support while preserving the original failure cause. |
| Whist browser scenario classes | Describe card, table and layout journeys through the adapter; execution and publication remain separate owners. |

## Test levels

| Level | Responsibility | Evidence |
|---|---|---|
| Unit | Pure selection, budgets and value projection in the owning modules | Native test results without Extent. |
| Component | Controlled lifecycle, result fidelity and common adapter contracts | Auditable Extent with declared preconditions and original comparisons, without screenshots. |
| Browser sanity | Short menu and game transitions on an identified site and checkpoint | A small illustrated flow with inline captures and assertions. |

## Preconditions and cleanup

- Identify the site, viewport, locale, font scale and game/storage fixture before observing the scene.
- Keep exhaustive geometry combinations in shared component contracts; select representative transitions for the browser journey.
- Isolate browser storage and own the session and host lifecycle; cleanup retains the original error when both execution and teardown fail.
- Keep generated results outside the source tree and publish only the current qualified dossier.

[Browser module](../../README.md) · [Testing guide](../../../docs/testing/ATF_TestingGuide.md)
