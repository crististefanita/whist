package atf.tests.testchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;

/** Admits editorial descendants without rebinding or rewriting the original proof. */
final class EditorialProofContinuation {
    private static final int MAX_COMMITS = 256;
    private EditorialProofContinuation() { }

    static TestChangeProof.Continuation verify(Path root, TestChangeProof.Identity wanted,
                                               TestChangeProof.Identity original) throws IOException, InterruptedException {
        if (!TestChangeControls.commitId(original.commit())
                || !GitTestChangeSnapshot.commit(root, original.commit()).equals(original.commit())
                || GitTestChangeSnapshot.git(root, "merge-base", "--is-ancestor", original.base(), original.commit()).exitCode() != 0
                || GitTestChangeSnapshot.git(root, "merge-base", "--is-ancestor", original.commit(), wanted.commit()).exitCode() != 0)
            throw TestChangeProof.failure("COMMIT", "original proof must be a locally available ancestor");
        String originalDiff = GitTestChangeSnapshot.rawDiff(root, original.base(), original.commit());
        if (!hash(originalDiff).equals(original.diffSha256())) throw TestChangeProof.failure("DIFF", "original Git diff differs");
        String protectedDiff = protectedDiff(originalDiff);
        if (!protectedDiff.equals(protectedDiff(GitTestChangeSnapshot.rawDiff(root, wanted.base(), wanted.commit()))))
            throw TestChangeProof.failure("COMMIT", "protected Git diff changed since the original proof");
        var history = GitTestChangeSnapshot.output(root, "rev-list", "--reverse", "--ancestry-path", "--parents",
                "--max-count=" + (MAX_COMMITS + 1), original.commit() + ".." + wanted.commit()).lines().toList();
        if (history.isEmpty() || history.size() > MAX_COMMITS) throw TestChangeProof.failure("COMMIT", "editorial history exceeds the admission boundary");
        var changed = new TreeSet<String>();
        String parent = original.commit();
        for (String line : history) {
            var commits = line.split(" ");
            if (commits.length != 2 || !commits[1].equals(parent))
                throw TestChangeProof.failure("COMMIT", "editorial continuation requires a linear descendant history");
            for (String file : GitTestChangeSnapshot.fields(GitTestChangeSnapshot.output(root, "diff", "--name-only", "-z",
                    "--no-renames", "--no-ext-diff", "--no-textconv", parent, commits[0], "--"))) {
                if (!editorial(file)) throw TestChangeProof.failure("COMMIT", "non-editorial input changed: " + file);
                String before = document(root, parent, file), after = document(root, commits[0], file);
                if (file.endsWith(".md") && !TestChangeControls.proofMetadata(before).equals(TestChangeControls.proofMetadata(after)))
                    throw TestChangeProof.failure("COMMIT", "testChanges metadata changed: " + file);
                changed.add(file);
            }
            parent = commits[0];
        }
        if (!parent.equals(wanted.commit())) throw TestChangeProof.failure("COMMIT", "editorial history is incomplete");
        return new TestChangeProof.Continuation(wanted.id(), original.commit(), wanted.commit(),
                hash(protectedDiff), List.copyOf(changed));
    }

    private static String document(Path root, String commit, String file) throws IOException, InterruptedException {
        String entry = GitTestChangeSnapshot.output(root, "ls-tree", "-z", commit, "--", file);
        if (entry.isEmpty()) return "";
        if (!entry.startsWith("100644 blob ")) throw TestChangeProof.failure("COMMIT", "editorial input is not a regular non-executable blob: " + file);
        return GitTestChangeSnapshot.output(root, "show", commit + ":" + file);
    }

    private static boolean editorial(String file) {
        if (file.matches("docs/game/ATF_Rules(?:Ro|En)\\.md")) return false;
        if (file.equals("config/documentation-editions.json") || file.equals("README.md")) return true;
        if (!file.endsWith(".md")) return false;
        if (file.startsWith("docs/") || file.startsWith("lib/README.")) return true;
        if (file.startsWith("src/") || file.contains("/src/")) return !file.matches("(?:.*/)?src/[^/]+/(?:assets|res|resources)/.*");
        return file.endsWith("/README.md");
    }

    private static String protectedDiff(String raw) throws IOException {
        var fields = GitTestChangeSnapshot.fields(raw);
        if (fields.size() % 2 != 0) throw TestChangeProof.failure("DIFF", "truncated raw Git pair");
        var result = new StringBuilder();
        for (int i = 0; i < fields.size(); i += 2) if (TestChangeReview.protectedPath(fields.get(i + 1)))
            result.append(fields.get(i)).append('\0').append(fields.get(i + 1)).append('\0');
        return result.toString();
    }

    private static String hash(String value) { return TestChangeProof.digest(value.getBytes(StandardCharsets.UTF_8)); }
}
