package eu.thechest.chestapi.game;

import eu.thechest.chestapi.maps.Map;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameType;
import eu.thechest.chestapi.server.ServerUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Created by zeryt on 27.03.2017.
 */
public class GameManager {
    private static ArrayList<Game> CURRENT_GAMES = new ArrayList<Game>();

    public static ArrayList<Game> getCurrentGames(){
        return CURRENT_GAMES;
    }

    public static Game initializeNewGame(GameType gameType, Map map){
        Game g = null;

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `games` (`game`,`server`,`map`) VALUES(?,?,?);", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,gameType.toString());
            ps.setString(2, ServerUtil.getServerName());
            if(map == null){
                ps.setInt(3,0);
            } else {
                ps.setInt(3,map.getID());
            }
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if(rs.first()){
                int gameID = rs.getInt(1);

                g = new Game(gameID, gameType, ServerUtil.getServerName(), map);

                CURRENT_GAMES.add(g);
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
        }

        return g;
    }
}
