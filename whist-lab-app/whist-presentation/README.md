# Whist presentation

## Overview

Game-owned portable projections consumed by Android and Web. This module keeps Whist labels, setup, public table content, achievements, score rows and checkpoint codecs outside both platform renderers.

## Dependency boundaries

The module depends on `game-core` and `card-table-core`. It contains no Android, DOM or reporting dependency. The historical Java package names remain stable for saved-state and consumer compatibility; their physical ownership no longer implies an Android application dependency.

The JSON API is compile-only: Android supplies its system API and the browser launcher supplies the pinned JVM library. The achievement catalog remains generated from the same Whist YAML and validated against the same runtime enums.

## Verification

Existing Whist component cases keep their identities and exercise this module through the application and browser adapters. The common UI matrix belongs to core; game schedules, eligibility and scoring remain game-owned.

[Module × level matrix](../../docs/testing/ATF_TestingGuide.md) · [Whist components](../../docs/testing/ATF_DisplayComponents.md) · [Browser adapter](../../atf-ui-browser/README.md)
