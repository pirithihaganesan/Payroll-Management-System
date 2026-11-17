package payroll;

import java.time.LocalDate;

/** One day's attendance for one employee. */
public class AttendanceRecord {
    private final int empId;
    private final LocalDate date;
    private final AttendanceStatus status;
    private final String checkIn;   // HH:mm or null
    private final String checkOut;  // HH:mm or null
    private final double workingHours;

    public AttendanceRecord(int empId, LocalDate date, AttendanceStatus status,
                            String checkIn, String checkOut, double workingHours) {
        this.empId = empId;
        this.date = date;
        this.status = status;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.workingHours = workingHours;
    }

    public int getEmpId() { return empId; }
    public LocalDate getDate() { return date; }
    public AttendanceStatus getStatus() { return status; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public double getWorkingHours() { return workingHours; }
}
