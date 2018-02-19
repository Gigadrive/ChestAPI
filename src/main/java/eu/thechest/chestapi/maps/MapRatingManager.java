package eu.thechest.chestapi.maps;

import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.mysql.MySQLManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by zeryt on 19.03.2017.
 */
public class MapRatingManager {
    public static Map MAP_TO_RATE = null;
    private static ArrayList<MapRating> CACHE = new ArrayList<MapRating>();

    public static boolean hasRatedMap(UUID uuid, Map map){
        if(map != null && uuid != null){
            return hasRatedMap(uuid,map.getID());
        } else {
            return false;
        }
    }

    public static boolean hasRatedMap(UUID uuid, int mapID){
        boolean b = false;

        for(MapRating r : CACHE){
            if(r.uuid == uuid && r.mapID == mapID) b = true;
        }

        if(b == false){
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `map_ratings` WHERE `uuid` = ? AND `mapID` = ?");
                ps.setString(1,uuid.toString());
                ps.setInt(2,mapID);
                ResultSet rs = ps.executeQuery();

                b = rs.first();

                if(b){
                    CACHE.add(new MapRating(uuid,mapID));
                }

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        return b;
    }

    public static void rateMap(UUID uuid, Map map, int rating){
        if(map != null && uuid != null){
            rateMap(uuid,map.getID(),rating);
        }
    }

    public static void rateMap(UUID uuid, int map, int rating){
        ChestAPI.async(() -> {
            if(uuid != null){
                if(!hasRatedMap(uuid,map)){
                    try {
                        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `map_ratings` (`uuid`,`mapID`,`rating`) VALUES(?,?,?)");
                        ps.setString(1,uuid.toString());
                        ps.setInt(2,map);
                        ps.setInt(3,rating);
                        ps.execute();
                        ps.close();

                        CACHE.add(new MapRating(uuid,map));
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
