package eu.thechest.chestapi.util;

import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.user.Rank;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by zeryt on 25.02.2017.
 */
public class PlayerUtilities {
    public static HashMap<String,UUID> NAME_UUID_CACHE = new HashMap<String,UUID>();
    public static HashMap<UUID,String> UUID_NAME_CACHE = new HashMap<UUID,String>();
    public static HashMap<UUID,Rank> UUID_RANK_CACHE = new HashMap<UUID,Rank>();
    public static HashMap<UUID,ArrayList<String>> UUID_FRIENDS_CACHE = new HashMap<UUID,ArrayList<String>>();
    public static HashMap<UUID,CrewTagData> UUID_CREWTAG_CACHE = new HashMap<UUID,CrewTagData>();

    public static ArrayList<String> getFriendsFromUUID(UUID uuid){
        if(uuid == null) return null;
        ArrayList<String> friends = null;

        if(UUID_FRIENDS_CACHE.containsKey(uuid)){
            friends = UUID_FRIENDS_CACHE.get(uuid);
        } else {
            try {
                friends = new ArrayList<String>();

                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `friendships` WHERE `player`=? OR `friend`=?");
                ps.setString(1,uuid.toString());
                ps.setString(2,uuid.toString());
                ResultSet rs = ps.executeQuery();
                rs.beforeFirst();

                while(rs.next()){
                    if(rs.getString("player").equals(uuid.toString())){
                        friends.add(rs.getString("friend"));
                    } else if(rs.getString("friend").equals(uuid.toString())){
                        friends.add(rs.getString("player"));
                    }
                }

                UUID_FRIENDS_CACHE.put(uuid,friends);

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        return friends;
    }

    public static CrewTagData getCrewTagFromUUID(UUID uuid){
        if(uuid == null) return null;
        CrewTagData tag = null;

        if(UUID_CREWTAG_CACHE.containsKey(uuid)){
            tag = UUID_CREWTAG_CACHE.get(uuid);
        } else {
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT crew_members.uuid, crew_members.rank, crews.tag FROM `crew_members` INNER JOIN `crews` on crew_members.crewID = crews.id WHERE crew_members.uuid = ?;");
                ps.setString(1,uuid.toString());
                ResultSet rs = ps.executeQuery();
                if(rs.first()){
                    if(rs.getString("rank").equals("LEADER")){
                        tag = new CrewTagData(rs.getString("tag"),true);
                    } else {
                        tag = new CrewTagData(rs.getString("tag"),false);
                    }
                }

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        return tag;
    }

    public static UUID getUUIDFromName(String name){
        if(name == null || name.isEmpty()) return null;
        UUID uuid = null;

        if(NAME_UUID_CACHE.containsKey(name)){
            uuid = NAME_UUID_CACHE.get(name);
        } else {
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `users` WHERE `username` = ?");
                ps.setString(1,name);
                ResultSet rs = ps.executeQuery();

                if(rs.first()){
                    name = rs.getString("username");
                    uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerUtilities.NAME_UUID_CACHE.put(name,uuid);
                    PlayerUtilities.UUID_NAME_CACHE.put(uuid,name);
                }

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        return uuid;
    }

    public static String getNameFromUUID(UUID uuid){
        if(uuid == null) return null;
        String name = null;

        if(UUID_NAME_CACHE.containsKey(uuid)){
            name = UUID_NAME_CACHE.get(uuid);
        } else {
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `users` WHERE `uuid` = ?");
                ps.setString(1,uuid.toString());
                ResultSet rs = ps.executeQuery();

                if(rs.first()){
                    name = rs.getString("username");
                    PlayerUtilities.NAME_UUID_CACHE.put(name,uuid);
                    PlayerUtilities.UUID_NAME_CACHE.put(uuid,name);
                }

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        return name;
    }

    public static Rank getRankFromUUID(UUID uuid){
        if(uuid == null) return null;
        Rank rank = Rank.USER;

        if(UUID_RANK_CACHE.containsKey(uuid)){
            rank = UUID_RANK_CACHE.get(uuid);
        } else {
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `users` WHERE `uuid` = ?");
                ps.setString(1,uuid.toString());
                ResultSet rs = ps.executeQuery();

                if(rs.first()){
                    rank = Rank.valueOf(rs.getString("rank"));
                    PlayerUtilities.UUID_RANK_CACHE.put(uuid,rank);
                }

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        return rank;
    }
}
