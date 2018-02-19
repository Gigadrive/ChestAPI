package eu.thechest.chestapi;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.authlib.GameProfile;
import de.dytanic.cloudnet.api.CloudNetAPI;
import de.dytanic.cloudnet.network.ServerInfo;
import de.dytanic.cloudnet.servergroup.ServerState;
import eu.thechest.chestapi.achievement.Achievement;
import eu.thechest.chestapi.cmd.MainExecutor;
import eu.thechest.chestapi.event.PlayerLandOnGroundEvent;
import eu.thechest.chestapi.game.Game;
import eu.thechest.chestapi.game.GameManager;
import eu.thechest.chestapi.game.GamePerk;
import eu.thechest.chestapi.items.ItemRarity;
import eu.thechest.chestapi.items.UnlockedChest;
import eu.thechest.chestapi.items.VaultItem;
import eu.thechest.chestapi.lang.TranslatedHologram;
import eu.thechest.chestapi.lang.Translation;
import eu.thechest.chestapi.listener.MainListener;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.punish.PunishMenu;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.server.ServerUtil;
import eu.thechest.chestapi.tasks.TaskManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.GlobalParty;
import eu.thechest.chestapi.user.Rank;
import eu.thechest.chestapi.user.UUIDFetcher;
import eu.thechest.chestapi.util.*;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.menubuilder.chat.ChatCommandListener;
import org.inventivetalent.menubuilder.inventory.InventoryListener;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Created by zeryt on 11.02.2017.
 */
public class ChestAPI extends JavaPlugin {
    public static ArrayList<String> NICKS = new ArrayList<String>();
    public static ArrayList<String> NICK_SKINS = new ArrayList<String>();
    public static HashMap<Entity,VaultItem> ARROW_TRAILS = new HashMap<Entity,VaultItem>();
    public static HashMap<String,EnderDragon> VICTORY_DRAGONS = new HashMap<String,EnderDragon>();

    public static final int MAX_PLAYER_LEVEL = 75;
    public static final int MAX_INVENTORY_SIZE = 9*6;

    public static final int CREW_COINS_LOSE = 10;
    public static final int CREW_COINS_VICTORY = 50;

    public ChatCommandListener chatCommandListener;
    public InventoryListener inventoryListener;

    public Field nameField;

    public void onEnable(){
        saveDefaultConfig();
        instance = this;

        new Translation("EN");
        new Translation("DE");

        // REGISTER LISTENERS
        MainListener listener = new MainListener();

        Bukkit.getPluginManager().registerEvents(listener,this);
        Bukkit.getPluginManager().registerEvents(new PunishMenu(),this);

        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "TheChest");
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", listener);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this, "TheChest", listener);

        // REGISTER COMMANDS
        MainExecutor exec = new MainExecutor();
        getCommand("fly").setExecutor(exec);
        getCommand("coins").setExecutor(exec);
        getCommand("gamemode").setExecutor(exec);
        getCommand("clearchat").setExecutor(exec);
        getCommand("skull").setExecutor(exec);
        getCommand("getpos").setExecutor(exec);
        getCommand("langdump").setExecutor(exec);
        getCommand("blockloc").setExecutor(exec);
        getCommand("nick").setExecutor(exec);
        getCommand("nicklist").setExecutor(exec);
        getCommand("register").setExecutor(exec);
        getCommand("addmaplocation").setExecutor(exec);
        getCommand("ratemap").setExecutor(exec);
        getCommand("giverandomchest").setExecutor(exec);
        getCommand("memory").setExecutor(exec);
        getCommand("vanish").setExecutor(exec);
        getCommand("switch").setExecutor(exec);
        getCommand("smite").setExecutor(exec);
        getCommand("vote").setExecutor(exec);
        getCommand("bungeecmd").setExecutor(exec);
        getCommand("swing").setExecutor(exec);
        getCommand("punish").setExecutor(exec);
        getCommand("victoryeffect").setExecutor(exec);
        getCommand("aacremovalnotice").setExecutor(exec);
        getCommand("fix").setExecutor(exec);

        getCommand("mbchat").setExecutor(chatCommandListener = new ChatCommandListener(this));
        Bukkit.getPluginManager().registerEvents(inventoryListener = new InventoryListener(this),this);

        Achievement.init();
        TaskManager.init();
        loadNicks();
        VaultItem.init();
        GamePerk.load();

