# Common fixtures for Gradle audit

## Overview

These test fixtures provide audit infrastructure for Gradle modules. Component tests own inputs and independent oracles; the adapter captures evaluated comparisons and reconciles the report with original JUnit invocations. [Common strategy](../../../../docs/testing/README.md)

| Responsibility | Contract |
|---|---|
| Capture | Retains the actual value and capture time for each declared input. |
| Comparison | Associates each evaluated check with its original invocation. |
| Verification | Reconciles invocation identities and outcomes between HTML and JUnit. |

The fixtures enter the consumer test variants. Their own verification is in [component catalogue](../test/README.md).
