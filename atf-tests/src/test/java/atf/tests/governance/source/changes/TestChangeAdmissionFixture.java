package atf.tests.governance.source.changes;

import atf.android.process.CommandProcess;
import atf.tests.testchange.TestChangeProof;
import atf.tests.testchange.TestChangeReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Real Git history and pinned parser binaries; proof contents are deliberately mocked boundary inputs. */
public final class TestChangeAdmissionFixture {
    public static final String ID = "FIXTURE-EARLY-ADMISSION";
    private static final String POSITIVE = "example.MockBoundary#accepts";
    private static final String NEGATIVE = "example.MockBoundary#rejects";
    public static final String SELECTOR_SOURCE = "src/test/java/example/MockBoundary.java";
    private TestChangeAdmissionFixture() { }

    public static String prepareReview(Path source) throws Exception {
        write(source, SELECTOR_SOURCE, """
                package example;
                import atf.testkit.AtfComponentTest;
                import org.junit.jupiter.api.Test;
                @AtfComponentTest class MockBoundary {
                    @Test void accepts() { }
                    @Test void rejects() { }
                }
                """);
        git(source, "init", "-q", "-b", "main");
        git(source, "config", "user.name", "ATF fixture");
        git(source, "config", "user.email", "fixture@example.invalid");
        git(source, "config", "core.autocrlf", "false");
        git(source, "config", "commit.gpgSign", "false");
        git(source, "add", "-A");
        git(source, "commit", "-qm", "fixture baseline");
        String base = git(source, "rev-parse", "HEAD");
        write(source, TestChangeGitFixture.FEEDBACK, control(base, "ACTIVE", LocalDate.now(java.time.ZoneOffset.UTC).plusDays(2)));
        git(source, "add", "-A");
        git(source, "commit", "-qm", "authorize the independent build snapshot fixture");
        return base;
    }

    public static Path verifiedProof(Path source, String base, Path proofRoot, LocalDate expires) throws Exception {
        Files.deleteIfExists(source.resolve(TestChangeGitFixture.FEEDBACK));
        write(source, TestChangeGitFixture.BASELINE, control(base, "VERIFIED", expires));
        write(source, "pom.xml", "<project><!-- controlled protected input --></project>\n");
        git(source, "add", "-A");
        git(source, "commit", "-qm", "mocked proof boundary candidate");
        Path folder = Files.createDirectories(proofRoot.resolve(ID));
        Files.writeString(folder.resolve(TestChangeProof.REPORT), "<html><body>Mock proof boundary input</body></html>");
        var plan = TestChangeReview.proofPlan(source, base, ID, expires);
        TestChangeProof.stage(plan, List.of(new TestChangeProof.Outcome(POSITIVE, "PASS")),
                List.of(new TestChangeProof.Outcome(NEGATIVE, "PASS")), folder);
        Files.move(folder.resolve(TestChangeProof.PENDING), folder.resolve(TestChangeProof.RECEIPT));
        return folder;
    }

    public static void replaceSelectorSource(Path source, String declaration) throws Exception {
        write(source, SELECTOR_SOURCE, declaration);
        git(source, "add", "-A");
        git(source, "commit", "-qm", "controlled selector mutation");
    }

    public static Path parserCache(Path directory) throws Exception {
        String version = new ObjectMapper().version().toString();
        for (Class<?> type : List.of(com.fasterxml.jackson.annotation.JsonProperty.class,
                com.fasterxml.jackson.core.JsonParser.class, ObjectMapper.class, YAMLFactory.class)) {
            Path jar = binary(type);
            String file = jar.getFileName().toString();
            String artifact = file.substring(0, file.length() - version.length() - 5);
            String group = artifact.equals("jackson-dataformat-yaml") ? "dataformat" : "core";
            Path target = directory.resolve("com/fasterxml/jackson/" + group + "/" + artifact + "/" + version);
            Files.createDirectories(target);
            Files.copy(jar, target.resolve(file));
            if (group.equals("dataformat")) Files.copy(jar.resolveSibling(artifact + "-" + version + ".pom"),
                    target.resolve(artifact + "-" + version + ".pom"));
        }
        Path snake = binary(org.yaml.snakeyaml.Yaml.class);
        String file = snake.getFileName().toString();
        String snakeVersion = file.substring("snakeyaml-".length(), file.length() - 4);
        Path target = directory.resolve("org/yaml/snakeyaml/" + snakeVersion);
        Files.createDirectories(target);
        Files.copy(snake, target.resolve(file));
        return directory;
    }

