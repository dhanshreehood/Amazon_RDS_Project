import java.sql.*;
import java.math.BigDecimal;

public class RDS {
    private Connection con;
    private final String url = "database-1.c7qkcsa8kotu.ap-south-1.rds.amazonaws.com:3306";
    private final String uid = "admin";
    private final String pw = "Gowtham190";
    private final String dbName = "mydb";

    public static void main(String[] args) {
        RDS app = new RDS();

        try {
            app.connect();
            app.drop();
            app.create();
            app.insert();
            app.delete();

            System.out.println("Query One Results:");
            System.out.println(resultSetToString(app.queryOne(), 100));

            System.out.println("\nQuery Two Results:");
            System.out.println(resultSetToString(app.queryTwo(), 100));

            System.out.println("\nQuery Three Results:");
            System.out.println(resultSetToString(app.queryThree(), 100));

            app.close();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Establish connection to the database
    public void connect() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        System.out.println("Connecting to MySQL server...");
        String initialJdbcUrl = "jdbc:mysql://" + url + "?user=" + uid + "&password=" + pw;
        con = DriverManager.getConnection(initialJdbcUrl);
        System.out.println("Connected to MySQL server successfully!");

        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
        }

        con.close();
        String jdbcUrl = "jdbc:mysql://" + url + "/" + dbName + "?user=" + uid + "&password=" + pw;
        System.out.println("Connecting to database " + dbName + "...");
        con = DriverManager.getConnection(jdbcUrl);
        System.out.println("Connected to the database successfully!");
    }

