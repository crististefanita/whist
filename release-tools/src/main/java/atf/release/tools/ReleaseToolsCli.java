package atf.release.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Map;

/** Command-line adapter for the release-tools module's read-only checks. */
public final class ReleaseToolsCli {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ReleaseToolsCli() { }

    public static void main(String[] args) {
        try {
            if (args.length == 0) throw new IllegalArgumentException("RELEASE_TOOLS_COMMAND_REQUIRED");
            switch (args[0]) {
                case "resolve-root" -> resolveRoot(args);
                case "verify-evidence" -> verifyEvidence(args);
                case "verify-rag" -> verifyRag(args);
                case "verify-signing" -> verifySigning(args);
                default -> throw new IllegalArgumentException("RELEASE_TOOLS_COMMAND_UNKNOWN");
            }
        } catch (Exception failure) {
            System.err.println(failure.getMessage());
            System.exit(1);
        }
    }

    private static void resolveRoot(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("USE_RESOLVE_ROOT_START_AND_OVERRIDE");
        Path root = ReleaseProjectRoot.resolve(Path.of(args[1]), args[2], System.getenv());
        System.out.println(JSON.writeValueAsString(Map.of("status", "PASS", "root", root.toString())));
    }

    private static void verifyEvidence(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("USE_VERIFY_EVIDENCE_CANDIDATE_AND_MANIFEST");
        var result = new ReleaseEvidenceGate().verify(Path.of(args[1]), Path.of(args[2]));
        System.out.println(JSON.writeValueAsString(Map.of("status", "PASS", "filesVerified", result.filesVerified(),
                "manifestSha256", result.manifestSha256())));
    }

    private static void verifySigning(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("USE_VERIFY_SIGNING_ROOT_AND_OBSERVED_CERTIFICATE");
        var result = new SigningIdentityGate().verify(Path.of(args[1]), args[2]);
        System.out.println(JSON.writeValueAsString(Map.of("status", "PASS", "certificateContinuity", result.certificateContinuity())));
    }

    private static void verifyRag(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("USE_VERIFY_RAG_PROJECT_CANDIDATE_SOURCE_VERDICT");
        var result = new RagAdvisoryGate().verify(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]), Path.of(args[4]));
        System.out.println(JSON.writeValueAsString(Map.of("status", "PASS", "verdict", result.verdict(),
                "releaseVersion", result.releaseVersion(), "verdictSha256", result.verdictSha256(),
                "candidateManifestSha256", result.candidateManifestSha256(), "checks", result.checks(),
                "humanReviewRequired", result.humanReviewRequired(), "mayPromote", result.mayPromote(),
                "maySign", result.maySign(), "mayPublish", result.mayPublish())));
    }
}
