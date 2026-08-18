import java.sql.*;
import java.util.UUID;

public class SeedLogs {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        String insert = "INSERT INTO system_logs (id, actor_id, actor_name, action_type, target_id, details, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP - (random() * interval '7 days'))";
        PreparedStatement ps = c.prepareStatement(insert);
        
        String[][] logs = {
            {"System", "SECURITY_ALERT", "sys-admin-01", "Cảnh báo truy cập trái phép từ IP lạ"},
            {"Nguyễn Minh Hải", "USER_LOGIN", "usr-1029", "Đăng nhập thành công vào hệ thống"},
            {"Trần Quốc Bảo", "PAYMENT_SUCCESS", "pay-8472", "Thanh toán gói Plus thành công qua VNPay"},
            {"Lê Thu Trang", "TEAM_CREATED", "team-9912", "Đăng ký mở xưởng mới: Xưởng Gia công OEM 4"},
            {"Nguyễn Minh Hải", "PASSWORD_RESET", "usr-1029", "Yêu cầu khôi phục mật khẩu"},
            {"Hệ thống", "BACKUP_COMPLETED", "sys-db", "Sao lưu cơ sở dữ liệu định kỳ hoàn tất"},
            {"Võ Ngọc Lan", "USER_LOGIN", "usr-1102", "Đăng nhập thành công vào hệ thống"},
            {"Phạm Hoàng Nam", "PAYMENT_SUCCESS", "pay-8821", "Thanh toán gia hạn gói Enterprise"},
            {"Hệ thống", "SECURITY_ALERT", "sys-fw", "Phát hiện đăng nhập thất bại nhiều lần"}
        };
        
        for (String[] log : logs) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, UUID.randomUUID()); // Fake actor ID
            ps.setString(3, log[0]); // actor_name
            ps.setString(4, log[1]); // action_type
            ps.setString(5, log[2]); // target_id
            ps.setString(6, log[3]); // details
            ps.addBatch();
        }
        
        int[] results = ps.executeBatch();
        System.out.println("Inserted system logs: " + results.length);
        
        c.close();
    }
}
