package ro.cardtable.testing.browser;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import ro.cardtable.testing.browser.BrowserCheckResults.Image;
import ro.cardtable.testing.browser.BrowserCheckResults.Result;

/** Owns Playwright, the browser and each context; scenario code supplies only observations. */
public final class BrowserAcceptanceSession implements AutoCloseable {
    @FunctionalInterface public interface Capture { Image take(Page page, Path output, String id, String title); }
    private final Playwright playwright;
    private final Browser browser;
    private final Capture capture;
    private BrowserAcceptanceSession(Playwright playwright, Browser browser, Capture capture) {
        this.playwright=playwright; this.browser=browser; this.capture=capture;
    }
    public static BrowserAcceptanceSession open(Path executable) {
        return open(Playwright::create, executable, BrowserCheckSupport::screenshot);
    }
    public static BrowserAcceptanceSession open(Supplier<Playwright> create, Path executable, Capture capture) {
        Playwright playwright = create.get();
        try {
            var options = new BrowserType.LaunchOptions().setHeadless(true).setArgs(List.of("--mute-audio"));
            if (executable != null) options.setExecutablePath(executable);
            return new BrowserAcceptanceSession(playwright, playwright.chromium().launch(options), capture);
        } catch (RuntimeException | Error original) {
            try { playwright.close(); } catch (RuntimeException | Error cleanup) { original.addSuppressed(cleanup); }
            throw original;
        }
    }

    public Result run(WhistBrowserChecks.Check check) {
        BrowserContext context = null; Page page = null;
        var errors = new ArrayList<String>(); var images = new ArrayList<Image>();
        Map<String,Object> observed = null; Throwable failure = null;
        String version = null, visibleText = "";
        try {
            context = browser.newContext(check.options()); page = context.newPage();
            page.setDefaultTimeout(12_000); page.onPageError(errors::add);
            observed = java.util.Objects.requireNonNull(check.scenario().run(page, images), "Scenario observations");
            BrowserCheckSupport.require(errors.isEmpty(), "JavaScript errors: " + errors);
            version = browser.version();
            images.add(capture.take(page,check.output(),check.id(),check.name()));
        } catch (RuntimeException | AssertionError original) {
            failure = original;
            if (page != null) {
                try { images.add(capture.take(page,check.output(),check.id()+"-failure",check.name())); }
                catch (RuntimeException | AssertionError secondary) { original.addSuppressed(secondary); }
                try { visibleText = page.locator("body").innerText(); }
                catch (RuntimeException | AssertionError secondary) { original.addSuppressed(secondary); }
            }
        } finally {
            if (context != null) try { context.close(); }
            catch (RuntimeException | Error cleanup) { if (failure == null) failure=cleanup; else failure.addSuppressed(cleanup); }
        }
        if (failure == null) {
            System.out.println("PASS " + check.name());
            return new Result(check.id(),check.name(),"PASS",check.expected(),Map.of("browser",version,"observations",observed),images);
        }
        var diagnostic = new java.io.StringWriter();
        failure.printStackTrace(new java.io.PrintWriter(diagnostic));
        diagnostic.append("\nPage errors: ").append(errors.toString());
        diagnostic.append("\nVisible page text: ").append(visibleText);
        System.out.println("FAIL " + check.name());
        return new Result(check.id(),check.name(),"FAIL",check.expected(),diagnostic.toString(),images);
    }

    @Override public void close() {
        Throwable failure = null;
        try { browser.close(); } catch (RuntimeException | Error original) { failure=original; throw original; }
        finally {
            try { playwright.close(); }
            catch (RuntimeException | Error cleanup) { if (failure != null) failure.addSuppressed(cleanup); else throw cleanup; }
        }
    }
}
