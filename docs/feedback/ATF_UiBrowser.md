# Qualification of the Web Adapter

## Overview

[Adapter documentation](../../atf-ui-browser/README.md) explains the APK, shared-package and Pages paths. This register tracks the remaining comparison on the same Android device.

[The shared three-module plan](ATF_CardUi_Extraction.md#ui-three-module-plan) owns extraction and the main display matrix. The [paired sanity contract](../testing/ATF_TableContinuity.md#paired-sanity-roles) governs essential transitions, ordered roles and capture budgets; same-device rendering acceptance remains separate.

## Qualification on the same device

The fractional-height bridge and the public-browser two-hand sanity pass. The [display contract](../testing/ATF_DisplayComponents.md) owns integer normalization and visible-viewport coordinates.

Same-device Android Chrome acceptance remains open; desktop mobile emulation does not close that boundary.

| ID | Remaining sample | Closure criterion |
|---|---|---|
| WEB-07 | Up to two Romanian 1–8–1 hands with four seats, from the same legal checkpoint in APK and Chrome on Android. | Compare legal moves, actor, last card and collection, score and history with adjustments. Check settings, Achievements, Play again and resume after reopening in RO/EN, with normal text and enlarged. |
| WEB-11 | A maximum-size hand in Android Chrome at 360 × 800 and 414 × 896, with enlarged text. | Confirm access to all 17 cards, including A, K, Q, J and `10`. Compare glyphs in hand, on the table and during collection, including browsers without `font-variant-emoji`. |

Use the same useful profile and document the density, scale of text, language, browser version and APK hash. Compare geometry by the reserved areas of the system. Record separately the rounding of edges, font measurement and interactive areas. [Display matrix](../testing/ATF_Display.md)

Extend coverage of AI decisions, streaks and playing for zero through component tests with a domain oracle. Captures and physical interactions remain part of visual verification. [Test strategy](../testing/README.md)

- Attach native and web inline captures in Extent, along with distribution, controls and comparisons.
- Select the transitions for the sample on the device based on the already available checks. [Components and Playwright](../../atf-ui-browser/README.md#verification)

## Shared-package adoption

| ID | Remaining work | Closure criterion |
|---|---|---|
| WEB-15 | Distribution stays deferred until Whist Web acceptance is complete. | Close Whist Web display gaps before packaging or distributing to Rentz/Hold’em. Require explicit measured-frame opt-in and negative checks that borrowing its theme leaves legacy geometry unchanged. Then qualify identical runtime/test bytes in consumers without changing Android artifacts; keep admitted packages immutable. |

## WEB-16 Responsive game surface and menu typography

**P1 · 88/100 · Native-calibrated proportional fitting and portable contracts available; same-device APK/browser acceptance remains open.**

Whist shares native seat, card and animation policies. Preserve that boundary and qualify the physical renderers independently.

- The [viewport policy](../../atf-ui-browser/src/main/web/viewport.mjs) derives one proportional fit from usable height, width and required logical content. [The DOM adapter](../../atf-ui-browser/src/main/web/surface-fit.mjs) fits menus and dialogs, pages long content and follows keyboard offsets. Published packages remain immutable.
- Explicit typography roles and [native/Web components](../../atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java) govern frames, cards, commands and the reserved action-label band. Collection overlays and AutoGame changes must not trigger fitting callbacks.

**Required direction**

1. Preserve the APK's proportions through one measured fit of the complete scene, including text inside seats, cards, header, help, dropdowns and score tables. Normalize native pixels by density and system insets. Phones use actual useful CSS dimensions; desktop magnifies a native-calibrated reference. Avoid independent font increases, unrelated breakpoint constants or clipping content to hide overflow.
2. Bidding, play, menus and every non-history dialog fit usable height without vertical scrolling. Rules, achievement groups and rankings use bounded pages; score history is the sole internal vertical-scroll exception. Preserve complete text, input fields, navigation and manual choices.
3. Keep frames fixed between bidding, play, collection and successive hand sizes. Verify the upper frames at four seats and the lower frames at five seats, including unknown and two-digit facts. Fitted text must retain its intrinsic height as in the APK; legacy multiline CSS must not reach nested fitted ink. Localized play labels sit above frames in bands reserved for both phases. Three-player variants must not intersect labels, bids, frames or the human hand.
4. Retain the native horizontal hand gesture for dense cards. Test first/last-card access rather than forcing all 17 cards into a narrow viewport by reducing their indices.
5. Select native policy from useful viewport height, then fit intrinsic scene demand. Keep minimum board reservation separate from allocated slack and old absolute-child bounds. Measure successive hands, not only several tricks in one hand; retain original failing traces when a validator is corrected.

[The shared phase contract](../testing/ATF_TableContinuity.md) owns legal combinations for 3–6 players, every actor and every winner through native/Web adapters. Keep the screenshot-free matrix in components and few illustrated E2E journeys; physical native acceptance remains separate.

**Closure evidence**

- [Components](../../atf-ui-browser/tests/responsive-surface.test.mjs) cover fixed-height menus/dialogs, paging, callback isolation, localized labels and 1.0×/1.3× text through a controlled HTTP boundary. [Real-engine checks](../../atf-ui-browser/tests/responsive-game.mjs) retain the same hand across resizing and language changes; neither suite claims native APK acceptance.
- Record page/panel scroll ranges and all visible bounds. Include compact and desktop profiles, three-player labels and dense hands. Hidden scrollbars or a single one-card picture cannot establish this contract.
- Keep one primary illustrated E2E report with few representative journeys at a readable resolution and record each deposit's before/after comparisons, including AI-to-human handoffs. Components own resolution, text-scale and anchor combinations without screenshots. The [intrinsic handoff regression](../../atf-ui-browser/tests/seat-handoff.test.mjs) covers prepared/mounted frames and inherited bottom anchors; keep physical acceptance open and retain individual frame, fact-row and deposited-face assertions in the [Web dossier](../../release/WEB/README.md).
- Compare intrinsic seat measurements and native heading/frame reserves, not only rectangles produced from identical injected heights. Verify dropdown options inherit the selected-value role and score columns preserve bid, won tricks, hand delta and cumulative total with balanced native typography. Dialog fitting must not apply text scale twice.
- Verify paging-label contrast and compact outlines in Achievements, rankings and help, in both languages and enabled/disabled states. Keep Previous/Next separate from Close; check real dismissal as well as the labels. Preserve compatible headers, trump text, seat rows and fitted ink during AI/human handoffs, with unchanged hand nodes, scroll and deposited faces. [Hand ownership](../../atf-ui-browser/tests/hand-lifecycle.test.mjs) and [delayed layout checks](../../atf-ui-browser/tests/layout-delivery.test.mjs) isolate stale callbacks, including font completion before layout. Stable outer rectangles alone do not establish paint continuity. Confirm the reported visual motion at three to six seats before closing physical acceptance.
- Render the same saved checkpoint and home/setup menus in APK and Chrome on one Android device. Compare a compact profile, RO/EN and normal/enlarged text; add a desktop browser profile without repeating complete matches at every size.
- Extent includes viewport, density, browser/APK identity, text sizes, bounds, scroll verdicts and visible game facts, with inline captures. Keep native acceptance open until the physical comparison passes.
- Treat a partial native instrumentation run as incomplete when any widget reports an error, even if its assertion-failure count is zero. Identify the APK/test artifact before attributing its results to current source.

Use Chess Web's explicit typography and viewport-derived limits as design references, retaining Whist's own engine and adapter ownership. WEB-07 and WEB-11 remain separate flow and glyph criteria.
