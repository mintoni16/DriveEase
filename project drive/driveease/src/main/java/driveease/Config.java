package driveease;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/** Settings from config.properties; environment variables (DRIVEEASE_DB_PASSWORD, ...) override them. */
final class Config {
    static String dbUrl, dbUser, dbPassword;
    static int port;
    static Path frontendDir, schemaFile;
    static boolean production;

    private Config() {}

    static void load() throws IOException {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            p.load(in);
        } catch (java.io.FileNotFoundException e) {
            System.out.println("config.properties not found - using defaults.");
        }
        dbUrl = pick(p, "db.url", "DRIVEEASE_DB_URL",
                "jdbc:mysql://localhost:3306/driveease?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
        dbUser = pick(p, "db.user", "DRIVEEASE_DB_USER", "root");
        dbPassword = pick(p, "db.password", "DRIVEEASE_DB_PASSWORD", "");
        port = Integer.parseInt(pick(p, "port", "PORT", "5000").trim());
        frontendDir = Path.of(pick(p, "frontend.dir", "DRIVEEASE_FRONTEND", "frontend")).toAbsolutePath().normalize();
        schemaFile = Path.of(pick(p, "schema.file", "DRIVEEASE_SCHEMA", "database/schema.sql")).toAbsolutePath().normalize();
        production = Boolean.parseBoolean(pick(p, "production", "PRODUCTION", "false").trim())
                || "1".equals(System.getenv("PRODUCTION"));
    }

    private static String pick(Properties p, String key, String envName, String fallback) {
        String env = System.getenv(envName);
        return env != null ? env : p.getProperty(key, fallback);
    }
}
