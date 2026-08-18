import java.sql.*;
import java.util.UUID;

public class SeedInventory {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        // Find team AnPhu
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT id FROM teams WHERE name = 'AnPhu'");
        if (!rs.next()) {
            System.out.println("Team AnPhu not found!");
            return;
        }
        String teamId = rs.getString(1);
        
        String insert = "INSERT INTO inventory_items (id, team_id, product_type, product_state, quantity, unit, low_stock_threshold, last_updated, version, is_featured) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0, false)";
        PreparedStatement ps = c.prepareStatement(insert);
        
        String[][] items = {
            {"Arabica", "GREEN", "150", "kg", "50"},
            {"Robusta", "GREEN", "250", "kg", "50"},
            {"Arabica (Light Roast)", "ROASTED", "50", "kg", "10"},
            {"Robusta (Dark Roast)", "ROASTED", "80", "kg", "10"},
            {"Bao bì 500g", "PACKAGED", "1000", "cái", "200"}
        };
        
        for (String[] item : items) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, UUID.fromString(teamId));
            ps.setString(3, item[0]);
            ps.setString(4, item[1]);
            ps.setDouble(5, Double.parseDouble(item[2]));
            ps.setString(6, item[3]);
            ps.setDouble(7, Double.parseDouble(item[4]));
            ps.addBatch();
        }
        
        ps.executeBatch();
        c.close();
        System.out.println("Inventory seeded.");
    }
}
