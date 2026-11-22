# Mock — deterministic HTTP transport

## Overview

`atf-mock` replaces the Core `ApiTransport` port with a local response catalog. Use it for offline components and contracts, with controlled inputs and an explicit fallback.

## Minimal example

`mock.yaml` in the working directory:

```yaml
mock:
  routes:
    - {method: GET, path: /health, status: 200, body: '{"status":"ready"}'}
  fallback: {status: 500, body: '{}'}
```

Snippet inside a method; the transport remains offline:

```java
import atf.core.http.request.ApiRequest;
import atf.mock.AtfMockTransport;
import java.nio.file.Path;

var mock = AtfMockTransport.fromConfiguration(Path.of("mock.yaml"));
var response = mock.send(ApiRequest.of("GET", "/health"),
        "https://offline.test/health", "none").join();
```

`response.status()` is `200`; a request with no matching route receives the `500` fallback. The test independently compares the response with the expected result.

## Catalogue and Matching

| Need | API or contract |
|---|---|
| Explicit YAML catalog | `AtfMockTransport.fromConfiguration(Path)` |
| Project configuration | `fromProjectConfiguration()`: `atf.mock.config` > `ATF_MOCK_CONFIG` > `atf-mock.yaml/yml` |
| Request conditions | Method, path, auth profile, query, required headers, and expected body in `MockRoute` |
| Request body | Default `json` mode, negative MIME probe `wrong-content-type`; numeric matching without conversion through `double` |
| Repeated query values | `ApiQueryParameters`; includes the required values with multiplicity |

- Without configuration, `fromProjectConfiguration()` uses the `defaults()` fixture; the application catalog remains local.
- The duration in a route is response metadata, not a simulated real delay.
- A mock probe verifies integration; acceptance of the real service, broker, or device remains separate.

## Verification of use

| Action | Observable result |
|---|---|
| Save the YAML as `mock.yaml` in the process working directory. | The example reads that exact local catalog. |
| Send the request `GET /health`. | The response has status `200`; its body matches the configured route. |
| Send a path with no matching route in the same catalog. | The response has fallback status `500`. |

The address `offline.test` supplies request context for the mock transport. A passing probe verifies catalog matching and the consumed contract; it does not confirm the availability of an external service.

---

The Mock pair is an external binary pinned in the POM and provenance register. Local tests verify the route and fallback used by this project; report defects with the version, hash, minimal catalog and expected/observed response in the [local register](../docs/feedback/README.md).

<a id="shared-library-source"></a>

> **Library source:** [Mock · ATF J0](https://github.com/crististefanita/atf_j0/blob/HEAD/lib/README.Mock.md).
> This project’s POM, provenance and tests define the consumed version and local acceptance.
