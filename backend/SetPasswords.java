import java.sql.*;

public class SetPasswords {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        String hash = "$2a$10$3gOAp1BnNUV/b9cbnz6mKejPqXY0qZZtqgiUkGGJCSqCMWGHGbZqe";
        
        String update = "UPDATE users SET password = ? WHERE username IN ('duongminhhieu43', 'duonghoanghung47', 'lyhongtrung36', 'admin')";
        PreparedStatement ps = c.prepareStatement(update);
        ps.setString(1, hash);
        
        int count = ps.executeUpdate();
        System.out.println("Updated passwords for " + count + " users.");
        
        c.close();
    }
}
