# Release catalog

## Overview

The catalog tracks the current Android edition and its immediate predecessor. A release is published only after its APK, source-bound evidence and diff-selected qualification pass the RAG process.

[Current Web test dossier](WEB/README.md) separately indexes component contracts without screenshots and the focused illustrated browser flow. Its manifest preserves report identity and publication state; Web evidence is not an Android edition.

## Current version

[0.25.0 — Android release](0.25.0/README.md) is published; the APK download and checksum passed complete verification.

## Previous version

[0.24.2 — Android release and evidence](0.24.2/README.md) is the immediately preceding published edition.

## Available versions

| Version | State | Dossier |
|---|---|---|
| 0.25.0 | Published; APK and checksum verified | [APK and evidence](0.25.0/README.md) |
| 0.24.2 | Previous published edition | [APK and evidence](0.24.2/README.md) |

## Publication and verification

Preparation, qualification and publication follow the [RAG process](../docs/testing/ATF_ReleaseQualification.md) and [Android guide](../docs/platform/ATF_AndroidDelivery.md). Technical details and checksums belong in each edition's manifest.

## Reading the audit

1. Open the current dossier for scope and download state.
2. Follow its Extent index for selections, provenance and limits.
3. Read the manifest for APK identity, qualification and payload inventory.

The [test strategy](../docs/testing/README.md) applies to every edition; its dossier holds the release-specific results.
