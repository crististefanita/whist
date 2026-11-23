package ro.cardtable.testing.browser;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Snapshot of a delivered flat Pages site; reused by browser admission and controlled HTTP tests. */
public final class StaticSiteHost implements AutoCloseable {
    private static final String PREFIX = "/whist/";
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8", "mjs", "text/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8", "woff2", "font/woff2", "ttf", "font/ttf",
            "png", "image/png", "txt", "text/plain; charset=utf-8");
    private static final Pattern HTML = Pattern.compile("\\b(?:src|href)\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern CSS = Pattern.compile("url\\(\\s*[\"']?([^)'\"\\s]+)");
    private static final Pattern MODULE = Pattern.compile(
            "(?:\\bfrom\\s+|\\bimport\\s*\\(\\s*|\\bimport\\s+|\\bnew\\s+URL\\s*\\(\\s*)[\"'](\\.{1,2}/[^\"']+\\.mjs(?:\\?[^\"']*)?)[\"']");
    private record Asset(String type, byte[] bytes) { }
    private final HttpServer server;

    private StaticSiteHost(Map<String, Asset> assets) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(PREFIX, exchange -> {
            try (exchange) {
                String name = exchange.getRequestURI().getRawPath().substring(PREFIX.length());
                if (name.isEmpty()) name = "index.html";
                Asset asset = assets.get(name);
                if (!exchange.getRequestMethod().equals("GET") || asset == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                exchange.getResponseHeaders().set("Content-Type", asset.type());
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                exchange.sendResponseHeaders(200, asset.bytes().length);
                exchange.getResponseBody().write(asset.bytes());
            }
        });
        server.start();
    }

    public static StaticSiteHost open(Path site) throws IOException {
        Path root = site.toRealPath();
        var assets = new LinkedHashMap<String, Asset>();
        try (var entries = Files.list(root)) {
            for (Path file : entries.sorted().toList()) {
                String name = file.getFileName().toString();
                int dot = name.lastIndexOf('.');
                String type = dot < 0 ? null : TYPES.get(name.substring(dot + 1));
                if (type == null || !name.matches("[A-Za-z0-9_.-]+")
                        || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                assets.put(name, new Asset(type, Files.readAllBytes(file)));
            }
        }
        if (!assets.containsKey("index.html")) throw new IOException("Missing site asset: index.html");
        for (var entry : assets.entrySet()) {
            Pattern references = entry.getKey().endsWith(".html") ? HTML
                    : entry.getKey().endsWith(".css") ? CSS : entry.getKey().endsWith(".mjs") ? MODULE : null;
            if (references == null) continue;
            var matcher = references.matcher(new String(entry.getValue().bytes(), StandardCharsets.UTF_8));
            while (matcher.find()) {
                String reference = matcher.group(1);
                if (reference.startsWith("#") || reference.startsWith("data:")) continue;
                String name = reference.split("[?#]", 2)[0];
                if (name.startsWith("./")) name = name.substring(2);
                if (!name.matches("[A-Za-z0-9_.-]+") || !assets.containsKey(name))
                    throw new IOException("Missing or indirect site asset: " + reference + " from " + entry.getKey());
            }
        }
        return new StaticSiteHost(Map.copyOf(assets));
    }

    public String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + PREFIX; }
    @Override public void close() { server.stop(0); }

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !args[1].equals("127.0.0.1") || !args[2].equals("0"))
            throw new IllegalArgumentException("Expected site directory, loopback address and ephemeral port");
        var host = open(Path.of(args[0]));
        Runtime.getRuntime().addShutdownHook(new Thread(host::close));
        System.out.println(host.url());
    }
}
