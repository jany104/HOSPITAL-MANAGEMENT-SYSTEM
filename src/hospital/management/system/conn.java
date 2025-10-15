package hospital.management.system;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class conn implements AutoCloseable {

    Connection connection;
    Statement statement;

    public conn() {
        Properties properties = loadProperties();
        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to the database. Verify db.properties and MySQL availability.", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found on the classpath.", e);
        }

    }

    private Properties loadProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("db.url", "jdbc:mysql://localhost:3306/hospital_management_system");
        defaults.setProperty("db.user", "root");
        defaults.setProperty("db.password", "AyushVish");

        Properties properties = new Properties(defaults);
        try (InputStream inputStream = locateConfigStream()) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load database configuration from db.properties", e);
        }
        return properties;
    }

    private InputStream locateConfigStream() throws IOException {
        Path externalFile = Paths.get("db.properties");
        if (Files.exists(externalFile)) {
            return Files.newInputStream(externalFile);
        }
        return conn.class.getClassLoader().getResourceAsStream("db.properties");
    }

    @Override
    public void close() {
        try {
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
        } catch (SQLException ignored) {
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

}
