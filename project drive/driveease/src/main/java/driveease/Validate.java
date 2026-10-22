package driveease;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Input checks. Every failure becomes a 400 response with a readable message. */
final class Validate {
    static final int MIN_PASSWORD_LEN = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[+\\d][\\d\\s\\-()]{6,19}$");

    private Validate() {}

    static String cleanStr(Object value, String field, int minLen, int maxLen) {
        if (!(value instanceof String s)) throw ApiException.badRequest(field + " is required");
        s = s.strip();
        if (s.length() < minLen) throw ApiException.badRequest(field + " is required");
        if (s.length() > maxLen) throw ApiException.badRequest(field + " must be at most " + maxLen + " characters");
        return s;
    }

    static String cleanEmail(Object value) {
        String email = cleanStr(value, "Email", 1, 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) throw ApiException.badRequest("Please enter a valid email address");
        return email;
    }

    /** Phone is optional: null / blank -> null. */
    static String cleanPhone(Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) return null;
        String phone = cleanStr(value, "Phone number", 1, 20);
        if (!PHONE.matcher(phone).matches()) throw ApiException.badRequest("Please enter a valid phone number");
        return phone;
    }

    static void checkPassword(Object value) {
        if (!(value instanceof String p) || p.length() < MIN_PASSWORD_LEN)
            throw ApiException.badRequest("Password must be at least " + MIN_PASSWORD_LEN + " characters");
        if (p.length() > 128) throw ApiException.badRequest("Password is too long");
    }

    static LocalDate parseDate(Object value, String field) {
        try {
            if (value instanceof String s) return LocalDate.parse(s);
        } catch (DateTimeParseException ignored) { /* fall through */ }
        throw ApiException.badRequest(field + " must be a valid date (YYYY-MM-DD)");
    }

    /** Accepts 3, 3.0 or "3". */
    static int toInt(Object value, String message) {
        try {
            if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) return n.intValue();
            if (value instanceof String s) return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) { /* fall through */ }
        throw ApiException.badRequest(message);
    }
}
