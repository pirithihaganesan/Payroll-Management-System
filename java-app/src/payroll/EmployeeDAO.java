package payroll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Data access for employees, departments and designations. */
public class EmployeeDAO {
    private static final String SELECT_EMP =
            "SELECT e.emp_id, e.emp_name, d.dept_name, g.title, e.basic_salary, e.join_date, e.status "
            + "FROM employee e JOIN department d ON d.dept_id = e.dept_id "
            + "JOIN designation g ON g.desig_id = e.desig_id ";

    private final Connection con;

    public EmployeeDAO(Connection con) {
        this.con = con;
    }

    private int getOrCreate(String table, String idCol, String nameCol, String value) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT " + idCol + " FROM " + table + " WHERE LOWER(" + nameCol + ") = LOWER(?)")) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO " + table + "(" + nameCol + ") VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, value);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Creates an employee (department/designation are created if new). Returns the new id. */
    public int add(String name, String department, String designation, double basic, LocalDate joinDate)
            throws SQLException {
        int deptId = getOrCreate("department", "dept_id", "dept_name", department);
        int desigId = getOrCreate("designation", "desig_id", "title", designation);
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO employee(emp_name, dept_id, desig_id, basic_salary, join_date, status) "
                + "VALUES (?,?,?,?,?, 'Active')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, deptId);
            ps.setInt(3, desigId);
            ps.setDouble(4, basic);
            ps.setString(5, joinDate.toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Employee findById(int empId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT_EMP + "WHERE e.emp_id = ?")) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Employee> listAll(boolean activeOnly) throws SQLException {
        String sql = SELECT_EMP + (activeOnly ? "WHERE e.status = 'Active' " : "") + "ORDER BY e.emp_id";
        List<Employee> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<Employee> searchByName(String fragment) throws SQLException {
        List<Employee> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                SELECT_EMP + "WHERE LOWER(e.emp_name) LIKE ? ORDER BY e.emp_id")) {
            ps.setString(1, "%" + fragment.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Returns true when a row was updated. */
    public boolean updateSalary(int empId, double basic) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE employee SET basic_salary = ? WHERE emp_id = ?")) {
            ps.setDouble(1, basic);
            ps.setInt(2, empId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateDepartment(int empId, String department) throws SQLException {
        int deptId = getOrCreate("department", "dept_id", "dept_name", department);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE employee SET dept_id = ? WHERE emp_id = ?")) {
            ps.setInt(1, deptId);
            ps.setInt(2, empId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateDesignation(int empId, String designation) throws SQLException {
        int desigId = getOrCreate("designation", "desig_id", "title", designation);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE employee SET desig_id = ? WHERE emp_id = ?")) {
            ps.setInt(1, desigId);
            ps.setInt(2, empId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Soft delete: history (attendance, payroll) is preserved. */
    public boolean deactivate(int empId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE employee SET status = 'Inactive' WHERE emp_id = ?")) {
            ps.setInt(1, empId);
            return ps.executeUpdate() == 1;
        }
    }

    private Employee map(ResultSet rs) throws SQLException {
        return new Employee(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getDouble(5), LocalDate.parse(rs.getString(6)), rs.getString(7));
    }
}
