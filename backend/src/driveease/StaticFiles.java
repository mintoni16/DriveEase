package driveease;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/** Serves the frontend folder. Some pages are private: signed-out visitors are sent to the login page. */
final class StaticFiles {
    private static final Set<String> PROTECTED_PAGES =
            Set.of("dashboard.htm", "bookings.htm", "my_bookings.htm", "profile.htm");

    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("htm", "text/html; charset=utf-8"), Map.entry("html", "text/html; charset=utf-8"),
            Map.entry("css", "text/css; charset=utf-8"), Map.entry("js", "text/javascript; charset=utf-8"),
            Map.entry("json", "application/json"), Map.entry("svg", "image/svg+xml"),
            Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp"), Map.entry("ico", "image/x-icon"),
            Map.entry("woff2", "font/woff2"), Map.entry("woff", "font/woff"), Map.entry("txt", "text/plain; charset=utf-8"));

    private StaticFiles() {}

    static void serve(Ctx c) throws IOException, SQLException {
        if (!c.method.equals("GET") && !c.method.equals("HEAD")) {
            c.addHeader("Allow", "GET, HEAD");
            c.bytes(405, "text/plain; charset=utf-8", "Method not allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }

        String rel = c.path.equals("/") ? "index.htm" : c.path.substring(1);
        for (String segment : rel.split("/")) {
            if (segment.startsWith(".")) { notFound(c); return; }   // no dotfiles, no ".."
        }

        Path file = Config.frontendDir.resolve(rel).normalize();
        if (!file.startsWith(Config.frontendDir) || !Files.isRegularFile(file)) { notFound(c); return; }

        boolean isProtected = PROTECTED_PAGES.contains(rel);
        if (isProtected && Auth.currentUser(c) == null) {
            String rawQuery = c.ex.getRequestURI().getRawQuery();
            String target = rel + (rawQuery != null ? "?" + rawQuery : "");
            c.redirect("/login.htm?next=" + URLEncoder.encode(target, StandardCharsets.UTF_8));
            return;
        }

        String name = file.getFileName().toString();
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (isProtected) c.addHeader("Cache-Control", "no-store");   // don't show private pages after logout
        c.bytes(200, TYPES.getOrDefault(ext, "application/octet-stream"), Files.readAllBytes(file));
    }

    private static void notFound(Ctx c) throws IOException {
        c.bytes(404, "text/plain; charset=utf-8", "Page not found".getBytes(StandardCharsets.UTF_8));
    }
}
