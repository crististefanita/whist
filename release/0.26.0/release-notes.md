# Whist Lab 0.26.0
Score-aware independent play, durable AI learning batches and improved table-state observation.

## Changes

- Keep independent-player decisions aligned with score position while preserving the Normal and Hard policy contracts.
- Publish complete offline learning batches atomically and preserve recovery when a process stops during publication.
- Preserve achievement navigation and capture rendered table state for stable component verification.
- Reuse component and signed APK flow evidence only when source, suite inputs and APK bytes pass the sealed reuse checks.

## Audit

The commit-pinned Extent audit link is added after the qualified dossier commit is pushed.