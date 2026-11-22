package atf.tests.testchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Runs the suite's canonical diff/proof checks without preparing a Maven reactor. */
public final class TestChangeAdmissionCli {
    private TestChangeAdmissionCli() { }

    public static void main(String[] arguments) throws Exception {
        var json = new ObjectMapper();
        String[] args = arguments.length == 1 && arguments[0].equals("--stdin")
                ? json.readValue(System.in, String[].class) : arguments;
        System.exit(run(args, System.getenv(), LocalDate.now(ZoneOffset.UTC),
                new PrintStream(System.out, true, StandardCharsets.UTF_8)));
    }

    public static int run(String[] args, Map<String, String> environment, LocalDate today, PrintStream output) {
        try {
            Request request = parse(args);
            TestChangeReview.Result result;
            List<String> controls = List.of();
            if (request.generate()) {
                Path root = request.root().toRealPath();
                var snapshot = GitTestChangeSnapshot.read(root, request.base(), true);
                controls = TestChangeReview.requiredProofs(root, request.base(), snapshot.head(), today);
                if (!request.control().isEmpty() && !controls.contains(request.control()))
                    throw TestChangeProof.failure("CONTROL", request.control() + " must authorize this diff as VERIFIED");
                result = TestChangeReview.inspect(root, request.base(), Map.of("CI", "true"), today);
            } else {
                result = TestChangeReview.verify(request.root(), request.base(), environment, today);
            }
            boolean passed = result.diagnostics().isEmpty();
            emit(output, new Response(1, "ATF_TEST_CHANGE_ADMISSION", passed ? "PASS" : "BLOCKED",
                    request.generate() ? "GENERATE" : "VERIFY", result.base(), result.protectedPaths(),
                    controls, result.diagnostics()));
            return passed ? 0 : 2;
        } catch (Exception invalid) {
            if (invalid instanceof InterruptedException) Thread.currentThread().interrupt();
            emit(output, new Response(1, "ATF_TEST_CHANGE_ADMISSION", "BLOCKED", "REJECTED", "", 0,
                    List.of(), List.of(invalid.getMessage() == null ? invalid.getClass().getSimpleName() : invalid.getMessage())));
            return 2;
        }
    }

    private static void emit(PrintStream output, Response response) {
        try { output.println(new ObjectMapper().writeValueAsString(response)); }
        catch (java.io.IOException failed) { throw new IllegalStateException("TEST-CHANGE-ADMISSION-JSON", failed); }
    }

    private static Request parse(String[] args) {
        if (args == null) throw new IllegalArgumentException("TEST-CHANGE-ADMISSION-ARGUMENTS: missing arguments");
        Path root = null;
        String base = "", control = "";
        boolean generate = false;
        var seen = new ArrayList<String>();
        for (int at = 0; at < args.length; at++) {
            String option = args[at];
            if (option == null || seen.contains(option)) throw new IllegalArgumentException("TEST-CHANGE-ADMISSION-ARGUMENTS");
            seen.add(option);
            if (option.equals("--generate")) { generate = true; continue; }
            if (++at >= args.length || args[at] == null || args[at].isBlank())
                throw new IllegalArgumentException("TEST-CHANGE-ADMISSION-ARGUMENTS: missing " + option);
            switch (option) {
                case "--root" -> root = Path.of(args[at]);
                case "--base" -> base = args[at];
                case "--control" -> control = args[at];
                default -> throw new IllegalArgumentException("TEST-CHANGE-ADMISSION-ARGUMENTS: unknown " + option);
            }
        }
        if (root == null || generate && base.isBlank() || !control.isEmpty()
                && (!generate || !control.matches("[A-Z][A-Z0-9-]{3,79}")))
            throw new IllegalArgumentException("TEST-CHANGE-ADMISSION-ARGUMENTS: expected root and valid generation selection");
        return new Request(root, base, generate, control);
    }

    private record Request(Path root, String base, boolean generate, String control) { }
    private record Response(int schemaVersion, String kind, String status, String mode, String base,
                            int protectedPaths, List<String> controls, List<String> diagnostics) { }
}
