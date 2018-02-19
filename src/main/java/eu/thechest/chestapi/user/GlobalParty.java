package eu.thechest.chestapi.user;

import eu.thechest.chestapi.mysql.MySQLManager;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by zeryt on 26.04.2017.
 */
public class GlobalParty {
    public static ArrayList<GlobalParty> STORAGE = new ArrayList<GlobalParty>();

    public static GlobalParty getParty(Player p){
        GlobalParty re = null;

        for(GlobalParty party : STORAGE){
            if(party.leader.toString().equals(p.getUniqueId().toString())){
                re = party;
            } else {
                for(UUID u : party.members){
                    if(u.toString().equals(p.getUniqueId().toString())){
                        re = party;
                    }
                }
            }
        }

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `parties` WHERE (`leader` = ? OR `members` LIKE '%" + p.getUniqueId().toString() + "%') AND `status` = ? ORDER BY `time` DESC LIMIT 1");
            ps.setString(1,p.getUniqueId().toString());
            ps.setString(2,"ALIVE");
            ResultSet rs = ps.executeQuery();
            if(rs.first()){
                ArrayList<UUID> members = new ArrayList<UUID>();

                int id = rs.getInt("id");
                UUID creator = UUID.fromString(rs.getString("creator"));
                UUID leader = UUID.fromString(rs.getString("leader"));

                if(rs.getString("members") != null && !rs.getString("members").isEmpty()){
                    for(String s : rs.getString("members").split(",")){
                        members.add(UUID.fromString(s));
                    }
                }

                re = new GlobalParty(id,creator,leader,members);
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
        }

        return re;
    }

    public int id;
    public UUID creator;
    public UUID leader;
    public ArrayList<UUID> members;

    public GlobalParty(int id, UUID creator, UUID leader, ArrayList<UUID> members){
        this.id = id;
        this.creator = creator;
        this.leader = leader;
        this.members = members;

        STORAGE.add(this);
    }
}
