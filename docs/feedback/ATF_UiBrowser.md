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

**P1 · 88/100 · Open; code-level finding, rendered parity not yet qualified.**

Whist already shares the portable seat and hand calculations. Keep that boundary; the gap is the browser shell, not a reason to duplicate table geometry or pull Chess-specific UI into Whist.

- On phones, `viewportGeometry` uses the supplied CSS viewport. On desktop, it starts with a 414 × 896 reference and applies one scale to the whole stage. This can leave wide screens underused and shrink the menu, labels, table and controls together in a short window. See [viewport policy](../../atf-ui-browser/src/main/web/viewport.mjs) and [stage sizing](../../atf-ui-browser/src/main/web/table.mjs).
- The Whist surface hardcodes `--whist-text-scale: 1.5`, while `table.mjs` separately reads the root font size for `/api/layout`. The browser therefore has no single explicit contract linking menu roles, browser text zoom and layout measurements. The current 1.5 factor may match a chosen APK profile, but it is not a substitute for testing default and enlarged browser text. See [menu typography and layout](../../atf-ui-browser/src/main/web/table.css).

**Required direction**

1. Replace whole-stage desktop zoom with content-first sizing based on the usable viewport and safe areas. Use available width for the game surface up to a readable maximum; keep seat anchors and table proportions stable as width and height change. Do not fix overflow by shrinking every element or adding isolated breakpoint overrides.
2. Define explicit typography roles for menu title, section headings, primary and secondary actions, and helper text. Map the roles to Android `sp` and Web `rem`; let browser text zoom affect text without scaling the board or touch targets. Check the actual RO and EN menu copy, not English labels alone.
3. Preserve the game's information hierarchy across adapters: player names, bids, tricks won, active player, trump, cards played, the human hand and score must remain easy to scan at the same relative prominence. Keep controls reachable and avoid clipping or unnecessary page scroll at supported profiles.

**Closure evidence**

- Component checks cover viewport sizing, typography roles, short/tall windows, long localized labels and 1.0×/1.3× text without requiring an emulator.
- One identical saved-game checkpoint and the home/setup menus are rendered on the same Android device in APK and Chrome at a compact profile; one desktop browser profile verifies width use and overflow. Include RO/EN and 1.0×/1.3× text in the focused comparison, not a full match on every resolution.
- Extent captures include viewport dimensions, browser and APK identity, font sizes, key element bounds, overflow/scroll verdicts and the visible game facts. Keep the issue open until the rendered comparison passes; component geometry alone is not visual parity.

Use Chess Web's fluid `rem`, `clamp()` and viewport-derived board limit as the responsive reference. Preserve Whist's own engine, table semantics, colors and adapter ownership. This criterion supplements WEB-07 and WEB-11; those flow and card-glyph checks do not close responsive menu typography or desktop sizing.
