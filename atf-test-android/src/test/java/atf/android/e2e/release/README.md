# Acceptance of the bid on the signed APK

## Overview

This collection verifies how the AI recommendation reaches the Android interface. Application components cover AI policy and exhaustive combinations; this journey checks visible selection, manual choice, dialogs and the saved contract.

| Level | Entry and contract | Limit |
|---|---|---|
| Component | [Decoder](turn/BidChoiceObservation.java) reads a single XML observation; [its component checks](turn/BidChoiceObservationComponentTest.java) refuses multiple selections, contradictory confirmations and illegal values. | It does not read a device and it does not estimate the bid. |
| E2E | The [scenario](ReleaseBidScenario.java) configures Romanian Whist through the UI with four players and Hard opponents; it checks the recommendation and a manual alternative on the first hand with two legal bids. Complete a preceding hand when the dealer has only one option. | Do not inject checkpoints or debug data or inspect opponents' private hands. |
| Manual choice under dialogue | [Returning component](../../../../../../../../whist-lab-app/app/src/test/java/ro/whistlab/app/bidding/HumanBidResumeComponentTest.java) checks the overlapping dialogues and the AI reply late. | On APK, the bidding scenario keeps the passage through the score dialog and manual confirmation. The historical test has a native check of the legend. |
| E2E AutoGame | [Scenario](autogame/AutoGameReleaseScenario.java) checks the tick position, automatic completion of the score and return to manual control. The next hand compares the boxes between the bidding and the game, the unique actor's marking and the visibility of the card. | Expect stable status of the interface. The marking check during the rapid shifts belongs to the projection components. |
| E2E table with three players | The nested scenario in [WhistReleaseBidE2eTest](WhistReleaseBidE2eTest.java) checks the first 17-card contract in Descending and Ascending games, access to every card and horizontal hand movement when required, then compares seat bounds after entering play. | Use one Android profile; component matrices retain combinations of resolution, density and enlarged fonts. |
| Signed-release responsive E2E | The same real transition from bidding to play uses configured profiles and restores the display afterward. | Synthetic fixture matrices remain in the debug responsive tests. |
| Menu, achievements, profile and history | [Common interface test](WhistReleaseHomeE2eTest.java) checks RO/EN, resuming the same hand in the score history, saving the name through Android dialogue and displaying after the recovery and opening the catalogue and details of a achievement. The component of the presentation checks order, counting, recharged progress and the texts RO/EN. The component of the profile checks cancellation, simulated persistence and refusal of writing. | Flows use the same data isolation limit; the catalogue does not start a game or change progress. |
| Handoff | The [test](WhistReleaseBidE2eTest.java) preserves the original cause and publishes observations, comparisons and PNGs through the shared UI reporting API. | Publishing a report does not replace the runner's data restoration. |

## Waiting for the turn

[Bid and play waits](turn/ReleaseTurnWaits.java) use the [controlled driver clock](../driver/waiting/UiStateAwaiter.java). Bidding uses the observation that satisfied the wait predicate. Card lookup uses the latest hierarchy, and the driver reads a fresh observation after dispatch.

The dialogue on achievements may renew the time limit for waiting for the score once.

[Mocked ADB components](turn/ReleaseTurnWaitsComponentTest.java) verify command order, timeout, final observation and propagation of the original cause. The APK scenario observes the visible transitions between bidding, play and score.

## Data isolation and teardown

[Context](ReleaseBidContext.java) requires the current exclusive reservation, the same series, the identity of the APK and a verified data limit before any new game. Choose one of two ways; receipts cannot claim backup and secondary isolation in the same run.

- **Backup and restoration: ** `setup-release.json` keeps the series, API, APK path, `requestedApkSha256`, `backupSha256`, `backupScopeValidated=true` and `restoreRequired=true`; archive `app-data-before.tar` must be real, non-zero and verified. Runner restores data after success or failure and faces hash.
- **Temporary secondary Android user:** when the system does not allow the application backup, [the isolation script](../../../../../../../../scripts/Invoke-AtfAndroidDisposableUser.ps1) create a secondary user, temporarily unlock it and extend its time to the screen's off, confirm that the White Lab was not installed there and install the candidate APK only for that user. The Teardown removes its profile and setting, and the receipts prove the initial status of the installation from the original profile. The original data is not deleted; the script does not add Google accounts. The root of the evidence is mandatory outside the checkout, and the runner refuses the paths from the project. The APK temporarily copied to the device is deleted immediately after installation. The profile and game saves only remain in the privacy of the Android temporary data removed at the teardown; no export of profile files or ATF.
- After E2E, call the `Teardown` phase of the same script even when the tests failed. It returns to the original user, only eliminates the user identified by the receipt of this run and issues `data-isolation-cleanup.json`. The lack of receipt `PASS` blocks the qualification.
- In the temporary profile, the runner deactivates Google background services that can block UI System upon initialization. The receipt lists the disabled packages. If there is already an APK with the same SHA-256, the runner activates it for the temporary profile by `install-existing`.
- `setup-release.json` and `device-lease.json` remain in the outer root of the running. The E2E report includes the training receipt; the release file keeps both receipts and the manifest links them through hash.
- Unconfirmed selection shall be maintained after the dialogue has been closed at the same session. The confirmed contract and the public status must be maintained after the process has stopped and resumed.
- The lack of emulator or isolation limit does not become success or E2E evidence. An unexecuted preflight remains `PENDING` in the candidate's file.

Call the common preflgighttel with the selected active booking and profile before installation or backup. [Command and criteria](../../../../../../../../docs/testing/ATF_ReleaseQualification.md#preflight-android)

## Execution

The tests shall be explicitly selected by `WhistReleaseBidE2eTest` and `WhistReleaseHomeE2eTest`, `atf.android.enabled=true`, `whist.records.runRoot`, `atf.android.artifacts` and the booked show. [Primary E2E suite](../../suites/AndroidE2eSuite.java) keep the property of the tests, and [Component suite](../../suites/AndroidComponentSuite.java) Discover the decoder's evidence separately.

For temporary user, prepare candidate after the running directory is in place and the active reserve corresponds to the series:

```powershell
$device = @{
    Phase='Prepare'; RunRoot=$RunRoot; Serial=$Serial; AdbExecutable=$AdbExecutable
    LeasePath=$LeasePath; LeaseId=$LeaseId; ServerSocket=$ServerSocket
    ApkPath=$CandidateApk; ApkSha256=$CandidateSha256; ExpectedApi=28
    ExpectedDisplayProfile='1080x1920@440'
}
.\scripts\Invoke-AtfAndroidDisposableUser.ps1 @device
try {
    # Run the Maven E2E selection with whist.records.runRoot=$RunRoot.
} finally {
    $device.Phase = 'Teardown'
    $device.Remove('ApkPath'); $device.Remove('ApkSha256')
    .\scripts\Invoke-AtfAndroidDisposableUser.ps1 @device
}
```

Phase `Prepare` checks the version, display, candidate hash, active profile and booking before creating the user. Teardown refuses different IDs, exchanged reservations or erasure of the original user.

[Qualification of a relay](../../../../../../../../docs/testing/ATF_ReleaseQualification.md) has the order of checks, inline captures, delimitation of reports and promotion of the dossier.
