# Whist Achievements - criteria remaining

## Overview

YAML validation at the building and compatibility of progress are documented in [contract of achievements](../game/ATF_Achievements.md). The works below aim at restoring the Android screen and calibration with human players.

## P1 · Restoration of the status of the common screen on Android

The [navigation snapshot contract](../game/ATF_Achievements.md#activity-owned-navigation) separates activity-owned list/selection state from durable progress. Controlled Bundle round trips and activity-composition checks do not certify native list placement, translated glyphs or platform recreation.

- On an identified APK in RO/EN, select an objective, scroll to a row, recreate the activity and verify the same position and selection, including a language change.
- The components shall cover the retention of the tail upon closure, the reappearance of unconfirmed notification and the refusal of an outdated confirmation. On APK, recreate activity before and after confirmation and face each appearance with tail status and saved progress. [Components of notification](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementNoticeCoordinatorComponentTest.java)
- Use existing checkpoints for catalogue and detail. Extend the sample with the recreation of activity in both languages and check for the return to the same objective, the same position and with the correct visible labels.

[Android adapter](../../whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/presentation/WhistAchievementsScreen.java) · [Shared screen](../../whist-lab-app/atf-card-table-ui/README.md#achievements) · [Acceptance of Android](../../atf-test-android/src/test/java/atf/android/e2e/release/WhistReleaseHomeE2eTest.java)

## P2 · Calibration of Order with Human Players

Offline simulation measures opportunities and rates for declared policies, not how natural the order feels to human players. Keep human observation separate from AI calibration and change thresholds only after a repeated signal.

- Uses RO/EN protocols and uniform criteria for understanding descriptions, difficulty and progress.
- Do not mix human results, development data and simulation holdout.
- Any change of threshold or eligibility keeps the IDs and passes through a proof of compatibility of the saved progress.

**Sources** · [Calibration model and protocol](../game/ATF_Achievements.md#verification) · [Offline suite](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementOfflineCalibrationTest.java).
