import java.sql.*;
import java.util.UUID;
import java.util.Random;

public class SeedOrders {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        // 1. Get AnPhu team ID (buyer this time)
        String buyerTeamId = null;
        ResultSet rsTeam = c.createStatement().executeQuery("SELECT id FROM teams WHERE name = 'AnPhu'");
        if (rsTeam.next()) buyerTeamId = rsTeam.getString(1);
        
        if (buyerTeamId == null) {
            System.out.println("Could not find AnPhu team!");
            return;
        }

        // 2. Get seller teams
        String[] sellerIds = new String[5];
        ResultSet rsSellers = c.createStatement().executeQuery("SELECT id FROM teams WHERE name != 'AnPhu' LIMIT 5");
        int sIdx = 0;
        while (rsSellers.next() && sIdx < 5) {
            sellerIds[sIdx++] = rsSellers.getString(1);
        }

        if (sIdx == 0) {
            System.out.println("Could not find any other teams!");
            return;
        }

        // 3. Get any user in AnPhu for buyer_user_id
        String buyerUserId = null;
        ResultSet rsUser = c.prepareStatement("SELECT user_id FROM team_members WHERE team_id = '" + buyerTeamId + "' LIMIT 1").executeQuery();
        if (rsUser.next()) buyerUserId = rsUser.getString(1);

        String insertOrder = "INSERT INTO inter_group_orders (" +
            "id, buyer_team_id, buyer_user_id, seller_team_id, title, description, quantity, " +
            "status, created_at, material_source, services, product_type, unit, " +
            "buyer_viewed, seller_viewed) " +
            "VALUES (?, ?::uuid, ?::uuid, ?::uuid, ?, ?, ?, ?, CURRENT_TIMESTAMP - (random() * interval '5 days'), ?, ?, ?, ?, false, true)";
            
        PreparedStatement ps = c.prepareStatement(insertOrder);
        
        Object[][] ordersData = {
            {"Mua vỏ bao bì in sẵn", "Bao bì zipper 500g đen nhám logo An Phú", 5000, "CONFIRMED", "FACTORY_PROVIDED", "PACKAGING", "Bao bì", "túi"},
            {"Gia công xử lý mật ong 2 tấn", "Cà phê tươi xử lý Honey process", 2000, "RFQ_CREATED", "CUSTOMER_PROVIDED", "PROCESSING", "Arabica", "kg"},
            {"Mua 500kg Cà phê nhân Robusta", "Nhân xô Robusta loại 1 S18", 500, "DELIVERED", "FACTORY_PROVIDED", "RAW_MATERIAL", "Robusta", "kg"},
            {"Thuê kho bảo quản 10 tấn", "Gửi kho lạnh bảo quản cà phê nhân", 10000, "QUOTED", "CUSTOMER_PROVIDED", "WAREHOUSING", "Nhân xô", "kg"},
            {"Gia công rang 200kg Culi", "Rang Dark Roast Culi", 200, "IN_PRODUCTION", "CUSTOMER_PROVIDED", "ROASTING", "Culi", "kg"},
            {"Mua thùng carton đóng gói", "Thùng carton in logo 3 lớp", 1000, "SHIPPING", "FACTORY_PROVIDED", "PACKAGING", "Thùng", "cái"}
        };
        
        Random rand = new Random();
        
        for (Object[] row : ordersData) {
            String randomSeller = sellerIds[rand.nextInt(sIdx)];
            
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, buyerTeamId);
            ps.setString(3, buyerUserId);
            ps.setString(4, randomSeller);
            ps.setString(5, (String) row[0]); // title
            ps.setString(6, (String) row[1]); // desc
            ps.setInt(7, (Integer) row[2]);   // qty
            ps.setString(8, (String) row[3]); // status
            ps.setString(9, (String) row[4]); // source
            ps.setString(10, (String) row[5]); // services
            ps.setString(11, (String) row[6]); // product_type
            ps.setString(12, (String) row[7]); // unit
            ps.addBatch();
        }

        int[] results = ps.executeBatch();
        System.out.println("Inserted " + results.length + " outgoing orders.");
        
        c.close();
    }
}
