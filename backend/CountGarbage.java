import java.sql.*;

public class CountGarbage {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM users");
        rs.next();
        System.out.println("Total users: " + rs.getInt(1));
        
        rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM users WHERE email LIKE '%factory%' OR full_name LIKE '%Chủ xưởng%'");
        rs.next();
        System.out.println("Remaining garbage users: " + rs.getInt(1));
        c.close();
    }
}
