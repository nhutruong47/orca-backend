import java.sql.*;
import java.util.UUID;

public class RunH2Correct {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        // Connect to the CORRECT H2 database
        String url = "jdbc:h2:file:C:/Users/TTN/Downloads/orcaktx/orca-backend/data/orca-local;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        Connection c = DriverManager.getConnection(url, "sa", "12345");
        
        System.out.println("Deleting old garbage users...");
        int deleted = c.createStatement().executeUpdate("DELETE FROM users WHERE email LIKE 'factory%'");
        System.out.println("Deleted " + deleted + " garbage users.");
        
        try {
            c.createStatement().execute("ALTER TABLE users ALTER COLUMN role SET DATA TYPE VARCHAR(255)");
            System.out.println("Role column altered to VARCHAR.");
        } catch(Exception e) {
            System.out.println("Could not alter role column, it might already be VARCHAR.");
        }
        
        String hash = "$2a$10$D26MSIVWtB2CRQaBARdM7uyKBzEbEjNNdb7V5MtOcfsEcCU0jkP8."; // 123456
        String[] names = {"Nguyễn Trần Gia Bảo", "Lê Thị Thu Thủy", "Phạm Hoàng Sơn", "Đỗ Quang Khải", "Vũ Nhật Nam"};
        String[] usernames = {"baonguyen", "thuthuyle", "sonpham", "khaido", "namvu"};
        String[] emails = {"gia.bao.nguyen@gmail.com", "thuy.le.thu@gmail.com", "hoangson.pham@gmail.com", "quangkhai.do@gmail.com", "nhatnam.vu@gmail.com"};
        
        PreparedStatement stmt = c.prepareStatement("INSERT INTO users (id, username, password, role, chip_id, locked, full_name, email, created_at) VALUES (?, ?, ?, 'FACTORY_OWNER', ?, false, ?, ?, CURRENT_TIMESTAMP)");
        
        for (int i = 0; i < names.length; i++) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, usernames[i]);
            stmt.setString(3, hash);
            stmt.setString(4, "USR-" + UUID.randomUUID().toString());
            stmt.setString(5, names[i]);
            stmt.setString(6, emails[i]);
            stmt.executeUpdate();
        }
        
        System.out.println("Inserted " + names.length + " realistic users.");
        c.close();
    }
}
