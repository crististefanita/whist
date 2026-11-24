# Match history and interface qualification

## Overview

The engine owns match data; the Android app owns their persistence and presentation. Qualify interface changes against the actual installed binary, with the scenario's state and purpose declared.

## Native acceptance of proportions and order

The display profiles guide holds geometry and text. The common interface registry holds the extraction of reusable components. [Profiles](../testing/ATF_Display.md#profiles-and-field-of-qualification) · [Common interface](ATF_CardUi_Extraction.md)

| Priority | Remaining qualification | Closure criterion |
|---|---|---|
| P1 | Extension of native qualification beyond the compact profile; components do not substitute the fonts and Android interaction. | High profile and increased text: 13 cards, a complete trick at five seats and the actors' succession to 3/5/6 places, with legible names and indices. Keep the captures, XMLs and APK hashes without repeating a full game on each profile. |
| P1 | Extend AutoGame's compact sample to the native matrix of the actor and positions. | On the identified APK, check the bidding and the game at 3–6 places, in RO/EN and on compact profile plus increased text: only the current actor has badge, the badge remains visible when AutoGame plays the human player's seat, the manual controls remain locked, and the collection of the trick and the score screen does not mark an actor. The places do not move when the phase changes, and the face and the lifting of the card remain visible. Components check projection and geometry; only Android observation with APK/hash and capture can close the criterion. |

Delivery files keep the sample compact with 13 cards and audit of the indices, as well as the AutoGame test at four places with a card, the hand-game marking and the score screen. Acceptance on hand 17 cards check horizontal scrolling and box stability. The marking remains open during the fast automatic move and the matrix with increased text. [Release catalog](../../release/README.md)

## The compatibility of history

### P1 - Native qualification of the two-column history

The native prefix confirms a complete row and the resumption in Romanian and English. The matrix below follows the rest of the population and the raster markings. The components qualify for projection, persistence and geometry.

| Surface | Closure criterion |
|---|---|
| Two columns | The exact bid and the total score after each hand per player; column `P` displays the balance of the row, including zero and negative values, without a third column. RO/EN, text normal and enlarged, first and last hand and all places, on an APK that includes this contract. |
| Streak rectangle | Normal contour only after five consecutive eligible successes; red contours after five consecutive eligible failures, including when the total remains positive. Bonuses and penalties ± 5 / ± 10 shall be set in total only by payment row, exclusively in cell `P`; the bid, header, neighbor and unpaid results must not receive a rectangle. |
| Rendering | The original capture confirms the four sides, the legibility of the number and the sign, the space to the contour and the lack of intersections, including the increased text. The presence of the node in XML does not demonstrate the rectangle and a report of an earlier APK does not qualify for the new marking. |
| Blunt-cut | A failed bid has a visible mark in the Android raster, including zero and increased text; only the red colour or accessible description does not certify the cut. Compare with a successful contract and keep the cipher readable. |
| Integrity | Exploration and resumption shall retain the totals and all rows; the marking shall remain in the right hand and place. |
| separators | Target of 2,1 dp, rounded in pixels once only; all player boundaries remain aligned between headers and body. The component PNG qualifies geometry and color in JVM rendering; Android font and pixels require APK identified. |

[ScoreHistory](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/ScoreHistory.java) keeps known suffixes of bids and tricks. The points derive from the hips and the failure compares the bid with the tricks; they do not deduce from the color or mark of the score.

| Priority | Remaining requirement | Closure criterion |
|---|---|---|
| P2 | The native acceptance of the import of an old register remains distinct from the components of migration. | The resumption shall retain the outstanding and the unfamiliar prefix; only the demonstrable result shall be marked. Check the hand-auctioned-point-total combination. |

A balance or a difference of points does not justify rebuilding some missing levatates. The contract implemented and the migration limit remain in [model of history](../game/ATF_ScoreHistory.md).

## Menu, Help and Configuration

[Rule components](../testing/ATF_GradleComponents.md) connect RO/EN help to the engine. Checkpoint components verify enabled and disabled streak settings for both Romanian variants and both bonus values. The Android scenario keeps opening and reading help, the selection of the bonus and the resumption of the same game after restart.

For Achievements, the presentation component reopens the progress in simulated storage and verifies the first chapter and the localized condition of the target. The native session demonstrates the menu route, catalogue, detail and return.

## Persistence and coordination

[Commands and persistence](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md#commands-persistence-and-errors) have component ports for rejection, checkpoints, rankings and continuation. Rendering does not record results; native storage recovery and device cost require separate qualification.

| Priority | Gap and source | Verification level and closure criterion |
|---|---|---|
| P2 | Synchronous checkpoint latency is not qualified on-device; component ordering does not establish responsiveness. | Identify environment and state; measure writing separately from AI computation and rendering. Preserve ordering and original errors before moving I/O between executors. |

## Qualification of visual changes

| Priority | Surface | Required check prior to acceptance |
|---|---|---|
| P2 | trick collection: native binding of qualified ports through components | APK identified at 3–6 places: measurement and clipping match the scene, transformations return to cancellation, and the last trick precedes the score. Confront `onPause` / restart, menu and overlapping dialogues, consecutive resizing, configuration notifications, detachment and disabled animations. Only the last geometry can receive the new scene; a delayed callback does not resume the abandoned route and does not consume the suspended time. The portrait policy remains the one in the manifest; the watch and event tests do not qualify pixels or fluidity. |
| P1 | Table, card and trump | Profiles declared, text enlarged, real intersections, logically coordinated mapped at raster and revision of original capture. |
| P1 | History and Resume | All hands closed and all accessible places without repeated points or advance the match. |
| P1 | Accessibility and location | Ordnance assisted, labels of unknown or negative values, first and last removable in both languages. In the enlarged text, face the full title of dialogue and the names of first/last column separately before and after the course; the qualification of the region of legend does not demonstrate the legibility of these regions. |
| P2 | Configured name and shift badge | Check the name at the maximum accepted length in all positions and with the active badge; the visible identity must be legible and the abbreviation must have an explicit rule and access to the full name. |

- **Region:** the turn action occupies a measured band above the frame; components check its size and separation with enlarged text.
- **Observation:** E2E uses the same decoder for play and bidding; native acceptance remains necessary for glyphs, actual wrapping and RO/EN captures.

- **Sanity:** observe the complete action above the name at every active seat, including after turn changes and resumption.
- **History:** check the first and last column after scrolling, card indices and continuous player separators.

A change of geometry or style is linked to the sources and APK inspected. The device qualification and component audit are separate records, kept externally.

- **Shared trump:** compare the complete frame at the same density and text scale across modes, using a turned card, a suit and no trump. Verify both complete regions and accessible identity, without an extra absence row.
- **Bidding:** check the complete RO/EN button label with normal and enlarged text; the inset outline remains separate from the touch target. [Display contract](../testing/ATF_Display.md) owns the shared sizes and criteria.

## Native acceptance on Android 17

- **Lint prerequisite:** remove inherited API-26 file operations from the API-24 debug replay fixture and qualify predictive-back handling for target API 36+. Do not waive these errors or claim a passing full Lint run; the checkpoint reader uses APIs available at the declared minimum.

Window protection and calculation of useful space are implemented; the acceptance on API 37 remains distinct from the components and verification on another Android system. The comparison between versions requires the identity of both APKs and the device.

| Priority | Remaining requirement | Closure criterion |
|---|---|---|
| P1 | API 37: window, resume and memory | On an identified device: navigation through gestures and three buttons, clipping, keyboard, restart and some hands; record memory, exit reason and APK hash without changing system boundaries. |

[Delivery contract](../platform/ATF_AndroidDelivery.md) distinguishes insets with the IME from the measurement of the window without the IME and defines the restoration and diagnosis of memory. Components shall verify the model and the native criterion shall remain open until the callbacks and scenario are executed on the target system.

## Adapter memory

The memory limit of the adapter remains distinct from the geometry of the interface.

| Priority | Gap and source | Verification level and closure criterion |
|---|---|---|
| P2 | [CommandProcess](../../atf-test-android/src/main/java/atf/android/process/CommandProcess.java) reads the flows in full, without separate budgets for text and binary. | Components with controlled output: explicit outflow of each budget, demarcated memory and incomplete PNG refused prior to publication; the term and initial cause shall be maintained. |

## Owning contracts

- [Profiling, history, leaderboards and series scenarios](../testing/ATF_Android.md#profile-history-leaderboards-and-series) has automated mechanisms and preconditions; the existence of the scenario does not qualify for a version.
- [Display components](../testing/ATF_DisplayComponents.md) cover mocked rendering and cache isolation; fonts and native interaction require separate verification.
- [Qualification of publication](ATF_Reporting.md) keeps the gap in the early validation of checkpoint; [handing over results](../reporting/ATF_Handoff.md) holds the distinction between functional verdict and publication.
- [Governance of publication](../testing/ATF_ReleaseQualification.md#evidence-freshness) follows the launcher and resumes after an incomplete GitHub response. [Guide RAG](../testing/ATF_ReleaseQualification.md#snapshot-and-preflight-before-maven) defines the identity of the file and the connection of the published audit.

[Testarea Android](../testing/ATF_Android.md) · [Screen profiles](../testing/ATF_Display.md) · [Interface Atlas](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md)

## Cost of acceptance on device

- Measure the reservation, profile preparation, ADB commands, execution and restoration separately. Check the active window to correctly assign an interruption.
- Validate each Extent reference to the original capture. Resume the flows without accepted proof for the same APK, snapshot and profile. [Qualification of delivery](../testing/ATF_ReleaseQualification.md#work-profiles-for-resumption)

## ANI-02 · Hand of 17 cards on compact screen

- Extend [APK's responsive evidence](../../release/README.md) at the remaining combinations of the matrix 3–6 places, RO/EN, text enlarged and AutoGame. Watch for access to the cards in the scrollable area, the actor's signaling and fixed positions between bid, game and collection. [Display strategy](../testing/ATF_Display.md)

## Android scenario stability

- [ ] Qualify on the device the notification of achievements that occurs after the score. At component level, the order of observations and gestures shall be checked with simulated DBD.
- [ ] The process of preparation shall confirm the active window of the temporary profile. A service in that profile opened the Android screen with details of an application during history. Isolate third-game processes in the temporary profile and keep the exact cause in the report.
