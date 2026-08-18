import java.sql.*;

public class ListNullUsers {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        ResultSet rs = c.createStatement().executeQuery("SELECT username, role, email FROM users WHERE email IS NULL OR email LIKE 'factory%' LIMIT 30");
        while(rs.next()) {
            System.out.println(rs.getString(1) + " - " + rs.getString(2) + " - " + rs.getString(3));
        }
        c.close();
    }
}
