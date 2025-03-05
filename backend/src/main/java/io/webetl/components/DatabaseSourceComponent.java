package io.webetl.components;

import io.webetl.annotation.ETLComponentDefinition;
import io.webetl.model.component.SourceComponent;
import io.webetl.model.component.parameter.StringParameter;
import io.webetl.model.component.parameter.SelectParameter;
import io.webetl.model.component.parameter.SecretParameter;
import io.webetl.model.component.parameter.SQLParameter;
import io.webetl.model.data.Row;
import io.webetl.runtime.ExecutionContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ETLComponentDefinition(
    id = "database-source",
    label = "Database Source",
    description = "Reads data from a database",
    icon = "DatabaseIcon",
    backgroundColor = "#e3f2fd"
)
public class DatabaseSourceComponent extends SourceComponent {
    
    // Constants for database drivers and URL templates
    private static final String POSTGRES_DRIVER = "org.postgresql.Driver";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    private static final String POSTGRES_URL_TEMPLATE = "jdbc:postgresql://%s:%s/%s";
    private static final String MYSQL_URL_TEMPLATE = "jdbc:mysql://%s:%s/%s";
    
    public DatabaseSourceComponent() {
        // Add database type parameter (dropdown)
        getParameters().add(SelectParameter.builder()
            .name("dbType")
            .label("Database Type")
            .description("Select the type of database to connect to")
            .options(Arrays.asList("PostgreSQL", "MySQL"))
            .defaultValue("PostgreSQL")
            .required(true)
            .build());
            
        // Add host parameter
        getParameters().add(StringParameter.builder()
            .name("host")
            .label("Host")
            .description("Database server hostname or IP address")
            .required(true)
            .maxLength(255)
            .build());
            
        // Add port parameter with default values based on common database ports
        getParameters().add(StringParameter.builder()
            .name("port")
            .label("Port")
            .description("Database server port")
            .required(true)
            .maxLength(10)
            .build());
            
        // Add database name parameter
        getParameters().add(StringParameter.builder()
            .name("database")
            .label("Database Name")
            .description("Name of the database to connect to")
            .required(true)
            .maxLength(255)
            .build());
            
        // Add username parameter
        getParameters().add(StringParameter.builder()
            .name("username")
            .label("Username")
            .description("Database authentication username")
            .required(true)
            .maxLength(100)
            .build());
            
        // Add password parameter (secret)
        getParameters().add(SecretParameter.builder()
            .name("password")
            .label("Password")
            .description("Database authentication password")
            .required(true)
            .build());
            
        // Add query parameter (SQL)
        getParameters().add(SQLParameter.builder()
            .name("query")
            .label("SQL Query")
            .description("SQL query to execute (SELECT statements only)")
            .required(true)
            .maxRows(1000)
            .build());
    }

    @Override
    protected void executeComponent(ExecutionContext context) throws Exception {
        // Log the start of execution
        info(context, "Starting database source component");
        
        // Get parameters
        String dbType = getParameter("dbType", String.class);
        String host = getParameter("host", String.class);
        String port = getParameter("port", String.class);
        String database = getParameter("database", String.class);
        String username = getParameter("username", String.class);
        String password = getParameter("password", String.class);
        String query = getParameter("query", String.class);
        
        // Set default values if parameters are null or empty
        if (dbType == null || dbType.isEmpty()) {
            dbType = "PostgreSQL";
            info(context, "Using default database type: PostgreSQL");
        }
        
        if (host == null || host.isEmpty()) {
            host = "localhost";
            info(context, "Using default host: localhost");
        }
        
        if (database == null || database.isEmpty()) {
            database = "postgres";
            info(context, "Using default database name: postgres");
        }
        
        // Default ports if not specified
        if (port == null || port.isEmpty()) {
            port = "PostgreSQL".equals(dbType) ? "5432" : "3306";
            info(context, "Using default port: " + port);
        }
        
        if (username == null || username.isEmpty()) {
            username = "postgres";
            info(context, "Using default username: postgres");
        }
        
        // Password should be provided by the user, but we can handle nulls
        if (password == null) {
            password = "";
            warn(context, "No password provided, using empty password");
        }
        
        if (query == null || query.isEmpty()) {
            query = "SELECT 1";
            warn(context, "No query provided, using default query: " + query);
        }
        
        // Build connection string
        String url = buildConnectionString(dbType, host, port, database);
        info(context, "Executing query: " + query);
        info(context, "Using connection URL: " + url);
        
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        
        try {
            // Load database driver
            loadDatabaseDriver(dbType);
            
            // Establish database connection
            info(context, "Establishing database connection...");
            connection = DriverManager.getConnection(url, username, password);
            
            // Create and execute statement
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Get column names
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            
            info(context, "Query executed successfully. Processing results...");
            
            // Process result set and send rows
            int rowCount = 0;
            while (resultSet.next()) {
                Row row = new Row();
                row.setId(UUID.randomUUID().toString());
                
                Map<String, Object> data = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    data.put(columnName, value);
                }
                
                row.setData(data);
                rowCount++;
                
                if (rowCount % 100 == 0) {
                    info(context, "Processed " + rowCount + " rows");
                }
                
                debug(context, "Sending row to output: " + row);
                super.sendRow(row);
            }
            
            info(context, "Query execution complete. Total rows processed: " + rowCount);
            
            // Send terminator row
            info(context, "Sending terminator row");
            super.sendRow(Row.createTerminator());
            
        } catch (SQLException e) {
            error(context, "Database error: " + e.getMessage(), e);
            throw e;
        } catch (ClassNotFoundException e) {
            error(context, "Database driver not found: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            error(context, "Error executing database query: " + e.getMessage(), e);
            throw e;
        } finally {
            // Close resources
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }
    
    /**
     * Build a connection string based on database type and parameters
     */
    private String buildConnectionString(String dbType, String host, String port, String database) {
        // Use a default value if dbType is null
        if (dbType == null) {
            dbType = "PostgreSQL";
        }
        
        String template = "PostgreSQL".equals(dbType) ? POSTGRES_URL_TEMPLATE : MYSQL_URL_TEMPLATE;
        
        // Provide defaults for any null values
        host = (host != null) ? host : "localhost";
        port = (port != null) ? port : ("PostgreSQL".equals(dbType) ? "5432" : "3306");
        database = (database != null) ? database : ("PostgreSQL".equals(dbType) ? "postgres" : "mysql");
        
        return String.format(template, host, port, database);
    }
    
    /**
     * Load the appropriate database driver
     */
    private void loadDatabaseDriver(String dbType) throws ClassNotFoundException {
        // Use a default value if dbType is null
        if (dbType == null) {
            dbType = "PostgreSQL";
        }
        
        String driverClass = "PostgreSQL".equals(dbType) ? POSTGRES_DRIVER : MYSQL_DRIVER;
        Class.forName(driverClass);
    }
    
    /**
     * Close a resource quietly (without throwing exceptions)
     */
    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Ignore exceptions during close
            }
        }
    }
    
    private <T> T getParameter(String name, Class<T> type) {
        return (T) getParameters().stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .map(p -> p.getValue())
            .orElse(null);
    }
} 