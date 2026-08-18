import java.sql.*;

public class CheckH2 {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection("jdbc:h2:file:../data/orca-local;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE", "sa", "12345");
        
        System.out.println("=== USERS IN H2 ===");
        ResultSet rs = c.createStatement().executeQuery("SELECT id, username, role FROM users");
        while(rs.next()) {
            System.out.println(rs.getString("username") + " (" + rs.getString("role") + ")");
        }
        
        System.out.println("=== TEAMS IN H2 ===");
        rs = c.createStatement().executeQuery("SELECT id, name FROM teams");
        while(rs.next()) {
            System.out.println(rs.getString("name"));
        }
        c.close();
    }
}