        new Thread(new Runnable() {

            @Override
            public void run() {
                while(true) {
                    for(String s : BarUtil.getPlayers()) {
                        Player o = Bukkit.getPlayer(s);
                        if(o != null) BarUtil.teleportBar(o);
                    }

                    try {
                        Thread.sleep(1000); // 1000 = 1 sec
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }).start();

        //ServerUtil.updateServerName(getConfig().getString("server-name"));
        ServerUtil.updateServerName(CloudNetAPI.getInstance().getServerInfo(CloudNetAPI.getInstance().getServerId()).getName());

        ServerSettingsManager.updateOnlinePlayers(Bukkit.getOnlinePlayers().size());
        ServerSettingsManager.setMaxPlayers(Bukkit.getMaxPlayers());

        /*try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `servers` SET `online` = ? WHERE `name` = ?");
            ps.setBoolean(1,true);
            ps.setString(2,ServerUtil.getServerName());
            ps.executeUpdate();
            ps.close();
        } catch(Exception e){
            e.printStackTrace();
        }*/

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
            public void run(){
                // RELOAD LOCAL CACHE

                PlayerUtilities.NAME_UUID_CACHE.clear();
                PlayerUtilities.UUID_NAME_CACHE.clear();
                PlayerUtilities.UUID_RANK_CACHE.clear();
                PlayerUtilities.UUID_FRIENDS_CACHE.clear();

                ChestAPI.async(() -> {
                    VaultItem.init();
                    GamePerk.load();
                });
            }
        }, 3*60*20, 3*60*20);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,new Runnable(){
            @Override
            public void run() {
                GlobalParty.STORAGE.clear();
            }
        }, 30*20, 30*20);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
            public void run(){
                for(Player all : Bukkit.getOnlinePlayers()){
                    doGroundCheck(all);
                }
            }
        }, 10L, 5L);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,new Runnable(){
            @Override
            public void run() {
                for(Player p : Bukkit.getOnlinePlayers()){
                    if(ChestUser.isLoaded(p)){
                        ChestUser u = ChestUser.getUser(p);

                        if(ServerUtil.isLobby()){
                            if(u.isNicked() && u.isVanished()){
                                BountifulAPI.sendActionBar(p,ChatColor.GREEN + "VANISHED" + ChatColor.GRAY + " | " + ChatColor.GREEN + "NICKED");
                            } else if(!u.isNicked() && u.isVanished()){
                                BountifulAPI.sendActionBar(p,ChatColor.GREEN + "VANISHED");
                            } else if(u.isNicked() && !u.isVanished()){
                                BountifulAPI.sendActionBar(p,ChatColor.GREEN + "NICKED");
                            }
                        } else {
                            if(u.isVanished()){
                                if(u.isNicked()){
                                    BountifulAPI.sendActionBar(p,ChatColor.GREEN + "VANISHED" + ChatColor.GRAY + " | " + ChatColor.GREEN + "NICKED");
                                } else {
                                    BountifulAPI.sendActionBar(p,ChatColor.GREEN + "VANISHED");
                                }
                            }
                        }
                    }
                }
            }
        },1*20,1*20);

        new BukkitRunnable(){
            @Override
            public void run() {
                if(ServerSettingsManager.ARROW_TRAILS){
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(ChestAPI.getInstance(), new Runnable(){
                        public void run(){
                            if(ServerSettingsManager.MAP_VOTING) ServerUtil.updateMapName("Voting..");

                            for(Entity e : ARROW_TRAILS.keySet()){
                                VaultItem trail = ARROW_TRAILS.get(e);

                                if(trail != null){
                                    if(e != null && !e.isDead() && e.getWorld() != null && Bukkit.getWorld(e.getWorld().getName()) != null){
                                        if(trail.getID() == 1){
                                            // CLOUD TRAIL

                                            ParticleEffect.CLOUD.display(0,0,0,0.005f,3,e.getLocation(),600);
                                        } else if(trail.getID() == 2){
                                            // SPARK TRAIL

                                            ParticleEffect.FIREWORKS_SPARK.display(0,0,0,0.0005f,1,e.getLocation(),600);
                                        } else if(trail.getID() == 10){
                                            // GOLD RUSH TRAIL

                                            ParticleEffect.BLOCK_CRACK.display(new ParticleEffect.BlockData(Material.GOLD_BLOCK, (byte)0), 0f, 0f, 0f, 0f, 10, e.getLocation(), 600);
                                        } else if(trail.getID() == 8){
                                            // FLAMES TRAIL

                                            ParticleEffect.FLAME.display(0,0,0,0.0005f,1,e.getLocation(),600);
                                        } else if(trail.getID() == 92){
                                            // SPARK TRAIL

                                            ParticleEffect.VILLAGER_HAPPY.display(0.002f,0.002f,0.002f,0.0005f,10,e.getLocation(),600);
                                        } else if(trail.getID() == 94){
                                            // BLACK DUST TRAIL

                                            ParticleEffect.REDSTONE.display(0.002f,0.002f,0.002f,0.0005f,10,e.getLocation(),600);
                                        } else if(trail.getID() == 93){
                                            // RED DUST TRAIL

                                            ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(255,0,0),e.getLocation(),600);
                                        } else if(trail.getID() == 95){
                                            // YELLOW DUST TRAIL

                                            ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(242,200,10),e.getLocation(),600);
                                        } else if(trail.getID() == 96){
                                            // GREEN DUST TRAIL

                                            ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(0,255,0),e.getLocation(),600);
                                        } else if(trail.getID() == 97){
                                            // BLUE DUST TRAIL

                                            ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(0,0,255),e.getLocation(),600);
                                        } else if(trail.getID() == 98){
                                            // PURPLE DUST TRAIL

                                            ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(166,18,126),e.getLocation(),600);
                                        }
                                    }
                                }
                            }
                        }
                    },1L,1L);
                }
            }
        }.runTaskLater(this,5*20);
    }

    // outcome = [ 1 = win, 0,5 = tie, 0 = lose ]
    // k = 20 [ https://de.wikipedia.org/wiki/Elo-Zahl ]
    public static int calculateRating(int p1Rating, int p2Rating, int outcome, double k){
        int diff = p1Rating - p2Rating;
        double expected = (double) (1.0 / (Math.pow(10.0, -diff / 400.0) + 1));
        //return (int) Math.round(p1Rating + k*(outcome - expected));
        int result = (int) Math.round(k*(outcome - expected));
        if(result == 0) result = 2;
        return result;
    }

    public static Player getPlayer(String name){
        Player p = Bukkit.getPlayer(name);

        if(p == null){
            for(Player all : Bukkit.getOnlinePlayers()){
                if(ChestUser.isLoaded(all)){
                    ChestUser a = ChestUser.getUser(all);

                    if(a.getNickData() != null && a.getNick().equalsIgnoreCase(name)){
                        p = all;
                    }
                }
            }
        }

        return p;
    }

    public static String getDisplayName(Player p, Player toShow){
        if(!ChestUser.isLoaded(p) || !ChestUser.isLoaded(toShow)) return p.getDisplayName();

        ChestUser u = ChestUser.getUser(p);
        ChestUser u2 = ChestUser.getUser(toShow);

        if(u2.hasPermission(Rank.VIP)){
            return u.getRank().getColor() + p.getName();
        } else {
            return p.getDisplayName();
        }
    }

    public static Field getField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void async(Runnable runnable){
        Bukkit.getScheduler().scheduleAsyncDelayedTask(ChestAPI.getInstance(),runnable);
    }

    public static void sync(Runnable runnable){
        Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(),runnable);
    }

    public static ArrayList<Location> getBlocksInRadius(Location loc, int radius){
        ArrayList<Location> a = new ArrayList<Location>();

        for (int x = -(radius); x <= radius; x ++){
            for (int y = -(radius); y <= radius; y ++){
                for (int z = -(radius); z <= radius; z ++){
                    if(loc.getWorld().getBlockAt(loc) != null){
                        Block b = loc.getWorld().getBlockAt(loc).getRelative(x,y,z);
                        if(b != null && b.getType() != null) a.add(b.getLocation());
                    }
                }
            }
        }

        return a;
    }

    public static double calculateKD(int kills, int deaths){
        if(deaths <= 0){
            return kills;
        } else if(kills <= 0) {
            return 0;
        } else {
            return StringUtils.round(((double)kills)/((double)deaths),2);
        }
    }

    public static double calculateWL(int wins, int loses){
        return calculateKD(wins,loses);
    }

    public static Location getBlockCenter(Location loc){
        if(loc == null){
            return null;
        } else {
            return new Location(loc.getWorld(),loc.getBlockX(),loc.getY(),loc.getBlockZ(),loc.getYaw(),loc.getPitch()).add(0.5,0,0.5);
        }
    }

    public static void stopServer(){
        Bukkit.shutdown();
    }

    public static ServerInfo getBestServer(String group){
        return getBestServer(group,0);
    }

    public static ServerInfo getBestServer(String group, int minFreeSlots){
        return getBestServer(group,0, null);
    }

    public static ServerInfo getBestServer(String group, int minFreeSlots, ServerState requiredState){
        return getBestServer(group,0, null, ServerSortMethod.GET_EMPTIEST_SERVER);
    }

    public static ServerInfo getBestServer(String group, int minFreeSlots, ServerState requiredState, ServerSortMethod method){
        if(method == null) method = ServerSortMethod.GET_EMPTIEST_SERVER;
        ArrayList<ServerInfo> potentials = new ArrayList<ServerInfo>();

        for(ServerInfo info : CloudNetAPI.getInstance().getCloudNetwork().getServers().values()){
            int freeSlots = info.getMaxPlayers()-info.getOnlineCount();

            if(info.getGroup().equalsIgnoreCase(group) && freeSlots >= minFreeSlots){
                if(requiredState == null){
                    potentials.add(info);
                } else {
                    if(info.getServerState() == requiredState){
                        potentials.add(info);
                    }
                }
            }
        }

        if(potentials.size() == 0){
            return null;
        } else if(potentials.size() == 1){
            return potentials.get(0);
        } else {
            final ServerSortMethod m = method;
            Collections.sort(potentials, new Comparator<ServerInfo>() {
                @Override
                public int compare(ServerInfo o1, ServerInfo o2) {
                    if(m == ServerSortMethod.GET_EMPTIEST_SERVER){
                        return ((Integer)o1.getOnlineCount()).compareTo(((Integer)o2.getOnlineCount()));
                    } else if(m == ServerSortMethod.GET_FULLEST_SERVER) {
                        return ((Integer)o2.getOnlineCount()).compareTo(((Integer)o1.getOnlineCount()));
                    } else {
                        return ((Integer)o1.getOnlineCount()).compareTo(((Integer)o2.getOnlineCount()));
                    }
                }
            });

            return potentials.get(0);
        }
    }

    public static int giveRandomChest(UUID uuid){
        if(uuid == null) return -1;
        /*ArrayList<ChestType> c = new ArrayList<ChestType>();

        for(ChestType t : ChestType.STORAGE.values()){
            if(t.canDropAfterGame()) c.add(t);
        }

        if(c.size() > 0){
            Collections.shuffle(c);

            return giveChest(c.get(0));
        }

        return -1;*/

        ArrayList<Integer> items = new ArrayList<Integer>();
        int legendary = 1;
        int epic = 1;
        int rare = 1;
        int common = 1;

        ArrayList<VaultItem> p = new ArrayList<VaultItem>();

        for(VaultItem i : VaultItem.STORAGE.values()){
            p.add(i);
        }

        Collections.shuffle(p);

        for(VaultItem i : p){
            if(i.canDropInRandomCrate()){
                if(i.getRarity() == ItemRarity.LEGENDARY){
                    if(legendary <= 1){
                        items.add(i.getID());
                        legendary++;
                    }
                } else if(i.getRarity() == ItemRarity.EPIC){
                    if(epic <= 1){
                        items.add(i.getID());
                        epic++;
                    }
                } else if(i.getRarity() == ItemRarity.RARE){
                    if(rare <= 2){
                        items.add(i.getID());
                        rare++;
                    }
                } else if(i.getRarity() == ItemRarity.COMMON){
                    if(common <= 3){
                        items.add(i.getID());
                        common++;
                    }
                }
            }
        }

        if(StringUtils.getChanceBoolean(1,300)){
            for(VaultItem i : VaultItem.STORAGE.values()){
                if(i.getRarity() == ItemRarity.MYTHIC && i.canDropInRandomCrate()){
                    items.add(i.getID());
                    break;
                }
            }
        }

        if(items.size() > 0){
            return giveChest(uuid,items);
        } else {
            return -1;
        }
    }

    public static int giveChest(UUID uuid, ArrayList<Integer> items){
        int r = -1;
        if(uuid == null) return r;
        String s = "";
        if(items != null && items.size() > 0){
            for(int i : items){
                if(s.isEmpty()){
                    s = String.valueOf(i);
                } else {
                    s = s + "," + String.valueOf(i);
                }
            }
        }

        if(s.isEmpty()) s = null;

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `lobbyShop_unlockedCrates` (`uuid`,`items`) VALUES(?,?)", java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,uuid.toString());
            ps.setString(2,s);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if(rs.next()){
                r = rs.getInt(1);

                Player p = Bukkit.getPlayer(uuid);
                if(p != null){
                    UnlockedChest c = UnlockedChest.get(r);
                    if(c != null) ChestUser.getUser(p).getUnlockedChests().put(rs.getInt(1),c);
                }
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
        }

        return r;
    }

    public void doGroundCheck(Player p){
        if(ChestUser.isLoaded(p)){
            ChestUser u = ChestUser.getUser(p);

            if(u.onGround == false && ((Entity)p).isOnGround() == true){
                Bukkit.getPluginManager().callEvent(new PlayerLandOnGroundEvent(p));
            }

            u.onGround = ((Entity)p).isOnGround();
        }
    }

    public void onDisable(){
        for(Player p : Bukkit.getOnlinePlayers()) ChestUser.getUser(p).saveData();

        for(Game g : GameManager.getCurrentGames()){
            g.setCompleted(true);
            g.saveData();
        }

        for(EnderDragon dragon : VICTORY_DRAGONS.values()){
            dragon.remove();
        }
        VICTORY_DRAGONS.clear();

        MySQLManager.getInstance().unload();
    }

    public static void nmsRemoveAI(Entity bukkitEntity) {
        net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity) bukkitEntity)
                .getHandle();
        NBTTagCompound tag = nmsEntity.getNBTTag();
        if (tag == null) {
            tag = new NBTTagCompound();
        }
        nmsEntity.c(tag);
        tag.setInt("NoAI", 1);
        nmsEntity.f(tag);
    }

    public static void nmsMakeSilent(Entity bukkitEntity) {
        net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity) bukkitEntity)
                .getHandle();
        NBTTagCompound tag = nmsEntity.getNBTTag();
        if (tag == null) {
            tag = new NBTTagCompound();
        }
        nmsEntity.c(tag);
        tag.setInt("Silent", 1);
        nmsEntity.f(tag);
    }

    private void loadNicks(){
        async(() -> {
            try {
                NICKS.clear();

                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `nicks`");
                ResultSet rs = ps.executeQuery();
                rs.beforeFirst();

                while(rs.next()){
                    NICKS.add(rs.getString("name"));
                }

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch (Exception e){
                e.printStackTrace();
            }

            try {
                NICK_SKINS.clear();

                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `users` ORDER BY RAND() LIMIT 50");
                ResultSet rs = ps.executeQuery();
                rs.beforeFirst();

                while(rs.next()){
                    NICK_SKINS.add(rs.getString("username"));
                }

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    public static boolean isLocationEqual(Location loc1, Location loc2){
        return loc1.getWorld().getName().equals(loc2.getWorld().getName()) && loc1.getX() == loc2.getX() && loc1.getY() == loc2.getY() && loc1.getZ() == loc2.getZ() && loc1.getYaw() == loc2.getYaw() && loc1.getPitch() == loc2.getPitch();
    }

    public static void executeBungeeCommand(String executor, String command){
        if(command.startsWith("/")) command = command.substring(1, command.length());

        Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);

        if(player != null){
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("globalcommand");
            out.writeUTF(executor);
            out.writeUTF(command);

            player.sendPluginMessage(ChestAPI.getInstance(), "BungeeCord", out.toByteArray());
        }
    }

    public static void sendPlayerToServer(String playerName, String server){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        Player p = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);

        try {
            out.writeUTF("ConnectOther");
            out.writeUTF(playerName);
            out.writeUTF(server);
        } catch (IOException e) {
            e.printStackTrace();
        }

        p.sendPluginMessage(ChestAPI.getInstance(), "BungeeCord", stream.toByteArray());
    }

    public static String executeShellCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }

    public static void giveAfterGameCrate(Player[] eligible){
        if(eligible != null){
            if(eligible.length > 0){
                for(Player p : eligible){
                    ChestUser u = ChestUser.getUser(p);

                    if(StringUtils.getChanceBoolean(1,7)){
                        int chestID = u.giveRandomChest();

                        if(chestID > 0){
                            p.sendMessage("");
                            p.sendMessage(ChatColor.AQUA + "[" + u.getTranslatedMessage("Chest Vault") + "] " + ChatColor.GOLD + u.getTranslatedMessage("You have unlocked a Mystery Chest!"));
                            p.sendMessage("");
                        }
                    }
                }
            }
        }
    }

    public static ArrayList<Location> getParticleCircle(Location center, double radius, int amount){
        World world = center.getWorld();
        double increment = (2 * Math.PI) / amount;
        ArrayList<Location> locations = new ArrayList<Location>();
        for(int i = 0;i < amount; i++) {
            double angle = i * increment;
            double x = center.getX() + (radius * Math.cos(angle));
            double z = center.getZ() + (radius * Math.sin(angle));
            locations.add(new Location(world, x, center.getY(), z));
        }

        return locations;
    }

    private static ChestAPI instance;
    public static ChestAPI getInstance(){
        return instance;
    }
}
