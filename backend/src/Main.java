import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        seedCars();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/cars", new CarHandler());
        server.createContext("/api/bookings", new BookingHandler());
        server.createContext("/api/profile", new ProfileHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("DriveEase backend running at http://localhost:8080");
        System.out.println("Press Ctrl+C to stop.");
    }

    static void cors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    static String read(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(data);
        }
    }

    // Small JSON parser for simple flat frontend request bodies.
    static Map<String,String> parseJson(String json) {
        Map<String,String> map = new HashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length()-1);

        boolean inQuotes = false;
        StringBuilder token = new StringBuilder();
        List<String> parts = new ArrayList<>();
        for (int i=0;i<json.length();i++) {
            char ch=json.charAt(i);
            if (ch=='"' && (i==0 || json.charAt(i-1)!='\\')) inQuotes=!inQuotes;
            if (ch==',' && !inQuotes) {
                parts.add(token.toString());
                token.setLength(0);
            } else token.append(ch);
        }
        if (token.length()>0) parts.add(token.toString());

        for (String part: parts) {
            int colon=part.indexOf(':');
            if (colon<0) continue;
            String key=clean(part.substring(0,colon));
            String value=clean(part.substring(colon+1));
            map.put(key,value);
        }
        return map;
    }

    static String clean(String s) {
        s=s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length()>=2)
            s=s.substring(1,s.length()-1);
        return s.replace("\\\"", "\"").replace("\\\\","\\");
    }

    private static void seedCars() throws SQLException {
        try (Connection c=DBConnection.getConnection();
             Statement s=c.createStatement();
             ResultSet r=s.executeQuery("SELECT COUNT(*) FROM cars")) {
            r.next();
            if (r.getInt(1)>0) return;
        }

        String sql="INSERT INTO cars(name,brand,type,price_per_day,seats,transmission,available) VALUES(?,?,?,?,?,?,TRUE)";
        try (Connection c=DBConnection.getConnection();
             PreparedStatement p=c.prepareStatement(sql)) {
            addCar(p,"Swift","Maruti Suzuki","Hatchback",2499,5,"Manual");
            addCar(p,"City","Honda","Sedan",3299,5,"Automatic");
            addCar(p,"Creta","Hyundai","SUV",4499,5,"Automatic");
            addCar(p,"XUV700","Mahindra","SUV",4999,7,"Automatic");
        }
    }

    private static void addCar(PreparedStatement p,String n,String b,String t,double price,int seats,String trans)
            throws SQLException {
        p.setString(1,n); p.setString(2,b); p.setString(3,t);
        p.setDouble(4,price); p.setInt(5,seats); p.setString(6,trans);
        p.executeUpdate();
    }
}
