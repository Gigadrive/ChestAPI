package eu.thechest.chestapi.maps;

import de.dytanic.cloudnet.api.CloudNetAPI;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.event.FinalMapLoadedEvent;
import eu.thechest.chestapi.event.VotingEndEvent;
import eu.thechest.chestapi.game.GameManager;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameState;
import eu.thechest.chestapi.server.GameType;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.server.ServerUtil;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.ScoreboardType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by zeryt on 27.05.2017.
 */
public class MapVotingManager {
    public static boolean VOTING_OPEN = true;
    public static Map FINAL_MAP = null;
    public static int lobbyCountdownCount = 60;
    public static BukkitTask lobbyCountdownTask;
    public static ArrayList<MapVote> VOTES = new ArrayList<MapVote>();
    public static String FINAL_MAP_WORLDNAME = null;

    public static int getMapVotes(Map m){
        /*if(VOTING_MAPS.contains(m)){
            int slot = 0;
            for(Map ma : VOTING_MAPS){
                if(ma == m) break;
                slot++;
            }

            int votes = 0;
            for(String ss : SurvivalGames.VOTES){
                if(SurvivalGames.VOTES.get(ss) == slot) votes++;
            }

            return votes;
        } else {
            return 0;
        }*/

        if(ServerSettingsManager.VOTING_MAPS.contains(m)){
            int votes = 0;

            for(MapVote v : VOTES){
                if(v.map == m) votes++;
            }

            return votes;
        } else {
            return 0;
        }
    }

    public static void cancelLobbyCountdown(){
        if(lobbyCountdownTask != null){
            lobbyCountdownCount = 60;
            lobbyCountdownTask.cancel();
            lobbyCountdownTask = null;

            for(Player all : Bukkit.getOnlinePlayers()){
                all.setExp((float) ((double) lobbyCountdownCount / 60D));
                all.setLevel(lobbyCountdownCount);
                all.playSound(all.getEyeLocation(),Sound.NOTE_BASS,1f,0.5f);
                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + ChestUser.getUser(all).getTranslatedMessage("The countdown has been cancelled."));
            }
        }
    }

    public static boolean hasVoted(Player p){
        for(MapVote v : VOTES){
            if(v.p == p) return true;
        }

        return false;
    }

    public static void chooseMapsForVoting(){
        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `maps` WHERE `mapType` = ? AND `active` = ? ORDER BY RAND() DESC LIMIT 4");
            ps.setString(1,ServerSettingsManager.RUNNING_GAME.toString());
            ps.setBoolean(2,true);
            ResultSet rs = ps.executeQuery();
            rs.beforeFirst();

            while(rs.next()){
                ServerSettingsManager.VOTING_MAPS.add(Map.getMap(rs.getInt("id")));
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            if(ServerSettingsManager.VOTING_MAPS.size() == 0){
                System.err.print("NO MAPS COULD BE LOADED!");
                System.err.print("SHUTTING DOWN!");
                ChestAPI.stopServer();
            } else {
                Collections.shuffle(ServerSettingsManager.VOTING_MAPS);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startLobbyCountdown(){
        if(lobbyCountdownTask == null){
            lobbyCountdownCount = 60;

            lobbyCountdownTask = new BukkitRunnable(){
                @Override
                public void run() {
                    if(lobbyCountdownCount == 0){
                        cancel();
                        lobbyCountdownCount = 60;
                        lobbyCountdownTask = null;

                        VotingEndEvent event = new VotingEndEvent(FINAL_MAP);
                        Bukkit.getPluginManager().callEvent(event);
                    } else {
                        for(Player all : Bukkit.getOnlinePlayers()){
                            ChestUser a = ChestUser.getUser(all);
                            all.setExp((float) ((double) lobbyCountdownCount / 60D));
                            all.setLevel(lobbyCountdownCount);

                            if(lobbyCountdownCount == 60 || lobbyCountdownCount == 30 || lobbyCountdownCount == 20 || lobbyCountdownCount == 10 || lobbyCountdownCount == 5 || lobbyCountdownCount == 4 || lobbyCountdownCount == 3 || lobbyCountdownCount == 2 || lobbyCountdownCount == 1){
                                all.playSound(all.getEyeLocation(), Sound.NOTE_BASS,1f,1f);
                                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The lobby phase ends in %s seconds!").replace("%s",ChatColor.AQUA.toString() + lobbyCountdownCount + ChatColor.GOLD.toString()));
                            }
                        }

                        if(lobbyCountdownCount == 30){
                            Map map = ServerSettingsManager.VOTING_MAPS.get(0);
                            int maxVotes = 0;

                            for(Map m : ServerSettingsManager.VOTING_MAPS){
                                int v = getMapVotes(m);

                                if(v > maxVotes){
                                    map = m;
                                    maxVotes = v;
                                }
                            }

                            FINAL_MAP = map;
                            ServerUtil.updateMapName(FINAL_MAP.getName());
                            VOTING_OPEN = false;
                            for(Player all : Bukkit.getOnlinePlayers()){
                                ChestUser.getUser(all).updateScoreboard(ScoreboardType.MAP_VOTING);
                                all.playSound(all.getEyeLocation(),Sound.ANVIL_LAND, 1f, 1f);
                                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + ChestUser.getUser(all).getTranslatedMessage("The map %m has won the voting!").replace("%m",ChatColor.AQUA + map.getName() + ChatColor.GOLD));
                            }
                        }

                        if(lobbyCountdownCount == 20){
                            FINAL_MAP_WORLDNAME = FINAL_MAP.loadMapToServer();

                            Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(),new Runnable(){
                                public void run(){
                                    FinalMapLoadedEvent event = new FinalMapLoadedEvent(FINAL_MAP,Bukkit.getWorld(FINAL_MAP_WORLDNAME));
                                    Bukkit.getPluginManager().callEvent(event);
                                }
                            }, 10*20);
                        }

                        lobbyCountdownCount--;
                    }
                }
            }.runTaskTimer(ChestAPI.getInstance(),20L,20L);
        }
    }
}
