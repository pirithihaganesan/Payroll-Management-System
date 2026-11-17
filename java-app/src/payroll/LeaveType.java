package payroll;

/** Leave categories. Loss of Pay leave is unpaid; all others are paid. */
public enum LeaveType {
    CASUAL("Casual", true),
    SICK("Sick", true),
    EARNED("Earned", true),
    LOSS_OF_PAY("Loss of Pay", false);

    private final String label;
    private final boolean paid;

    LeaveType(String label, boolean paid) {
        this.label = label;
        this.paid = paid;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPaid() {
        return paid;
    }

    public static LeaveType fromLabel(String text) {
        for (LeaveType t : values()) {
            if (t.label.equalsIgnoreCase(text.trim())) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown leave type: " + text);
    }

    @Override
    public String toString() {
        return label;
    }
}
