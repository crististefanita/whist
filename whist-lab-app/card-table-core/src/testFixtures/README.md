# Neutral presentation fixtures

## Overview

These immutable observations and continuity assertions belong to the shared presentation contract. They contain no game rules, platform widgets or executable test invocations. Game adapters supply legal public states; the same oracle compares every seat before and after a transition.

## Verification boundary

Producer component cases and adapter consumers exercise the fixtures. Isolated unit cases verify snapshot ownership. Fixtures do not publish a separate report or create an additional test population.

Follow the [shared testing strategy](../../../../docs/testing/README.md) for layer ownership, evidence and reuse.
