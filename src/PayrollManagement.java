
import java.sql.*;
import java.util.Scanner;

public class PayrollManagement {

    static final String URL = "jdbc:sqlite:database/payroll.db";
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try (Connection con = DriverManager.getConnection(URL)) {

            String createTable = """
                CREATE TABLE IF NOT EXISTS payroll (
                    payroll_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    employee_id INTEGER,
                    payroll_month TEXT,
                    working_days INTEGER,
                    present_days INTEGER,
                    absent_days INTEGER,
                    basic_salary REAL,
                    deduction REAL,
                    net_salary REAL,
                    FOREIGN KEY(employee_id)
                    REFERENCES employee(employee_id)
                )
                """;

            con.createStatement().execute(createTable);

            while (true) {

                System.out.println("\n===== PAYROLL MANAGEMENT =====");
                System.out.println("1. Calculate Monthly Salary");
                System.out.println("2. View Payroll Reports");
                System.out.println("3. Exit");
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
                        calculateSalary(con);
                        break;

                    case 2:
                        viewPayroll(con);
                        break;

                    case 3:
                        System.out.println("Exiting Payroll Management.");
                        return;

                    default:
                        System.out.println("Invalid choice!");
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    static void calculateSalary(Connection con) {

        try {
            System.out.print("Enter Employee ID: ");
            int empId = Integer.parseInt(sc.nextLine());

            System.out.print("Enter payroll month (YYYY-MM): ");
            String month = sc.nextLine();

            if (!month.matches("\\d{4}-(0[1-9]|1[0-2])")) {
                System.out.println("Invalid month! Use YYYY-MM.");
                return;
            }

            System.out.print("Enter total working days: ");
            int workingDays = Integer.parseInt(sc.nextLine());

            if (workingDays <= 0 || workingDays > 31) {
                System.out.println("Invalid working days!");
                return;
            }

            double basicSalary;

            String employeeSql =
                "SELECT basic_salary FROM employee WHERE employee_id = ?";

            try (PreparedStatement ps = con.prepareStatement(employeeSql)) {

                ps.setInt(1, empId);

                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    System.out.println("Employee not found!");
                    return;
                }

                basicSalary = rs.getDouble("basic_salary");
            }

            String attendanceSql = """
                SELECT
                COUNT(DISTINCT CASE WHEN status = 'Present'
                    THEN attendance_date END) AS present_days,
                COUNT(DISTINCT CASE WHEN status = 'Absent'
                    THEN attendance_date END) AS absent_days
                FROM attendance
                WHERE employee_id = ?
                AND substr(attendance_date, 1, 7) = ?
                """;

            int presentDays;
            int absentDays;

            try (PreparedStatement ps =
                     con.prepareStatement(attendanceSql)) {

                ps.setInt(1, empId);
                ps.setString(2, month);

                ResultSet rs = ps.executeQuery();

                rs.next();

                presentDays = rs.getInt("present_days");
                absentDays = rs.getInt("absent_days");
            }

            if (presentDays + absentDays > workingDays) {
                System.out.println(
                    "Attendance records exceed working days!"
                );
                return;
            }

            double dailySalary = basicSalary / workingDays;

            double deduction = dailySalary * absentDays;

            double netSalary = basicSalary - deduction;

            System.out.println("\n===== SALARY DETAILS =====");
            System.out.println("Employee ID: " + empId);
            System.out.println("Payroll Month: " + month);
            System.out.println("Basic Salary: Rs. " + basicSalary);
            System.out.println("Working Days: " + workingDays);
            System.out.println("Present Days: " + presentDays);
            System.out.println("Absent Days: " + absentDays);
            System.out.printf("Daily Salary: Rs. %.2f%n", dailySalary);
            System.out.printf("Deduction: Rs. %.2f%n", deduction);
            System.out.printf("Net Salary: Rs. %.2f%n", netSalary);

            String insertSql = """
                INSERT INTO payroll
                (employee_id, payroll_month, working_days,
                 present_days, absent_days, basic_salary,
                 deduction, net_salary)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement ps =
                     con.prepareStatement(insertSql)) {

                ps.setInt(1, empId);
                ps.setString(2, month);
                ps.setInt(3, workingDays);
                ps.setInt(4, presentDays);
                ps.setInt(5, absentDays);
                ps.setDouble(6, basicSalary);
                ps.setDouble(7, deduction);
                ps.setDouble(8, netSalary);

                ps.executeUpdate();

                System.out.println("\nPayroll saved successfully!");
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter valid numeric values!");

        } catch (SQLException e) {
            System.out.println("Payroll error: " + e.getMessage());
        }
    }

    static void viewPayroll(Connection con) {

        String sql = """
            SELECT p.*, e.name
            FROM payroll p
            JOIN employee e
            ON p.employee_id = e.employee_id
            ORDER BY p.payroll_id
            """;

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n========== MONTHLY PAYROLL REPORT ==========");

            System.out.printf(
                "%-18s %-12s %-12s %-12s %-12s%n",
                "EMPLOYEE", "MONTH", "BASIC", "DEDUCTION", "NET SALARY"
            );

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf(
                    "%-18s %-12s %-12.2f %-12.2f %-12.2f%n",
                    rs.getString("name"),
                    rs.getString("payroll_month"),
                    rs.getDouble("basic_salary"),
                    rs.getDouble("deduction"),
                    rs.getDouble("net_salary")
                );
            }

            if (!found) {
                System.out.println("No payroll records found.");
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
