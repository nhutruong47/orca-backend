import java.sql.*;

public class GetAccounts {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        String query = "SELECT t.name, u.username FROM teams t JOIN users u ON t.owner_id = u.id WHERE t.name IN ('Xưởng Rang cà phê 7', 'Xưởng Xử lý sau thu hoạch 20', 'Xưởng Cung ứng cà phê nhân 6', 'Xưởng Rang Đắk Lắk')";
        PreparedStatement ps = c.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            System.out.println(rs.getString(1) + " : " + rs.getString(2));
        }
        
        c.close();
    }
}
