package eu.thechest.chestapi.user;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mysql.jdbc.Statement;
import de.dytanic.cloudnet.api.CloudNetAPI;
import de.dytanic.cloudnet.bukkitproxy.api.CloudProxy;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.achievement.Achievement;
import eu.thechest.chestapi.achievement.AchievementUnlock;
import eu.thechest.chestapi.event.PlayerDataLoadedEvent;
import eu.thechest.chestapi.event.PlayerLocaleChangeEvent;
import eu.thechest.chestapi.event.PlayerToggleVanishEvent;
import eu.thechest.chestapi.game.GamePerk;
import eu.thechest.chestapi.items.*;
import eu.thechest.chestapi.lang.TranslatedHologram;
import eu.thechest.chestapi.lang.Translation;
import eu.thechest.chestapi.maps.*;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameType;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.server.ServerUtil;
import eu.thechest.chestapi.util.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Scoreboard;
import org.inventivetalent.nicknamer.api.NickNamerAPI;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map;

/**
 * Created by zeryt on 11.02.2017.
 */
public class ChestUser {
    private static HashMap<Player,ChestUser> STORAGE = new HashMap<Player,ChestUser>();

    public static ChestUser getUser(Player p){
        if(STORAGE.containsKey(p)){
            return STORAGE.get(p);
        } else {
            return null;
        }
    }

    public static void loadData(Player p){
        new ChestUser(p);
    }

    public static boolean isLoaded(Player p){
        if(STORAGE.containsKey(p)){
            return STORAGE.get(p).loaded;
        } else {
            return false;
        }
    }

    public static void unregister(Player p){
        if(STORAGE.containsKey(p)){
            STORAGE.remove(p);
        }
    }

    private Player p;
    private int level;
    private double exp;
    private int startCoins;
    private int coins;
    private int startResetTokens;
    private int resetTokens;
    private int startKeys;
    private int keys;
    private int startShards;
    private int shards;
    private int startChestsOpened;
    private int chestsOpened;
    private boolean vanish;
    private Rank rank;
    private Timestamp firstJoin;
    private long playtime;
    private GameProfile originalGameProfile;
    //public ArmorStand fameTitlePlate;
    public Hologram fameTitlePlate;
    public TextLine fameTitlePlateLine;
    public boolean showFameTitlePlate = true;

    public boolean onGround = true;

    private Scoreboard scoreboard;
    private ScoreboardType currentBoard;

    private Translation currentLang;

    private ArrayList<AchievementUnlock> unlockedAchievements;
    private ArrayList<VaultItem> unlockedItems;
    private ArrayList<VaultItem> activeItems;
    private ArrayList<GamePerk> unlockedPerks;
    private HashMap<Integer,UnlockedChest> unlockedCrates;
    private ArrayList<Integer> claimedLevelRewards;

    private Timestamp creationDate;
    private PermissionAttachment permAttachment;
    private ActiveNickData nickData;

    private boolean setting_friendRequests;
    private boolean setting_privateMessages;
    private boolean setting_partyRequests;
    private boolean setting_headSeat;
    private boolean setting_lobbySpeed;
    private boolean setting_allowChallengerRequests;
    private boolean setting_nickUpdateSelf;
    private boolean setting_nickHide;

    public boolean mayChat = true;
    public boolean showingDragonVictoryEffect = false;
    public boolean loaded = false;
    public int acceptedReportCoins = 0;