    /** Artifact/snapshot components control the admission neighbour; admission integration uses the real host. */
    public static Path mockedLauncherHost(Path real, Path directory) throws Exception {
        Path scripts = Files.createDirectories(directory.resolve("scripts"));
        try (var files = Files.list(real.resolve("scripts"))) {
            for (Path file : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".ps1")).toList())
                Files.copy(file, scripts.resolve(file.getFileName()));
        }
        Path tools = Files.createDirectories(directory.resolve("atf-tests/tools"));
        try (var files = Files.list(real.resolve("atf-tests/tools"))) {
            for (Path file : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".mjs")
                    && !p.toString().endsWith(".test.mjs")).toList()) Files.copy(file, tools.resolve(file.getFileName()));
        }
        // Runtime module imports own their pinned configuration beside the copied tool host.
        Path config = Files.createDirectories(directory.resolve("config"));
        try (var files = Files.list(real.resolve("config"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) Files.copy(file, config.resolve(file.getFileName()));
        }
        Path releasePolicy = Files.createDirectories(directory.resolve("release-tools/config"))
                .resolve("release-platform-rag-policy.json");
        Files.copy(real.resolve("release-tools/config/release-platform-rag-policy.json"), releasePolicy);
        Files.writeString(scripts.resolve("AtfTestChangeAdmission.ps1"), """
                function Invoke-AtfTestChangeAdmission {
                  param($SourceRoot,$BaseRef,$TempRoot,$JavaHome,$MavenCache,$ReceiptFile,[switch]$GenerateProofs,$Control)
                  if($GenerateProofs){throw 'The admission neighbour mock cannot generate verified proofs'}
                  [IO.File]::WriteAllText($ReceiptFile,'{"schemaVersion":1,"kind":"MOCK_TEST_CHANGE_ADMISSION","status":"PASS","mode":"VERIFY"}')
                }
                """);
        Files.writeString(scripts.resolve("AtfQualificationSnapshot.ps1"), """

                # Adjacent editorial acceptance is controlled here; metadata and snapshot logic remain real.
                function Assert-AtfMetadataEditorial {
                    param($SourceRoot,$TempRoot,$JavaHome,$MavenCache,$Receipt)
                    [IO.File]::WriteAllText($Receipt,'{"status":"PASS","kind":"MOCK_EDITORIAL_NEIGHBOUR"}')
                }
                """, java.nio.file.StandardOpenOption.APPEND);
        return scripts;
    }

    /** The proof-admission component exercises the canonical source validator and controls editorial admission. */
    public static Path admissionLauncherHost(Path real, Path directory) throws Exception {
        Path scripts = mockedLauncherHost(real, directory);
        Files.writeString(scripts.resolve("AtfTestChangeAdmission.ps1"),
                ". '" + real.resolve("scripts/AtfTestChangeAdmission.ps1").toString().replace("'", "''") + "'\n" + preparedValidatorPort());
        return scripts;
    }

    /** The metadata component exercises canonical editorial admission and controls the source-proof neighbour. */
    public static Path editorialLauncherHost(Path real, Path directory) throws Exception {
        Path scripts = mockedLauncherHost(real, directory);
        Files.copy(real.resolve("scripts/AtfQualificationSnapshot.ps1"), scripts.resolve("AtfQualificationSnapshot.ps1"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(real.resolve("pom.xml"), directory.resolve("pom.xml"));
        Files.writeString(scripts.resolve("AtfStandaloneJava.ps1"),
                ". '" + real.resolve("scripts/AtfStandaloneJava.ps1").toString().replace("'", "''") + "'\n" + preparedValidatorPort());
        return scripts;
    }

    /** Compilation has its own components; launcher ordering uses the parent's prepared canonical CLI classes. */
    private static String preparedValidatorPort() {
        return """
                function Invoke-AtfJvmCli {
                    param($ToolRoot,$ProjectRoot,$ScratchRoot,$JavaHome,$ClassPath,$MainClass,$Arguments,[ref]$Result,$AdditionalSourcePaths)
                    if ($MainClass -cnotin @('atf.tests.testchange.TestChangeAdmissionCli','atf.tests.documentation.cli.DocumentationGovernanceCli') -or
                        !$env:ATF_FIXTURE_VALIDATOR_CLASSPATH) { throw 'UNBOUND_PREPARED_VALIDATOR' }
                    $request = @('--root',$ProjectRoot) + @($Arguments)
                    $identity = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($env:ATF_FIXTURE_VALIDATOR_CLASSPATH)))
                    [IO.File]::AppendAllText($env:ATF_FIXTURE_VALIDATOR_CALLS, ((@{kind='CONTROLLED_COMPILER_CANONICAL_VALIDATOR';mainClass=$MainClass;classpathSha256=$identity;arguments=$request}|ConvertTo-Json -Compress) + [Environment]::NewLine))
                    $java = if ($JavaHome) { Join-Path $JavaHome 'bin/java' } else { (Get-Command java).Source }
                    ($request|ConvertTo-Json -Compress -EscapeHandling EscapeNonAscii) | & $java '-classpath' $env:ATF_FIXTURE_VALIDATOR_CLASSPATH $MainClass '--stdin'
                    $Result.Value = $LASTEXITCODE
                }
                """;
    }

    private static Path binary(Class<?> type) throws Exception {
        Path file = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isRegularFile(file)) throw new IllegalStateException("Expected a pinned parser JAR: " + type);
        return file;
    }

    private static String control(String base, String status, LocalDate expires) {
        return "```yaml\ntestChanges:\n  - id: " + ID + "\n    status: " + status + "\n    base: " + base
                + "\n    expires: " + expires + "\n    reason: Isolate source snapshot admission from mocked Maven execution."
                + "\n    proof: 'mvn -pl atf-tests test'\n    checks:\n      positive: ['" + POSITIVE
                + "']\n      negative: ['" + NEGATIVE + "']\n    paths: ['pom.xml', '" + SELECTOR_SOURCE + "']\n```\n";
    }

    private static void write(Path root, String file, String text) throws Exception {
        Path target = root.resolve(file);
        Files.createDirectories(target.getParent());
        Files.writeString(target, text);
    }

    static String git(Path source, String... arguments) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", source.toString()));
        command.addAll(List.of(arguments));
        var result = CommandProcess.execute(command, Map.of("GIT_TERMINAL_PROMPT", "0"), Duration.ofSeconds(30), false);
        if (result.exitCode() != 0) throw new IllegalStateException(result.stderr());
        return new String(result.stdout(), StandardCharsets.UTF_8).strip();
    }
}
