package atf.tests.governance.support;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static atf.tests.governance.support.SourceInputFixture.*;

@AtfComponentTest
class SigningSourceIsolationComponentTest {
    @Test void gitReconstructionMatchesACheckoutContainingALocalKey(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        Path script = scratch.resolve("reconstruct.ps1");
        Files.writeString(script, """
                param($Root,$Archive,$Rebuilt,$InventoryScript,$ResultFile)
                $ErrorActionPreference='Stop'
                & git -c core.autocrlf=false -c core.eol=lf init --quiet $Root
                if ($LASTEXITCODE) { throw 'fixture init failed' }
                & git -c core.autocrlf=false -c core.eol=lf -C $Root add .
                if ($LASTEXITCODE) { throw 'fixture index failed' }
                & git -c core.autocrlf=false -c core.eol=lf -c user.name=Fixture -c user.email=fixture@example.invalid -C $Root commit --quiet -m fixture
                if ($LASTEXITCODE) { throw 'fixture commit failed' }
                & git -c core.autocrlf=false -c core.eol=lf -C $Root archive --format=zip --output=$Archive HEAD
                if ($LASTEXITCODE) { throw 'fixture archive failed' }
                Expand-Archive -LiteralPath $Archive -DestinationPath $Rebuilt
                [IO.File]::WriteAllText((Join-Path $Root 'release-tools/.atf-signing/whist-android-test.keystore'), 'dummy private material')
                $archiveInventory = (& $InventoryScript -SourceRoot $Rebuilt -AsJson | Out-String).Trim()
                $checkoutInventory = (& $InventoryScript -SourceRoot $Root -AsJson | Out-String).Trim()
                [ordered]@{archive=$archiveInventory;checkout=$checkoutInventory} |
                    ConvertTo-Json -Compress | Set-Content -LiteralPath $ResultFile -Encoding utf8NoBOM
                """);
        Path rebuilt = scratch.resolve("rebuilt");
        Path result = scratch.resolve("inventories.json");
        Path inventoryScript = GovernanceProject.locateProjectRoot().resolve("scripts/Get-AtfSourceInputs.ps1");
        runSetup(script, List.of(root.toString(), scratch.resolve("source.zip").toString(), rebuilt.toString(),
                inventoryScript.toString(), result.toString()), scratch);
        var inventories = new ObjectMapper().readTree(result.toFile());
        AuditedAssertions.assertEquals(new ObjectMapper().readTree(inventories.required("archive").asText()),
                new ObjectMapper().readTree(inventories.required("checkout").asText()),
                "The same Git source qualifies with or without a local key");
    }

    @Test void localKeysAndTheirRotationDoNotChangeThePublicSourceSnapshot(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        var clean = inventory(root, scratch);
        for (String key : List.of("whist-android-test.keystore", "backup/key.p12", "local-password.txt")) {
            write(root, "release-tools/.atf-signing/" + key, "dummy private material");
        }
        AuditedAssertions.assertEquals(clean, inventory(root, scratch), "A local key does not affect qualification identity");
        write(root, "release-tools/.atf-signing/whist-android-test.keystore", "rotated dummy private material");
        var rotated = inventory(root, scratch);
        AuditedAssertions.assertEquals(clean, rotated, "Private key rotation is not a source change");
        Path stage = scratch.resolve("export");
        copy(root, stage, rotated, scratch, 0);
        AuditedAssertions.assertEquals("public certificate", Files.readString(stage.resolve("release-tools/.atf-signing/whist-android-test.pem")));
        try (var files = Files.list(stage.resolve("release-tools/.atf-signing"))) {
            AuditedAssertions.assertEquals(List.of(".gitignore", "README.md", "whist-android-test.fingerprint", "whist-android-test.pem"),
                    files.map(file -> file.getFileName().toString()).sorted().toList());
        }
        AuditedAssertions.assertEquals("rotated dummy private material", Files.readString(root.resolve("release-tools/.atf-signing/whist-android-test.keystore")),
                "Qualification never alters the local key");
    }

    @Test void publicIdentityAndUnexpectedSourcesRemainFingerprintInputs(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        var before = inventory(root, scratch);
        write(root, "release-tools/.atf-signing/whist-android-test.fingerprint", "changed public identity");
        write(root, "unexpected-source.txt", "unreviewed input");
        var after = inventory(root, scratch);
        AuditedAssertions.assertEquals(hash("changed public identity"), after.get("release-tools/.atf-signing/whist-android-test.fingerprint"));
        AuditedAssertions.assertNotEquals(before, after, "The private exclusion cannot hide other source changes");
        AuditedAssertions.assertEquals(hash("unreviewed input"), after.get("unexpected-source.txt"));
    }