    // Drop tables if they exist
    public void drop() throws SQLException {
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS stockprice");
            stmt.executeUpdate("DROP TABLE IF EXISTS company");
            System.out.println("Tables dropped successfully.");
        }
    }

    // Create the tables
    public void create() throws SQLException {
        String createCompanyTable = """
            CREATE TABLE company (
                id INT PRIMARY KEY,
                name VARCHAR(50),
                ticker CHAR(10),
                annualRevenue DECIMAL(15,2),
                numEmployees INT
            )
        """;

        String createStockPriceTable = """
            CREATE TABLE stockprice (
                companyId INT,
                priceDate DATE,
                openPrice DECIMAL(10,2),
                highPrice DECIMAL(10,2),
                lowPrice DECIMAL(10,2),
                closePrice DECIMAL(10,2),
                volume INT,
                PRIMARY KEY (companyId, priceDate),
                FOREIGN KEY (companyId) REFERENCES company(id)
            )
        """;

        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(createCompanyTable);
            stmt.executeUpdate(createStockPriceTable);
            System.out.println("Tables created successfully.");
        }
    }

    // Insert records into tables
    public void insert() throws SQLException {
        String insertCompanySQL = """
            INSERT INTO company (id, name, ticker, annualRevenue, numEmployees) VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = con.prepareStatement(insertCompanySQL)) {
            Object[][] companies = {
                {1, "Apple", "AAPL", "387540000000.00", 154000},
                {2, "GameStop", "GME", "611000000.00", 12000},
                {3, "Handy Repair", null, "2000000.00", 50},
                {4, "Microsoft", "MSFT", "198270000000.00", 221000},
                {5, "StartUp", null, "50000.00", 3}
            };

            for (Object[] company : companies) {
                pstmt.setInt(1, (int) company[0]);
                pstmt.setString(2, (String) company[1]);
                pstmt.setString(3, company[2] == null ? null : (String) company[2]);
                pstmt.setBigDecimal(4, new BigDecimal((String) company[3]));
                pstmt.setInt(5, (int) company[4]);
                pstmt.executeUpdate();
            }
        }

        String insertStockPriceSQL = """
            INSERT INTO stockprice (companyId, priceDate, openPrice, highPrice, lowPrice, closePrice, volume)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = con.prepareStatement(insertStockPriceSQL)) {
            Object[][] stockPrices = {
                {1, "2022-08-15", 171.52, 173.39, 171.35, 173.19, 54091700},
                // Add all the stock prices here...
            };

            for (Object[] record : stockPrices) {
                pstmt.setInt(1, (int) record[0]);
                pstmt.setDate(2, Date.valueOf((String) record[1]));
                pstmt.setBigDecimal(3, BigDecimal.valueOf((double) record[2]));
                pstmt.setBigDecimal(4, BigDecimal.valueOf((double) record[3]));
                pstmt.setBigDecimal(5, BigDecimal.valueOf((double) record[4]));
                pstmt.setBigDecimal(6, BigDecimal.valueOf((double) record[5]));
                pstmt.setInt(7, (int) record[6]);
                pstmt.executeUpdate();
            }
        }
        System.out.println("Data inserted successfully.");
    }

    // Delete records based on conditions
    public void delete() throws SQLException {
        String deleteSQL = "DELETE FROM stockprice WHERE priceDate < ? OR companyId = ?";
        try (PreparedStatement pstmt = con.prepareStatement(deleteSQL)) {
            pstmt.setDate(1, Date.valueOf("2022-08-20"));
            pstmt.setInt(2, 2);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Deleted " + rowsAffected + " rows from stockprice table.");
        }
    }

    // Query One
    public ResultSet queryOne() throws SQLException {
        String querySQL = """
            SELECT name, annualRevenue, numEmployees
            FROM company
            WHERE numEmployees > 10000 OR annualRevenue < 1000000
            ORDER BY name ASC
        """;
        return con.createStatement().executeQuery(querySQL);
    }

    // Query Two
    public ResultSet queryTwo() throws SQLException {
        String querySQL = """
            SELECT c.name, c.ticker,
                   MIN(sp.lowPrice) AS lowest_price,
                   MAX(sp.highPrice) AS highest_price,
                   AVG(sp.closePrice) AS avg_closing_price,
                   AVG(sp.volume) AS avg_volume
            FROM company c
            JOIN stockprice sp ON c.id = sp.companyId
            WHERE sp.priceDate BETWEEN '2022-08-22' AND '2022-08-26'
            GROUP BY c.name, c.ticker
            ORDER BY avg_volume DESC
        """;
        return con.createStatement().executeQuery(querySQL);
    }

    // Query Three
    public ResultSet queryThree() throws SQLException {
        String querySQL = """
            SELECT c.name, c.ticker, sp2.closePrice
            FROM company c
            LEFT JOIN (
                SELECT companyId, AVG(closePrice) AS avg_close_price
                FROM stockprice
                WHERE priceDate BETWEEN '2022-08-15' AND '2022-08-19'
                GROUP BY companyId
            ) sp1 ON c.id = sp1.companyId
            LEFT JOIN (
                SELECT companyId, closePrice
                FROM stockprice
                WHERE priceDate = '2022-08-30'
            ) sp2 ON c.id = sp2.companyId
            WHERE c.ticker IS NULL OR (sp2.closePrice >= sp1.avg_close_price * 0.9)
            ORDER BY c.name ASC
        """;
        return con.createStatement().executeQuery(querySQL);
    }

    // Helper to convert ResultSet to String
    public static String resultSetToString(ResultSet rst, int maxrows) throws SQLException {
        StringBuilder buf = new StringBuilder();
        ResultSetMetaData meta = rst.getMetaData();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            buf.append(meta.getColumnName(i));
            if (i < columnCount) buf.append(", ");
        }
        buf.append('\n');

        int rowCount = 0;
        while (rst.next() && rowCount < maxrows) {
            for (int i = 1; i <= columnCount; i++) {
                buf.append(rst.getObject(i));
                if (i < columnCount) buf.append(", ");
            }
            buf.append('\n');
            rowCount++;
        }
        buf.append("Total results: ").append(rowCount);
        return buf.toString();
    }

    // Close the connection
    public void close() throws SQLException {
        if (con != null) {
            con.close();
            System.out.println("Connection closed.");
        }
    }
}
