# iOS delivery status

## Overview

The Whist APK is an Android package and cannot be installed on iPhone or iPad. The
iOS bridge is a separate SwiftUI application that adapts the shared Java game
engine through J2ObjC. The shared engine remains the source for rules, AI, bidding,
scoring and saved-game behavior.

## Release status

- iOS delivery is parked; the executable release policy records
  `platforms.ios.status = PARKED` and `activeReleaseGate = false`.
- Current release plans qualify Android only. An Android APK is never evidence of
  iOS support.
- The retained native workflow and iOS gate definitions are not part of active
  release qualification.

## Reactivation criteria

Reactivation requires a reviewed policy change and qualified native delivery gates.
Before enabling the platform, verify the current Apple toolchain and runner
availability, restore a complete native test path, produce the declared Apple
artifact, and validate signing and distribution independently from Android. The
policy must then explicitly make iOS an active release target; documentation or a
successful Android release cannot do this implicitly.

The retained [iOS workflow](../../.github/workflows/ios.yml) can still be invoked
for isolated engineering checks. Such a run does not qualify a release while the
policy marks iOS as parked.

[Apple platform releases](https://developer.apple.com/news/releases/) ·
[Xcode system requirements](https://developer.apple.com/xcode/system-requirements/) ·
[J2ObjC project and limits](https://github.com/google/j2objc)

## Delivery boundary

When work resumes, build and test the native app on a reviewed source commit,
retain the simulator result, then separately qualify signing, physical-device
installation and the chosen Apple distribution channel. Keep the resulting
`xcarchive` or IPA and its evidence distinct from the Android APK and release
receipts.

[Android delivery](ATF_AndroidDelivery.md) · [iOS bridge](../../ios/README.md)
