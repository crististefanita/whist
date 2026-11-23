# Portable presentation component tests

## Overview

Components exercise the real portable implementation using explicit content, dimensions and controlled collaborators. Each component records its inputs and evaluated expected/actual assertions in Extent. Continuity and resolution matrices contain no pictures.

## Organization

The original Java test identities remain unchanged after extraction. Package ownership follows the production responsibility: cards, hands, menus, score, seats, table and turn pacing. Boundary components reject cross-module coupling and invalid observations.

The separate `unitTest` source set checks isolated normalization and immutable ownership with JUnit only. Its cases are declared as units in the same inventory and do not become Extent component invocations.

## Adapter contracts

- Core fixtures accept immutable identified regions in logical coordinates; one oracle compares identities, order and bounds.
- Android measurements remove density and insets; browser measurements remove scene origin and scale.
- Game-specific schedules and legal cases remain in the game provider.

[Testing strategy](../../../../docs/testing/README.md) · [Detailed matrix](../../../../docs/testing/ATF_TestingGuide.md) · [Shared phase contract](../../../../docs/testing/ATF_TableContinuity.md)
