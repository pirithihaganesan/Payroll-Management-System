package payroll;

/** Daily attendance status values (match the CHECK constraint in schema.sql). */
public enum AttendanceStatus {
    PRESENT("Present"),
    ABSENT("Absent"),
    HALF_DAY("Half-Day"),
    PAID_LEAVE("Paid Leave"),
    UNPAID_LEAVE("Unpaid Leave");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AttendanceStatus fromLabel(String text) {
        for (AttendanceStatus s : values()) {
            if (s.label.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown attendance status: " + text);
    }

    @Override
    public String toString() {
        return label;
    }
}
