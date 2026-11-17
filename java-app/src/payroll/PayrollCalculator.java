package payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/** Pure business logic for salary calculation (no database access, fully unit-testable). */
public final class PayrollCalculator {

    private PayrollCalculator() {
    }

    /** Working days = Monday to Friday in the month. */
    public static int workingDays(YearMonth ym) {
        int count = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            if (isWorkingDay(ym.atDay(d))) {
                count++;
            }
        }
        return count;
    }

    public static boolean isWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    /** Number of working days from start to end, inclusive. */
    public static int workingDaysBetween(LocalDate start, LocalDate end) {
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isWorkingDay(d)) {
                count++;
            }
        }
        return count;
    }

    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Calculates a payslip.
     * payable days = present + 0.5 * half-days + paid-leave days (capped at working days);
     * LOP days     = working days - payable days (absent, unpaid leave and unrecorded days);
     * LOP amount   = gross / working days * LOP days.
     */
    public static PayrollRecord calculate(int empId, String empName, double basic, YearMonth ym,
                                          int presentDays, int halfDays, int paidLeaveDays) {
        if (basic <= 0) {
            throw new IllegalArgumentException("Basic salary must be positive");
        }
        int workingDays = workingDays(ym);
        double payable = presentDays + SalaryRules.HALF_DAY_FACTOR * halfDays + paidLeaveDays;
        payable = Math.min(payable, workingDays);
        double lopDays = round2(workingDays - payable);

        double hra = round2(basic * SalaryRules.HRA_RATE);
        double da = round2(basic * SalaryRules.DA_RATE);
        double gross = round2(basic + hra + da);
        double pf = round2(basic * SalaryRules.PF_RATE);
        double tax = gross > SalaryRules.PROF_TAX_THRESHOLD ? SalaryRules.PROF_TAX : 0.0;
        double lop = round2(gross / workingDays * lopDays);
        double totalDeductions = round2(pf + tax + lop);
        double net = round2(gross - totalDeductions);
        if (net < 0) {               // deductions can never exceed what is earned
            net = 0.0;
            totalDeductions = gross;
        }

        return new PayrollRecord(empId, empName, ym, workingDays, payable, lopDays, basic, hra, da,
                gross, pf, tax, lop, totalDeductions, net);
    }
}
