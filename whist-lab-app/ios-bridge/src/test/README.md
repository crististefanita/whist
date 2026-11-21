# iOS bridge tests

## Overview

This directory verifies the adaptation between the common engine and the native interface iOS. The tests run on JVM; they do not qualify the Xcode building, simulator or distribution to the devices.

## Verified contract

- The public state projection keeps the hands of the opponents hidden and only exposes the legal actions of the player.
- Translation between common models and bridge representation shall preserve rules, bids and scores.
- The limit cases refuse invalid entries without changing the game status.

## Running tests

From directory `whist-lab-app`, run:

```powershell
./gradlew :ios-bridge:test --no-daemon
```

The retained iOS CI workflow may run shared-engine and bridge checks separately. It does not qualify an iOS application, simulator support, signing or distribution; the platform remains parked.

[Common test strategy](../../../../docs/testing/README.md)
