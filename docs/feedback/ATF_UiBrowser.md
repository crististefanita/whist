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

**P1 · 88/100 · Native-calibrated proportional fitting and portable contracts available; same-device APK/browser acceptance remains open.**

Whist shares native seat, card and animation policies. Preserve that boundary and qualify the physical renderers independently.

- The [viewport policy](../../atf-ui-browser/src/main/web/viewport.mjs) derives one proportional fit from usable height, width and required logical content. [The DOM adapter](../../atf-ui-browser/src/main/web/surface-fit.mjs) fits menus and dialogs, pages long content and follows keyboard offsets. Published packages remain immutable.
- Explicit typography roles and [native/Web components](../../atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java) govern frames, cards, commands and the reserved action-label band. Collection overlays and AutoGame changes must not trigger fitting callbacks.

**Required direction**

1. Preserve the APK's proportions through one measured fit of the complete scene, including text inside seats, cards, header, help, dropdowns and score tables. Normalize native pixels by density and system insets. Phones use actual useful CSS dimensions; desktop magnifies a native-calibrated reference. Avoid independent font increases, unrelated breakpoint constants or clipping content to hide overflow.
2. Bidding, play, menus and every non-history dialog fit usable height without vertical scrolling. Rules, achievement groups and rankings use bounded pages; score history is the sole internal vertical-scroll exception. Preserve complete text, input fields, navigation and manual choices.
3. Keep frames fixed between bidding, play and collection. Localized play labels sit above their frames in bands reserved for both phase labels. Three-player Oh Hell, Reverse Oh Hell and descending variants must not intersect labels, bid buttons, other frames or the human hand.
4. Retain the native horizontal hand gesture for dense cards. Test first/last-card access rather than forcing all 17 cards into a narrow viewport by reducing their indices.

**Closure evidence**

- [Components](../../atf-ui-browser/tests/responsive-surface.test.mjs) cover fixed-height menus/dialogs, paging, callback isolation, localized labels and 1.0×/1.3× text through a controlled HTTP boundary. [Real-engine checks](../../atf-ui-browser/tests/responsive-game.mjs) retain the same hand across resizing and language changes; neither suite claims native APK acceptance.
- Record page/panel scroll ranges and all visible bounds. Include compact and desktop profiles, three-player labels and dense hands. Hidden scrollbars or a single one-card picture cannot establish this contract.
- Compare intrinsic seat measurements and native heading/frame reserves, not only rectangles produced from identical injected heights. Verify dropdown options inherit the selected-value role and score columns preserve bid, won tricks, hand delta and cumulative total with balanced native typography. Dialog fitting must not apply text scale twice.
- Verify paging-label contrast and compact outlines in Achievements, rankings and help, in both languages and enabled/disabled states. Preserve compatible seat rows and fitted ink during AI/human handoffs; stable outer rectangles alone do not establish paint continuity. Confirm the reported visual motion at five and six seats before closing physical acceptance.
- Render the same saved checkpoint and home/setup menus in APK and Chrome on one Android device. Compare a compact profile, RO/EN and normal/enlarged text; add a desktop browser profile without repeating complete matches at every size.
- Extent includes viewport, density, browser/APK identity, text sizes, bounds, scroll verdicts and visible game facts, with inline captures. Keep native acceptance open until the physical comparison passes.
- Treat a partial native instrumentation run as incomplete when any widget reports an error, even if its assertion-failure count is zero. Identify the APK/test artifact before attributing its results to current source.

Use Chess Web's explicit typography and viewport-derived limits as design references, retaining Whist's own engine and adapter ownership. WEB-07 and WEB-11 remain separate flow and glyph criteria.

## WEB-17 AutoGame during an in-flight manual move

**P2 · 83/100 · Preserve automation intent while a committed move is being presented.**

- A checkbox can be selected after the hand display updates but before the manual command finishes. The selection can then clear without advancing; activation after the command becomes idle works. The pending availability gate in [the shell](../../atf-ui-browser/src/main/web/table.mjs) is the leading cause to verify, separately from typography and viewport fitting.
- Treat temporary command ownership as a wait or pause, not a completed hand. Preserve a submitted move, reject concurrent commands and let deselection prevent the next automatic decision.
- Close with controlled-transport component cases for selection during playback/collection, cancellation, errors and command ordering. Retain the initial failed outcome when a stable-state continuation passes; qualify native/Web behavior independently.
