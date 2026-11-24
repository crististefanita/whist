# Native flow audit

## Overview

Seven focused signed-APK scenarios passed on Android 9 / API 28 at 1080 × 1920, 440 dpi. Each report contains ordered actions, observed values, individual assertions and inline PNG captures. No loose image or duplicate JUnit XML is retained.

| Flow | Invocations | Evidence | Identity |
|---|---:|---|---|
| history-release-sanity | 1 | [Extent](history-release-sanity/report.html) | [Receipt](history-release-sanity/execution.json) |
| responsive-ui-sanity | 1 | [Extent](responsive-ui-sanity/report.html) | [Receipt](responsive-ui-sanity/execution.json) |
| signed-release-achievements | 1 | [Extent](signed-release-achievements/report.html) | [Receipt](signed-release-achievements/execution.json) |
| signed-release-auto-game | 1 | [Extent](signed-release-auto-game/report.html) | [Receipt](signed-release-auto-game/execution.json) |
| signed-release-bidding | 1 | [Extent](signed-release-bidding/report.html) | [Receipt](signed-release-bidding/execution.json) |
| signed-release-home-profile | 1 | [Extent](signed-release-home-profile/report.html) | [Receipt](signed-release-home-profile/execution.json) |
| signed-release-smoke | 1 | [Extent](signed-release-smoke/report.html) | [Receipt](signed-release-smoke/execution.json) |

## Execution boundaries

Each flow used a disposable Android user. The ready and cleanup receipts preserve its lease and the original user/package state; the emulator lease is released. The original native execution source is retained even though the final documentation revision is newer. [Byte-identical build reconciliation](../diagnostics/build-reconciliation.json) explains that boundary.

The first smoke capture contains a transient Android Messages setup banner; its final home checkpoint is unobscured. Broader Android/Web parity, enlarged-text matrices and consumer-device qualification remain in the [active feedback](../../../../docs/feedback/README.md).

[Shared testing strategy](../../../../docs/testing/README.md)
