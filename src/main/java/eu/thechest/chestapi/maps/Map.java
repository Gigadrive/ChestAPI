package eu.thechest.chestapi.maps;

import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameType;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.server.ServerUtil;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.StringUtils;
import net.lingala.zip4j.core.ZipFile;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by zeryt on 24.02.2017.
 */
public class Map {
    public static Map getMap(int id){
        if(MAP_STORAGE.containsKey(id)){
            return MAP_STORAGE.get(id);
        } else {
            new Map(id);

            if(MAP_STORAGE.containsKey(id)){
                return MAP_STORAGE.get(id);
            } else {
                return null;
            }
        }
    }

    private static HashMap<Integer,Map> MAP_STORAGE = new HashMap<Integer,Map>();

    private int id;
    private String name;
    private String author;
    private String link;
    private MapType mapType;
    private Timestamp timeAdded;
    private UUID addedBy;
    private boolean active;
    private String originalWorldName;
    private Blob zipFile;

    private ArrayList<String> bukkitWorlds;
    private ArrayList<MapLocationData> locations;

    public Map(int id){
        this.id = id;
        this.locations = new ArrayList<MapLocationData>();
        this.bukkitWorlds = new ArrayList<String>();

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `maps` WHERE `id` = ?");
            ps.setInt(1,id);

            ResultSet rs = ps.executeQuery();

            if(rs.first()){
                this.name = rs.getString("name");
                this.author = rs.getString("author");
                this.link = rs.getString("link");
                this.mapType = MapType.valueOf(rs.getString("mapType"));
                this.timeAdded = rs.getTimestamp("time_added");
                if(rs.getString("addedBy") != null && !rs.getString("addedBy").isEmpty()) this.addedBy = UUID.fromString(rs.getString("addedBy"));
                this.active = rs.getBoolean("active");
                this.originalWorldName = rs.getString("worldName");
                this.zipFile = rs.getBlob("zipFile");

                loadLocations();
                MAP_STORAGE.put(id,this);
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public int getID(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public String getAuthor(){
        return this.author;
    }

    public String getLink(){
        return this.link;
    }

    public MapType getMapType(){
        return this.mapType;
    }

    public Timestamp getTimeAdded(){
        return this.timeAdded;
    }

    public UUID getAddedBy(){
        return this.addedBy;
    }

    public boolean isActive(){
        return this.active;
    }

    @Deprecated
    public ArrayList<MapLocationData> getSpawnpoints(){
        return getLocations(MapLocationType.SPAWNPOINT);
    }

    public String getOriginalWorldName(){
        return this.originalWorldName;
    }

    public String loadMapToServer() {
        return loadMapToServer(true);
    }

    public String loadMapToServer(boolean async) {
        return loadMapToServer(true,false);
    }

    public String loadMapToServer(boolean async, boolean unzipOnly) {
        String worldName = null;

        try {
            String path = Bukkit.getWorldContainer().getAbsolutePath() + "/map" + getID() + ".zip";

            InputStream inputStream = zipFile.getBinaryStream();
            OutputStream outputStream = new FileOutputStream(path);

            int bytesRead = -1;
            byte[] buffer = new byte[4096];
            while((bytesRead = inputStream.read(buffer)) != -1){
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            ZipFile zip = new ZipFile(path);
            if(!zip.isEncrypted()){
                zip.extractAll(Bukkit.getWorldContainer().getAbsolutePath());
            }

            if(ServerSettingsManager.ALLOW_MULITPLE_MAPS){
                while(worldName == null || bukkitWorlds.contains(worldName)) worldName = getOriginalWorldName() + "-" + StringUtils.randomInteger(1,9999);
            } else {
                worldName = getOriginalWorldName();
            }
            //ChestAPI.executeShellCommand("mv " + Bukkit.getWorldContainer().getAbsolutePath() + "/" + getOriginalWorldName() + "/ " + Bukkit.getWorldContainer().getAbsolutePath() + "/" + worldName + "/");
            //move(new File(Bukkit.getWorldContainer().getAbsolutePath() + "/" + getOriginalWorldName() + "/"), new File(Bukkit.getWorldContainer().getAbsolutePath() + "/" + worldName + "/"));
            Files.move(Paths.get(new File(Bukkit.getWorldContainer().getAbsolutePath() + "/" + getOriginalWorldName() + "/").getPath()),Paths.get(new File(Bukkit.getWorldContainer().getAbsolutePath() + "/" + worldName + "/").getPath()));

            final String w = worldName;

            File uidFile = new File(Bukkit.getWorldContainer().getAbsolutePath() + "/" + worldName + "/" + "uid.dat");
            if(uidFile.exists()) FileUtils.forceDelete(uidFile);

            if(!unzipOnly){
                if(async){
                    ChestAPI.async(() -> {
                        try {
                            WorldCreator wc = new WorldCreator(w);
                            ChestAPI.getInstance().getServer().createWorld(wc);
                        } catch(IllegalStateException e){
                            // do nothing
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    });
                } else {
                    WorldCreator wc = new WorldCreator(w);
                    ChestAPI.getInstance().getServer().createWorld(wc);
                }
            }

            //new File(path).delete();
            FileUtils.forceDelete(new File(path));
            bukkitWorlds.add(worldName);
        } catch(Exception e){
            e.printStackTrace();
        }

        return worldName;
    }

    public void unregister(){
        for(String s : bukkitWorlds) removeMap(s,false);
        bukkitWorlds.clear();

        MAP_STORAGE.remove(getID());
    }

    public void removeMap(String s){
        removeMap(s,true);
    }

    public void removeMap(String s,boolean b){
        if(b) if(bukkitWorlds.contains(s)) bukkitWorlds.remove(s);
        if(Bukkit.getWorld(s) == null) return;

        for(Player p : Bukkit.getWorld(s).getPlayers()){
            ChestUser u = ChestUser.getUser(p);

            if(ServerUtil.getServerName().startsWith("Lobby") || ServerUtil.getServerName().startsWith("PremiumLobby")){
                p.kickPlayer(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("World unloaded!"));
            } else {
                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("World unloaded!"));
                u.connectToLobby();
            }
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(), new Runnable() {
            @Override
            public void run() {
                Bukkit.unloadWorld(s,false);

                try {
                    FileUtils.deleteDirectory(new File(Bukkit.getWorldContainer().getAbsolutePath() + "/" + s + "/"));
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        //FileUtils.deleteDirectory(new File(getWorldName()));
        /*File path = new File(getWorldName());

        handleFiles(path);*/
    }

    private void handleFiles(File path){
        if(path.exists()) {
            File files[] = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    handleFiles(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
    }

    public ArrayList<MapLocationData> getLocations(){
        return this.locations;
    }

    public ArrayList<MapLocationData> getLocations(MapLocationType type){
        if(type == null) return null;

        ArrayList<MapLocationData> a = new ArrayList<MapLocationData>();

        for(MapLocationData m : getLocations()){
            if(m.type == type) a.add(m);
        }

        return a;
    }

    private void loadLocations() throws SQLException {
        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `map_locations` WHERE `mapID` = ?");
        ps.setInt(1,this.id);

        ResultSet rs = ps.executeQuery();
        rs.beforeFirst();

        while(rs.next()){
            locations.add(new MapLocationData(rs.getDouble("x"),rs.getDouble("y"),rs.getDouble("z"),rs.getFloat("yaw"),rs.getFloat("pitch"),MapLocationType.valueOf(rs.getString("type"))));
        }

        MySQLManager.getInstance().closeResources(rs,ps);
    }

    public void sendMapCredits(){
        for(Player p : Bukkit.getOnlinePlayers()){
            sendMapCredits(p);
        }
    }

    public void sendMapCredits(Player p){
        ChestUser u = ChestUser.getUser(p);

        /*p.sendMessage(ChatColor.YELLOW + "[MAP] " + u.getTranslatedMessage("Name") + ": " + ChatColor.AQUA + getName());
        p.sendMessage(ChatColor.YELLOW + "[MAP] " + u.getTranslatedMessage("Author") + ": " + ChatColor.AQUA + getAuthor());
        p.sendMessage(ChatColor.YELLOW + "[MAP] " + u.getTranslatedMessage("Link") + ": " + ChatColor.AQUA + getLink());*/

        /*p.sendMessage(GameType.NONE.getPrefix() + ChatColor.GREEN + "----------------------------------------");
        p.sendMessage(GameType.NONE.getPrefix());
        p.sendMessage(GameType.NONE.getPrefix() + ChatColor.YELLOW + u.getTranslatedMessage("Name") + ": " + ChatColor.AQUA + getName());
        p.sendMessage(GameType.NONE.getPrefix() + ChatColor.YELLOW + u.getTranslatedMessage("Creator") + ": " + ChatColor.AQUA + getAuthor());
        p.sendMessage(GameType.NONE.getPrefix() + ChatColor.YELLOW + u.getTranslatedMessage("Link") + ": " + ChatColor.AQUA + getLink());
        p.sendMessage(GameType.NONE.getPrefix() + ChatColor.YELLOW + u.getTranslatedMessage("Have fun!"));
        p.sendMessage(GameType.NONE.getPrefix());
        p.sendMessage(GameType.NONE.getPrefix() + ChatColor.GREEN + "----------------------------------------");*/

        /*p.sendMessage("");
        u.sendCenteredMessage(ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + getName());
        u.sendCenteredMessage(ChatColor.AQUA + getAuthor());

        if(getLink() != null){
            String link = getLink();

            String[] s = u.sendCenteredMessage(ChatColor.GREEN.toString() + ChatColor.BOLD.toString() + u.getTranslatedMessage("[Visit website]"),false).split(ChatColor.GREEN.toString() + ChatColor.BOLD.toString());
            p.spigot().sendMessage(new ComponentBuilder(s[0]).append(s[1]).color(net.md_5.bungee.api.ChatColor.GREEN).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL,link)).create());
        }*/
    }

    public void sendRateMapInfo(){
        for(Player p : Bukkit.getOnlinePlayers()){
            sendRateMapInfo(p);
        }
    }

    public void sendRateMapInfo(Player p){
        if(MapRatingManager.MAP_TO_RATE == this){
            if(!MapRatingManager.hasRatedMap(p.getUniqueId(),this)){
                ChestUser u = ChestUser.getUser(p);

                /*p.sendMessage(" ");
                p.sendMessage("         " + ChatColor.AQUA + u.getTranslatedMessage("Click to rate this map!"));
                p.spigot().sendMessage(new ComponentBuilder("        ").append("1").color(net.md_5.bungee.api.ChatColor.DARK_RED).bold(true).append("    ").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 1")).append("2").color(net.md_5.bungee.api.ChatColor.RED).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 2")).append("    ").append("3").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 3")).append("    ").append("4").color(net.md_5.bungee.api.ChatColor.GREEN).append("    ").bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 4")).append("5").color(net.md_5.bungee.api.ChatColor.DARK_GREEN).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 5")).create());
                p.sendMessage(" ");*/

                /*p.sendMessage(" " + ChatColor.YELLOW + "-- " + ChatColor.LIGHT_PURPLE + u.getTranslatedMessage("Rate this map by clicking a number") + ":");
                p.spigot().sendMessage(new ComponentBuilder(" ").append("--").color(net.md_5.bungee.api.ChatColor.YELLOW).append("    ").append("1").color(net.md_5.bungee.api.ChatColor.DARK_RED).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 1")).append("    ").append("2").color(net.md_5.bungee.api.ChatColor.RED).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 2")).append("    ").append("3").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 3")).append("    ").append("4").color(net.md_5.bungee.api.ChatColor.GREEN).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 4")).append("    ").append("5").color(net.md_5.bungee.api.ChatColor.DARK_GREEN).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 5")).create());
                p.sendMessage(" " + ChatColor.YELLOW + "-- " + ChatColor.LIGHT_PURPLE + u.getTranslatedMessage("This is an opportunity to have your voice heard"));
                p.sendMessage(" " + ChatColor.YELLOW + "-- " + ChatColor.WHITE + ChatColor.BOLD.toString() + u.getTranslatedMessage("Thanks for helping improve TheChest!"));*/

                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GRAY + u.getTranslatedMessage("Click to rate this map!"));
                p.spigot().sendMessage(new ComponentBuilder(ServerSettingsManager.RUNNING_GAME.getPrefix() + " ").append("1").color(net.md_5.bungee.api.ChatColor.DARK_RED).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 1")).append("  ").append("2").color(net.md_5.bungee.api.ChatColor.RED).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 2")).append("  ").append("3").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 3")).append("  ").append("4").color(net.md_5.bungee.api.ChatColor.GREEN).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 4")).append("  ").append("5").color(net.md_5.bungee.api.ChatColor.DARK_GREEN).bold(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ratemap 5")).create());
            }
        }
    }
}
