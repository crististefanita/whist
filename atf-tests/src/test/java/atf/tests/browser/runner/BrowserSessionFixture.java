package atf.tests.browser.runner;

import com.microsoft.playwright.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import ro.cardtable.testing.browser.BrowserAcceptanceSession;
import ro.cardtable.testing.browser.BrowserCheckResults.Image;

/** Protocol-only neighbours; no Playwright driver, browser process, screenshot or network. */
final class BrowserSessionFixture {
    final List<String> events = new ArrayList<>();
    final Map<String,RuntimeException> failures = new LinkedHashMap<>();
    BrowserType.LaunchOptions launchOptions;
    Consumer<String> pageError;
    private final Locator body = stub(Locator.class,(method,args)-> {
        event("body.text"); return "Original visible text";
    });
    private final Page page = stub(Page.class,(method,args)-> {
        return switch(method) {
            case "setDefaultTimeout" -> { event("page.timeout"); yield null; }
            case "onPageError" -> { event("page.errors"); pageError = consumer(args[0]); yield null; }
            case "locator" -> { if(!args[0].equals("body")) throw new UnsupportedOperationException("Unexpected locator"); yield body; }
            default -> throw new UnsupportedOperationException(method);
        };
    });
    private final BrowserContext context = stub(BrowserContext.class,(method,args)-> {
        return switch(method) {
            case "newPage" -> { event("context.page"); yield page; }
            case "close" -> { event("context.close"); yield null; }
            default -> throw new UnsupportedOperationException(method);
        };
    });
    private final Browser browser = stub(Browser.class,(method,args)-> {
        return switch(method) {
            case "newContext" -> { event("browser.context"); yield context; }
            case "version" -> { event("browser.version"); yield "Controlled browser"; }
            case "close" -> { event("browser.close"); yield null; }
            default -> throw new UnsupportedOperationException(method);
        };
    });
    private final BrowserType chromium = stub(BrowserType.class,(method,args)-> {
        if(!method.equals("launch")) throw new UnsupportedOperationException(method);
        launchOptions = (BrowserType.LaunchOptions)args[0]; event("browser.launch"); return browser;
    });
    final Playwright playwright = stub(Playwright.class,(method,args)-> {
        return switch(method) {
            case "chromium" -> { event("playwright.chromium"); yield chromium; }
            case "close" -> { event("playwright.close"); yield null; }
            default -> throw new UnsupportedOperationException(method);
        };
    });
    final BrowserAcceptanceSession.Capture capture = (page,output,id,title)-> {
        event("capture"); return new Image(id+".png",title);
    };
    void event(String name) {
        events.add(name); var failure=failures.get(name); if(failure!=null) throw failure;
    }
    @SuppressWarnings("unchecked") private static Consumer<String> consumer(Object value) { return (Consumer<String>)value; }
    @FunctionalInterface private interface Call { Object invoke(String method,Object[] arguments); }
    private static <T>T stub(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)-> {
            if(method.getDeclaringClass()==Object.class) return switch(method.getName()) {
                case "toString" -> "Controlled "+type.getSimpleName();
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy==args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
            return call.invoke(method.getName(),args==null?new Object[0]:args);
        }));
    }
}
