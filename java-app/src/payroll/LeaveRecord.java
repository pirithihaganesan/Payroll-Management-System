package payroll;

import java.time.LocalDate;

/** A leave application. */
public class LeaveRecord {
    private int leaveId;
    private final int empId;
    private final LeaveType type;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int days;
    private final String reason;
    private String status;

    public LeaveRecord(int leaveId, int empId, LeaveType type, LocalDate startDate,
                       LocalDate endDate, int days, String reason, String status) {
        this.leaveId = leaveId;
        this.empId = empId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = days;
        this.reason = reason;
        this.status = status;
    }

    public int getLeaveId() { return leaveId; }
    public int getEmpId() { return empId; }
    public LeaveType getType() { return type; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getDays() { return days; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public void setLeaveId(int leaveId) { this.leaveId = leaveId; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("#%-3d emp %-3d %-12s %s to %s (%d day%s) %-8s %s",
                leaveId, empId, type, startDate, endDate, days, days == 1 ? "" : "s", status,
                reason == null ? "" : reason);
    }
}
