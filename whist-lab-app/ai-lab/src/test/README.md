# Tests of the independent laboratory

## Overview

The laboratory qualifies the independent player and the offline evaluation mechanisms. [Common strategy](../../../../docs/testing/README.md) has the level of verification; integration with the engine and research on the corpus have separate fields.

## Boundaries and inputs

| Boundary | Responsible verification |
|---|---|
| Local Java contract | Types, states and inputs legally generated, without dependence on game archives. |
| Tools Node.js | Corpus, reservation, selection and composition, with controlled fixtures near the responsible tool. |
| Engine integration | The engine tests compare observation and legal actions through the adapter. |
| Strategic evaluation | Separate protocol, frozen sources and reserved lots; the concrete result remains external. |

## Execution and interpretation

[Gradle](../../build.gradle) owns `check` and local entry points; the [laboratory guide](../../README.md) defines prerequisites. [Tools](../../tools/README.md) have their own contracts and tests; experiments kept separately do not fall into active policy.

A local sample validates the contract exercised without demonstrating the superiority of the player. Select the corpus and held-out evaluation explicitly, with the identity and exposure of the data retained.

[AI lab](../../README.md) · [Common strategy](../../../../docs/testing/README.md) · [Protocol offline](../../../../docs/learning/README.md)
