package payroll;

import java.time.LocalDate;

/** Employee entity (joined with department and designation names for display). */
public class Employee {
    private int empId;
    private String name;
    private String department;
    private String designation;
    private double basicSalary;
    private LocalDate joinDate;
    private String status;

    public Employee(int empId, String name, String department, String designation,
                    double basicSalary, LocalDate joinDate, String status) {
        this.empId = empId;
        this.name = name;
        this.department = department;
        this.designation = designation;
        this.basicSalary = basicSalary;
        this.joinDate = joinDate;
        this.status = status;
    }

    public int getEmpId() { return empId; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public double getBasicSalary() { return basicSalary; }
    public LocalDate getJoinDate() { return joinDate; }
    public String getStatus() { return status; }

    public void setEmpId(int empId) { this.empId = empId; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }

    public boolean isActive() {
        return "Active".equals(status);
    }

    @Override
    public String toString() {
        return String.format("%-4d %-20s %-16s %-17s %10.2f %s %s",
                empId, name, department, designation, basicSalary, joinDate, status);
    }
}
