package payroll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/** Data access for daily attendance. */
public class AttendanceDAO {
    private final Connection con;

    public AttendanceDAO(Connection con) {
        this.con = con;
    }

    public boolean exists(int empId, LocalDate date) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM attendance WHERE emp_id = ? AND att_date = ?")) {
            ps.setInt(1, empId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Records attendance after business-rule validation.
     * @throws IllegalArgumentException when a rule is violated (duplicate, weekend, future date, ...)
     */
    public void mark(Employee emp, LocalDate date, AttendanceStatus status,
                     String checkIn, String checkOut) throws SQLException {
        if (emp == null || !emp.isActive()) {
            throw new IllegalArgumentException("Employee not found or inactive");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot mark attendance for a future date");
        }
        if (date.isBefore(emp.getJoinDate())) {
            throw new IllegalArgumentException("Date is before the employee's joining date");
        }
        if (!PayrollCalculator.isWorkingDay(date)) {
            throw new IllegalArgumentException("Weekend - not a working day");
        }
        if (exists(emp.getEmpId(), date)) {
            throw new IllegalArgumentException("Attendance already recorded for this employee on " + date);
        }

        double hours;
        switch (status) {
            case PRESENT:
            case HALF_DAY:
                if (checkIn != null && checkOut != null) {
                    hours = Validator.hoursBetween(checkIn, checkOut);
                } else {
                    checkIn = null;
                    checkOut = null;
                    hours = status == AttendanceStatus.PRESENT
                            ? SalaryRules.FULL_DAY_HOURS : SalaryRules.HALF_DAY_HOURS;
                }
                break;
            default:
                checkIn = null;
                checkOut = null;
                hours = 0;
        }
        insert(new AttendanceRecord(emp.getEmpId(), date, status, checkIn, checkOut, hours));
    }

    /** Low-level insert (used by leave approval, which has already validated the dates). */
    void insert(AttendanceRecord r) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO attendance(emp_id, att_date, status, check_in, check_out, working_hours) "
                + "VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, r.getEmpId());
            ps.setString(2, r.getDate().toString());
            ps.setString(3, r.getStatus().getLabel());
            ps.setString(4, r.getCheckIn());
            ps.setString(5, r.getCheckOut());
            ps.setDouble(6, r.getWorkingHours());
            ps.executeUpdate();
        }
    }

    /** Counts of each status for one employee in a month: present, half, absent, paid, unpaid. */
    public int[] monthCounts(int empId, YearMonth ym) throws SQLException {
        int[] out = new int[5];
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT status, COUNT(*) FROM attendance "
                + "WHERE emp_id = ? AND strftime('%Y-%m', att_date) = ? GROUP BY status")) {
            ps.setInt(1, empId);
            ps.setString(2, ym.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceStatus s = AttendanceStatus.fromLabel(rs.getString(1));
                    out[indexOf(s)] = rs.getInt(2);
                }
            }
        }
        return out;
    }

    private static int indexOf(AttendanceStatus s) {
        switch (s) {
            case PRESENT: return 0;
            case HALF_DAY: return 1;
            case ABSENT: return 2;
            case PAID_LEAVE: return 3;
            default: return 4;
        }
    }

    public int recordsInMonth(int empId, YearMonth ym) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM attendance WHERE emp_id = ? AND strftime('%Y-%m', att_date) = ?")) {
            ps.setInt(1, empId);
            ps.setString(2, ym.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
