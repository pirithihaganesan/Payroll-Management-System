
import java.sql.*;
import java.util.Scanner;
import java.time.LocalDate;

public class AttendanceManagement {

    static final String URL = "jdbc:sqlite:database/payroll.db";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        try (Connection con = DriverManager.getConnection(URL)) {

            String createTable = """
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    employee_id INTEGER,
                    attendance_date TEXT,
                    status TEXT,
                    FOREIGN KEY(employee_id) REFERENCES employee(id)
                )
                """;

            con.createStatement().execute(createTable);

            while (true) {
                System.out.println("\n===== ATTENDANCE MANAGEMENT =====");
                System.out.println("1. Mark Attendance");
                System.out.println("2. View Attendance");
                System.out.println("3. Exit");
                System.out.print("Enter your choice: ");

                int choice = sc.nextInt();

                if (choice == 1) {
                    System.out.print("Enter Employee ID: ");
                    int empId = sc.nextInt();

                    System.out.print("Enter status (Present/Absent): ");
                    String status = sc.next();

                    String sql = """
                        INSERT INTO attendance
                        (employee_id, attendance_date, status)
                        VALUES (?, ?, ?)
                        """;

                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setInt(1, empId);
                    ps.setString(2, LocalDate.now().toString());
                    ps.setString(3, status);

                    ps.executeUpdate();

                    System.out.println("Attendance marked successfully!");

                } else if (choice == 2) {

                    String sql = """
    SELECT a.id, e.name, a.attendance_date, a.status
    FROM attendance a
    JOIN employee e ON a.employee_id = e.employee_id
    ORDER BY a.id
    """;
                    ResultSet rs =
                        con.createStatement().executeQuery(sql);

                    System.out.println("\n===== ATTENDANCE RECORDS =====");

                    while (rs.next()) {
                        System.out.println(
                            rs.getInt("id") + " | " +
                            rs.getString("name") + " | " +
                            rs.getString("attendance_date") + " | " +
                            rs.getString("status")
                        );
                    }

                } else if (choice == 3) {
                    System.out.println("Exiting Attendance Management.");
                    break;

                } else {
                    System.out.println("Invalid choice!");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        sc.close();
    }
}
