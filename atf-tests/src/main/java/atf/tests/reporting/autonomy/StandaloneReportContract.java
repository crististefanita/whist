package atf.tests.reporting.autonomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/** Handoff admission after producer-specific qualification; never reconstructs an original verdict. */
public final class StandaloneReportContract {
    private static final Pattern CSS_URL = Pattern.compile("(?is)url\\(\\s*['\"]?([^'\"\\s)]+)");
    private StandaloneReportContract() { }
    public record Verified(String sha256, long bytes, int inlineReferences) { }

    public static Verified verify(Path report, String originalSha256) throws IOException {
        if (originalSha256 == null || !originalSha256.matches("[a-f0-9]{64}")) throw new IOException("REPORT_ORIGINAL_SEAL_REQUIRED");
        if (!Files.isRegularFile(report) || Files.isSymbolicLink(report)) throw new IOException("REPORT_DETACHED_FILE_REQUIRED");
        byte[] bytes = Files.readAllBytes(report);
        final String actual;
        try { actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        if (!actual.equals(originalSha256)) throw new IOException("REPORT_BYTES_CHANGED");
        var document = Jsoup.parse(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        verifyReferences(document);
        return new Verified(actual, bytes.length, document.select("a[data-atf-screenshot-ref],a[data-atf-screenshot=base64]").size());
    }

    public static void verifyReferences(Document document) throws IOException {
        for (var node : document.select("script[src],link[rel=stylesheet][href],img[src],source[src],iframe[src],object[data],video[src],audio[src]")) {
            String attribute = node.hasAttr("src") ? "src" : node.hasAttr("data") ? "data" : "href";
            requireLocal(node.attr(attribute), "REPORT_EXTERNAL_RESOURCE");
        }
        if (!document.select("[srcset],iframe[srcdoc],base[href],meta[http-equiv=refresh]").isEmpty())
            throw new IOException("REPORT_INDIRECT_RESOURCE");
        for (var node : document.select("style,[style]")) {
            String css = node.normalName().equals("style") ? node.data() : node.attr("style");
            if (Pattern.compile("(?i)@import\\b").matcher(css).find()) throw new IOException("REPORT_EXTERNAL_STYLESHEET");
            var urls = CSS_URL.matcher(css);
            while (urls.find()) requireLocal(urls.group(1), "REPORT_EXTERNAL_STYLE_RESOURCE");
        }
        for (var anchor : document.select("a[href]")) {
            String href = anchor.attr("href").strip();
            if (href.isEmpty() || href.startsWith("#") || href.startsWith("data:")) continue;
            // Optional project/source hyperlinks cannot substitute an external evidence attachment.
            if (!href.matches("(?i)https?://.*") || href.matches("(?is).*\\.(?:xml|json|png|jpe?g|log|properties|txt|zip)(?:[?#].*)?$"))
                throw new IOException("REPORT_EXTERNAL_EVIDENCE_LINK: " + href);
        }
        for (var anchor : document.select("a[data-atf-screenshot-ref]")) {
            String id = anchor.attr("data-atf-screenshot-ref");
            var asset = document.getElementById("atf-screenshot-" + id);
            if (asset == null || !asset.attr("href").startsWith("data:image/png;base64,"))
                throw new IOException("REPORT_MISSING_INLINE_CAPTURE: " + id);
        }
    }
    private static void requireLocal(String value, String diagnostic) throws IOException {
        if (!value.strip().startsWith("data:")) throw new IOException(diagnostic + ": " + value);
    }

    /** Browser review reads the already-qualified original DOM before executing any report scripts. */
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) throw new IllegalArgumentException("Use <detached-html> <qualified-original-sha256>");
        Path file = Path.of(arguments[0]); var verified = verify(file, arguments[1]);
        var doc = Jsoup.parse(file.toFile(), "UTF-8");
        var owner = doc.select("li.test-item[test-id]").last();
        if (owner == null) throw new IOException("REPORT_EXTENT_OWNER_REQUIRED");
        var row = owner.select("table.helm-validation-table tbody tr").last();
        var pre = owner.select("pre").last();
        if (row == null || pre == null) throw new IOException("REPORT_ORIGINAL_COMPARISON_REQUIRED");
        String body = pre.wholeText();
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("sha256", verified.sha256()); result.put("bytes", verified.bytes()); result.put("renderer", "extent");
        result.put("ownerId", owner.attr("test-id"));
        result.put("ownerName", owner.selectFirst(".name").text());
        result.put("lastCaption", row.child(0).text()); result.put("bodyTail", body.substring(Math.max(0, body.length()-240)));
        result.put("inlineReferences", verified.inlineReferences());
        System.out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
    }
}
