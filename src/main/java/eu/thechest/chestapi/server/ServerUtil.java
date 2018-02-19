package eu.thechest.chestapi.server;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.ScoreboardType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;

/**
 * Created by zeryt on 27.03.2017.
 */
public class ServerUtil {
    private static String SERVER_NAME = null;
    private static String MAP_NAME = null;

    @Deprecated
    private static void requestServerName(){
        if(Bukkit.getOnlinePlayers().size() > 0 && SERVER_NAME == null){
            Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(), new Runnable(){
                public void run(){
                    Player p = Bukkit.getOnlinePlayers().iterator().next();
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("GetServer");
                    p.sendPluginMessage(ChestAPI.getInstance(), "BungeeCord", out.toByteArray());
                }
            },2*20);
        }
    }

    public static void updateServerName(String s){
        if(SERVER_NAME == null){
            SERVER_NAME = s;

            for(Player all : Bukkit.getOnlinePlayers()){
                ChestUser a = ChestUser.getUser(all);

                if(a.getCurrentBoard() == ScoreboardType.LOBBY){
                    a.updateScoreboard(ScoreboardType.LOBBY);
                }
            }
        }
    }

    public static void updateMapName(String s){
        MAP_NAME = s;

        /*try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `servers` SET `map` = ? WHERE `name` = ?");
            ps.setString(1,s);
            ps.setString(2,SERVER_NAME);
            ps.executeUpdate();
            ps.close();
        } catch(Exception e){
            e.printStackTrace();
        }*/
    }

    public static boolean isLobby(){
        return isUserLobby() || isPremiumLobby();
    }

    public static boolean isUserLobby(){
        return (SERVER_NAME != null && SERVER_NAME.startsWith("Lobby"));
    }

    public static boolean isPremiumLobby(){
        return (SERVER_NAME != null && SERVER_NAME.startsWith("PremiumLobby"));
    }

    public static String getMapName(){
        return MAP_NAME;
    }

    public static String getServerName(){
        if(SERVER_NAME == null) requestServerName();

        return SERVER_NAME;
    }
}
