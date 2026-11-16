
import java.sql.*;
import java.util.Scanner;

public class EmployeeManagement {

    static final String URL = "jdbc:sqlite:database/payroll.db";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        while (true) {

            System.out.println("\n===== EMPLOYEE MANAGEMENT =====");
            System.out.println("1. Add Employee");
            System.out.println("2. View All Employees");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            int choice;

            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
                continue;
            }

            switch (choice) {
                case 1:
                    addEmployee();
                    break;

                case 2:
                    viewEmployees();
                    break;

                case 3:
                    System.out.println("Exiting Employee Management.");
                    return;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // Add a new employee
    static void addEmployee() {

        try (Connection con = DriverManager.getConnection(URL)) {

            System.out.print("Enter employee name: ");
            String name = sc.nextLine().trim();

            System.out.print("Enter department: ");
            String department = sc.nextLine().trim();

            System.out.print("Enter designation: ");
            String designation = sc.nextLine().trim();

            System.out.print("Enter basic salary: ");
            double salary = Double.parseDouble(sc.nextLine());

            if (name.isEmpty() || department.isEmpty()
                    || designation.isEmpty() || salary <= 0
                    || !Double.isFinite(salary)) {
                System.out.println("Invalid employee details!");
                return;
            }

            String sql = """
                INSERT INTO employee
                (name, department, designation, basic_salary)
                VALUES (?, ?, ?, ?)
                """;

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, department);
                ps.setString(3, designation);
                ps.setDouble(4, salary);

                ps.executeUpdate();

                System.out.println("Employee added successfully!");
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid salary.");

        } catch (SQLException e) {
            System.out.println("Error adding employee: " + e.getMessage());
        }
    }

    // Display all employees
    static void viewEmployees() {

        String sql = "SELECT * FROM employee";

        try (Connection con = DriverManager.getConnection(URL);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n========== EMPLOYEE DETAILS ==========");

            System.out.printf("%-8s %-20s %-15s %-20s %-12s%n",
                    "ID", "NAME", "DEPARTMENT",
                    "DESIGNATION", "SALARY");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf("%-8d %-20s %-15s %-20s %.2f%n",
                        rs.getInt("employee_id"),
                        rs.getString("name"),
                        rs.getString("department"),
                        rs.getString("designation"),
                        rs.getDouble("basic_salary"));
            }

            if (!found) {
                System.out.println("No employees found.");
            }

        } catch (SQLException e) {
            System.out.println("Error viewing employees: " + e.getMessage());
        }
    }
}