    public ChestUser(Player p){
        if(STORAGE.containsKey(p)) return;
        if(p.getName().contains(" ")||p.getName().contains("§")) return;
        this.p = p;
        this.currentLang = null;
        this.unlockedAchievements = new ArrayList<AchievementUnlock>();
        this.unlockedItems = new ArrayList<VaultItem>();
        this.unlockedCrates = new HashMap<Integer,UnlockedChest>();
        this.activeItems = new ArrayList<VaultItem>();
        this.claimedLevelRewards = new ArrayList<Integer>();
        this.creationDate = new Timestamp(System.currentTimeMillis());
        this.unlockedPerks = new ArrayList<GamePerk>();
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.originalGameProfile = ((CraftPlayer)p).getProfile();

        ChestAPI.async(() -> {
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `users` WHERE `uuid` = ?");
                ps.setString(1,p.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();

                if(rs.first()){
                    // USER EXISTS

                    this.level = rs.getInt("level");
                    this.exp = rs.getDouble("exp");
                    this.startCoins = rs.getInt("coins");
                    this.startResetTokens = rs.getInt("resetTokens");
                    this.startKeys = rs.getInt("keys");
                    this.startShards = rs.getInt("vaultShards");
                    this.startChestsOpened = rs.getInt("chestsOpened");
                    this.rank = Rank.valueOf(rs.getString("rank"));
                    this.vanish = rs.getBoolean("vanish");
                    if(!hasPermission(Rank.MOD)) this.vanish = false;
                    this.firstJoin = rs.getTimestamp("firstjoin");
                    this.playtime = rs.getLong("playtime");
                    this.currentLang = Translation.getLanguage(rs.getString("language"));
                    this.acceptedReportCoins = rs.getInt("acceptedReportCoins");
                    ChestAPI.sync(() -> this.permAttachment = p.addAttachment(ChestAPI.getInstance()));

                    this.setting_friendRequests = rs.getBoolean("setting_friendRequests");
                    this.setting_privateMessages = rs.getBoolean("setting_privateMessages");
                    this.setting_partyRequests = rs.getBoolean("setting_partyRequests");
                    this.setting_headSeat = rs.getBoolean("setting_headSeat");
                    this.setting_allowChallengerRequests = rs.getBoolean("setting_allowChallengerRequests");
                    this.setting_lobbySpeed = rs.getBoolean("setting_lobbySpeed");
                    this.setting_nickUpdateSelf = rs.getBoolean("setting_nickUpdateSelf");
                    this.setting_nickHide = rs.getBoolean("setting_nickHide");

                    if(ServerSettingsManager.UPDATE_TAB_NAME_WITH_SCOREBOARD == true){
                        List<Rank> ranks = Arrays.asList(Rank.values());
                        Collections.reverse(ranks);

                        int i = 1;
                        for(Rank r : Rank.values()){
                            Team t = this.scoreboard.registerNewTeam("pl" + r.getScoreboardChar() + "_" + r.getID());
                            if(r.getPrefix() != null){
                                t.setPrefix(r.getColor().toString());
                            } else {
                                t.setPrefix(r.getColor().toString());
                            }

                            i++;
                        }
                    }

                    MySQLManager.getInstance().closeResources(rs,ps);

                    loadAdditionalData();
                } else {
                    // USER DOES NOT EXIST

                    PreparedStatement insert = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `users` (`uuid`,`username`,`playtime`) VALUES(?,?,?)");
                    insert.setString(1,p.getUniqueId().toString());
                    insert.setString(2,p.getName());
                    insert.setLong(3,0);
                    if(insert.execute()){
                        new ChestUser(p);
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        });
    }

    private void loadAdditionalData(){
        try {
            // GET UNLOCKED ACHIEVEMENTS

            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `unlocked_achievements` WHERE `uuid` = ?");
            ps.setString(1,p.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            rs.beforeFirst();
            while(rs.next()){
                unlockedAchievements.add(new AchievementUnlock(rs.getInt("id"),p,rs.getInt("achievementID"),rs.getTimestamp("time")));
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            // GET NICK DATA

            ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `active_nicks` WHERE `uuid` = ? AND `active` = ? ORDER BY `time` DESC LIMIT 1");
            ps.setString(1,p.getUniqueId().toString());
            ps.setBoolean(2,true);
            rs = ps.executeQuery();
            if(rs.first()){
                GameProfileBuilder.GameProfileSerializer serializer = new GameProfileBuilder.GameProfileSerializer();
                Gson gson = new Gson();

                this.nickData = new ActiveNickData(rs.getString("nick"), rs.getString("skin"));
            }
            MySQLManager.getInstance().closeResources(rs,ps);

            // GET UNLOCKED LOBBY ITEMS

            ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `lobbyShop_boughtItems` WHERE `uuid` = ? ORDER BY `time`");
            ps.setString(1,p.getUniqueId().toString());
            rs = ps.executeQuery();

            rs.beforeFirst();
            while(rs.next()){
                unlockedItems.add(VaultItem.getItem(rs.getInt("lobbyShopItem")));

                if(rs.getBoolean("active")) activeItems.add(VaultItem.getItem(rs.getInt("lobbyShopItem")));
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            // GET UNLOCKED CRATES

            ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `lobbyShop_unlockedCrates` WHERE `uuid` = ?");
            ps.setString(1,p.getUniqueId().toString());
            rs = ps.executeQuery();

            rs.beforeFirst();
            while(rs.next()){
                ArrayList<Integer> items = new ArrayList<Integer>();
                for(String s : rs.getString("items").split(",")){
                    if(StringUtils.isValidInteger(s)){
                        items.add(Integer.parseInt(s));
                    }
                }

                //unlockedCrates.put(rs.getInt("id"),ChestType.getType(rs.getInt("crateType")));
                unlockedCrates.put(rs.getInt("id"),new UnlockedChest(rs.getInt("id"),items,rs.getString("icon"),rs.getString("name"),rs.getTimestamp("time")));
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            // GET CLAIMED LEVEL REWARDS

            ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `claimedLevelRewards` WHERE `uuid` = ?");
            ps.setString(1,p.getUniqueId().toString());
            rs = ps.executeQuery();

            rs.beforeFirst();
            while(rs.next()){
                int level = rs.getInt("level");

                if(!claimedLevelRewards.contains(level)) claimedLevelRewards.add(level);
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            // GET GAME PERKS

            ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `gamePerks_unlocked` WHERE `uuid` = ?");
            ps.setString(1,p.getUniqueId().toString());
            rs = ps.executeQuery();

            rs.beforeFirst();
            while(rs.next()){
                GamePerk perk = GamePerk.get(rs.getInt("perk"));

                if(perk != null){
                    if(!unlockedPerks.contains(perk)) unlockedPerks.add(perk);
                }
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            STORAGE.put(p,this);

            updateName();

            PlayerDataLoadedEvent event = new PlayerDataLoadedEvent(p);
            Bukkit.getPluginManager().callEvent(event);

            loaded = true;
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public HashMap<Integer,UnlockedChest> getUnlockedChests(){
        return this.unlockedCrates;
    }

    public ArrayList<VaultItem> getUnlockedItems(){
        return this.unlockedItems;
    }

    public ArrayList<VaultItem> getActiveItems(){
        return this.activeItems;
    }

    public GameProfile getOriginalGameProfile() {
        return originalGameProfile;
    }

    public void giveBukkitPermission(String permission){
        if(this.permAttachment != null){
            this.permAttachment.setPermission(permission, true);
        }
    }

    public void takeBukkitPermission(String permission){
        if(this.permAttachment != null){
            this.permAttachment.setPermission(permission, false);
        }
    }

    public void sendGameLogMessage(int gameID){
        p.spigot().sendMessage(new ComponentBuilder(ServerSettingsManager.RUNNING_GAME.getPrefix()).append(getTranslatedMessage("Click here to view statistics of your game!")).color(net.md_5.bungee.api.ChatColor.YELLOW).event(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://thechest.eu/game/?id=" + gameID)).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.GREEN + "https://thechest.eu/game/?id=" + gameID))).create());
    }

    public ActiveNickData getNickData(){
        return this.nickData;
    }

    public void setNickData(ActiveNickData nickData){
        this.nickData = nickData;
    }

    public boolean isActive(VaultItem item){
        if(getActiveItems().contains(item)){
            return true;
        } else {
            for(VaultItem i : getActiveItems()){
                if(i.getID() == item.getID()) return true;
            }

            return false;
        }
    }

    public boolean isActive(int item){
        for(VaultItem i : getActiveItems()){
            if(i.getID() == item) return true;
        }

        return false;
    }

    public VaultItem getActiveItem(ItemCategory category){
        for(VaultItem i : getActiveItems()){
            if(i.getCategory() == category) return i;
        }

        return null;
    }

    public ArrayList<GamePerk> getGamePerks(){
        return this.unlockedPerks;
    }

    public boolean hasGamePerk(GamePerk perk){
        return hasGamePerk(perk.getID());
    }

    public boolean hasGamePerk(int id){
        for(GamePerk perk : getGamePerks()){
            if(perk.getID() == id) return true;
        }

        return false;
    }

    public void giveCrewCoins(int coins){
        if(coins > 0){
            ChestAPI.async(() -> {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("givecrewcoins");
                out.writeUTF(p.getName());
                out.writeUTF(String.valueOf(coins));

                p.sendPluginMessage(ChestAPI.getInstance(), "BungeeCord", out.toByteArray());
            });
        }
    }

    public void giveGamePerk(GamePerk perk){
        if(!hasGamePerk(perk)){
            unlockedPerks.add(perk);

            new BukkitRunnable(){
                @Override
                public void run() {
                    try {
                        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `gamePerks_unlocked` (`uuid`,`perk`) VALUES(?,?);");
                        ps.setString(1,p.getUniqueId().toString());
                        ps.setInt(2,perk.getID());
                        ps.executeUpdate();
                        ps.close();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(ChestAPI.getInstance());
        }
    }

    public int getChestsOpened(){
        return this.chestsOpened+this.startChestsOpened;
    }

    public void addChestsOpened(int i){
        this.chestsOpened += i;
    }

    public void disableItem(ItemCategory c){
        VaultItem activeItem = getActiveItem(c);

        if(activeItem != null){
            activeItems.remove(activeItem);

            new BukkitRunnable(){
                @Override
                public void run() {
                    try {
                        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `lobbyShop_boughtItems` SET `active` = ? WHERE `uuid` = ? AND `lobbyShopItem` = ?");
                        ps.setBoolean(1,false);
                        ps.setString(2,p.getUniqueId().toString());
                        ps.setInt(3,activeItem.getID());
                        ps.executeUpdate();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(ChestAPI.getInstance());
        }
    }

    public void setActiveItem(VaultItem item){
       if((item != null && hasUnlocked(item))){
           VaultItem currentItem = getActiveItem(item.getCategory());

           if(currentItem != null) activeItems.remove(currentItem);
           activeItems.add(item);
           new BukkitRunnable(){
               @Override
               public void run() {
                   if(currentItem != null){
                       try {
                           PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `lobbyShop_boughtItems` SET `active` = ? WHERE `uuid` = ? AND `lobbyShopItem` = ?");
                           ps.setBoolean(1,false);
                           ps.setString(2,p.getUniqueId().toString());
                           ps.setInt(3,currentItem.getID());
                           ps.executeUpdate();
                       } catch(Exception e){
                           e.printStackTrace();
                       }
                   }

                   try {
                       PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `lobbyShop_boughtItems` SET `active` = ? WHERE `uuid` = ? AND `lobbyShopItem` = ?");
                       ps.setBoolean(1,true);
                       ps.setString(2,p.getUniqueId().toString());
                       ps.setInt(3,item.getID());
                       ps.executeUpdate();
                   } catch(Exception e){
                       e.printStackTrace();
                   }
               }
           }.runTaskAsynchronously(ChestAPI.getInstance());
       }
    }

    public void unlockItem(VaultItem item){
        if(!getUnlockedItems().contains(item)){
            getUnlockedItems().add(item);

            new BukkitRunnable(){
                @Override
                public void run() {
                    try {
                        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `lobbyShop_boughtItems` (`uuid`,`lobbyShopItem`,`active`) VALUES(?,?,?);");
                        ps.setString(1,p.getUniqueId().toString());
                        ps.setInt(2,item.getID());
                        ps.setBoolean(3,false);
                        ps.executeUpdate();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(ChestAPI.getInstance());
        }
    }

    public Player getBukkitPlayer(){
        return this.p;
    }

    public int getCoins(){
        return this.startCoins+this.coins;
    }

    public void addCoins(int coins){
        for(int i = 0; i < coins; i++){
            //if((startCoins+this.coins+i)<=0) break;

            this.coins++;
        }

        if(currentBoard == ScoreboardType.LOBBY) updateScoreboard(ScoreboardType.LOBBY);
    }

    public void reduceCoins(int coins){
        for(int i = 0; i < coins; i++){
            if((startCoins+this.coins+(i/-1))<=0) break;

            this.coins--;
        }

        if(currentBoard == ScoreboardType.LOBBY) updateScoreboard(ScoreboardType.LOBBY);
    }

    public int getKeys(){
        return this.startKeys+this.keys;
    }

    public void addKeys(int keys){
        for(int i = 0; i < keys; i++){
            //if((startKeys+this.keys+i)<=0) break;

            this.keys++;
        }

        if(currentBoard == ScoreboardType.LOBBY) updateScoreboard(ScoreboardType.LOBBY);
    }

    public void reduceKeys(int keys){
        for(int i = 0; i < keys; i++){
            if((startKeys+this.keys+(i/-1))<=0) break;

            this.keys--;
        }

        if(currentBoard == ScoreboardType.LOBBY) updateScoreboard(ScoreboardType.LOBBY);
    }

    public int getVaultShards(){
        return this.startShards+this.shards;
    }

    public void addVaultShards(int shards){
        for(int i = 0; i < shards; i++){
            //if((startShards+this.shards+i)<=0) break;

            this.shards++;
        }
    }

    public void reduceVaultShards(int shards){
        for(int i = 0; i < shards; i++){
            if((startShards+this.shards+(i/-1))<=0) break;

            this.shards--;
        }
    }

    public int getResetTokens(){
        return this.startResetTokens+this.resetTokens;
    }

    public void addResetTokens(int tokens){
        for(int i = 0; i < tokens; i++){
            //if((startResetTokens+this.resetTokens+i)<=0) break;

            this.resetTokens++;
        }
    }

    public void reduceResetTokens(int tokens){
        for(int i = 0; i < tokens; i++){
            if((startResetTokens+this.resetTokens+(i/-1))<=0) break;

            this.resetTokens--;
        }
    }

    public void toggleVanish(){
        this.vanish = !vanish;
        updateVanishStatus();
        updateFameTitleAboveHead();

        PlayerToggleVanishEvent event = new PlayerToggleVanishEvent(p);
        Bukkit.getPluginManager().callEvent(event);
    }

    public boolean isVanished(){
        return this.vanish;
    }

    public void updateVanishStatus(){
        if(this.vanish){
            for(Player all : Bukkit.getOnlinePlayers()){
                if(all != p){
                    if(!ChestUser.getUser(all).hasPermission(Rank.MOD)){
                        all.hidePlayer(p);
                    }/* else {
                        all.showPlayer(p);
                    }*/
                }
            }
        } else {
            for(Player all : Bukkit.getOnlinePlayers()){
                if(all != p){
                    all.showPlayer(p);
                }
            }
        }
    }

    public Rank getRank(){
        return this.rank;
    }

    public Timestamp getFirstJoin(){
        return this.firstJoin;
    }

    public boolean hasPermission(Rank minRank){
        if(minRank == null){
            return true;
        } else {
            return rank.getID() >= minRank.getID();
        }
    }

    public long getPlaytime(){
        return this.playtime;
    }

    public String getTranslatedMessage(String originalMessage){
        if(currentLang != null && currentLang != Translation.getLanguage("EN")){
            if(currentLang.getPhrases().containsKey(originalMessage)){
                return ChatColor.translateAlternateColorCodes('&',currentLang.getPhrases().get(originalMessage));
            } else {
                return ChatColor.translateAlternateColorCodes('&',originalMessage);
            }
        } else {
            return ChatColor.translateAlternateColorCodes('&',originalMessage);
        }
    }

    public String getTranslatedMessage(String originalMessage, Object... format){
        return String.format(getTranslatedMessage(originalMessage), format);
    }

    public Translation getCurrentLanguage(){
        return this.currentLang;
    }

    public ArrayList<Integer> getClaimedLevelRewards(){
        return this.claimedLevelRewards;
    }

    public boolean hasClaimedLevelReward(int level){
        return this.claimedLevelRewards.contains(level);
    }

    public void markLevelRewardAsClaimed(int level){
        if(!hasClaimedLevelReward(level) && level >= 1){
            claimedLevelRewards.add(level);

            new BukkitRunnable(){
                @Override
                public void run() {
                    try {
                        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `claimedLevelRewards` (`uuid`,`level`) VALUES(?,?);");
                        ps.setString(1,p.getUniqueId().toString());
                        ps.setInt(2,level);
                        ps.executeUpdate();
                        ps.close();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(ChestAPI.getInstance());
        }
    }

    public void sendAfterGamePremiumAd(){
        if(!hasPermission(Rank.TITAN)){
            p.sendMessage(ChatColor.GOLD + StringUtils.LINE_SEPERATOR);
            p.sendMessage(" ");
            sendCenteredMessage(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Did you enjoy playing this minigame?"));
            sendCenteredMessage(ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Consider purchasing a rank to help support us!"));
            sendCenteredMessage(ChatColor.GREEN.toString() + ChatColor.BOLD.toString() + "https://store.thechest.eu");
            p.sendMessage(" ");
            p.sendMessage(ChatColor.GOLD + StringUtils.LINE_SEPERATOR);
        }
    }

    public boolean isNicked(){
        return getNickData() != null;
    }

    public String getNick(){
        return getNickData() != null ? getNickData().nick : null;
    }

    public void hideEntity(Entity e){
        if(e instanceof Player){
            p.hidePlayer((Player)e);
        } else {
            PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(e.getEntityId());
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
        }
    }

    private Color getColor(int i) {
        Color c = null;
        if(i==1){
            c=Color.AQUA;
        }
        if(i==2){
            c=Color.BLACK;
        }
        if(i==3){
            c=Color.BLUE;
        }
        if(i==4){
            c=Color.FUCHSIA;
        }
        if(i==5){
            c=Color.GRAY;
        }
        if(i==6){
            c=Color.GREEN;
        }
        if(i==7){
            c=Color.LIME;
        }
        if(i==8){
            c=Color.MAROON;
        }
        if(i==9){
            c=Color.NAVY;
        }
        if(i==10){
            c=Color.OLIVE;
        }
        if(i==11){
            c=Color.ORANGE;
        }
        if(i==12){
            c=Color.PURPLE;
        }
        if(i==13){
            c=Color.RED;
        }
        if(i==14){
            c=Color.SILVER;
        }
        if(i==15){
            c=Color.TEAL;
        }
        if(i==16){
            c=Color.WHITE;
        }
        if(i==17){
            c=Color.YELLOW;
        }

        return c;
    }

    public void playVictoryEffect(){
        VaultItem victoryEffect = getActiveItem(ItemCategory.VICTORY_EFFECTS);

        if(victoryEffect != null){
            if(victoryEffect.getID() == 3){
                for(int i = 0; i <= 5; i++){
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            Firework fw = (Firework) p.getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK);
                            FireworkMeta fwm = fw.getFireworkMeta();

                            Random r = new Random();

                            int rt = r.nextInt(4) + 1;
                            FireworkEffect.Type type = FireworkEffect.Type.BALL;
                            if (rt == 1) type = FireworkEffect.Type.BALL;
                            if (rt == 2) type = FireworkEffect.Type.BALL_LARGE;
                            if (rt == 3) type = FireworkEffect.Type.BURST;
                            if (rt == 4) type = FireworkEffect.Type.CREEPER;
                            if (rt == 5) type = FireworkEffect.Type.STAR;

                            //Get our random colours
                            int r1i = r.nextInt(17) + 1;
                            int r2i = r.nextInt(17) + 1;
                            Color c1 = getColor(r1i);
                            Color c2 = getColor(r2i);

                            FireworkEffect effect = FireworkEffect.builder().flicker(r.nextBoolean()).withColor(c1).withFade(c2).with(type).trail(r.nextBoolean()).build();

                            fwm.addEffect(effect);

                            int rp = r.nextInt(2) + 1;
                            fwm.setPower(rp);
                            fw.setFireworkMeta(fwm);
                        }
                    }.runTaskLater(ChestAPI.getInstance(), i*20);
                }
            } else if(victoryEffect.getID() == 150){
                EnderDragon dragon = (EnderDragon)p.getWorld().spawnEntity(p.getLocation(),EntityType.ENDER_DRAGON);
                if(dragon != null && !dragon.isDead()){
                    showingDragonVictoryEffect = true;

                    dragon.setPassenger(p);
                    ChestAPI.VICTORY_DRAGONS.put(p.getUniqueId().toString(),dragon);
                }
            }
        }
    }

    public void updateFameTitleAboveHead(){
        double height = 2.55;

        if(ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD){
            if(getActiveItem(ItemCategory.FAME_TITLE) != null && !isNicked() && showFameTitlePlate && !isVanished()){
                VaultItem title = getActiveItem(ItemCategory.FAME_TITLE);
                /*ArmorStand a = null;
                if(fameTitlePlate != null) a = fameTitlePlate;
                if(a == null) a = (ArmorStand)p.getWorld().spawnEntity(p.getEyeLocation(),EntityType.ARMOR_STAND);

                a.setArms(false);
                a.teleport(p.getLocation().add(0,0.15,0));
                a.setGravity(false);
                //p.setPassenger(a);
                a.setVisible(false);
                a.setSmall(false);
                a.setCustomNameVisible(true);

                fameTitlePlate = a;

                a.setCustomName(getActiveTitle().color + getActiveTitle().title);

                PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(a.getEntityId());
                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);*/

                Hologram h = null;
                if(fameTitlePlate != null) h = fameTitlePlate;
                if(h == null) h = HologramsAPI.createHologram(ChestAPI.getInstance(),p.getLocation().add(0,height,0));

                if(h.size() == 0){
                    fameTitlePlateLine = h.appendTextLine(title.getFameTitleColor() + title.getName());
                } else {
                    if(fameTitlePlateLine != null){
                        fameTitlePlateLine.setText(title.getFameTitleColor() + title.getName());
                    } else {
                        h.getLine(0).removeLine();
                        fameTitlePlateLine = h.appendTextLine(title.getFameTitleColor() + title.getName());
                    }
                }

                h.getVisibilityManager().setVisibleByDefault(true);
                h.getVisibilityManager().hideTo(p);

                h.teleport(p.getLocation().add(0,height,0));
                fameTitlePlate = h;
            } else {
                removeFameTitleAboveHead();
            }
        }
    }

    public void unnick(){
        if(isNicked()) p.performCommand("nick off");
    }

    public void removeFameTitleAboveHead(){
        if(ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD){
            if(fameTitlePlate != null){
                fameTitlePlate.delete();
                fameTitlePlate = null;
                fameTitlePlateLine = null;
            }
        }
    }

    public void setPlayerPrefix(String player, String prefix){
        player = StringUtils.limitString(player,16);

        Team t = null;
        if(getScoreboard().getTeam(player) != null){
            t = getScoreboard().getTeam(player);
        } else {
            t = getScoreboard().registerNewTeam(player);
        }

        if(!t.hasEntry(player)) t.addEntry(player);

        t.setPrefix(prefix);
    }

    public void setPlayerSuffix(String player, String suffix){
        player = StringUtils.limitString(player,16);

        Team t = null;
        if(getScoreboard().getTeam(player) != null){
            t = getScoreboard().getTeam(player);
        } else {
            t = getScoreboard().registerNewTeam(player);
        }

        if(!t.hasEntry(player)) t.addEntry(player);

        t.setSuffix(suffix);
    }

    public void updateName(){
        updateName(false);
    }

    public void updateName(boolean displayNameOnly){
        p.setDisplayName(getRank().getColor() + p.getName());
        p.setPlayerListName(null);

        if(!displayNameOnly && ServerSettingsManager.UPDATE_TAB_NAME_WITH_SCOREBOARD) {
            boolean isNicked = isNicked();
            String nick = getNick();

            for(Player all : Bukkit.getOnlinePlayers()){
                if(!ChestUser.isLoaded(all)) continue;

                ChestUser a = ChestUser.getUser(all);

                if(isNicked){
                    p.setDisplayName(ChatColor.GRAY + getNick());

                    Team tt = null;

                    for(Team t : a.getScoreboard().getTeams()){
                        if(t.hasEntry(nick)){
                            t.removeEntry(nick);
                        }

                        if(t.getName().equalsIgnoreCase("pl" + Rank.USER.getScoreboardChar() + "_" + StringUtils.limitString(nick,12))){
                            tt = t;
                        }
                    }

                    if(tt == null){
                        tt = a.getScoreboard().registerNewTeam("pl" + Rank.USER.getScoreboardChar() + "_" + StringUtils.limitString(nick,12));
                        tt.setPrefix(Rank.USER.getColor().toString());
                    }

                    tt.addEntry(nick);

                    if(a.getScoreboard() != null){
                        all.setScoreboard(a.getScoreboard());
                    }
                }

                Team tt = null;

                for(Team t : a.getScoreboard().getTeams()){
                    if(t.hasEntry(p.getName())){
                        t.removeEntry(p.getName());
                    }

                    if(t.getName().equalsIgnoreCase("pl" + getRank().getScoreboardChar() + "_" + StringUtils.limitString(p.getName(),12))){
                        tt = t;
                    }
                }

                if(tt == null){
                    tt = a.getScoreboard().registerNewTeam("pl" + getRank().getScoreboardChar() + "_" + StringUtils.limitString(p.getName(),12));
                }

                if(tt != null){
                    CrewTagData tag = PlayerUtilities.getCrewTagFromUUID(p.getUniqueId());
                    if(tag != null){
                        tt.setSuffix(" " + ChatColor.GRAY + "[" + ChatColor.YELLOW + tag + ChatColor.GRAY + "]");

                        /*if(tag.isLeader){
                            tt.setPrefix(ChatColor.YELLOW + "[" + tag.tag + "] " + getRank().getColor().toString());
                        } else {
                            tt.setPrefix(ChatColor.GRAY + "[" + tag.tag + "] " + getRank().getColor().toString());
                        }*/
                    }

                    tt.setPrefix(getRank().getColor().toString());

                    tt.addEntry(p.getName());
                }

                if(a.getScoreboard().getTeam("npcNameClear") == null){
                    Team clear = a.getScoreboard().registerNewTeam("npcNameClear");
                    clear.setNameTagVisibility(NameTagVisibility.NEVER);
                    clear.addEntry(ChatColor.GREEN.toString());
                }

                if(a.getScoreboard() != null){
                    all.setScoreboard(a.getScoreboard());
                }

                /*if(NickNamerAPI.getNickManager().isNicked(p.getUniqueId())){
                    p.setDisplayName(ChatColor.GRAY + NickNamerAPI.getNickManager().getNick(p.getUniqueId()));

                    String nick = NickNamerAPI.getNickManager().getNick(p.getUniqueId());
                    Team tt = null;

                    for(Team t : a.getScoreboard().getTeams()){
                        if(t.hasEntry(nick)){
                            t.removeEntry(nick);
                        }

                        if(t.getName().endsWith("_" + Rank.USER.getID())){
                            tt = t;
                        }
                    }

                    tt.addEntry(nick);
                    if(a.getScoreboard() != null){
                        all.setScoreboard(a.getScoreboard());
                    }
                }

                Team tt = null;

                for(Team t : a.getScoreboard().getTeams()){
                    if(t.hasEntry(p.getName())){
                        t.removeEntry(p.getName());
                    }

                    if(t.getName().endsWith("_" + getRank().getID())){
                        tt = t;
                    }
                }

                tt.addEntry(p.getName());
                if(a.getScoreboard() != null){
                    all.setScoreboard(a.getScoreboard());
                }*/
            }
        }
    }

    public void updateTabList(){
        if(ServerSettingsManager.RUNNING_GAME != GameType.NONE){
            BountifulAPI.sendTabTitle(p,
                    ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "TheChest" + ChatColor.YELLOW + ".eu" + "\n" +
                    ChatColor.GRAY + ServerSettingsManager.RUNNING_GAME.getName() + "\n"

                    ,

                    "\n" +
                     ChatColor.GREEN.toString() + "Website: " + ChatColor.GRAY.toString() + "https://thechest.eu" + "\n" +
                     ChatColor.GREEN.toString() + "TeamSpeak: " + ChatColor.GRAY.toString() + "thechest.eu"
            );
        } else {
            BountifulAPI.sendTabTitle(p,
                    ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "TheChest" + ChatColor.YELLOW + ".eu" + "\n"

                    ,

                    "\n" +
                    ChatColor.GREEN.toString() + "Website: " + ChatColor.GRAY.toString() + "https://thechest.eu" + "\n" +
                    ChatColor.GREEN.toString() + "TeamSpeak: " + ChatColor.GRAY.toString() + "thechest.eu"
            );
        }
    }

    public Scoreboard getScoreboard(){
        return this.scoreboard;
    }

    public ScoreboardType getCurrentBoard(){
        return this.currentBoard;
    }

    public void clearScoreboard(){
        updateScoreboard(null);
    }

    public int giveRandomChest(){
        return ChestAPI.giveRandomChest(p.getUniqueId());
    }

    public int giveChest(ArrayList<Integer> items){
        return ChestAPI.giveChest(p.getUniqueId(),items);
    }

    public void removeChest(int id){
        new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    if(getUnlockedChests().containsKey(id)){
                        getUnlockedChests().remove(id);

                        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("DELETE FROM `lobbyShop_unlockedCrates` WHERE `id` = ?");
                        ps.setInt(1,id);
                        ps.executeUpdate();
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(ChestAPI.getInstance());
    }

    public void updateScoreboard(ScoreboardType type){
        updateScoreboard(type,0);
    }

    public void updateScoreboard(ScoreboardType type, int reducePlayerAmount){
        if(this.scoreboard == null) this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(this.scoreboard);

        this.currentBoard = type;

        if(type == null){
            this.scoreboard.clearSlot(DisplaySlot.SIDEBAR);
            if(this.scoreboard.getObjective("side") != null) this.scoreboard.getObjective("side").unregister();
        } else if(type == ScoreboardType.LOBBY){
            String displayName = ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "TheChest" + ChatColor.YELLOW + ".eu";

            Objective o = this.scoreboard.getObjective(DisplaySlot.SIDEBAR);
            if(o != null){
                if(!o.getDisplayName().equals(displayName)){
                    this.scoreboard.clearSlot(DisplaySlot.SIDEBAR);
                    this.scoreboard.getObjective("side").unregister();

                    o = this.scoreboard.registerNewObjective("side","dummy");
                    o.setDisplayName(displayName);
                } else {
                    clearScoreboard();
                    updateScoreboard(type);
                    return;
                }
            } else {
                o = this.scoreboard.registerNewObjective("side","dummy");
                o.setDisplayName(displayName);
            }

            o.getScore("     ").setScore(12);
            o.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Rank") + ":").setScore(11);
            o.getScore(getRank().getColor() + getRank().getName()).setScore(10);
            o.getScore("    ").setScore(9);
            o.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Coins") + ":").setScore(8);
            o.getScore(ChatColor.WHITE.toString() + getCoins()).setScore(7);
            o.getScore("       ").setScore(6);
            o.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Keys") + ":").setScore(5);
            o.getScore(ChatColor.WHITE.toString() + getKeys() + " ").setScore(4);
            o.getScore("        ").setScore(3);
            o.getScore(ChatColor.YELLOW.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Lobby") + ":").setScore(2);
            if(ServerUtil.getServerName() == null){
                o.getScore(ChatColor.WHITE.toString() + "Fetching..").setScore(1);
            } else {
                o.getScore(ChatColor.WHITE.toString() + ServerUtil.getServerName()).setScore(1);
            }

            o.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else if(type == ScoreboardType.MAP_VOTING){
            Scoreboard b = getScoreboard();
            Objective o = null;
            if(b.getObjective(DisplaySlot.SIDEBAR) == null){
                o = b.registerNewObjective("side","dummy");
            } else {
                o = b.getObjective(DisplaySlot.SIDEBAR);
            }

            o.setDisplayName(ServerSettingsManager.RUNNING_GAME.getColor() + ServerSettingsManager.RUNNING_GAME.getName());
            o.setDisplaySlot(DisplaySlot.SIDEBAR);
            o.getScore(" ").setScore(15);
            b.resetScores(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Players") + ": " + ChatColor.YELLOW.toString() + (Bukkit.getOnlinePlayers().size()-1));
            b.resetScores(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Players") + ": " + ChatColor.YELLOW.toString() + (Bukkit.getOnlinePlayers().size()+1));
            if(reducePlayerAmount != 0) b.resetScores(ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Players") + ": " + ChatColor.YELLOW.toString() + (Bukkit.getOnlinePlayers().size()));
            o.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Players") + ": " + ChatColor.YELLOW.toString() + (Bukkit.getOnlinePlayers().size()-reducePlayerAmount)).setScore(14);
            o.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Min. Players") + ": " + ChatColor.YELLOW.toString() + ServerSettingsManager.MIN_PLAYERS).setScore(13);
            o.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Max. Players") + ": " + ChatColor.YELLOW.toString() + ServerSettingsManager.MAX_PLAYERS).setScore(12);
            o.getScore("     ").setScore(11);

            o.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("Server") + ": ").setScore(10);
            String s = StringUtils.limitString(ServerUtil.getServerName(),16);
            o.getScore(s).setScore(9);
            setPlayerPrefix(s,ChatColor.YELLOW.toString());

            o.getScore("  ").setScore(8);
            int i = 7;
            ArrayList<eu.thechest.chestapi.maps.Map> maps = new ArrayList<eu.thechest.chestapi.maps.Map>();
            for(eu.thechest.chestapi.maps.Map ma : ServerSettingsManager.VOTING_MAPS) maps.add(ma);
            Collections.sort(maps, new Comparator<eu.thechest.chestapi.maps.Map>() {
                public int compare(eu.thechest.chestapi.maps.Map m1, eu.thechest.chestapi.maps.Map m2) {
                    return MapVotingManager.getMapVotes(m2) - MapVotingManager.getMapVotes(m1);
                }
            });

            for(eu.thechest.chestapi.maps.Map m : maps){
                /*if(added != 0){
                    //b.resetScores(ChatColor.GREEN + StringUtils.limitString(m.getName(),7) + ": " + ChatColor.YELLOW + (getMapVotes(m)-added));
                    b.resetScores(StringUtils.limitString(m.getName(),16));
                }*/

                //o.getScore(ChatColor.GREEN + StringUtils.limitString(m.getName(),8) + ": " + ChatColor.YELLOW + getMapVotes(m)).setScore(i);
                o.getScore(StringUtils.limitString(m.getName(),16).trim()).setScore(i);
                setPlayerPrefix(StringUtils.limitString(m.getName(),16).trim(),ChatColor.GREEN.toString());
                setPlayerSuffix(StringUtils.limitString(m.getName(),16).trim(),": " + ChatColor.YELLOW + MapVotingManager.getMapVotes(m));

                i--;
            }

            o.getScore("    ").setScore(3);
            o.getScore(StringUtils.SCOREBOARD_LINE_SEPERATOR).setScore(2);
            o.getScore(StringUtils.SCOREBOARD_FOOTER_IP).setScore(1);
        }
    }

    public ArrayList<AchievementUnlock> getUnlockedAchievements(){
        return unlockedAchievements;
    }

    public boolean hasAchieved(Achievement a){
        for(AchievementUnlock u : unlockedAchievements){
            if(u.getAchievement().getID() == a.getID()){
                return true;
            }
        }

        return false;
    }

    public boolean hasUnlocked(VaultItem i){
        if(getUnlockedItems().contains(i)){
            return true;
        } else {
            for(VaultItem item : getUnlockedItems()){
                if(item.getID() == i.getID()) return true;
            }

            return false;
        }
    }

    public boolean hasUnlocked(int i){
        for(VaultItem item : getUnlockedItems()){
            if(item.getID() == i) return true;
        }

        return false;
    }

    public boolean hasAchieved(int id){
        return hasAchieved(Achievement.getAchievement(id));
    }

    public void achieve(int id){
        achieve(Achievement.getAchievement(id));
    }

    public void achieve(Achievement a){
        if(a != null){
            if(!hasAchieved(a)){
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        try {
                            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `unlocked_achievements` (`uuid`,`achievementID`) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
                            ps.setString(1,p.getUniqueId().toString());
                            ps.setInt(2,a.getID());
                            ps.executeUpdate();
                            ResultSet rs = ps.getGeneratedKeys();

                            int insertID = -1;

                            if(rs.next()) insertID = rs.getInt(1);

                            ps.close();
                            rs.close();

                            if(insertID != -1){
                                ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `unlocked_achievements` WHERE `id` = ?");
                                ps.setInt(1,insertID);
                                rs = ps.executeQuery();

                                if(rs.first()){
                                    AchievementUnlock u = new AchievementUnlock(rs.getInt("id"),p,rs.getInt("achievementID"),rs.getTimestamp("time"));

                                    unlockedAchievements.add(u);

                                    p.playSound(p.getEyeLocation(), Sound.LEVEL_UP, 1f, 1f);

                                    p.sendMessage(ChatColor.DARK_GREEN + StringUtils.LINE_SEPERATOR);
                                    sendCenteredMessage(ChatColor.YELLOW.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("ACHIEVEMENT UNLOCKED"));
                                    p.sendMessage("");
                                    sendCenteredMessage(ChatColor.WHITE + getTranslatedMessage(a.getTitle()));
                                    sendCenteredMessage(ChatColor.GRAY + getTranslatedMessage(a.getDescription()));
                                    p.sendMessage("");
                                    p.sendMessage(ChatColor.DARK_GREEN + StringUtils.LINE_SEPERATOR);

                                    giveExp(20);
                                } else {
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + getTranslatedMessage("Failed to give achievement! Could not retrieve data!") + " ID: " + insertID);
                                    return;
                                }

                                MySQLManager.getInstance().closeResources(rs,ps);
                            } else {
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + getTranslatedMessage("Failed to give achievement! Could not retrieve ID!") + " ID: " + a.getID());
                                return;
                            }
                        } catch(Exception e){
                            e.printStackTrace();
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + getTranslatedMessage("Failed to give achievement!") + " ID: " + a.getID());
                            return;
                        }
                    }
                }.runTaskAsynchronously(ChestAPI.getInstance());
            }
        }
    }

    /*public void sendCenteredMessage(String message){
        Player player = this.p;
        int CENTER_PX = 154;

        if(message == null || message.equals("")) player.sendMessage("");
        message = ChatColor.translateAlternateColorCodes('&', message);

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for(char c : message.toCharArray()){
            if(c == ChatColor.COLOR_CHAR){
                previousCode = true;
                continue;
            }else if(previousCode == true){
                previousCode = false;
                if(c == 'l' || c == 'L'){
                    isBold = true;
                    continue;
                }else isBold = false;
            }else{
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while(compensated < toCompensate){
            sb.append(" ");
            compensated += spaceLength;
        }
        player.sendMessage(sb.toString() + message);
    }*/

    public int getPing(){
        return ((CraftPlayer)p).getHandle().ping;
    }

    public void sendCenteredMessage(String message){
        sendCenteredMessage(message,true);
    }

    public String sendCenteredMessage(String message, boolean send){
        Player player = p;
        int CENTER_PX = 154;
        int MAX_PX = 250;

        message = ChatColor.translateAlternateColorCodes('&', message);
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        int charIndex = 0;
        int lastSpaceIndex = 0;
        String toSendAfter = null;
        String recentColorCode = "";
        for(char c : message.toCharArray()){
            if(c == '§'){
                previousCode = true;
                continue;
            }else if(previousCode == true){
                previousCode = false;
                recentColorCode = "§" + c;
                if(c == 'l' || c == 'L'){
                    isBold = true;
                    continue;
                }else isBold = false;
            }else if(c == ' ') lastSpaceIndex = charIndex;
            else{
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
            if(messagePxSize >= MAX_PX){
                toSendAfter = recentColorCode + message.substring(lastSpaceIndex + 1, message.length());
                message = message.substring(0, lastSpaceIndex + 1);
                break;
            }
            charIndex++;
        }
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while(compensated < toCompensate){
            sb.append(" ");
            compensated += spaceLength;
        }
        String s = sb.toString() + message;
        if(send) player.sendMessage(s);
        if(toSendAfter != null) sendCenteredMessage(toSendAfter);
        return s;
    }

    public void connect(String servername){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF("Connect");
            out.writeUTF(servername);
        } catch (IOException e) {
            e.printStackTrace();
        }

        p.sendPluginMessage(ChestAPI.getInstance(), "BungeeCord", stream.toByteArray());
    }

    public void connectToLobby(){
        ChestAPI.executeBungeeCommand(this.p.getName(),"hub");
    }

    public void setLanguage(Translation lang){
        PlayerLocaleChangeEvent event = new PlayerLocaleChangeEvent(p,this.currentLang,lang);
        Bukkit.getPluginManager().callEvent(event);

        this.currentLang = lang;

        for(TranslatedHologram h : TranslatedHologram.STORAGE) h.update(getBukkitPlayer());
    }

    public void swingArm(){
        for(Player all : Bukkit.getOnlinePlayers()) swingArm(all);
    }

    public void swingArm(Player a){
        EntityPlayer ep = ((CraftPlayer)p).getHandle();
        PacketPlayOutAnimation packet = new PacketPlayOutAnimation(ep,0);
        ((CraftPlayer)a).getHandle().playerConnection.sendPacket(packet);
    }

    public int getMinecraftVersion(){
        ViaAPI api = Via.getAPI();
        return api.getPlayerVersion(p.getUniqueId());
    }

    public boolean allowsFriendRequests(){
        return this.setting_friendRequests;
    }

    public void setFriendRequests(boolean b){
        this.setting_friendRequests = b;
    }

    public boolean allowsPrivateMessages(){
        return this.setting_privateMessages;
    }

    public void setPrivateMessages(boolean b){
        this.setting_privateMessages = b;
    }

    public boolean allowsPartyRequests(){
        return this.setting_partyRequests;
    }

    public void setPartyRequests(boolean b){
        this.setting_partyRequests = b;
    }

    public boolean allowsHeadSeat(){
        return this.setting_headSeat;
    }

    public void setHeadSeat(boolean b){
        this.setting_headSeat = b;
    }

    public boolean allowsChallengerRequests(){
        return this.setting_allowChallengerRequests;
    }

    public void setChallengerRequests(boolean b){
        this.setting_allowChallengerRequests = b;
    }

    public boolean enabledLobbySpeed(){
        return this.setting_lobbySpeed;
    }

    public void setLobbySpeed(boolean b){
        this.setting_lobbySpeed = b;
    }

    public boolean enabledNickUpdateSelf(){
        return this.setting_nickUpdateSelf;
    }

    public void setNickUpdateSelf(boolean b){
        this.setting_nickUpdateSelf = b;
    }

    public boolean enabledNickHide(){
        return this.setting_nickHide;
    }

    public void setHideNick(boolean b){
        this.setting_nickHide = b;
    }

    public int getLevel(){
        return this.level;
    }

    public double getEXP(){
        return this.exp;
    }

    public void giveExp(double p){
        if(!mayGetEXP()) return;
        double oldExp = this.exp;
        this.exp += p;

        checkLevelUp();
        updateLevelBar();
    }

    public void checkLevelUp(){
        int levels = 0;
        double required = 0;

        while((this.exp >= (required = calculateNeededEXP(this.level+levels))) && (this.level + levels < ChestAPI.MAX_PLAYER_LEVEL)){
            this.exp -= required;
            levels++;
        }

        if(levels > 0){
            levelUp(levels);
        }

        updateLevelBar();
    }

    public boolean mayGetEXP(){
        if(this.level >= ChestAPI.MAX_PLAYER_LEVEL){
            return false;
        } else {
            return true;
        }
    }

    public void updateLevelBar(){
        if(ServerSettingsManager.SHOW_LEVEL_IN_EXP_BAR){
            p.setLevel(this.level);
            p.setExp((float)(this.exp/calculateNeededEXP(this.level+1)));
        }
    }

    public void levelUp(int times){
        for(int i = 0; i < times; i++){
            if(mayGetEXP()){
                this.level++;
            }
        }

        p.playSound(p.getEyeLocation(),Sound.LEVEL_UP,1f,1f);
        p.sendMessage("");
        sendCenteredMessage(ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + getTranslatedMessage("LEVEL UP!"));
        sendCenteredMessage(ChatColor.GREEN + getTranslatedMessage("You are now level %l!").replace("%l",ChatColor.YELLOW.toString() + String.valueOf(this.level) + ChatColor.GREEN.toString()));
        p.sendMessage("");

        if(this.level >= ChestAPI.MAX_PLAYER_LEVEL) this.exp = 0;

        updateLevelBar();
        saveData();
    }

    public void bukkitReset(){
        p.setGameMode(GameMode.SURVIVAL);

        p.setMaxHealth(20);
        p.setHealth(p.getMaxHealth());

        p.setFireTicks(0);
        p.setFoodLevel(20);

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        p.setWalkSpeed(0.2f);
        p.setLevel(0);
        p.setExp(0f);

        p.setAllowFlight(false);
        p.setFlying(p.getAllowFlight());

        for(PotionEffect pe : p.getActivePotionEffects()) p.removePotionEffect(pe.getType());
    }

    public void saveSettings(){
        new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `users` SET `setting_friendRequests` = ?, `setting_privateMessages` = ?, `setting_partyRequests` = ?, `setting_headSeat` = ?, `setting_allowChallengerRequests` = ?, `setting_lobbySpeed` = ?, `setting_nickUpdateSelf` = ?, `setting_nickHide` = ?, `language` = ?, `vanish`=? WHERE `uuid` = ?");
                    ps.setBoolean(1,setting_friendRequests);
                    ps.setBoolean(2,setting_privateMessages);
                    ps.setBoolean(3,setting_partyRequests);
                    ps.setBoolean(4,setting_headSeat);
                    ps.setBoolean(5,setting_allowChallengerRequests);
                    ps.setBoolean(6,setting_lobbySpeed);
                    ps.setBoolean(7,setting_nickUpdateSelf);
                    ps.setBoolean(8,setting_nickHide);
                    ps.setString(9,currentLang.getLanguageKey());
                    ps.setBoolean(10,vanish);
                    ps.setString(11,p.getUniqueId().toString());
                    ps.executeUpdate();

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("reloadSettings");
                    out.writeUTF(p.getUniqueId().toString());

                    p.sendPluginMessage(ChestAPI.getInstance(), "BungeeCord", out.toByteArray());
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(ChestAPI.getInstance());
    }

    public void saveData(){
        new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    long newPlayTime = playtime;
                    Timestamp now = new Timestamp(System.currentTimeMillis());

                    long secondsToAdd = (now.getTime() - creationDate.getTime())/1000;
                    newPlayTime += secondsToAdd;

                    // don't update rank via spigot!
                    PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `users` SET `username` = ?, `coins`=`coins`+?, `resetTokens`=`resetTokens`+?, `keys`=`keys`+?, `vaultShards`=`vaultShards`+?, `playtime` = ?, `chestsOpened`=`chestsOpened`+?, `exp`=?, `level`=?, `lastMCVersion`=? WHERE `uuid` = ?");
                    ps.setString(1,p.getName());
                    ps.setInt(2,coins);
                    ps.setInt(3,resetTokens);
                    ps.setInt(4,keys);
                    ps.setInt(5,shards);
                    ps.setString(6,String.valueOf(newPlayTime));
                    ps.setInt(7,chestsOpened);
                    ps.setDouble(8,exp);
                    ps.setInt(9,level);
                    ps.setInt(10,getMinecraftVersion());
                    ps.setString(11,p.getUniqueId().toString());
                    ps.executeUpdate();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(ChestAPI.getInstance());
    }

    public static double calculateNeededEXP(int level){
        return 3*Math.pow(level,2)+16*level+32;
    }

    public static int calculateLevel(double exp){
        if(exp < 0){
            return 1;
        } else {
            int level = 1;

            while(calculateNeededEXP(level) <= exp){
                level++;
            }

            return level;
        }
    }
}
