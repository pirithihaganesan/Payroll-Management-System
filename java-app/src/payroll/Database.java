package payroll;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens the SQLite connection and creates the schema on first run. */
public class Database implements AutoCloseable {
    private final Connection connection;

    public Database(String dbFile) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /** True when the employee table already exists. */
    public boolean isInitialised() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='employee'")) {
            return rs.next();
        }
    }

    /** Runs a .sql script (statements separated by semicolons; -- comments ignored). */
    public void runScript(Path script) throws SQLException, IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(script, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (!t.startsWith("--")) {
                sb.append(line).append('\n');
            }
        }
        for (String stmt : sb.toString().split(";")) {
            if (!stmt.trim().isEmpty()) {
                try (Statement st = connection.createStatement()) {
                    st.execute(stmt);
                }
            }
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
