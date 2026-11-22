# ATF libraries

## Overview

Java 21 and Maven; five thin JARs, with transitive dependencies declared in their POMs. These guides are usable without the producer's source code.

| Guide | Artifact, group `atf.api` | Use |
|---|---|---|
| [Core](README.Core.md) | `atf-core` | HTTP, configuration, JSON and lifecycle. |
| [Reporting](README.Reporting.md) | `atf-reporting` | Extent/fallback and auditable evidence. |
| [Console](README.Console.md) | `atf-console` | Maven and IntelliJ diagnostics. |
| [Mock](README.Mock.md) | `atf-mock` | Components and offline HTTP contracts. |
| [Test Kit](README.TestKit.md) | `atf-test-kit` · audit and a neutral JUnit lifecycle | Classification, comparisons and test support. |

## Integration

1. In a TMP copy of an ATF consumer, `mvn -B -ntp -N validate` installs the pairs from lib into the selected Maven repository.
2. Declare the dependency in the calling module; its coordinates and version are in the POM beside the JAR.
3. IntelliJ uses the same JDK and Maven repository; reload the root POM.

<details>
<summary>New consumer without the ATF reactor</summary>

Copy the JAR/POM pairs into lib; from the new project in TMP:

```powershell
Get-ChildItem lib\*.pom | ForEach-Object {
    mvn org.apache.maven.plugins:maven-install-plugin:3.2.0:install-file "-Dfile=$([IO.Path]::ChangeExtension($_.FullName,'jar'))" "-DpomFile=$($_.FullName)" -DgeneratePom=false
    if ($LASTEXITCODE) { throw "Installation failed: $($_.Name)" }
}
```

For example, declare `atf.api:atf-reporting:<versiunea din POM>` as a dependency. Use `compile` scope for support in src/main and `test` only in src/test; Maven resolves transitive dependencies.

</details>

## Identity and verification

- One JAR/POM pair per library, the index and five guides, directly in lib.
- In ATF, pom.xml selects versions; library-provenance.json records hashes and origin.
- Governance and acceptance run in TMP; each consumer verifies its own profile and classpath.

## Choice of minimum verification

| Library | Starting point | Observable contract |
|---|---|---|
| Core | [Minimal example](README.Core.md#minimal-example) | URL construction and preservation of JSON precision. |
| Reporting | [Minimal example](README.Reporting.md#minimal-example) | Publication completes before a failed comparison is propagated. |
| Console | [Minimal example](README.Console.md#minimal-example) | Visible message or disabled console, according to the explicit policy. |
| Mock | [Minimal example](README.Mock.md#minimal-example) | Configuration and request produce the declared response or fallback. |
| Test Kit | [Minimal example](README.TestKit.md#minimal-example) | One evaluated comparison, PASS/FAIL and the original JUnit cause. |

First verify compilation against the physical JAR/POM pairs, then the snippet's behavior. Qualify application contracts, real transport and interactive presentation separately in the consumer.

## Maintenance and feedback

Libraries are consumed as versioned JAR/POM pairs; their source code belongs outside this project. Retain exact coordinates and hashes, then verify the contract by compiling examples and running local integration tests. For an observed limitation, attach the pair, hash, reproducer and expected/observed values to [project feedback](../docs/feedback/README.md).

<a id="shared-library-source"></a>

> **Library sources:** [Catalog · ATF J0](https://github.com/crististefanita/atf_j0/blob/HEAD/lib/README.md).
> This project’s POM, provenance and tests define the consumed version and local acceptance.
