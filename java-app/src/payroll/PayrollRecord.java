package payroll;

import java.time.YearMonth;

/** A monthly payslip (stored as a snapshot so later salary changes do not alter history). */
public class PayrollRecord {
    private int empId;
    private String empName;
    private final YearMonth period;
    private final int workingDays;
    private final double payableDays;
    private final double lopDays;
    private final double basic;
    private final double hra;
    private final double da;
    private final double gross;
    private final double pf;
    private final double profTax;
    private final double lopDeduction;
    private final double totalDeductions;
    private final double net;

    public PayrollRecord(int empId, String empName, YearMonth period, int workingDays,
                         double payableDays, double lopDays, double basic, double hra, double da,
                         double gross, double pf, double profTax, double lopDeduction,
                         double totalDeductions, double net) {
        this.empId = empId;
        this.empName = empName;
        this.period = period;
        this.workingDays = workingDays;
        this.payableDays = payableDays;
        this.lopDays = lopDays;
        this.basic = basic;
        this.hra = hra;
        this.da = da;
        this.gross = gross;
        this.pf = pf;
        this.profTax = profTax;
        this.lopDeduction = lopDeduction;
        this.totalDeductions = totalDeductions;
        this.net = net;
    }

    public int getEmpId() { return empId; }
    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }
    public YearMonth getPeriod() { return period; }
    public int getWorkingDays() { return workingDays; }
    public double getPayableDays() { return payableDays; }
    public double getLopDays() { return lopDays; }
    public double getBasic() { return basic; }
    public double getHra() { return hra; }
    public double getDa() { return da; }
    public double getGross() { return gross; }
    public double getPf() { return pf; }
    public double getProfTax() { return profTax; }
    public double getLopDeduction() { return lopDeduction; }
    public double getTotalDeductions() { return totalDeductions; }
    public double getNet() { return net; }

    /** Printable payslip. */
    public String toPayslip() {
        StringBuilder sb = new StringBuilder();
        sb.append("==================== PAYSLIP ====================\n");
        sb.append(String.format("Employee : %d - %s%n", empId, empName == null ? "" : empName));
        sb.append(String.format("Period   : %s%n", period));
        sb.append(String.format("Working days %d | Payable days %.1f | LOP days %.1f%n",
                workingDays, payableDays, lopDays));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Basic                      %12.2f%n", basic));
        sb.append(String.format("HRA (20%%)                  %12.2f%n", hra));
        sb.append(String.format("DA (10%%)                   %12.2f%n", da));
        sb.append(String.format("GROSS                      %12.2f%n", gross));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("PF (12%% of basic)          %12.2f%n", pf));
        sb.append(String.format("Professional tax           %12.2f%n", profTax));
        sb.append(String.format("Loss-of-pay deduction      %12.2f%n", lopDeduction));
        sb.append(String.format("TOTAL DEDUCTIONS           %12.2f%n", totalDeductions));
        sb.append("==================================================\n");
        sb.append(String.format("NET SALARY                 %12.2f%n", net));
        sb.append("==================================================\n");
        return sb.toString();
    }
}
