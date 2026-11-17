package payroll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Scanner;

/** Reads validated user input; re-prompts until the value is acceptable. */
public class Console {
    private final Scanner in;

    public Console(Scanner in) {
        this.in = in;
    }

    public String line(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {
            throw new IllegalStateException("End of input");
        }
        return in.nextLine().trim();
    }

    public int integer(String prompt, int min, int max) {
        while (true) {
            String s = line(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) {
                    return v;
                }
            } catch (NumberFormatException ignored) {
                // fall through to message
            }
            System.out.printf("  Enter a whole number between %d and %d.%n", min, max);
        }
    }

    public double salary(String prompt) {
        while (true) {
            try {
                return Validator.salary(Double.parseDouble(line(prompt)));
            } catch (NumberFormatException e) {
                System.out.println("  Enter a valid number.");
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    public String name(String prompt) {
        while (true) {
            try {
                return Validator.name(line(prompt));
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    public String label(String prompt, String what) {
        while (true) {
            try {
                return Validator.label(line(prompt), what);
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    public LocalDate date(String prompt) {
        while (true) {
            try {
                return Validator.date(line(prompt));
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    public YearMonth month(String prompt) {
        while (true) {
            try {
                return Validator.month(line(prompt));
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }
}
