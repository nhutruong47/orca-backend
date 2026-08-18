import java.sql.*;

public class PrintUpdated {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        ResultSet rs = c.createStatement().executeQuery("SELECT full_name, email FROM users WHERE role = 'FACTORY_OWNER' AND email != 'minh.tuan@gmail.com' AND email != 'hoang.yen@gmail.com' AND email != 'quoc.bao@gmail.com' LIMIT 5");
        while(rs.next()) {
            System.out.println(rs.getString(1) + " - " + rs.getString(2));
        }
        c.close();
    }
}
