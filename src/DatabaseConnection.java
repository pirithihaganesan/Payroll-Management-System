
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    public static void main(String[] args) {

        String url = "jdbc:sqlite:database/payroll.db";

        try (Connection connection =
                 DriverManager.getConnection(url)) {

            System.out.println("SQLite database connected successfully!");

            String sql = """
                CREATE TABLE IF NOT EXISTS employee (
                    employee_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    department TEXT NOT NULL,
                    designation TEXT NOT NULL,
                    basic_salary REAL NOT NULL
                )
                """;

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }

            System.out.println("Employee table created successfully!");

        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}
