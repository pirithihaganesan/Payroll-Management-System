package payroll;

/** Predefined salary rules used by the payroll calculation (see SRS section 3.4). */
public final class SalaryRules {
    public static final double HRA_RATE = 0.20;               // 20% of basic
    public static final double DA_RATE = 0.10;                // 10% of basic
    public static final double PF_RATE = 0.12;                // 12% of basic
    public static final double PROF_TAX = 200.0;              // flat monthly
    public static final double PROF_TAX_THRESHOLD = 15000.0;  // applies when gross > threshold
    public static final double HALF_DAY_FACTOR = 0.5;
    public static final double FULL_DAY_HOURS = 8.0;
    public static final double HALF_DAY_HOURS = 4.0;

    private SalaryRules() {
    }
}
