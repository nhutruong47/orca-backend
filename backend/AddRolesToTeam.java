import java.sql.*;

public class AddRolesToTeam {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        String metadata = "{\"roles\":[\"Rang xay\", \"Đóng gói\", \"QA / QC\", \"Giao hàng\", \"Phân loại\"]}";
        
        PreparedStatement updateTeam = c.prepareStatement("UPDATE teams SET metadata = ? WHERE name = 'AnPhu'");
        updateTeam.setString(1, metadata);
        int rows = updateTeam.executeUpdate();
        
        if (rows > 0) {
            System.out.println("Successfully added roles to AnPhu team metadata.");
        } else {
            System.out.println("Team AnPhu not found.");
        }
        
        c.close();
    }
}
