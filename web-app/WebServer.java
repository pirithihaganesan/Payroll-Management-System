
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import payroll.Database;
import payroll.Employee;
import payroll.EmployeeDAO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServer {

    private static Connection connection;
    private static EmployeeDAO employeeDAO;

    public static void main(String[] args) throws Exception {

        Database database = new Database("web-app/payroll-web.db");
        connection = database.getConnection();
        employeeDAO = new EmployeeDAO(connection);

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0);

        register(server, "/api/employees", "GET",
                exchange -> employeesJson());

        register(server, "/api/attendance", "GET",
                exchange -> attendanceJson(exchange));

        register(server, "/api/leaves", "GET",
                exchange -> leavesJson(exchange));

        register(server, "/api/payroll", "GET",
                exchange -> payrollJson(exchange));

        register(server, "/api/reports", "GET",
                exchange -> reportsJson(exchange));

        server.start();

        System.out.println("======================================");
        System.out.println(" PAYTRACK PAYROLL WEB API");
        System.out.println("======================================");
        System.out.println("Employees  : http://localhost:8080/api/employees");
        System.out.println("Attendance : http://localhost:8080/api/attendance");
        System.out.println("Leaves     : http://localhost:8080/api/leaves");
        System.out.println("Payroll    : http://localhost:8080/api/payroll");
        System.out.println("Reports    : http://localhost:8080/api/reports");
        System.out.println("Server running on port 8080.");
        System.out.println("Press Ctrl+C to stop.");
    }

    @FunctionalInterface
    interface ApiAction {
        String execute(HttpExchange exchange) throws Exception;
    }

    private static void register(
            HttpServer server,
            String path,
            String method,
            ApiAction action) {

        server.createContext(path, exchange -> {

            addCorsHeaders(exchange);

            try {
                if ("OPTIONS".equalsIgnoreCase(
                        exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }

                if (!method.equalsIgnoreCase(
                        exchange.getRequestMethod())) {
                    send(exchange, 405,
                            "{\"error\":\"Method not allowed\"}");
                    return;
                }

                String response = action.execute(exchange);
                send(exchange, 200, response);

            } catch (IllegalArgumentException e) {
                send(exchange, 400,
                        "{\"error\":\"" + escape(e.getMessage()) + "\"}");

            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500,
                        "{\"error\":\"Unable to process request\"}");
            }
        });
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
    }

    private static String employeesJson() throws Exception {

        List<Employee> employees = employeeDAO.listAll(false);

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < employees.size(); i++) {

            Employee e = employees.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                .append("\"id\":").append(e.getEmpId()).append(",")
                .append("\"name\":").append(jsonString(e.getName())).append(",")
                .append("\"department\":").append(jsonString(e.getDepartment())).append(",")
                .append("\"designation\":").append(jsonString(e.getDesignation())).append(",")
                .append("\"salary\":").append(e.getBasicSalary()).append(",")
                .append("\"joinDate\":").append(jsonString(e.getJoinDate().toString())).append(",")
                .append("\"status\":").append(jsonString(e.getStatus()))
                .append("}");
        }

        return json.append("]").toString();
    }

    private static String attendanceJson(HttpExchange exchange)
            throws Exception {

        Map<String, String> params = parameters(exchange);

        String sql =
                "SELECT a.att_id, a.emp_id, e.emp_name, a.att_date, " +
                "a.status, a.check_in, a.check_out, a.working_hours " +
                "FROM attendance a " +
                "JOIN employee e ON e.emp_id = a.emp_id ";

        String month = params.get("month");

        if (month != null && !month.isBlank()) {
            YearMonth ym = YearMonth.parse(month);
            sql += "WHERE strftime('%Y-%m', a.att_date) = ? ";
            sql += "ORDER BY a.att_date DESC, a.emp_id";
            return queryJson(sql, ym.toString());
        }

        sql += "ORDER BY a.att_date DESC, a.emp_id";
        return queryJson(sql);
    }

    private static String leavesJson(HttpExchange exchange)
            throws Exception {

        Map<String, String> params = parameters(exchange);

        String status = params.get("status");

        String sql =
                "SELECT l.leave_id, l.emp_id, e.emp_name, " +
                "l.leave_type, l.start_date, l.end_date, " +
                "l.days, l.reason, l.status " +
                "FROM leave_record l " +
                "JOIN employee e ON e.emp_id = l.emp_id ";

        if (status != null && !status.isBlank()) {
            sql += "WHERE LOWER(l.status) = LOWER(?) ";
            sql += "ORDER BY l.start_date DESC";
            return queryJson(sql, status);
        }

        sql += "ORDER BY l.start_date DESC";
        return queryJson(sql);
    }

    private static String payrollJson(HttpExchange exchange)
            throws Exception {

        Map<String, String> params = parameters(exchange);

        String month = params.get("month");

        String sql =
                "SELECT p.payroll_id, p.emp_id, e.emp_name, " +
                "p.pay_month, p.pay_year, p.working_days, " +
                "p.payable_days, p.lop_days, p.basic, p.hra, p.da, " +
                "p.gross_salary, p.pf_deduction, p.prof_tax, " +
                "p.lop_deduction, p.total_deductions, p.net_salary, " +
                "p.generated_on " +
                "FROM payroll p " +
                "JOIN employee e ON e.emp_id = p.emp_id ";

        if (month != null && !month.isBlank()) {

            YearMonth ym = YearMonth.parse(month);

            sql += "WHERE p.pay_year = ? AND p.pay_month = ? ";
            sql += "ORDER BY e.emp_id";

            return queryJson(
                    sql, ym.getYear(), ym.getMonthValue());
        }

        sql += "ORDER BY p.pay_year DESC, p.pay_month DESC, e.emp_id";

        return queryJson(sql);
    }

    private static String reportsJson(HttpExchange exchange)
            throws Exception {

        Map<String, String> params = parameters(exchange);

        String month = params.get("month");

        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now()
                : YearMonth.parse(month);

        String attendanceSql =
                "SELECT COUNT(*) AS totalRecords, " +
                "SUM(CASE WHEN status='Present' THEN 1 ELSE 0 END) AS present, " +
                "SUM(CASE WHEN status='Absent' THEN 1 ELSE 0 END) AS absent, " +
                "SUM(CASE WHEN status='Half-Day' THEN 1 ELSE 0 END) AS halfDay, " +
                "SUM(CASE WHEN status='Paid Leave' THEN 1 ELSE 0 END) AS paidLeave, " +
                "SUM(CASE WHEN status='Unpaid Leave' THEN 1 ELSE 0 END) AS unpaidLeave " +
                "FROM attendance WHERE strftime('%Y-%m', att_date) = ?";

        String payrollSql =
                "SELECT COUNT(*) AS payrollCount, " +
                "COALESCE(SUM(gross_salary),0) AS totalGross, " +
                "COALESCE(SUM(total_deductions),0) AS totalDeductions, " +
                "COALESCE(SUM(net_salary),0) AS totalNet " +
                "FROM payroll WHERE pay_year = ? AND pay_month = ?";

        String attendance = queryJson(
                attendanceSql, ym.toString());

        String payroll = queryJson(
                payrollSql, ym.getYear(), ym.getMonthValue());

        return "{\"month\":" + jsonString(ym.toString()) +
                ",\"attendance\":" + attendance +
                ",\"payroll\":" + payroll + "}";
    }

    private static String queryJson(
            String sql, Object... values) throws Exception {

        StringBuilder json = new StringBuilder("[");

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {

                ResultSetMetaData metadata = rs.getMetaData();
                int columns = metadata.getColumnCount();
                boolean firstRow = true;

                while (rs.next()) {

                    if (!firstRow) {
                        json.append(",");
                    }

                    firstRow = false;
                    json.append("{");

                    for (int i = 1; i <= columns; i++) {

                        if (i > 1) {
                            json.append(",");
                        }

                        String column = metadata.getColumnLabel(i);
                        Object value = rs.getObject(i);

                        json.append(jsonString(column))
                            .append(":")
                            .append(jsonValue(value));
                    }

                    json.append("}");
                }
            }
        }

        return json.append("]").toString();
    }

    private static Map<String, String> parameters(
            HttpExchange exchange) {

        Map<String, String> result = new HashMap<>();

        String query = exchange.getRequestURI().getRawQuery();

        if (query == null || query.isBlank()) {
            return result;
        }

        for (String part : query.split("&")) {

            String[] pair = part.split("=", 2);

            String key = URLDecoder.decode(
                    pair[0], StandardCharsets.UTF_8);

            String value = pair.length > 1
                    ? URLDecoder.decode(
                            pair[1], StandardCharsets.UTF_8)
                    : "";

            result.put(key, value);
        }

        return result;
    }

    private static String jsonValue(Object value) {

        if (value == null) {
            return "null";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        return jsonString(value.toString());
    }

    private static String jsonString(String value) {

        if (value == null) {
            return "null";
        }

        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private static void send(
            HttpExchange exchange,
            int status,
            String response) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
