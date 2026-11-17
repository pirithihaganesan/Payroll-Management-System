package payroll;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Dependency-free unit tests for the business logic (no database needed).
 * Expected figures for TC-U07..U10 are taken from payroll rows in seed_data.sql.
 * Run:  java -cp out payroll.TestRunner
 */
public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // ---- working days
        check("U01 working days Aug 2026 = 21", PayrollCalculator.workingDays(YearMonth.of(2026, 8)), 21);
        check("U02 working days Sep 2026 = 22", PayrollCalculator.workingDays(YearMonth.of(2026, 9)), 22);
        check("U03 working days Feb 2026 = 20", PayrollCalculator.workingDays(YearMonth.of(2026, 2)), 20);
        check("U04 working days between Fri-Mon = 2",
                PayrollCalculator.workingDaysBetween(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 12)), 2);
        check("U05 Saturday is not a working day", PayrollCalculator.isWorkingDay(LocalDate.of(2026, 10, 10)), false);

        // ---- salary: hand-computed full attendance, no professional tax (gross 13,000 <= 15,000)
        PayrollRecord full = PayrollCalculator.calculate(1, "T", 10000, YearMonth.of(2026, 9), 22, 0, 0);
        check("U06 full attendance net (hand calc 11,800)", full.getNet(), 11800.00);
        check("U06b no prof tax at gross 13,000", full.getProfTax(), 0.0);

        // ---- salary: vectors from the seeded database
        PayrollRecord p1 = PayrollCalculator.calculate(1, "T", 20500, YearMonth.of(2026, 9), 21, 0, 0);
        check("U07 emp1 Sep LOP deduction", p1.getLopDeduction(), 1211.36);
        check("U07b emp1 Sep net", p1.getNet(), 22778.64);

        PayrollRecord p34 = PayrollCalculator.calculate(34, "T", 12000, YearMonth.of(2026, 10), 17, 2, 0);
        check("U08 emp34 Oct payable days (half-days)", p34.getPayableDays(), 18.0);
        check("U08b emp34 Oct LOP days", p34.getLopDays(), 4.0);
        check("U08c emp34 Oct net", p34.getNet(), 11123.64);

        PayrollRecord p7 = PayrollCalculator.calculate(7, "T", 52000, YearMonth.of(2026, 8), 16, 1, 2);
        check("U09 emp7 Aug payable (paid leave counted)", p7.getPayableDays(), 18.5);
        check("U09b emp7 Aug net", p7.getNet(), 53112.38);

        PayrollRecord p14 = PayrollCalculator.calculate(14, "T", 23000, YearMonth.of(2026, 9), 14, 1, 0);
        check("U10 emp14 Sep net (heavy absenteeism)", p14.getNet(), 16746.82);

        // ---- edge cases
        PayrollRecord capped = PayrollCalculator.calculate(1, "T", 10000, YearMonth.of(2026, 9), 30, 0, 0);
        check("U11 payable days capped at working days", capped.getPayableDays(), 22.0);
        check("U12 zero basic rejected", throwsIllegal(() ->
                PayrollCalculator.calculate(1, "T", 0, YearMonth.of(2026, 9), 1, 0, 0)), true);
        PayrollRecord zero = PayrollCalculator.calculate(1, "T", 10000, YearMonth.of(2026, 9), 0, 0, 0);
        check("U13 no attendance -> net floored at 0", zero.getNet(), 0.0);

        // ---- validator
        check("U14 valid name accepted", Validator.name("  Asha Menon ").equals("Asha Menon"), true);
        check("U15 name with digits rejected", throwsIllegal(() -> Validator.name("Asha123")), true);
        check("U16 negative salary rejected", throwsIllegal(() -> Validator.salary(-5)), true);
        check("U17 bad date rejected", throwsIllegal(() -> Validator.date("31-12-2026")), true);
        check("U18 bad month rejected", throwsIllegal(() -> Validator.month("2026-13")), true);
        check("U19 hours between 09:00 and 17:30", Validator.hoursBetween("09:00", "17:30"), 8.5);
        check("U20 checkout before checkin rejected",
                throwsIllegal(() -> Validator.hoursBetween("17:00", "09:00")), true);
        check("U21 enum lookup", AttendanceStatus.fromLabel("half-day"), AttendanceStatus.HALF_DAY);
        check("U22 LOP leave is unpaid", LeaveType.LOSS_OF_PAY.isPaid(), false);

        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean throwsIllegal(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static void check(String name, Object actual, Object expected) {
        boolean ok = (actual instanceof Double && expected instanceof Double)
                ? Math.abs((Double) actual - (Double) expected) < 0.005
                : actual.equals(expected);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.printf("%-6s %s  (expected=%s, actual=%s)%n", ok ? "PASS" : "FAIL", name, expected, actual);
    }
}
