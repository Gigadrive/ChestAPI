package eu.thechest.chestapi.tasks;

import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * Created by zeryt on 19.02.2017.
 */
public class AutoAnnouncementTask extends BukkitRunnable {
    private static ArrayList<String> TO_BROADCAST;
    private int lastLine = 0;

    public static void init(){
        if(TO_BROADCAST == null) TO_BROADCAST = new ArrayList<String>();
        TO_BROADCAST.clear();

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `ingame_broadcasts`");
            ResultSet rs = ps.executeQuery();

            rs.beforeFirst();

            while(rs.next()){
                TO_BROADCAST.add(rs.getString("text"));
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        if(TO_BROADCAST.size() > 0){
            if(lastLine >= TO_BROADCAST.size()-1){
                lastLine = 0;
            } else {
                lastLine++;
            }

            for(Player all : Bukkit.getOnlinePlayers()){
                ChestUser u = ChestUser.getUser(all);

                all.sendMessage(" ");
                all.sendMessage(ChatColor.RED + "[" + ChatColor.YELLOW + "!" + ChatColor.RED + "] " + ChatColor.GRAY + u.getTranslatedMessage(TO_BROADCAST.get(lastLine)));
                all.sendMessage(" ");
            }
        }
    }
}
