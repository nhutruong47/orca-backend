import java.sql.*;
import java.util.UUID;

public class AddOwnerToTeam {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        ResultSet rsTeam = c.createStatement().executeQuery("SELECT id FROM teams WHERE name = 'AnPhu'");
        rsTeam.next();
        String teamIdStr = rsTeam.getString("id");
        
        ResultSet rsUser = c.createStatement().executeQuery("SELECT id FROM users WHERE username = 'nguyen.minh.hai'");
        rsUser.next();
        String userIdStr = rsUser.getString("id");
        
        PreparedStatement checkTm = c.prepareStatement("SELECT id FROM team_members WHERE team_id = ?::uuid AND user_id = ?::uuid");
        checkTm.setString(1, teamIdStr);
        checkTm.setString(2, userIdStr);
        ResultSet rsTm = checkTm.executeQuery();
        if (!rsTm.next()) {
            PreparedStatement insTm = c.prepareStatement("INSERT INTO team_members (id, team_id, user_id, group_role, joined_at) VALUES (?::uuid, ?::uuid, ?::uuid, 'ADMIN', CURRENT_TIMESTAMP)");
            insTm.setString(1, UUID.randomUUID().toString());
            insTm.setString(2, teamIdStr);
            insTm.setString(3, userIdStr);
            insTm.executeUpdate();
            System.out.println("Inserted owner into team_members.");
        } else {
            System.out.println("Owner already in team_members.");
        }
        c.close();
    }
}
