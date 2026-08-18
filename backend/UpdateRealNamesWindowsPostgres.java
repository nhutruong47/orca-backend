import java.sql.*;
import java.util.*;

public class UpdateRealNamesWindowsPostgres {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        String[] firstNames = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"};
        String[] middleNames = {"Văn", "Thị", "Minh", "Quốc", "Gia", "Thanh", "Hoàng", "Hữu", "Đức", "Ngọc", "Thu", "Mai", "Xuân", "Hồng", "Tuấn", "Phương"};
        String[] lastNames = {"Anh", "Bình", "Cường", "Dũng", "Dương", "Đạt", "Hải", "Hào", "Hiếu", "Hòa", "Hùng", "Huy", "Khang", "Khánh", "Khoa", "Kiên", "Lâm", "Long", "Nam", "Nghĩa", "Ngọc", "Phong", "Phúc", "Quân", "Quang", "Quốc", "Sơn", "Tài", "Tâm", "Thái", "Thành", "Thiên", "Thịnh", "Tiến", "Toàn", "Trí", "Trung", "Tuấn", "Tùng", "Vinh", "Việt", "Xuân"};
        
        List<String[]> realNames = new ArrayList<>();
        Random r = new Random(42); // deterministic
        for (int i=0; i<100; i++) {
            String f = firstNames[r.nextInt(firstNames.length)];
            String m = middleNames[r.nextInt(middleNames.length)];
            String l = lastNames[r.nextInt(lastNames.length)];
            String fullName = f + " " + m + " " + l;
            String username = normalize(f + m + l) + (r.nextInt(90) + 10);
            String email = normalize(f + "." + m + "." + l) + (r.nextInt(90) + 10) + "@gmail.com";
            realNames.add(new String[]{fullName, username, email});
        }
        
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE email LIKE 'factory%'");
        List<String> ids = new ArrayList<>();
        while (rs.next()) {
            ids.add(rs.getString(1));
        }
        
        System.out.println("Found " + ids.size() + " garbage users to update.");
        
        PreparedStatement updateStmt = c.prepareStatement("UPDATE users SET full_name = ?, username = ?, email = ? WHERE id = ?::uuid");
        for (int i=0; i<ids.size(); i++) {
            String[] data = realNames.get(i % realNames.size());
            updateStmt.setString(1, data[0]);
            updateStmt.setString(2, data[1]);
            updateStmt.setString(3, data[2]);
            updateStmt.setString(4, ids.get(i));
            updateStmt.executeUpdate();
        }
        
        System.out.println("Updated successfully!");
        c.close();
    }
    
    private static String normalize(String str) {
        String s = str.toLowerCase();
        s = s.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        s = s.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        s = s.replaceAll("[ìíịỉĩ]", "i");
        s = s.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        s = s.replaceAll("[ùúụủũưừứựửữ]", "u");
        s = s.replaceAll("[ỳýỵỷỹ]", "y");
        s = s.replaceAll("đ", "d");
        return s.replaceAll("[^a-z0-9.]", "");
    }
}
