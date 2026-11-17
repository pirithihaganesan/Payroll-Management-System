package payroll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Generates and retrieves monthly payroll. */
public class PayrollDAO {
    private final Connection con;
    private final AttendanceDAO attendanceDAO;
    private final EmployeeDAO employeeDAO;

    public PayrollDAO(Connection con, AttendanceDAO attendanceDAO, EmployeeDAO employeeDAO) {
        this.con = con;
        this.attendanceDAO = attendanceDAO;
        this.employeeDAO = employeeDAO;
    }

    public boolean exists(int empId, YearMonth ym) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM payroll WHERE emp_id = ? AND pay_month = ? AND pay_year = ?")) {
            ps.setInt(1, empId);
            ps.setInt(2, ym.getMonthValue());
            ps.setInt(3, ym.getYear());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Generates and stores a payslip for one employee. */
    public PayrollRecord generate(int empId, YearMonth ym) throws SQLException {
        Employee emp = employeeDAO.findById(empId);
        if (emp == null || !emp.isActive()) {
            throw new IllegalArgumentException("Employee " + empId + " not found or inactive");
        }
        if (ym.isAfter(YearMonth.now())) {
            throw new IllegalArgumentException("Cannot generate payroll for a future month");
        }
        if (ym.isBefore(YearMonth.from(emp.getJoinDate()))) {
            throw new IllegalArgumentException("Month is before the employee's joining date");
        }
        if (exists(empId, ym)) {
            throw new IllegalArgumentException("Payroll for " + ym + " already generated for employee " + empId);
        }
        if (attendanceDAO.recordsInMonth(empId, ym) == 0) {
            throw new IllegalArgumentException("No attendance recorded for " + ym + "; cannot generate payroll");
        }
        int[] c = attendanceDAO.monthCounts(empId, ym);   // present, half, absent, paid, unpaid
        PayrollRecord p = PayrollCalculator.calculate(empId, emp.getName(), emp.getBasicSalary(), ym,
                c[0], c[1], c[3]);
        save(p);
        return p;
    }

    /** Generates payroll for every active employee that has attendance and no payslip yet. */
    public int generateAll(YearMonth ym) throws SQLException {
        int created = 0;
        for (Employee e : employeeDAO.listAll(true)) {
            try {
                generate(e.getEmpId(), ym);
                created++;
            } catch (IllegalArgumentException skip) {
                System.out.printf("  skipped %d (%s): %s%n", e.getEmpId(), e.getName(), skip.getMessage());
            }
        }
        return created;
    }

    private void save(PayrollRecord p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO payroll(emp_id, pay_month, pay_year, working_days, payable_days, lop_days, basic, "
                + "hra, da, gross_salary, pf_deduction, prof_tax, lop_deduction, total_deductions, net_salary, "
                + "generated_on) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, p.getEmpId());
            ps.setInt(2, p.getPeriod().getMonthValue());
            ps.setInt(3, p.getPeriod().getYear());
            ps.setInt(4, p.getWorkingDays());
            ps.setDouble(5, p.getPayableDays());
            ps.setDouble(6, p.getLopDays());
            ps.setDouble(7, p.getBasic());
            ps.setDouble(8, p.getHra());
            ps.setDouble(9, p.getDa());
            ps.setDouble(10, p.getGross());
            ps.setDouble(11, p.getPf());
            ps.setDouble(12, p.getProfTax());
            ps.setDouble(13, p.getLopDeduction());
            ps.setDouble(14, p.getTotalDeductions());
            ps.setDouble(15, p.getNet());
            ps.setString(16, LocalDate.now().toString());
            ps.executeUpdate();
        }
    }

    public PayrollRecord find(int empId, YearMonth ym) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT p.emp_id, e.emp_name, p.working_days, p.payable_days, p.lop_days, p.basic, p.hra, p.da, "
                + "p.gross_salary, p.pf_deduction, p.prof_tax, p.lop_deduction, p.total_deductions, p.net_salary "
                + "FROM payroll p JOIN employee e ON e.emp_id = p.emp_id "
                + "WHERE p.emp_id = ? AND p.pay_month = ? AND p.pay_year = ?")) {
            ps.setInt(1, empId);
            ps.setInt(2, ym.getMonthValue());
            ps.setInt(3, ym.getYear());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs, ym) : null;
            }
        }
    }

    public List<PayrollRecord> listMonth(YearMonth ym) throws SQLException {
        List<PayrollRecord> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT p.emp_id, e.emp_name, p.working_days, p.payable_days, p.lop_days, p.basic, p.hra, p.da, "
                + "p.gross_salary, p.pf_deduction, p.prof_tax, p.lop_deduction, p.total_deductions, p.net_salary "
                + "FROM payroll p JOIN employee e ON e.emp_id = p.emp_id "
                + "WHERE p.pay_month = ? AND p.pay_year = ? ORDER BY p.emp_id")) {
            ps.setInt(1, ym.getMonthValue());
            ps.setInt(2, ym.getYear());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs, ym));
                }
            }
        }
        return out;
    }

    private PayrollRecord map(ResultSet rs, YearMonth ym) throws SQLException {
        return new PayrollRecord(rs.getInt(1), rs.getString(2), ym, rs.getInt(3), rs.getDouble(4),
                rs.getDouble(5), rs.getDouble(6), rs.getDouble(7), rs.getDouble(8), rs.getDouble(9),
                rs.getDouble(10), rs.getDouble(11), rs.getDouble(12), rs.getDouble(13), rs.getDouble(14));
    }
}
