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

## Responsive game surface and menu typography

| ID | Remaining gap | Closure criterion |
|---|---|---|
| WEB-16 | The browser shares portable seat and hand geometry, but its outer viewport still starts from a fixed 414 × 896 stage and uniformly scales that whole stage on desktop. Menu typography layers a hardcoded `--whist-text-scale: 1.5` over the root font size, while JavaScript derives a separate text scale for table geometry. That split leaves the menu/font-to-geometry contract implicit: short desktop windows can shrink the menu and game together, while enlarged menu text consumes space without a corresponding responsive layout decision. | Adopt Chess Web's content-first viewport approach without importing Chess-specific UI: measure the usable viewport and safe areas, let the table use available width up to a readable cap, and keep seat anchors stable. Define shared typography roles for menu title, section heading, primary/secondary action and helper text; map them to Android sp and Web rem so browser text zoom changes text independently from board sizing. Compare one identical saved game and the home/setup menus on compact phone, reference phone and desktop, in RO/EN at default and 1.3× text. Confirm the same hierarchy and readable proportions for player names, bids, tricks won, trump, cards in play, hand, actor and score. Record menu font sizes, table bounds, clipping/overflow and scroll behavior beside APK/Web captures in Extent. Use components for the sizing and typography policies; keep only representative rendered checks per viewport class. Do not claim pixel identity between native and browser fonts. |

The code-level basis is `viewport.mjs` (`REFERENCE` 414 × 896 and desktop `scale`), `table.mjs` (whole-stage transform), and `table.css` (`--whist-text-scale` and menu rules). Chess Web provides the responsive reference through its fluid `rem` typography, `clamp()` spacing and board limit derived from `100dvh`; preserve Whist's own engine, table semantics, colors and adapter ownership.
