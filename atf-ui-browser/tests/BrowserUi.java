import com.google.gson.JsonParser;
import ro.cardtable.testing.browser.BrowserAcceptanceSession;
import ro.cardtable.testing.browser.BrowserCheckResults;
import ro.cardtable.testing.browser.StaticSiteHost;
import ro.cardtable.testing.browser.WhistBrowserChecks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Thin diagnostic entry: admit inputs, select unchanged scenarios, own resources and publish results. */
class BrowserUi {
    public static void main(String[] args) throws IOException {
        if (args.length < 3 || args.length > 4) throw new IllegalArgumentException("Use <site> <hand-fixture> <new-output> [browser]");
        Path site = Path.of(args[0]).toRealPath();
        String checkpoint = Files.readString(Path.of(args[1]));
        String wideCheckpoint = JsonParser.parseString(checkpoint).getAsJsonObject().get("wideCheckpoint").getAsString();
        Path output = Path.of(args[2]).toRealPath();
        String selection = System.getProperty("whist.browserCheck", "all");
        BrowserCheckResults results;
        try (var host = StaticSiteHost.open(site)) {
            var plan = WhistBrowserChecks.plan(selection,host.url(),checkpoint,wideCheckpoint,output);
            results = new BrowserCheckResults(output);
            try (results; var browser = BrowserAcceptanceSession.open(args.length == 4 ? Path.of(args[3]) : null)) {
                for (var check : plan) results.add(browser.run(check));
            }
        }
        if (results.failed()) System.exit(1);
    }
}
