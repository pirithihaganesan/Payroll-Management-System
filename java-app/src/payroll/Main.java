package payroll;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Scanner;

/**
 * Employee Attendance and Payroll Management Analysis System - console application.
 *
 * Usage: java -cp "out:lib/*" payroll.Main [--db file] [--schema file] [--seed file] [--demo]
 */
public class Main {
    private final Console console;
    private final EmployeeDAO employees;
    private final AttendanceDAO attendance;
    private final LeaveDAO leaves;
    private final PayrollDAO payroll;
    private final ReportService reports;

    public Main(Database db, Console console) {
        this.console = console;
        this.employees = new EmployeeDAO(db.getConnection());
        this.attendance = new AttendanceDAO(db.getConnection());
        this.leaves = new LeaveDAO(db.getConnection(), attendance);
        this.payroll = new PayrollDAO(db.getConnection(), attendance, employees);
        this.reports = new ReportService(db.getConnection(), payroll);
    }

    public static void main(String[] args) throws Exception {
        String dbFile = "../database/payroll.db";
        Path schema = Paths.get("../database/schema.sql");
        Path seed = null;
        boolean demo = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--db": dbFile = args[++i]; break;
                case "--schema": schema = Paths.get(args[++i]); break;
                case "--seed": seed = Paths.get(args[++i]); break;
                case "--demo": demo = true; break;
                default:
                    System.out.println("Unknown option " + args[i]);
                    return;
            }
        }
        try (Database db = new Database(dbFile)) {
            if (!db.isInitialised()) {
                System.out.println("Initialising new database " + dbFile);
                db.runScript(schema);
                if (seed != null && Files.exists(seed)) {
                    db.runScript(seed);
                }
            }
            Main app = new Main(db, new Console(new Scanner(System.in)));
            if (demo) {
                app.runDemo();
            } else {
                app.menu();
            }
        } catch (IllegalStateException e) {
            System.out.println("\nInput ended. Goodbye.");
        }
    }

    // ------------------------------------------------------------------ menus

    private void menu() {
        while (true) {
            System.out.println("\n===== EMPLOYEE ATTENDANCE & PAYROLL SYSTEM =====");
            System.out.println("1. Employee management");
            System.out.println("2. Attendance");
            System.out.println("3. Leave management");
            System.out.println("4. Payroll");
            System.out.println("5. Reports");
            System.out.println("0. Exit");
            int c = console.integer("Choice: ", 0, 5);
            try {
                switch (c) {
                    case 1: employeeMenu(); break;
                    case 2: attendanceMenu(); break;
                    case 3: leaveMenu(); break;
                    case 4: payrollMenu(); break;
                    case 5: reportMenu(); break;
                    default: System.out.println("Goodbye."); return;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Not done: " + e.getMessage());
            }
        }
    }

    private void employeeMenu() throws SQLException {
        System.out.println("\n-- Employees --\n1 Add  2 View all  3 Search by name  4 Update salary"
                + "  5 Change department  6 Change designation  7 Deactivate  0 Back");
        switch (console.integer("Choice: ", 0, 7)) {
            case 1: {
                String name = console.name("Name: ");
                String dept = console.label("Department: ", "Department");
                String desig = console.label("Designation: ", "Designation");
                double basic = console.salary("Basic salary: ");
                LocalDate join = console.date("Joining date (yyyy-MM-dd): ");
                if (join.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("Joining date cannot be in the future");
                }
                int id = employees.add(name, dept, desig, basic, join);
                System.out.println("Employee created with ID " + id);
                break;
            }
            case 2: printEmployees(employees.listAll(false)); break;
            case 3: printEmployees(employees.searchByName(console.line("Name contains: "))); break;
            case 4: {
                int id = console.integer("Employee ID: ", 1, Integer.MAX_VALUE);
                double basic = console.salary("New basic salary: ");
                System.out.println(employees.updateSalary(id, basic) ? "Salary updated." : "Employee not found.");
                break;
            }
            case 5: {
                int id = console.integer("Employee ID: ", 1, Integer.MAX_VALUE);
                String dept = console.label("New department: ", "Department");
                System.out.println(employees.updateDepartment(id, dept) ? "Department updated." : "Employee not found.");
                break;
            }
            case 6: {
                int id = console.integer("Employee ID: ", 1, Integer.MAX_VALUE);
                String desig = console.label("New designation: ", "Designation");
                System.out.println(employees.updateDesignation(id, desig) ? "Designation updated." : "Employee not found.");
                break;
            }
            case 7: {
                int id = console.integer("Employee ID: ", 1, Integer.MAX_VALUE);
                System.out.println(employees.deactivate(id) ? "Employee deactivated (history kept)." : "Employee not found.");
                break;
            }
            default: break;
        }
    }

    private void attendanceMenu() throws SQLException {
        System.out.println("\n-- Attendance --\n1 Mark attendance  2 Monthly summary  0 Back");
        switch (console.integer("Choice: ", 0, 2)) {
            case 1: {
                int id = console.integer("Employee ID: ", 1, Integer.MAX_VALUE);
                Employee emp = employees.findById(id);
                if (emp == null) {
                    throw new IllegalArgumentException("Employee not found");
                }
                LocalDate date = console.date("Date (yyyy-MM-dd): ");
                System.out.println("Status: 1 Present  2 Absent  3 Half-Day");
                AttendanceStatus[] opts = {AttendanceStatus.PRESENT, AttendanceStatus.ABSENT, AttendanceStatus.HALF_DAY};
                AttendanceStatus st = opts[console.integer("Choice: ", 1, 3) - 1];
                String in = null;
                String out = null;
                if (st != AttendanceStatus.ABSENT) {
                    in = console.line("Check-in HH:mm (blank for default hours): ");
                    if (!in.isEmpty()) {
                        out = console.line("Check-out HH:mm: ");
                    } else {
                        in = null;
                    }
                }
                attendance.mark(emp, date, st, in, out);
                System.out.println("Attendance recorded.");
                break;
            }
            case 2: reports.attendanceSummary(console.month("Month (yyyy-MM): ")); break;
            default: break;
        }
    }

    private void leaveMenu() throws SQLException {
        System.out.println("\n-- Leave --\n1 Apply  2 Approve  3 Reject  4 List pending  5 List all  0 Back");
        switch (console.integer("Choice: ", 0, 5)) {
            case 1: {
                int id = console.integer("Employee ID: ", 1, Integer.MAX_VALUE);
                Employee emp = employees.findById(id);
                System.out.println("Type: 1 Casual  2 Sick  3 Earned  4 Loss of Pay");
                LeaveType t = LeaveType.values()[console.integer("Choice: ", 1, 4) - 1];
                LocalDate s = console.date("Start date (yyyy-MM-dd): ");
                LocalDate e = console.date("End date (yyyy-MM-dd): ");
                String reason = console.line("Reason: ");
                int lid = leaves.apply(emp, t, s, e, reason);
                System.out.println("Leave request #" + lid + " submitted (Pending).");
                break;
            }
            case 2: {
                leaves.approve(console.integer("Leave ID: ", 1, Integer.MAX_VALUE));
                System.out.println("Leave approved; attendance updated.");
                break;
            }
            case 3: {
                leaves.reject(console.integer("Leave ID: ", 1, Integer.MAX_VALUE));
                System.out.println("Leave rejected.");
                break;
            }
            case 4: printLeaves(leaves.list("Pending")); break;
            case 5: printLeaves(leaves.list(null)); break;
            default: break;
        }
    }

    private void payrollMenu() throws SQLException {
        System.out.println("\n-- Payroll --\n1 Generate for one employee  2 Generate for all  3 View payslip  0 Back");
        switch (console.integer("Choice: ", 0, 3)) {
            case 1: {
                int id = console.integer("Employee ID: ", 1, Integer.MAX_VALUE);
                YearMonth ym = console.month("Month (yyyy-MM): ");
                System.out.println(payroll.generate(id, ym).toPayslip());
                break;
            }
            case 2: {
                YearMonth ym = console.month("Month (yyyy-MM): ");
                System.out.println("Payslips generated: " + payroll.generateAll(ym));
                break;
            }
            case 3: {
                int id = console.integer("Employee ID: ", 1, Integer.MAX_VALUE);
                YearMonth ym = console.month("Month (yyyy-MM): ");
                PayrollRecord p = payroll.find(id, ym);
                System.out.println(p == null ? "No payslip found." : p.toPayslip());
                break;
            }
            default: break;
        }
    }

    private void reportMenu() throws SQLException {
        System.out.println("\n-- Reports --\n1 Employee attendance summary  2 Department attendance"
                + "  3 Monthly payroll  4 Department payroll  5 Frequent absentees  6 Leave summary  0 Back");
        switch (console.integer("Choice: ", 0, 6)) {
            case 1: reports.attendanceSummary(console.month("Month (yyyy-MM): ")); break;
            case 2: reports.departmentAttendance(console.month("Month (yyyy-MM): ")); break;
            case 3: reports.payrollReport(console.month("Month (yyyy-MM): ")); break;
            case 4: reports.departmentPayroll(console.month("Month (yyyy-MM): ")); break;
            case 5: reports.frequentAbsentees(console.integer("Minimum absent days: ", 1, 100)); break;
            case 6: reports.leaveSummary(); break;
            default: break;
        }
    }

    private void printEmployees(List<Employee> list) {
        System.out.printf("%-4s %-20s %-16s %-17s %10s %-10s %s%n",
                "ID", "Name", "Department", "Designation", "Basic", "Joined", "Status");
        if (list.isEmpty()) {
            System.out.println("(none)");
        }
        for (Employee e : list) {
            System.out.println(e);
        }
    }

    private void printLeaves(List<LeaveRecord> list) {
        if (list.isEmpty()) {
            System.out.println("(none)");
        }
        for (LeaveRecord l : list) {
            System.out.println(l);
        }
    }

    // ------------------------------------------------------------------- demo

    /** Scripted walk-through used to produce sample output (run on an empty database). */
    private void runDemo() throws SQLException {
        System.out.println("=== DEMO: scripted scenario ===");
        int a = employees.add("Asha Menon", "Engineering", "Senior Executive", 32000, LocalDate.of(2025, 1, 6));
        int b = employees.add("Bala Krishnan", "Sales", "Executive", 21000, LocalDate.of(2025, 3, 3));
        int c = employees.add("Chitra Devi", "Human Resources", "Manager", 55000, LocalDate.of(2024, 7, 1));
        printEmployees(employees.listAll(false));

        YearMonth ym = YearMonth.of(2026, 10);
        System.out.println("\nMarking attendance for " + ym + " (Asha: 2 absent, 1 half-day; Bala: leave; Chitra: all present)");
        int day = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            if (!PayrollCalculator.isWorkingDay(date)) {
                continue;
            }
            day++;
            attendance.mark(employees.findById(c), date, AttendanceStatus.PRESENT, "09:00", "17:30");
            AttendanceStatus sa = (day == 5 || day == 12) ? AttendanceStatus.ABSENT
                    : day == 20 ? AttendanceStatus.HALF_DAY : AttendanceStatus.PRESENT;
            attendance.mark(employees.findById(a), date, sa, null, null);
            if (day < 8 || day > 10) {   // days 8-10 are covered by Bala's leave
                attendance.mark(employees.findById(b), date, AttendanceStatus.PRESENT, "09:15", "18:00");
            }
        }
        int lid = leaves.apply(employees.findById(b), LeaveType.SICK,
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12), "Fever");
        System.out.println("\nLeave request submitted:");
        printLeaves(leaves.list("Pending"));
        leaves.approve(lid);
        System.out.println("Approved leave #" + lid);

        System.out.println("\nDuplicate attendance check:");
        try {
            attendance.mark(employees.findById(a), ym.atDay(1), AttendanceStatus.PRESENT, null, null);
        } catch (IllegalArgumentException e) {
            System.out.println("  rejected -> " + e.getMessage());
        }

        System.out.println("\nGenerating payroll for " + ym + ": " + payroll.generateAll(ym) + " payslips");
        System.out.println(payroll.find(a, ym).toPayslip());
        reports.attendanceSummary(ym);
        reports.payrollReport(ym);
        reports.departmentPayroll(ym);
        reports.frequentAbsentees(2);
        reports.leaveSummary();
    }
}
