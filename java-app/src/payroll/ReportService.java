package payroll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

/** Console reports (all driven by SQL aggregate queries). */
public class ReportService {
    private final Connection con;
    private final PayrollDAO payrollDAO;

    public ReportService(Connection con, PayrollDAO payrollDAO) {
        this.con = con;
        this.payrollDAO = payrollDAO;
    }

    /** Employee-wise attendance summary for a month. */
    public void attendanceSummary(YearMonth ym) throws SQLException {
        String sql = "SELECT e.emp_id, e.emp_name, "
                + "SUM(a.status='Present'), SUM(a.status='Half-Day'), SUM(a.status='Absent'), "
                + "SUM(a.status='Paid Leave'), SUM(a.status='Unpaid Leave'), ROUND(SUM(a.working_hours),1) "
                + "FROM employee e JOIN attendance a ON a.emp_id = e.emp_id "
                + "WHERE strftime('%Y-%m', a.att_date) = ? GROUP BY e.emp_id, e.emp_name ORDER BY e.emp_id";
        System.out.println("\nEMPLOYEE ATTENDANCE SUMMARY - " + ym);
        System.out.printf("%-4s %-20s %7s %5s %6s %6s %7s %7s%n",
                "ID", "Name", "Present", "Half", "Absent", "PdLeav", "UnLeave", "Hours");
        boolean any = false;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ym.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-4d %-20s %7d %5d %6d %6d %7d %7.1f%n", rs.getInt(1), rs.getString(2),
                            rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getDouble(8));
                }
            }
        }
        if (!any) {
            System.out.println("(no attendance recorded for this month)");
        }
    }

    /** Department-wise attendance percentages for a month. */
    public void departmentAttendance(YearMonth ym) throws SQLException {
        String sql = "SELECT d.dept_name, COUNT(*), "
                + "ROUND(100.0*SUM(a.status='Present')/COUNT(*),2), ROUND(100.0*SUM(a.status='Absent')/COUNT(*),2) "
                + "FROM attendance a JOIN employee e ON e.emp_id=a.emp_id JOIN department d ON d.dept_id=e.dept_id "
                + "WHERE strftime('%Y-%m', a.att_date) = ? GROUP BY d.dept_name ORDER BY 4 DESC";
        System.out.println("\nDEPARTMENT-WISE ATTENDANCE - " + ym);
        System.out.printf("%-18s %8s %10s %10s%n", "Department", "Records", "Present %", "Absent %");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ym.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%-18s %8d %10.2f %10.2f%n", rs.getString(1), rs.getInt(2),
                            rs.getDouble(3), rs.getDouble(4));
                }
            }
        }
    }

    /** Monthly payroll report. */
    public void payrollReport(YearMonth ym) throws SQLException {
        List<PayrollRecord> list = payrollDAO.listMonth(ym);
        System.out.println("\nMONTHLY PAYROLL REPORT - " + ym);
        if (list.isEmpty()) {
            System.out.println("(no payroll generated for this month)");
            return;
        }
        System.out.printf("%-4s %-20s %5s %7s %5s %11s %11s %11s%n",
                "ID", "Name", "WDays", "Payable", "LOP", "Gross", "Deduct", "Net");
        double g = 0, d = 0, n = 0;
        for (PayrollRecord p : list) {
            System.out.printf("%-4d %-20s %5d %7.1f %5.1f %11.2f %11.2f %11.2f%n", p.getEmpId(), p.getEmpName(),
                    p.getWorkingDays(), p.getPayableDays(), p.getLopDays(), p.getGross(),
                    p.getTotalDeductions(), p.getNet());
            g += p.getGross();
            d += p.getTotalDeductions();
            n += p.getNet();
        }
        System.out.printf("%-36s %11.2f %11.2f %11.2f%n", "TOTAL", g, d, n);
    }

    /** Department-wise payroll cost. */
    public void departmentPayroll(YearMonth ym) throws SQLException {
        String sql = "SELECT d.dept_name, COUNT(*), ROUND(SUM(p.gross_salary),2), ROUND(SUM(p.net_salary),2), "
                + "ROUND(AVG(p.net_salary),2) FROM payroll p JOIN employee e ON e.emp_id=p.emp_id "
                + "JOIN department d ON d.dept_id=e.dept_id WHERE p.pay_year=? AND p.pay_month=? "
                + "GROUP BY d.dept_name ORDER BY 4 DESC";
        System.out.println("\nDEPARTMENT-WISE PAYROLL - " + ym);
        System.out.printf("%-18s %5s %13s %13s %11s%n", "Department", "Emps", "Total Gross", "Total Net", "Avg Net");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ym.getYear());
            ps.setInt(2, ym.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%-18s %5d %13.2f %13.2f %11.2f%n", rs.getString(1), rs.getInt(2),
                            rs.getDouble(3), rs.getDouble(4), rs.getDouble(5));
                }
            }
        }
    }

    /** Employees whose total absent days reach the threshold (absenteeism pattern). */
    public void frequentAbsentees(int threshold) throws SQLException {
        String sql = "SELECT e.emp_id, e.emp_name, d.dept_name, COUNT(*) FROM attendance a "
                + "JOIN employee e ON e.emp_id=a.emp_id JOIN department d ON d.dept_id=e.dept_id "
                + "WHERE a.status='Absent' GROUP BY e.emp_id, e.emp_name, d.dept_name "
                + "HAVING COUNT(*) >= ? ORDER BY 4 DESC";
        System.out.println("\nFREQUENT ABSENTEES (absent days >= " + threshold + ")");
        System.out.printf("%-4s %-20s %-16s %6s%n", "ID", "Name", "Department", "Absent");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-4d %-20s %-16s %6d%n", rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getInt(4));
                }
                if (!any) {
                    System.out.println("(none)");
                }
            }
        }
    }

    /** Approved leave days per employee and leave type. */
    public void leaveSummary() throws SQLException {
        String sql = "SELECT e.emp_id, e.emp_name, l.leave_type, SUM(l.days) FROM leave_record l "
                + "JOIN employee e ON e.emp_id=l.emp_id WHERE l.status='Approved' "
                + "GROUP BY e.emp_id, e.emp_name, l.leave_type ORDER BY e.emp_id, l.leave_type";
        System.out.println("\nLEAVE SUMMARY (approved)");
        System.out.printf("%-4s %-20s %-12s %5s%n", "ID", "Name", "Leave type", "Days");
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("%-4d %-20s %-12s %5d%n", rs.getInt(1), rs.getString(2),
                        rs.getString(3), rs.getInt(4));
            }
        }
    }
}
