package atf.tests.governance.documentation.factual;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

@AtfComponentTest
class DocumentedModuleMapGovernanceTest {
    @Test void rootMapMatchesTheBuildDeclarations() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        AuditedAssertions.assertEquals(List.of(), DocumentedModuleMap.inspect(root, Files.readString(root.resolve("README.md"))));
    }

    @Test void thePublishedApkIsFollowedByTheArchitectureMap() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String readme = Files.readString(root.resolve("README.md"));
        var sections = java.util.regex.Pattern.compile("(?m)^## (.+)$").matcher(readme).results()
                .map(heading -> heading.group(1)).toList();
        boolean english = atf.tests.documentation.policy.DocumentationContentPolicy.documentationLanguage(root).equals("en");
        int product = sections.indexOf(english ? "Game" : "Jocul");
        AuditedAssertions.assertTrue(product >= 0 && product + 1 < sections.size(), sections.toString());
        AuditedAssertions.assertEquals(english ? "Module map" : "Harta modulelor", sections.get(product + 1), sections.toString());
    }

    @Test void aMissingModuleAChangedScopeAndAnInvertedEdgeAreRejected() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String readme = Files.readString(root.resolve("README.md"));
        String uses = atf.tests.documentation.policy.DocumentationContentPolicy.documentationLanguage(root).equals("en")
                ? "uses" : "folosește";
        for (var mutation : List.of(
                List.of("| Maven | atf-validation, atf-test-android, atf-tests |",
                        "| Maven | atf-validation, atf-tests |", "Maven modules"),
                List.of("| Maven | atf-validation | test | atf-test-kit |",
                        "| Maven | atf-validation | compile | atf-test-kit |", "dependency scopes"),
                List.of("| Gradle | game-core | testImplementation | ai-lab |",
                        "| Gradle | game-core | implementation | ai-lab |", "dependency scopes"),
                List.of("app -->|" + uses + "| game_core",
                        "game_core -->|" + uses + "| app", "product diagram edges"))) {
            AuditedAssertions.assertTrue(readme.contains(mutation.get(0)), "The mutation must change a real documented claim");
            var failures = DocumentedModuleMap.inspect(root, readme.replace(mutation.get(0), mutation.get(1)));
            AuditedAssertions.assertTrue(failures.stream().anyMatch(failure -> failure.contains(mutation.get(2))), failures.toString());
        }
    }

    @Test void explicitModuleLocationsRejectDuplicateAndEscapingAliases(@org.junit.jupiter.api.io.TempDir java.nio.file.Path checkout) throws Exception {
        var gradle = Files.createDirectories(checkout.resolve("build-root"));
        Files.createDirectories(gradle.resolve("gradle"));
        Files.writeString(gradle.resolve("gradle/component-audit.gradle"),
                "owner.dependencies.add('testImplementation', owner.dependencies.testFixtures(project(':test-audit')))");
        var port = Files.createDirectories(checkout.resolve("portable"));
        Files.writeString(port.resolve("build.gradle"), "plugins { id 'java-library' }\n");
        String alias = "project(\":port\").projectDir = file(\"../portable\")\n";
        Files.writeString(gradle.resolve("settings.gradle"), alias);
        AuditedAssertions.assertEquals(1, BuildModuleDeclarations.gradleEdges(gradle, java.util.Set.of("port")).size(),
                "A literal sibling module inside the checkout is supported");
        for (String invalid : List.of(alias + alias, alias.replace("../portable", "../../outside"))) {
            Files.writeString(gradle.resolve("settings.gradle"), invalid);
            var failure = AuditedAssertions.assertThrows(IllegalArgumentException.class,
                    () -> BuildModuleDeclarations.gradleEdges(gradle, java.util.Set.of("port")));
            AuditedAssertions.assertTrue(failure.getMessage().contains("DOC-BUILD-MAP"), "Preserve the precise inventory failure");
        }
    }
}
