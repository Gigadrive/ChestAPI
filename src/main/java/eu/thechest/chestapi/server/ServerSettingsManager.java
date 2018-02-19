package eu.thechest.chestapi.server;

import de.dytanic.cloudnet.api.CloudNetAPI;
import de.dytanic.cloudnet.bukkitproxy.api.CloudServer;
import de.dytanic.cloudnet.servergroup.ServerState;
import eu.thechest.chestapi.maps.Map;
import eu.thechest.chestapi.mysql.MySQLManager;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;

import java.sql.PreparedStatement;
import java.util.ArrayList;

/**
 * Created by zeryt on 11.02.2017.
 */
public class ServerSettingsManager {
    static {
        UPDATE_TAB_NAME_WITH_SCOREBOARD = true;
        ADJUST_CHAT_FORMAT = true;
        RUNNING_GAME = GameType.NONE;
        updateGameState(GameState.UNDEFINED);
        AUTO_OP = true;
        VIP_JOIN = true;
        MIN_PLAYERS = 2;
        MAX_PLAYERS = 50;
        PROTECT_ITEM_FRAMES = true;
        PROTECT_ARMORSTANDS = true;
        PROTECT_FARMS = true;
        ENABLE_CHAT = true;
        ENABLE_NICK = true;
        SHOW_FAME_TITLE_ABOVE_HEAD = false;
        ARROW_TRAILS = false;
        KILL_EFFECTS = false;
        MAP_VOTING = false;
        VOTING_MAPS = new ArrayList<Map>();
        SHOW_LEVEL_IN_EXP_BAR = false;
        ALLOW_MULITPLE_MAPS = true;
    }

    public static boolean UPDATE_TAB_NAME_WITH_SCOREBOARD;
    public static boolean ADJUST_CHAT_FORMAT;
    public static GameType RUNNING_GAME;
    public static GameState CURRENT_GAMESTATE = GameState.UNDEFINED;
    public static boolean AUTO_OP;
    public static boolean VIP_JOIN;
    public static int MIN_PLAYERS;
    public static int MAX_PLAYERS;
    public static boolean PROTECT_ITEM_FRAMES;
    public static boolean PROTECT_ARMORSTANDS;
    public static boolean PROTECT_FARMS;
    public static boolean ENABLE_CHAT;
    public static boolean ENABLE_NICK;
    public static boolean SHOW_FAME_TITLE_ABOVE_HEAD;
    public static boolean ARROW_TRAILS;
    public static boolean KILL_EFFECTS;
    public static boolean MAP_VOTING;
    public static ArrayList<Map> VOTING_MAPS;
    public static boolean SHOW_LEVEL_IN_EXP_BAR;
    public static boolean ALLOW_MULITPLE_MAPS;

    @Deprecated
    public static void updateOnlinePlayers(int amount){
        /*try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `servers` SET `players.online` = ? WHERE `name` = ?");
            ps.setInt(1,amount);
            ps.setString(2,ServerUtil.getServerName());
            ps.executeUpdate();
            ps.close();
        } catch(Exception e){
            e.printStackTrace();
        }*/
    }

    public static void setMaxPlayers(int amount){
        MAX_PLAYERS = amount;
        CloudServer server = CloudServer.getInstance();
        server.setMaxPlayers(amount);
        server.update();

        /*try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `servers` SET `players.max` = ? WHERE `name` = ?");
            ps.setInt(1,amount);
            ps.setString(2,ServerUtil.getServerName());
            ps.executeUpdate();
            ps.close();
        } catch(Exception e){
            e.printStackTrace();
        }*/
    }

    @Deprecated
    public static void updateGameState(GameState newGameState){
        CURRENT_GAMESTATE = newGameState;
        ((CraftServer) Bukkit.getServer()).getServer().setMotd(newGameState.getDisplay());
        CloudServer server = CloudServer.getInstance();

        if(newGameState == GameState.LOBBY){
            server.setServerState(ServerState.LOBBY);
            server.update();
        } else if(newGameState == GameState.WARMUP || newGameState == GameState.INGAME || newGameState == GameState.ENDING){
            server.setServerState(ServerState.INGAME);
            server.update();
        }

        /*try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `servers` SET `gamestate` = ? WHERE `name` = ?");
            ps.setString(1,newGameState.toString());
            ps.setString(2,ServerUtil.getServerName());
            ps.executeUpdate();
            ps.close();
        } catch(Exception e){
            e.printStackTrace();
        }*/
    }
}