    @Test void aForgedManifestCannotCopyPrivateMaterialOrPartiallyExportPublicFiles(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        write(root, "release-tools/.atf-signing/whist-android-test.keystore", "dummy private material");
        Path cases = scratch.resolve("private-path-cases.json");
        new ObjectMapper().writeValue(cases.toFile(), List.of(
                "release-tools/.atf-signing/whist-android-test.keystore",
                "RELEASE-TOOLS/.ATF-SIGNING/WHIST-ANDROID-TEST.KEYSTORE",
                "legal/../release-tools/.atf-signing/whist-android-test.keystore",
                "release-tools/.atf-signing\\whist-android-test.keystore",
                "release-tools/.atf-signing/unknown.pem",
                ".atf-signing/whist-android-test.keystore"));
        Path script = scratch.resolve("verify-private-path-cases.ps1");
        Files.writeString(script, """
                param($Root,$CopyScript,$CasesFile,$Scratch)
                $ErrorActionPreference='Stop'
                $paths=Get-Content -LiteralPath $CasesFile -Raw | ConvertFrom-Json
                for ($index=0; $index -lt $paths.Count; $index++) {
                    $entries=@(
                        [ordered]@{path='release-tools/.atf-signing/README.md';sha256='%s'},
                        [ordered]@{path=[string]$paths[$index];sha256='%s'}
                    )
                    $manifest=Join-Path $Scratch "forged-$index.json"
                    ConvertTo-Json -InputObject $entries -Depth 4 | Set-Content -LiteralPath $manifest -Encoding utf8NoBOM
                    $destination=Join-Path $Scratch "refused-$index"
                    $rejected=$false
                    try { & $CopyScript -SourceRoot $Root -Destination $destination -InputManifest $manifest }
                    catch {
                        $message=$_.ToString()
                        if ($message -notmatch 'SOURCE-STAGE-PRIVATE-SIGNING:') { throw }
                        if ($message -match 'dummy private material') { throw 'Private key contents leaked into diagnostics.' }
                        $rejected=$true
                    }
                    if (-not $rejected) { throw "Private alias was accepted: $($paths[$index])" }
                    if (Test-Path -LiteralPath $destination) { throw 'Rejected manifest created a partial staging tree.' }
                }
                """.formatted(hash("public signing contract"), hash("dummy private material")));
        Path copyScript = GovernanceProject.locateProjectRoot().resolve("scripts/Copy-AtfSourceInputs.ps1");
        runSetup(script, List.of(root.toString(), copyScript.toString(), cases.toString(), scratch.toString()), scratch);
    }

    @Test void aPublicPathCannotFollowALinkToPrivateMaterial(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        Path privateDirectory = Files.createDirectory(scratch.resolve("private"));
        write(privateDirectory, "key", "dummy private material");
        Path script = scratch.resolve("junction.ps1");
        Files.writeString(script, "param($Link,$Target)\nNew-Item -ItemType Junction -Path $Link -Target $Target -ErrorAction Stop | Out-Null\n");
        runSetup(script, List.of(root.resolve("linked").toString(), privateDirectory.toString()), scratch);
        String inventoryFailure = execute("Get-AtfSourceInputs.ps1", List.of("-SourceRoot", root.toString(), "-AsJson"), scratch, 1);
        AuditedAssertions.assertTrue(inventoryFailure.contains("linked"), "Inventory refuses an unaccounted link");
        Path stage = scratch.resolve("indirect-export");
        String copyFailure = copy(root, stage, Map.of("linked/key", hash("dummy private material")), scratch, 1);
        AuditedAssertions.assertTrue(copyFailure.contains("SOURCE-STAGE-INDIRECT:"));
        AuditedAssertions.assertFalse(Files.exists(stage), "A forged manifest cannot bypass link validation");
    }

    private static Path fixture(Path scratch) throws Exception {
        Path root = Files.createDirectory(scratch.resolve("checkout"));
        write(root, "config/atf-governance-policy.json", "{\"mavenModules\":[],\"additionalSourceRoots\":[]}");
        write(root, "release-tools/.atf-signing/.gitignore", "*\n!.gitignore\n!README.md\n!whist-android-test.pem\n!whist-android-test.fingerprint\n");
        write(root, "release-tools/.atf-signing/README.md", "public signing contract");
        write(root, "release-tools/.atf-signing/whist-android-test.pem", "public certificate");
        write(root, "release-tools/.atf-signing/whist-android-test.fingerprint", "public identity");
        write(root, ".atf-signing/legacy-private.key", "dummy private material");
        return root;
    }

    private static void runSetup(Path script, List<String> arguments, Path scratch) throws Exception {
        var command = new java.util.ArrayList<>(List.of("pwsh", "-NoProfile", "-File", script.toString()));
        command.addAll(arguments);
        Path output = Files.createTempFile(scratch, "setup-", ".txt");
        var builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().put("TEMP", scratch.toString());
        builder.environment().put("TMP", scratch.toString());
        Process process = builder.start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS));
            AuditedAssertions.assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            if (process.isAlive()) { process.destroyForcibly(); process.waitFor(); }
        }
    }
}
