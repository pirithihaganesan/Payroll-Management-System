package payroll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Data access for leave applications; approval writes the matching attendance rows. */
public class LeaveDAO {
    private final Connection con;
    private final AttendanceDAO attendanceDAO;

    public LeaveDAO(Connection con, AttendanceDAO attendanceDAO) {
        this.con = con;
        this.attendanceDAO = attendanceDAO;
    }

    /** Creates a Pending leave request. Returns the leave id. */
    public int apply(Employee emp, LeaveType type, LocalDate start, LocalDate end, String reason)
            throws SQLException {
        if (emp == null || !emp.isActive()) {
            throw new IllegalArgumentException("Employee not found or inactive");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date is before start date");
        }
        if (start.isBefore(emp.getJoinDate())) {
            throw new IllegalArgumentException("Leave starts before the joining date");
        }
        int days = PayrollCalculator.workingDaysBetween(start, end);
        if (days == 0) {
            throw new IllegalArgumentException("Selected range contains no working days");
        }
        if (overlaps(emp.getEmpId(), start, end)) {
            throw new IllegalArgumentException("Overlaps an existing pending/approved leave");
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO leave_record(emp_id, leave_type, start_date, end_date, days, reason, status) "
                + "VALUES (?,?,?,?,?,?, 'Pending')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, emp.getEmpId());
            ps.setString(2, type.getLabel());
            ps.setString(3, start.toString());
            ps.setString(4, end.toString());
            ps.setInt(5, days);
            ps.setString(6, reason);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private boolean overlaps(int empId, LocalDate start, LocalDate end) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM leave_record WHERE emp_id = ? AND status IN ('Pending','Approved') "
                + "AND start_date <= ? AND end_date >= ?")) {
            ps.setInt(1, empId);
            ps.setString(2, end.toString());
            ps.setString(3, start.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public LeaveRecord findById(int leaveId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT leave_id, emp_id, leave_type, start_date, end_date, days, reason, status "
                + "FROM leave_record WHERE leave_id = ?")) {
            ps.setInt(1, leaveId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<LeaveRecord> list(String statusOrNull) throws SQLException {
        String sql = "SELECT leave_id, emp_id, leave_type, start_date, end_date, days, reason, status "
                + "FROM leave_record " + (statusOrNull == null ? "" : "WHERE status = ? ")
                + "ORDER BY start_date DESC, leave_id DESC";
        List<LeaveRecord> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (statusOrNull != null) {
                ps.setString(1, statusOrNull);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    /**
     * Approves a pending leave and writes one attendance row per working day
     * (Paid Leave / Unpaid Leave) inside a single transaction.
     */
    public void approve(int leaveId) throws SQLException {
        LeaveRecord lr = findById(leaveId);
        if (lr == null) {
            throw new IllegalArgumentException("Leave #" + leaveId + " not found");
        }
        if (!"Pending".equals(lr.getStatus())) {
            throw new IllegalArgumentException("Leave #" + leaveId + " is already " + lr.getStatus());
        }
        boolean oldAuto = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            AttendanceStatus st = lr.getType().isPaid()
                    ? AttendanceStatus.PAID_LEAVE : AttendanceStatus.UNPAID_LEAVE;
            for (LocalDate d = lr.getStartDate(); !d.isAfter(lr.getEndDate()); d = d.plusDays(1)) {
                if (PayrollCalculator.isWorkingDay(d)) {
                    if (attendanceDAO.exists(lr.getEmpId(), d)) {
                        throw new IllegalArgumentException(
                                "Attendance already recorded on " + d + "; cannot apply leave");
                    }
                    attendanceDAO.insert(new AttendanceRecord(lr.getEmpId(), d, st, null, null, 0));
                }
            }
            setStatus(leaveId, "Approved");
            con.commit();
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAuto);
        }
    }

    public void reject(int leaveId) throws SQLException {
        LeaveRecord lr = findById(leaveId);
        if (lr == null) {
            throw new IllegalArgumentException("Leave #" + leaveId + " not found");
        }
        if (!"Pending".equals(lr.getStatus())) {
            throw new IllegalArgumentException("Leave #" + leaveId + " is already " + lr.getStatus());
        }
        setStatus(leaveId, "Rejected");
    }

    private void setStatus(int leaveId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE leave_record SET status = ? WHERE leave_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, leaveId);
            ps.executeUpdate();
        }
    }

    private LeaveRecord map(ResultSet rs) throws SQLException {
        return new LeaveRecord(rs.getInt(1), rs.getInt(2), LeaveType.fromLabel(rs.getString(3)),
                LocalDate.parse(rs.getString(4)), LocalDate.parse(rs.getString(5)),
                rs.getInt(6), rs.getString(7), rs.getString(8));
    }
}
