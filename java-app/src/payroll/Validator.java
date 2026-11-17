package payroll;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Input validation helpers. All methods throw IllegalArgumentException with a friendly message. */
public final class Validator {

    private Validator() {
    }

    public static String name(String text) {
        String t = text == null ? "" : text.trim();
        if (!t.matches("[A-Za-z][A-Za-z .'-]{1,59}")) {
            throw new IllegalArgumentException("Name must be 2-60 letters (spaces, . ' - allowed)");
        }
        return t;
    }

    public static String label(String text, String what) {
        String t = text == null ? "" : text.trim();
        if (t.length() < 2 || t.length() > 40) {
            throw new IllegalArgumentException(what + " must be 2-40 characters");
        }
        return t;
    }

    public static double salary(double value) {
        if (Double.isNaN(value) || value <= 0 || value > 1_000_000) {
            throw new IllegalArgumentException("Salary must be between 1 and 1,000,000");
        }
        return value;
    }

    public static LocalDate date(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid date '" + text + "'. Use yyyy-MM-dd");
        }
    }

    public static YearMonth month(String text) {
        try {
            return YearMonth.parse(text.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid month '" + text + "'. Use yyyy-MM");
        }
    }

    public static LocalTime time(String text) {
        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid time '" + text + "'. Use HH:mm");
        }
    }

    /** Hours between two HH:mm values; check-out must be after check-in. */
    public static double hoursBetween(String in, String out) {
        LocalTime a = time(in);
        LocalTime b = time(out);
        if (!b.isAfter(a)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
        long minutes = java.time.Duration.between(a, b).toMinutes();
        return Math.round(minutes / 60.0 * 100.0) / 100.0;
    }
}
