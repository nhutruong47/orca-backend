import java.sql.*;

public class FakeAddress {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        String update = "UPDATE teams SET business_address = ?, business_license = ? WHERE name = ?";
        PreparedStatement ps = c.prepareStatement(update);
        
        String[][] data = {
            {"Xưởng Rang cà phê 7", "234/12 Lê Trọng Tấn, Phường Tây Thạnh, Quận Tân Phú, TP. HCM", "0314298172"},
            {"Xưởng Cung ứng cà phê nhân 1", "Lô C2, KCN Lộc Sơn, Phường Lộc Sơn, TP. Bảo Lộc, Lâm Đồng", "5801239845"},
            {"Xưởng Gia công OEM 4", "Số 45 Đường số 8, KCN Sóng Thần 1, Phường Dĩ An, TP. Dĩ An, Bình Dương", "3702118745"}
        };
        
        for (String[] row : data) {
            ps.setString(1, row[1]);
            ps.setString(2, row[2]);
            ps.setString(3, row[0]);
            ps.addBatch();
        }
        
        int[] results = ps.executeBatch();
        System.out.println("Updated rows: " + results.length);
        
        c.close();
    }
}
