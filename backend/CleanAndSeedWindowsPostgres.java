import java.sql.*;
import java.util.UUID;

public class CleanAndSeedWindowsPostgres {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        System.out.println("Deleting old garbage users...");
        int deleted = c.createStatement().executeUpdate("DELETE FROM users WHERE email LIKE 'factory%'");
        System.out.println("Deleted " + deleted + " garbage users.");
        
        try {
            c.createStatement().execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            c.createStatement().execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role::text = ANY (ARRAY['ADMIN'::text, 'MEMBER'::text, 'FACTORY_OWNER'::text]))");
            System.out.println("Updated users_role_check constraint to allow FACTORY_OWNER.");
        } catch(Exception e) {
            System.out.println("Could not alter role constraint: " + e.getMessage());
        }
        
        String hash = "$2a$10$D26MSIVWtB2CRQaBARdM7uyKBzEbEjNNdb7V5MtOcfsEcCU0jkP8."; // 123456
        String[] names = {"Nguyễn Minh Tuấn", "Trần Thị Hoàng Yến", "Lê Quốc Bảo", "Đỗ Quang Khải", "Vũ Nhật Nam"};
        String[] usernames = {"minh.tuan", "hoang.yen", "quoc.bao", "quang.khai", "nhat.nam"};
        String[] emails = {"minh.tuan@gmail.com", "hoang.yen@gmail.com", "quoc.bao@gmail.com", "quang.khai@gmail.com", "nhat.nam@gmail.com"};
        
        PreparedStatement stmt = c.prepareStatement("INSERT INTO users (id, username, password, role, chip_id, locked, full_name, email, created_at) VALUES (?::uuid, ?, ?, 'FACTORY_OWNER', ?, false, ?, ?, CURRENT_TIMESTAMP) ON CONFLICT (username) DO NOTHING");
        
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
