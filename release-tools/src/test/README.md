# Release-tools component tests

## Overview

This source root owns focused JVM component tests for the release-tools Maven module. The suite verifies portable project-root discovery, exact evidence bytes, advisory-only RAG admission, signing-certificate continuity and the boundary between release adapters and game-test owners.

The [shared testing strategy](../../../docs/testing/README.md) defines layer selection, Extent ownership, source-bound evidence and negative-case requirements. Changes to these tests are qualified by the `release-tools` component selection.
