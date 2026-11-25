# Validation contract tests

## Overview

The module compares observed responses with contracts, retaining differences and the original cause. [Common strategy](../../../docs/testing/README.md) separates pure policy from integration with documents and reporting.

## Checked properties

| Family | Oracle and boundary |
|---|---|
| Equality and result | Type of value, difference observed and verdict of policy. |
| Contract of Response | JSON document controlled, mandatory fields and localized messages. |
| Schema | Supported keywords, exact budget boundaries and refusal of unsupported or over-budget inputs; pure matrices remain unit tests without Extent. |
| Evidence | Preview, drafting and preserving the values necessary to explain the failure. |

## Execution and limits

[Catalog](../../../config/atf-test-suites.json) separates `ValidationUnitSuite` and `ValidationComponentSuite`. [Shared launcher](../../../scripts/run-component-suites.ps1) publish only the selection of the component; unit tests keep separate entry points and results.

- Use [module entry](../../README.md) in a temporary copy of the dependencies identified.
- The negative samples alter a single relevant condition and keep the witness valid; a defect in the fixture does not demonstrate the requested refusal.

The [budget component](../../../atf-tests/src/test/java/atf/tests/contracts/validation/SchemaBudgetComponentTest.java) checks public contract collaboration, failure/warning policies, paths and fresh evaluation state in auditable Extent without screenshots. Reporting tests check transmitted data separately from renderer acceptance; [evaluation limits](../../README.md#cost-and-limits) define the deterministic boundary.

[Validation API](../../README.md) · [Common strategy](../../../docs/testing/README.md)
