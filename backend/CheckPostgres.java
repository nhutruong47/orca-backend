import java.sql.*;

public class CheckPostgres {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        System.out.println("=== USERS ===");
        ResultSet rs = c.createStatement().executeQuery("SELECT id, username, role FROM users");
        while(rs.next()) {
            System.out.println(rs.getString("username") + " (" + rs.getString("role") + ")");
        }
        
        System.out.println("=== TEAMS ===");
        rs = c.createStatement().executeQuery("SELECT id, name FROM teams");
        while(rs.next()) {
            System.out.println(rs.getString("name"));
        }
        c.close();
    }
}
