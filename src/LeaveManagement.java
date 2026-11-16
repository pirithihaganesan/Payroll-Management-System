
import java.sql.*;
import java.util.Scanner;
import java.time.LocalDate;

public class LeaveManagement {

    static final String URL = "jdbc:sqlite:database/payroll.db";
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try (Connection con = DriverManager.getConnection(URL)) {

            String createTable = """
                CREATE TABLE IF NOT EXISTS employee_leave (
                    leave_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    employee_id INTEGER,
                    leave_date TEXT,
                    reason TEXT,
                    FOREIGN KEY(employee_id)
                    REFERENCES employee(employee_id)
                )
                """;

            con.createStatement().execute(createTable);

            while (true) {
                System.out.println("\n===== LEAVE MANAGEMENT =====");
                System.out.println("1. Apply Leave");
                System.out.println("2. View Leave Records");
                System.out.println("3. Count Leave Days");
                System.out.println("4. Exit");
                System.out.print("Enter your choice: ");

                int choice;

                try {
                    choice = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Enter a valid number!");
                    continue;
                }

                switch (choice) {

                    case 1:
                        applyLeave(con);
                        break;

                    case 2:
                        viewLeaves(con);
                        break;

                    case 3:
                        countLeaves(con);
                        break;

                    case 4:
                        System.out.println("Exiting Leave Management.");
                        return;

                    default:
                        System.out.println("Invalid choice!");
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    static void applyLeave(Connection con) {

        try {
            System.out.print("Enter Employee ID: ");
            int empId = Integer.parseInt(sc.nextLine());

            String check = "SELECT employee_id FROM employee WHERE employee_id = ?";

            try (PreparedStatement ps = con.prepareStatement(check)) {
                ps.setInt(1, empId);

                if (!ps.executeQuery().next()) {
                    System.out.println("Employee not found!");
                    return;
                }
            }

            System.out.print("Enter leave date (YYYY-MM-DD): ");
            String date = sc.nextLine();

            LocalDate.parse(date);

            System.out.print("Enter leave reason: ");
            String reason = sc.nextLine().trim();

            if (reason.isEmpty()) {
                System.out.println("Reason cannot be empty!");
                return;
            }

            String sql = """
                INSERT INTO employee_leave
                (employee_id, leave_date, reason)
                VALUES (?, ?, ?)
                """;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, empId);
                ps.setString(2, date);
                ps.setString(3, reason);

                ps.executeUpdate();

                System.out.println("Leave recorded successfully!");
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid employee ID!");

        } catch (java.time.format.DateTimeParseException e) {
            System.out.println("Invalid date! Use YYYY-MM-DD.");

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void viewLeaves(Connection con) {

        String sql = """
            SELECT l.leave_id, e.name, l.leave_date, l.reason
            FROM employee_leave l
            JOIN employee e
            ON l.employee_id = e.employee_id
            ORDER BY l.leave_id
            """;

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n===== LEAVE RECORDS =====");

            System.out.printf("%-8s %-20s %-15s %-25s%n",
                    "ID", "EMPLOYEE", "DATE", "REASON");

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.printf("%-8d %-20s %-15s %-25s%n",
                        rs.getInt("leave_id"),
                        rs.getString("name"),
                        rs.getString("leave_date"),
                        rs.getString("reason"));
            }

            if (!found) {
                System.out.println("No leave records found.");
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void countLeaves(Connection con) {

        String sql = """
            SELECT e.name, COUNT(l.leave_id) AS total_leaves
            FROM employee e
            LEFT JOIN employee_leave l
            ON e.employee_id = l.employee_id
            GROUP BY e.employee_id, e.name
            """;

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n===== EMPLOYEE LEAVE SUMMARY =====");

            while (rs.next()) {
                System.out.println(
                    rs.getString("name") +
                    " | Leave Days: " +
                    rs.getInt("total_leaves")
                );
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
