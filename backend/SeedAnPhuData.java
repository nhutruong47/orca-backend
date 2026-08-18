import java.sql.*;
import java.util.UUID;

public class SeedAnPhuData {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/orca_db", "postgres", "123456");
        
        String hash = "$2a$10$3gOAp1BnNUV/b9cbnz6mKejPqXY0qZZtqgiUkGGJCSqCMWGHGbZqe";
        c.createStatement().execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        c.createStatement().execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role::text = ANY (ARRAY['ADMIN'::text, 'MEMBER'::text, 'FACTORY_OWNER'::text]))");
        
        // 1. Ensure Manager
        PreparedStatement checkMgr = c.prepareStatement("SELECT id FROM users WHERE username = ?");
        checkMgr.setString(1, "nguyen.minh.hai");
        ResultSet rsMgr = checkMgr.executeQuery();
        String mgrIdStr;
        if (rsMgr.next()) {
            mgrIdStr = rsMgr.getString("id");
            PreparedStatement updateMgr = c.prepareStatement("UPDATE users SET password = ?, role = 'FACTORY_OWNER', full_name = 'Nguyễn Minh Hải' WHERE id = ?::uuid");
            updateMgr.setString(1, hash);
            updateMgr.setString(2, mgrIdStr);
            updateMgr.executeUpdate();
            System.out.println("Updated manager.");
        } else {
            mgrIdStr = UUID.randomUUID().toString();
            PreparedStatement insMgr = c.prepareStatement("INSERT INTO users (id, username, password, role, chip_id, locked, full_name, created_at) VALUES (?::uuid, ?, ?, 'FACTORY_OWNER', ?, false, ?, CURRENT_TIMESTAMP)");
            insMgr.setString(1, mgrIdStr);
            insMgr.setString(2, "nguyen.minh.hai");
            insMgr.setString(3, hash);
            insMgr.setString(4, "USR-" + UUID.randomUUID());
            insMgr.setString(5, "Nguyễn Minh Hải");
            insMgr.executeUpdate();
            System.out.println("Inserted manager.");
        }
        
        // 2. Ensure Team
        PreparedStatement checkTeam = c.prepareStatement("SELECT id FROM teams WHERE name = 'AnPhu'");
        ResultSet rsTeam = checkTeam.executeQuery();
        String teamIdStr;
        if (rsTeam.next()) {
            teamIdStr = rsTeam.getString("id");
            System.out.println("Found team AnPhu.");
        } else {
            teamIdStr = UUID.randomUUID().toString();
            PreparedStatement insTeam = c.prepareStatement("INSERT INTO teams (id, name, owner_id, is_published, is_verified, created_at) VALUES (?::uuid, ?, ?::uuid, false, false, CURRENT_TIMESTAMP)");
            insTeam.setString(1, teamIdStr);
            insTeam.setString(2, "AnPhu");
            insTeam.setString(3, mgrIdStr);
            insTeam.executeUpdate();
            System.out.println("Inserted team AnPhu.");
        }
        
        // 3. Add Members
        String[] fullNames = {"Trần Quốc Bảo", "Lê Thu Trang", "Võ Ngọc Lan", "Phạm Hoàng Nam", "Đỗ Thành Công", "Bùi Anh Tuấn", "Hoàng Mai Phương", "Phan Đức Long"};
        String[] userNames = {"tran.quoc.bao", "le.thu.trang", "vo.ngoc.lan", "pham.hoang.nam", "do.thanh.cong", "bui.anh.tuan", "hoang.mai.phuong", "phan.duc.long"};
        
        PreparedStatement checkUsr = c.prepareStatement("SELECT id FROM users WHERE username = ?");
        PreparedStatement insUsr = c.prepareStatement("INSERT INTO users (id, username, password, role, chip_id, locked, full_name, created_at) VALUES (?::uuid, ?, ?, 'MEMBER', ?, false, ?, CURRENT_TIMESTAMP)");
        PreparedStatement checkTm = c.prepareStatement("SELECT id FROM team_members WHERE team_id = ?::uuid AND user_id = ?::uuid");
        PreparedStatement insTm = c.prepareStatement("INSERT INTO team_members (id, team_id, user_id, group_role, joined_at) VALUES (?::uuid, ?::uuid, ?::uuid, 'MEMBER', CURRENT_TIMESTAMP)");
        
        for (int i=0; i<userNames.length; i++) {
            checkUsr.setString(1, userNames[i]);
            ResultSet rsU = checkUsr.executeQuery();
            String uidStr;
            if (rsU.next()) {
                uidStr = rsU.getString("id");
                PreparedStatement upU = c.prepareStatement("UPDATE users SET password = ?, full_name = ? WHERE id = ?::uuid");
                upU.setString(1, hash);
                upU.setString(2, fullNames[i]);
                upU.setString(3, uidStr);
                upU.executeUpdate();
            } else {
                uidStr = UUID.randomUUID().toString();
                insUsr.setString(1, uidStr);
                insUsr.setString(2, userNames[i]);
                insUsr.setString(3, hash);
                insUsr.setString(4, "USR-" + UUID.randomUUID());
                insUsr.setString(5, fullNames[i]);
                insUsr.executeUpdate();
            }
            
            checkTm.setString(1, teamIdStr);
            checkTm.setString(2, uidStr);
            ResultSet rsTm = checkTm.executeQuery();
            if (!rsTm.next()) {
                insTm.setString(1, UUID.randomUUID().toString());
                insTm.setString(2, teamIdStr);
                insTm.setString(3, uidStr);
                insTm.executeUpdate();
            }
        }
        System.out.println("Inserted members.");
        c.close();
    }
}
