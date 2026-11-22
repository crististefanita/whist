# Qualification of the Web Adapter

## Overview

[Adapter documentation](../../atf-ui-browser/README.md) explains the course of APK, JAR and Pages. The registry follows the comparison on the same Android device.

## Qualification on the same device

| ID | Remaining sample | Closure criterion |
|---|---|---|
| WEB-07 | A Romanian 1–8–1 match with four seats, from the same checkpoint in APK and Chrome on Android. | Compare legal moves, actor, last card and collection, score and history with adjustments. Check settings, Achievements, Play again and resume after reopening in RO/EN, with normal text and enlarged. |
| WEB-11 | Maximum Hand in Chrome Android at 360 × 800 and 414 × 896, with enhanced text. | Confirm access to all 17 cards, including A, K, Q, J and `10`. Compare glyphs in hand, table and collection, including in browser without `font-variant-emoji`. |

Use the same useful profile and document the density, scale of text, language, browser version and APK hash. Compare geometry by the reserved areas of the system. Record separately the rounding of edges, font measurement and interactive areas. [Display matrix](../testing/ATF_Display.md)

Extend coverage of AI decisions, streaks and playing for zero through component tests with a domain oracle. Captures and physical interactions remain part of visual verification. [Test strategy](../testing/README.md)

- Attach native and web inline captures in Extent, along with distribution, controls and comparisons.
- Select the transitions for the sample on the device based on the already available checks. [Components and Playwright](../../atf-ui-browser/README.md#verification)

## Shared-package adoption

| ID | Remaining work | Closure criterion |
|---|---|---|
| WEB-15 | Complete adoption of adaptive presentation contracts in remaining consumers. | Repackage runtime and shared tests after source qualification; verify identical package bytes in consumers without changing the Android artifact. The currently qualified package remains immutable. |

## WEB-16 Responsive game surface and menu typography

**P1 · 88/100 · Source implementation qualified offline; same-device APK/browser acceptance remains open.**

Whist already shares the portable seat and hand calculations. Keep that boundary; the gap is the browser shell, not a reason to duplicate table geometry or pull Chess-specific UI into Whist.

- Whist now opts into usable-viewport sizing with no whole-stage transform, a centered readable width limit and local menu scrolling in short windows. The immutable published package and existing consumers retain the legacy reference-frame adapter until a separately qualified delivery. See [viewport policy](../../atf-ui-browser/src/main/web/viewport.mjs) and [stage sizing](../../atf-ui-browser/src/main/web/table.mjs).
- Explicit menu typography roles replace the implicit global multiplier. Root text changes trigger layout measurement through the same adapter; the table receives its actual usable dimensions. [Menu typography and layout](../../atf-ui-browser/src/main/web/table.css) preserve the approved default hierarchy without scaling the entire scene.

**Required direction**

1. Preserve content-first sizing, safe areas and the shared seat/card geometry. Do not reintroduce whole-stage shrinkage or isolated breakpoint overrides to conceal overflow.
2. Keep explicit typography roles and independent text enlargement. The APK's native `sp` policies remain authoritative for the pending comparison; browser components alone do not prove native font equivalence.
3. Preserve the game's information hierarchy across adapters: player names, bids, tricks won, active player, trump, cards played, the human hand and score must remain easy to scan at the same relative prominence. Keep controls reachable and avoid clipping or unnecessary page scroll at supported profiles.

**Closure evidence**

- [Component checks](../../atf-ui-browser/tests/responsive-surface.test.mjs) cover viewport sizing, typography roles, short/tall windows, long localized labels and 1.0×/1.3× text without requiring an emulator. Their HTTP boundary is a controlled fixture. [Real-engine browser checks](../../atf-ui-browser/tests/responsive-game.mjs) keep the same Whist hand across resizing and language changes; neither suite claims native APK acceptance.
- One identical saved-game checkpoint and the home/setup menus are rendered on the same Android device in APK and Chrome at a compact profile; one desktop browser profile verifies width use and overflow. Include RO/EN and 1.0×/1.3× text in the focused comparison, not a full match on every resolution.
- Extent captures include viewport dimensions, browser and APK identity, font sizes, key element bounds, overflow/scroll verdicts and the visible game facts. Keep the issue open until the rendered comparison passes; component geometry alone is not visual parity.

Use Chess Web's fluid `rem`, `clamp()` and viewport-derived board limit as the responsive reference. Preserve Whist's own engine, table semantics, colors and adapter ownership. This criterion supplements WEB-07 and WEB-11; those flow and card-glyph checks do not close responsive menu typography or desktop sizing.
