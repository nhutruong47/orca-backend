import java.sql.*;
public class ViewH2 {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection("jdbc:h2:file:C:/Users/TTN/Downloads/orcaktx/orca-backend/data/orca-local;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE", "sa", "12345");
        ResultSet rs = c.createStatement().executeQuery("SELECT id, username, email FROM users");
        while(rs.next()) {
            System.out.println(rs.getString(2) + " - " + rs.getString(3));
        }
        c.close();
    }
}
