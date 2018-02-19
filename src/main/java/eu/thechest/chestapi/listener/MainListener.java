package eu.thechest.chestapi.listener;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.mysql.jdbc.MySQLConnection;
import eu.the5zig.mod.server.api.ModUser;
import eu.the5zig.mod.server.api.events.The5zigModUserJoinEvent;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.cmd.MainExecutor;
import eu.thechest.chestapi.event.PlayerDataLoadedEvent;
import eu.thechest.chestapi.items.ItemCategory;
import eu.thechest.chestapi.items.ItemUtil;
import eu.thechest.chestapi.items.VaultItem;
import eu.thechest.chestapi.lang.TranslatedHologram;
import eu.thechest.chestapi.maps.Map;
import eu.thechest.chestapi.maps.MapVote;
import eu.thechest.chestapi.maps.MapVotingManager;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameState;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.server.ServerUtil;
import eu.thechest.chestapi.user.ActiveNickData;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.Rank;
import eu.thechest.chestapi.user.ScoreboardType;
import eu.thechest.chestapi.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.konsolas.aac.api.AACAPIProvider;
import me.konsolas.aac.api.PlayerViolationEvent;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutCustomPayload;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.nicknamer.api.NickNamerAPI;
import org.inventivetalent.nicknamer.api.event.NickNamerSelfUpdateEvent;
import org.inventivetalent.nicknamer.api.event.NickNamerUpdateEvent;
import org.inventivetalent.nicknamer.api.event.disguise.NickDisguiseEvent;
import org.inventivetalent.nicknamer.api.event.disguise.SkinDisguiseEvent;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by zeryt on 11.02.2017.
 */
public class MainListener implements Listener, PluginMessageListener {
    @EventHandler
    public void onLogin(PlayerLoginEvent e){
        Player p = e.getPlayer();

        if(ServerSettingsManager.VIP_JOIN && ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY){
            if(Bukkit.getOnlinePlayers().size() >= ServerSettingsManager.MAX_PLAYERS && e.getResult() == PlayerLoginEvent.Result.KICK_FULL){
                Rank rank = PlayerUtilities.getRankFromUUID(p.getUniqueId());
                if(rank.getID() >= Rank.PRO.getID()){
                    ArrayList<Player> potentials = new ArrayList<Player>();

                    for(Player pa : Bukkit.getOnlinePlayers()){
                        if(!ChestUser.getUser(pa).hasPermission(rank)){
                            potentials.add(p);
                        }
                    }

                    Collections.sort(potentials, new Comparator<Player>() {
                        public int compare(Player p1, Player p2) {
                            return ChestUser.getUser(p1).getRank().getID() - ChestUser.getUser(p2).getRank().getID();
                        }
                    });

                    if(potentials.size() > 0){
                        Player toKick = potentials.get(0);
                        toKick.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + ChestUser.getUser(toKick).getTranslatedMessage("You were kicked from the server to make space for a premium or staff member."));
                        ChestUser.getUser(toKick).connectToLobby();

                        e.setResult(PlayerLoginEvent.Result.ALLOWED);
                    } else {
                        e.setResult(PlayerLoginEvent.Result.KICK_FULL);
                        e.setKickMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "Sorry, but no slot could be reserved for you. Please try a different server.");
                    }
                } else {
                    e.setResult(PlayerLoginEvent.Result.KICK_FULL);
                    e.setKickMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "The server is full!");
                }
            }
        } else {
            if(e.getResult() == PlayerLoginEvent.Result.KICK_FULL){
                e.setKickMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "The server is full!");
            }
        }

        if(e.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST){
            Rank rank = PlayerUtilities.getRankFromUUID(p.getUniqueId());
            if(rank.getID() >= Rank.ADMIN.getID()){
                e.setResult(PlayerLoginEvent.Result.ALLOWED);
            } else {
                e.setKickMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You are not whitelisted on this server!");
            }
        }
    }

    @EventHandler
    public void onAACViolation(PlayerViolationEvent e){
        Player p = e.getPlayer();

        if(ChestUser.isLoaded(p)){
            ChestUser u = ChestUser.getUser(p);

            ChestAPI.async(() -> {
                try {
                    PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `aac_violations` (`uuid`,`hackType`,`message`,`violationLevel`,`server`) VALUES(?,?,?,?,?);");
                    ps.setString(1,p.getUniqueId().toString());
                    ps.setString(2,e.getHackType().toString());
                    ps.setString(3,e.getMessage());
                    ps.setInt(4,e.getViolations());
                    ps.setString(5,ServerUtil.getServerName());
                    ps.executeUpdate();
                    ps.close();
                } catch(Exception e1){
                    e1.printStackTrace();
                }
            });
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e){
        Player p = e.getPlayer();

        if(ChestUser.isLoaded(p)){
            ChestUser u = ChestUser.getUser(p);

            if(!e.isSneaking()){
                u.updateFameTitleAboveHead();
            } else {
                u.removeFameTitleAboveHead();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(CreatureSpawnEvent e){
        if(e.getEntity().getType() == EntityType.SILVERFISH || e.getEntity().getType() == EntityType.ENDERMITE){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPing(ServerListPingEvent e){
        //e.setMotd(ServerSettingsManager.CURRENT_GAMESTATE.getDisplay());
        if(ServerUtil.getMapName() == null){
            e.setMotd("Loading..");
        } else {
            e.setMotd(ServerUtil.getMapName());
        }

        e.setMaxPlayers(ServerSettingsManager.MAX_PLAYERS);
    }

    @EventHandler
    public void onDisguise(SkinDisguiseEvent e){
        if(ServerSettingsManager.ENABLE_NICK == false){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDisguise(NickDisguiseEvent e){
        if(ServerSettingsManager.ENABLE_NICK == false){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onUpdateSelf(NickNamerSelfUpdateEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onUpdate(NickNamerUpdateEvent e){
        Player p = e.getPlayer();
        Player p2 = e.getObserver();

        if(p == p2) e.setCancelled(true);

        if(p != null && p2 != null && p.isOnline() && p2.isOnline() && p != p2 && ChestUser.isLoaded(p) && ChestUser.isLoaded(p2)){
            ChestUser u = ChestUser.getUser(p);
            ChestUser u2 = ChestUser.getUser(p2);

            if(u2.hasPermission(Rank.VIP)){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onManipulate(PlayerArmorStandManipulateEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        if(ServerSettingsManager.PROTECT_ARMORSTANDS){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRefresh(org.inventivetalent.nicknamer.api.event.refresh.PlayerRefreshEvent e){
        e.setSelf(false);

        if(ChestUser.isLoaded(e.getPlayer())) ChestUser.getUser(e.getPlayer()).updateName();
    }

    @EventHandler
    public void onLoaded(PlayerDataLoadedEvent e){
        Player p = e.getPlayer();
        ChestUser u = e.getUser();
        ActiveNickData nickData = u.getNickData();

        new BukkitRunnable(){
            @Override
            public void run() {
                if(nickData != null){
                    NickNamerAPI.getNickManager().setNick(p.getUniqueId(), nickData.nick);
                    NickNamerAPI.getNickManager().setSkin(p.getUniqueId(), nickData.skin);
                    //NickNamerAPI.getNickManager().refreshPlayer(p.getUniqueId());
                } else {
                    boolean update = false;

                    if(NickNamerAPI.getNickManager().getNick(p.getUniqueId()) != null){
                        NickNamerAPI.getNickManager().removeNick(p.getUniqueId());
                        update = true;
                    }

                    if(NickNamerAPI.getNickManager().getSkin(p.getUniqueId()) != null){
                        update = true;
                        NickNamerAPI.getNickManager().removeSkin(p.getUniqueId());
                    }

                    //if(update) NickNamerAPI.getNickManager().refreshPlayer(p.getUniqueId());
                }
            }
        }.runTaskLaterAsynchronously(ChestAPI.getInstance(),10);

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY){
            if(ServerSettingsManager.MAP_VOTING){
                StringUtils.sendJoinMessage(p);
            }
        }

        /*if(u.hasPermission(Rank.VIP) && ServerSettingsManager.ENABLE_NICK == true && u.enabledAutoNick() && NickNamerAPI.getNickManager().isNicked(p.getUniqueId()) == false){
            p.performCommand("nick");
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(), new Runnable(){
                public void run(){
                    u.updateName();
                }
            },10);
        }*/
        Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(), new Runnable(){
            public void run(){
                for(Player all : Bukkit.getOnlinePlayers()){
                    if(ChestUser.isLoaded(all)){
                        ChestUser a = ChestUser.getUser(all);

                        a.updateName();
                        a.updateVanishStatus();
                    }
                }
            }
        },20);

        u.updateTabList();
        /*ChestAPI.sync(() -> {
            u.updateFameTitleAboveHead();
        });*/
        //u.achieve(1);

        HashMap<LabyModFeature,Boolean> laby = new HashMap<LabyModFeature,Boolean>();
        laby.put(LabyModFeature.DAMAGEINDICATOR,false);
        laby.put(LabyModFeature.MINIMAP_RADAR,false);

        setLabyModFeature(p,laby);

        if(ServerSettingsManager.AUTO_OP){
            p.setOp(u.hasPermission(Rank.CM));
        }

        if(u.acceptedReportCoins > 0){
            u.addCoins(u.acceptedReportCoins);
            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Your report has been accepted!");
            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GRAY + "+" + ChatColor.YELLOW + String.valueOf(u.acceptedReportCoins) + " " + ChatColor.GRAY + u.getTranslatedMessage("Coins"));

            u.acceptedReportCoins = 0;

            ChestAPI.async(() -> {
                try {
                    PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `users` SET `acceptedReportCoins` = ? WHERE `uuid` = ?");
                    ps.setInt(1,0);
                    ps.setString(2,p.getUniqueId().toString());
                    ps.executeUpdate();
                    ps.close();
                } catch(Exception e1){
                    e1.printStackTrace();
                }
            });
        }

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY){
            if(ServerSettingsManager.MAP_VOTING){
                ChestAPI.sync(() -> p.teleport(new Location(Bukkit.getWorld("waitingLobby"),8.504,32,11.3859,-90.666f,-16.7900f)));
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.setFireTicks(0);
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);

                p.setExp((float) ((double) MapVotingManager.lobbyCountdownCount / 60D));
                p.setLevel(MapVotingManager.lobbyCountdownCount);

                if(MapVotingManager.VOTING_OPEN) p.getInventory().addItem(ItemUtil.namedItem(Material.MAGMA_CREAM, org.bukkit.ChatColor.GREEN + u.getTranslatedMessage("Map Voting"), null));
                p.getInventory().setItem(8, ItemUtil.namedItem(Material.CHEST, org.bukkit.ChatColor.RED + u.getTranslatedMessage("Back to Lobby"), null));

                for(Player all : Bukkit.getOnlinePlayers()){
                    ChestUser.getUser(all).updateScoreboard(ScoreboardType.MAP_VOTING);
                }

                if(Bukkit.getOnlinePlayers().size() == ServerSettingsManager.MIN_PLAYERS){
                    MapVotingManager.startLobbyCountdown();
                }
            }
        }

        u.checkLevelUp();
        u.updateLevelBar();
        u.updateVanishStatus();

        for(Player all : Bukkit.getOnlinePlayers()){
            if(ChestUser.isLoaded(all)){
                ChestUser a = ChestUser.getUser(all);

                a.updateName();
                a.updateVanishStatus();
            }
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(),new Runnable(){
            @Override
            public void run() {
                if(u.hasPermission(Rank.PRO)) u.achieve(29);

                if(u.getPlaytime()/60/60 >= 10) u.achieve(36);
            }
        },2*20);

        if(u.hasPermission(Rank.MOD)){
            ChestAPI.sync(() -> {
                u.giveBukkitPermission("AAC.verbose");
                u.giveBukkitPermission("AAC.notify");
                u.giveBukkitPermission("AAC.bypass");

                AACAPIProvider.getAPI().reloadPermissionCache();
            });
        }

        for(TranslatedHologram h : TranslatedHologram.STORAGE) h.update(p);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();

        e.setJoinMessage(null);
        ChestUser.loadData(p);
    }

    @EventHandler
    public void on5zigJoin(The5zigModUserJoinEvent e){
        Player p = e.getPlayer();
        ModUser modUser = e.getModUser();

        modUser.getStatsManager().setLobby(ServerUtil.getServerName());
    }

    private void setLabyModFeature(Player p, HashMap<LabyModFeature, Boolean> list) {
        try {
            HashMap<String, Boolean> nList = new HashMap<String, Boolean>();
            for(LabyModFeature f : list.keySet()) {
                nList.put(f.name(), list.get(f));
            }
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(nList);
            ByteBuf a = Unpooled.copiedBuffer(byteOut.toByteArray());
            PacketDataSerializer b = new PacketDataSerializer(a);
            PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload("LABYMOD", b);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        e.setQuitMessage(null);

        if(MainExecutor.NICK_COOLDOWN.contains(p.getName())) MainExecutor.NICK_COOLDOWN.remove(p.getName());

        /*if(NickNamerAPI.getNickManager().isNicked(p.getUniqueId())){
            ChestAPI.NICKS.add(NickNamerAPI.getNickManager().getNick(p.getUniqueId()));

            NickNamerAPI.getNickManager().removeNick(p.getUniqueId());
            NickNamerAPI.getNickManager().removeSkin(p.getUniqueId());
        }*/

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY){
            if(ServerSettingsManager.MAP_VOTING){
                StringUtils.sendQuitMessage(p);

                ArrayList<MapVote> toRemove = new ArrayList<MapVote>();

                for(MapVote v : MapVotingManager.VOTES){
                    if(v.p == p) toRemove.add(v);
                }

                MapVotingManager.VOTES.removeAll(toRemove);

                if(Bukkit.getOnlinePlayers().size()-1 < ServerSettingsManager.MIN_PLAYERS){
                    MapVotingManager.cancelLobbyCountdown();
                }

                for(Player all : Bukkit.getOnlinePlayers()){
                    if(ChestUser.isLoaded(all)) ChestUser.getUser(all).updateScoreboard(ScoreboardType.MAP_VOTING,1);
                }
            }
        }

        u.saveData();

        if(ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD){
            u.removeFameTitleAboveHead();
        }

        if(ChestAPI.VICTORY_DRAGONS.containsKey(p.getUniqueId().toString())){
            EnderDragon dragon = ChestAPI.VICTORY_DRAGONS.get(p.getUniqueId().toString());
            dragon.remove();
            ChestAPI.VICTORY_DRAGONS.remove(p.getUniqueId().toString());
        }

        new BukkitRunnable(){
            @Override
            public void run() {
                ChestUser.unregister(p);
            }
        }.runTaskLater(ChestAPI.getInstance(),20);
        BarUtil.removeBar(p);
    }

    @EventHandler
    public void onExit(VehicleExitEvent e){
        if(e.getExited() instanceof Player){
            Player p = (Player)e.getExited();

            if(ChestAPI.VICTORY_DRAGONS.containsKey(p.getUniqueId().toString())){
                EnderDragon dragon = ChestAPI.VICTORY_DRAGONS.get(p.getUniqueId().toString());
                dragon.remove();
                ChestAPI.VICTORY_DRAGONS.remove(p.getUniqueId().toString());
            }
        }
    }

    @EventHandler
    public void onTabComplete(PlayerChatTabCompleteEvent e){
        e.getTabCompletions().clear();
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent e){
        if(e.getEntity() instanceof Player){
            Player p = (Player)e.getEntity();
            ChestUser u = ChestUser.getUser(p);

            if(ServerSettingsManager.ARROW_TRAILS && !e.isCancelled() && e.getProjectile() != null){
                VaultItem trail = u.getActiveItem(ItemCategory.ARROW_TRAILS);

                if(trail != null){
                    ChestAPI.ARROW_TRAILS.put(e.getProjectile(),trail);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        if(e.getEntity().getKiller() != null && e.getEntity().getKiller() != e.getEntity()){
            Player p = e.getEntity().getKiller();
            ChestUser u = ChestUser.getUser(p);

            if(ServerSettingsManager.KILL_EFFECTS){
                VaultItem killEffect = u.getActiveItem(ItemCategory.KILL_EFFECTS);

                if(killEffect != null){
                    Location loc = e.getEntity().getLocation();

                    if(killEffect.getID() == 4){
                        // LIGHTNING
                        p.getWorld().strikeLightningEffect(loc);
                    } else if(killEffect.getID() == 5){
                        // FLAMES
                        p.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 10);
                    } else if(killEffect.getID() == 9){
                        // EXPLOSION
                        ParticleEffect.EXPLOSION_LARGE.display(0f,0f,0f,0f,15,loc,30);
                        p.getWorld().playSound(loc, Sound.EXPLODE,1f,1f);
                    } else if(killEffect.getID() == 102){
                        // BLOOD
                        //loc.getWorld().playSound(loc, Sound.HURT_FLESH, 1F, 1F);
                        loc.getWorld().playSound(loc, Sound.STEP_STONE, 1F, 1F);
                        loc.getWorld().playEffect(loc.add(0.0D, 0.8D, 0.0D), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
                    } else if(killEffect.getID() == 151 || killEffect.getID() == 152 || killEffect.getID() == 153 || killEffect.getID() == 154){
                        String signText = null;
                        switch(killEffect.getID()){
                            case 151:
                                signText = "#rekt";
                                break;
                            case 152:
                                signText = "Get Good";
                                break;
                            case 153:
                                signText = "eZ";
                                break;
                            case 154:
                                signText = "Take the L";
                                break;
                        }

                        if(signText != null){
                            Hologram holo = HologramsAPI.createHologram(ChestAPI.getInstance(),loc.clone().add(0,1,0));
                            holo.appendTextLine(ChatColor.AQUA + signText);

                            new BukkitRunnable(){
                                @Override
                                public void run() {
                                    holo.delete();
                                }
                            }.runTaskLater(ChestAPI.getInstance(),2*20);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e){
        if(ChestAPI.ARROW_TRAILS.containsKey(e.getEntity())){
            ChestAPI.ARROW_TRAILS.remove(e.getEntity());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player){
            Player p = (Player)e.getWhoClicked();
            ChestUser u = ChestUser.getUser(p);
            Inventory inv = e.getInventory();

            if(ServerSettingsManager.CURRENT_GAMESTATE != GameState.LOBBY) return;
            if(ServerSettingsManager.MAP_VOTING == false) return;

            if(MapVotingManager.VOTING_OPEN == false){
                e.setCancelled(true);
                p.closeInventory();
                return;
            }

            if(inv.getName().equals("Map Voting")){
                e.setCancelled(true);
                if(e.getCurrentItem() != null && e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().getDisplayName() != null){
                    if(e.getRawSlot() == 1 || e.getRawSlot() == 3 || e.getRawSlot() == 5 || e.getRawSlot() == 7){
                        int mapToVoteFor = ((Double)((double)(e.getRawSlot()/2))).intValue();

                        if(ServerSettingsManager.VOTING_MAPS.size() > mapToVoteFor){
                            Map map = ServerSettingsManager.VOTING_MAPS.get(mapToVoteFor);

                            if(MapVotingManager.hasVoted(p)){
                                e.setCancelled(true);
                                p.closeInventory();
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + org.bukkit.ChatColor.RED + u.getTranslatedMessage("You've already voted for a map."));
                            } else {
                                e.setCancelled(true);
                                p.closeInventory();

                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + org.bukkit.ChatColor.GREEN + u.getTranslatedMessage("You've successfully voted for the map %m.").replace("%m", org.bukkit.ChatColor.YELLOW + map.getName() + org.bukkit.ChatColor.GREEN));

                                //SurvivalGames.VOTES.put(p.getName(),mapToVoteFor);
                                for(int i = 0; i < u.getRank().getVotingPower(); i++){
                                    MapVotingManager.VOTES.add(new MapVote(p,map));
                                }

                                for(Player all : Bukkit.getOnlinePlayers()){
                                    ChestUser.getUser(all).updateScoreboard(ScoreboardType.MAP_VOTING);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);
        String msg = e.getMessage();

        if(!u.mayChat){
            e.setCancelled(true);
            return;
        }

        if(u.isVanished()){
            e.setCancelled(true);
            p.playSound(p.getEyeLocation(),Sound.NOTE_BASS,1f,0.5f);
            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You can't do that in vanish mode."));
            return;
        }

        if(ServerSettingsManager.ENABLE_CHAT){
            if(ServerSettingsManager.ADJUST_CHAT_FORMAT){
                /*if(ServerSettingsManager.ENABLE_NICK){
                    e.setCancelled(true);

                    for(Player all : Bukkit.getOnlinePlayers()){
                        ChestUser a = ChestUser.getUser(all);

                        if(NickNamerAPI.getNickManager().isNicked(p.getUniqueId())){
                            String nick = NickNamerAPI.getNickManager().getNick(p.getUniqueId());

                            if(a.hasPermission(Rank.MOD)){
                                all.sendMessage(ChatColor.GRAY + nick + " " + ChatColor.AQUA + "(" + p.getName() + ")" + ChatColor.GRAY + ": " + ChatColor.WHITE + msg);
                            } else {
                                all.sendMessage(ChatColor.GRAY + nick + ": " + ChatColor.WHITE + msg);
                            }
                        } else {
                            if(u.getRank().getPrefix() != null){
                                all.sendMessage(u.getRank().getColor() + "[" + u.getRank().getPrefix() + "] " + p.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + msg);
                            } else {
                                all.sendMessage(u.getRank().getColor() + p.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + msg);
                            }
                        }
                    }

                    System.out.println("[CHAT] " + p.getName() + ": " + msg);
                } else {
                    if(u.getRank().getPrefix() != null){
                        e.setFormat(u.getRank().getColor() + "[" + u.getRank().getPrefix() + "] " + p.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + "%2$s");
                    } else {
                        e.setFormat(u.getRank().getColor() + p.getName() + ChatColor.GRAY + ": " + "%2$s");
                    }
                }*/

                if(ServerSettingsManager.ENABLE_NICK){
                    if(!e.isCancelled()){
                        e.setCancelled(true);

                        for(Player all : Bukkit.getOnlinePlayers()){
                            ChestUser a = ChestUser.getUser(all);

                            if(u.isNicked()){
                                String nick = u.getNick();

                                if(a.hasPermission(Rank.VIP)){
                                    //all.sendMessage(ChatColor.GRAY + nick + " " + ChatColor.AQUA + "(" + p.getName() + ")" + " " + ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + msg);
                                    all.sendMessage(ChatColor.DARK_GRAY + "<[" + ChatColor.AQUA + p.getName() + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + nick + ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + msg);
                                } else {
                                    //all.sendMessage(ChatColor.GRAY + nick + " " + ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + msg);
                                    all.sendMessage(ChatColor.DARK_GRAY + "<" + ChatColor.GRAY + nick + ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + msg);
                                }
                            } else {
                                if(u.getRank().getPrefix() != null){
                                    //all.sendMessage(u.getRank().getColor() + p.getName() + " " + ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + msg);
                                    all.sendMessage(ChatColor.DARK_GRAY + "<[" + ChatColor.GRAY + u.getRank().getPrefix() + ChatColor.DARK_GRAY + "] " + u.getRank().getColor() + p.getName() + ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + msg);
                                } else {
                                    //all.sendMessage(u.getRank().getColor() + p.getName() + " " + ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + msg);
                                    all.sendMessage(ChatColor.DARK_GRAY + "<" + u.getRank().getColor() + p.getName() + ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + msg);
                                }
                            }
                        }
                    }

                    System.out.println("[CHAT] " + p.getName() + ": " + msg);
                } else {
                    if(u.getRank().getPrefix() != null){
                        e.setFormat(u.getRank().getColor() + p.getName() + " " + ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + "%2$s");
                    } else {
                        e.setFormat(u.getRank().getColor() + p.getName() + " " + ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + "%2$s");
                    }
                }
            }

            if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.ENDING && e.getMessage().equalsIgnoreCase("gg")){
                u.achieve(2);
            }
        } else {
            e.setCancelled(true);
            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("The chat has been disabled."));
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e){
        Player p = e.getPlayer();

        if(ChestUser.isLoaded(p)){
            final ChestUser u = ChestUser.getUser(p);

            if(ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD){
                u.removeFameTitleAboveHead();

                Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(), new Runnable(){
                    @Override
                    public void run() {
                        u.updateFameTitleAboveHead();
                    }
                },10L);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e){
        Player p = e.getPlayer();
        final ChestUser u = ChestUser.getUser(p);

        if(ChestUser.isLoaded(p) && ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD){
            Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(), new Runnable(){
                @Override
                public void run() {
                    u.updateFameTitleAboveHead();
                }
            },10L);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e){
        Player p = e.getPlayer();

        if(ChestUser.isLoaded(p)){
            ChestUser u = ChestUser.getUser(p);

            ChestAPI.getInstance().doGroundCheck(p);

            if(ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD && !p.isSneaking()) u.updateFameTitleAboveHead();
        }
    }

    @EventHandler
    public void onInteract(EntityInteractEvent e){
        if(e.getEntity().getType() == EntityType.PLAYER) return;

        if(ServerSettingsManager.PROTECT_FARMS){
            if(e.getBlock() != null && e.getBlock().getType() == Material.SOIL){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
            if(e.getClickedBlock() != null){
                if(p.getInventory() != null && p.getItemInHand().getItemMeta() != null && p.getItemInHand().getItemMeta().getDisplayName() != null){
                    if(p.getItemInHand().getItemMeta().getDisplayName().equals(org.bukkit.ChatColor.RED + "BlockLoc")){
                        if(u.hasPermission(Rank.ADMIN)){
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "WORLD: " + e.getClickedBlock().getLocation().getWorld().getName());
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "X: " + e.getClickedBlock().getLocation().getBlockX());
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Y: " + e.getClickedBlock().getLocation().getBlockY());
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Z: " + e.getClickedBlock().getLocation().getBlockZ());
                        }
                    }
                }
            }
        }

        if(e.getAction() == Action.PHYSICAL){
            if(ServerSettingsManager.PROTECT_FARMS){
                if(e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.SOIL){
                    e.setCancelled(true);
                }
            }
        }

        if(ServerSettingsManager.MAP_VOTING && ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY){
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR){
                if(p.getItemInHand() != null && p.getItemInHand().getItemMeta() != null && p.getItemInHand().getItemMeta().getDisplayName() != null){
                    String dis = p.getItemInHand().getItemMeta().getDisplayName();

                    if(dis.equals(org.bukkit.ChatColor.GREEN + u.getTranslatedMessage("Map Voting"))){
                        if(MapVotingManager.VOTING_OPEN){
                            if(MapVotingManager.hasVoted(p)){
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + org.bukkit.ChatColor.RED + u.getTranslatedMessage("You've already voted for a map."));
                            } else {
                                Inventory inv = Bukkit.createInventory(null,9,"Map Voting");

                                inv.setItem(0,ItemUtil.namedItem(Material.STAINED_GLASS_PANE," ",null,15));
                                inv.setItem(2,ItemUtil.namedItem(Material.STAINED_GLASS_PANE," ",null,15));
                                inv.setItem(4,ItemUtil.namedItem(Material.STAINED_GLASS_PANE," ",null,15));
                                inv.setItem(6,ItemUtil.namedItem(Material.STAINED_GLASS_PANE," ",null,15));
                                inv.setItem(8,ItemUtil.namedItem(Material.STAINED_GLASS_PANE," ",null,15));

                                for(Map map : ServerSettingsManager.VOTING_MAPS){
                                    int votes = MapVotingManager.getMapVotes(map);

                                    inv.addItem(ItemUtil.namedItem(Material.EMPTY_MAP, org.bukkit.ChatColor.GREEN + map.getName(), new String[]{org.bukkit.ChatColor.GRAY + map.getAuthor()," ", org.bukkit.ChatColor.YELLOW.toString() + votes + org.bukkit.ChatColor.AQUA.toString() + " " + u.getTranslatedMessage("Votes")}));
                                }

                                /*int slot = 0;
                                for(Map map : SurvivalGames.VOTING_MAPS){
                                    int votes = 0;
                                    for(String ss : SurvivalGames.VOTES.keySet()){
                                        if(SurvivalGames.VOTES.get(ss) == slot) votes++;
                                    }

                                    inv.addItem(ItemUtil.namedItem(Material.EMPTY_MAP, ChatColor.GREEN + map.getName(), new String[]{ChatColor.GRAY + map.getAuthor()," ",ChatColor.YELLOW.toString() + votes + ChatColor.AQUA.toString() + " " + u.getTranslatedMessage("Votes")}));
                                    slot++;
                                }*/

                                p.openInventory(inv);
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + org.bukkit.ChatColor.RED + u.getTranslatedMessage("Voting isn't open right now."));
                        }
                    } else if(dis.equals(org.bukkit.ChatColor.RED + u.getTranslatedMessage("Back to Lobby"))){
                        u.connectToLobby();
                    }
                }
            }

            if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
                if(e.getClickedBlock() != null && e.getClickedBlock().getType() != null){
                    ArrayList<Material> DISALLOWED_BLOCKS = new ArrayList<Material>();
                    DISALLOWED_BLOCKS.add(Material.BREWING_STAND);
                    DISALLOWED_BLOCKS.add(Material.FURNACE);
                    DISALLOWED_BLOCKS.add(Material.BURNING_FURNACE);
                    DISALLOWED_BLOCKS.add(Material.WORKBENCH);
                    DISALLOWED_BLOCKS.add(Material.TRAP_DOOR);
                    DISALLOWED_BLOCKS.add(Material.CHEST);
                    DISALLOWED_BLOCKS.add(Material.TRAPPED_CHEST);
                    DISALLOWED_BLOCKS.add(Material.FENCE_GATE);
                    DISALLOWED_BLOCKS.add(Material.SPRUCE_FENCE_GATE);
                    DISALLOWED_BLOCKS.add(Material.BIRCH_FENCE_GATE);
                    DISALLOWED_BLOCKS.add(Material.JUNGLE_FENCE_GATE);
                    DISALLOWED_BLOCKS.add(Material.DARK_OAK_FENCE_GATE);
                    DISALLOWED_BLOCKS.add(Material.ACACIA_FENCE_GATE);
                    DISALLOWED_BLOCKS.add(Material.DIODE_BLOCK_OFF);
                    DISALLOWED_BLOCKS.add(Material.DIODE_BLOCK_ON);
                    DISALLOWED_BLOCKS.add(Material.REDSTONE_COMPARATOR_OFF);
                    DISALLOWED_BLOCKS.add(Material.REDSTONE_COMPARATOR_ON);
                    DISALLOWED_BLOCKS.add(Material.HOPPER);
                    DISALLOWED_BLOCKS.add(Material.DROPPER);
                    DISALLOWED_BLOCKS.add(Material.DISPENSER);
                    DISALLOWED_BLOCKS.add(Material.BED_BLOCK);
                    DISALLOWED_BLOCKS.add(Material.BEACON);
                    DISALLOWED_BLOCKS.add(Material.ANVIL);
                    DISALLOWED_BLOCKS.add(Material.ENCHANTMENT_TABLE);
                    //DISALLOWED_BLOCKS.add(Material.STONE_BUTTON);
                    //DISALLOWED_BLOCKS.add(Material.WOOD_BUTTON);
                    DISALLOWED_BLOCKS.add(Material.JUKEBOX);
                    DISALLOWED_BLOCKS.add(Material.NOTE_BLOCK);
                    DISALLOWED_BLOCKS.add(Material.LEVER);

                    if(DISALLOWED_BLOCKS.contains(e.getClickedBlock().getType())){
                        e.setCancelled(true);
                        e.setUseItemInHand(Event.Result.DENY);
                        e.setUseInteractedBlock(Event.Result.DENY);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        if(p.getGameMode() == GameMode.CREATIVE && u.hasPermission(Rank.STAFF)){
            Sign s = (Sign)e.getBlock().getState();

            for(int i = 0; i < 4; i++){
                e.setLine(i,ChatColor.translateAlternateColorCodes('&',e.getLine(i)));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e){
        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY && ServerSettingsManager.MAP_VOTING){
            e.setCancelled(true);
            return;
        }

        if(e.getEntity() instanceof ArmorStand){
            if(ServerSettingsManager.PROTECT_ARMORSTANDS){
                e.setCancelled(true);
            }
        }

        if(e.getEntity() instanceof Player){
            Player p = (Player)e.getEntity();

            if(ChestUser.getUser(p) != null && ChestUser.getUser(p).isLoaded(p)){
                ChestUser u = ChestUser.getUser(p);

                if(u.isVanished()){
                    e.setCancelled(true);
                }
            }
        }

        if(e.getEntity() instanceof EnderDragon){
            if(e.getEntity().getPassenger() != null && (e.getEntity().getPassenger() instanceof Player)){
                Player p = (Player)e.getEntity().getPassenger();
                ChestUser u = ChestUser.getUser(p);

                if(u.showingDragonVictoryEffect) e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageBy(EntityDamageByEntityEvent e){
        if(e.getEntity() instanceof ArmorStand){
            if(e.getDamager() instanceof Player){
                Player p = (Player)e.getDamager();

                if(ServerSettingsManager.PROTECT_ARMORSTANDS) e.setCancelled(true);
            }
        }

        if(e.getDamager() instanceof Player){
            Player p = (Player)e.getDamager();
            ChestUser u = ChestUser.getUser(p);

            if(u.isVanished()){
                e.setCancelled(true);
                p.playSound(p.getEyeLocation(),Sound.NOTE_BASS,1f,0.5f);
                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You can't do that in vanish mode."));
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e){
        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY && ServerSettingsManager.MAP_VOTING){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY && ServerSettingsManager.MAP_VOTING){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e){
        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY && ServerSettingsManager.MAP_VOTING){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickUp(PlayerPickupItemEvent e){
        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY && ServerSettingsManager.MAP_VOTING){
            e.setCancelled(true);
        }
    }

    //
    // ITEM FRAME LISTENERS
    //

    @EventHandler
    public void onDestroyByEntity(HangingBreakByEntityEvent e){
        if(!ServerSettingsManager.PROTECT_ITEM_FRAMES) return;

        if(e.getRemover() instanceof Player){
            Player p = (Player)e.getRemover();
            ChestUser u = ChestUser.getUser(p);

            if((e.getEntity().getType() == EntityType.ITEM_FRAME || e.getEntity().getType() == EntityType.PAINTING) && (p.getGameMode() != GameMode.CREATIVE || p.getGameMode() == GameMode.CREATIVE && !u.hasPermission(Rank.ADMIN))){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlaceByEntity(HangingPlaceEvent e){
        if(!ServerSettingsManager.PROTECT_ITEM_FRAMES) return;

        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        if((e.getEntity().getType() == EntityType.ITEM_FRAME || e.getEntity().getType() == EntityType.PAINTING) && (p.getGameMode() != GameMode.CREATIVE || p.getGameMode() == GameMode.CREATIVE && !u.hasPermission(Rank.ADMIN))){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void canRotate(PlayerInteractEntityEvent e){
        if(!ServerSettingsManager.PROTECT_ITEM_FRAMES) return;

        Entity entity = e.getRightClicked();
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        if(entity.getType() == EntityType.ITEM_FRAME){
            ItemFrame frame = (ItemFrame)entity;

            if(p.getGameMode() != GameMode.CREATIVE || p.getGameMode() == GameMode.CREATIVE && !u.hasPermission(Rank.ADMIN)){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void ItemRemoval(EntityDamageByEntityEvent e){
        if(!ServerSettingsManager.PROTECT_ITEM_FRAMES) return;

        if(e.getDamager() instanceof Player){
            Player p = (Player)e.getDamager();
            ChestUser u = ChestUser.getUser(p);

            if(e.getEntity().getType() == EntityType.ITEM_FRAME){
                if(p.getGameMode() != GameMode.CREATIVE || p.getGameMode() == GameMode.CREATIVE && !u.hasPermission(Rank.ADMIN)){
                    e.setCancelled(true);
                }
            }
        } else if(e.getDamager() instanceof Projectile && e.getEntity().getType() == EntityType.ITEM_FRAME){
            Projectile pr = (Projectile)e.getDamager();

            if(pr.getShooter() instanceof Player){
                Player p = (Player)pr.getShooter();
                ChestUser u = ChestUser.getUser(p);

                if(p.getGameMode() != GameMode.CREATIVE || p.getGameMode() == GameMode.CREATIVE && !u.hasPermission(Rank.ADMIN)){
                    e.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player p, byte[] message) {
        if(channel.equals("BungeeCord")){
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();

            if(subchannel.equals("GetServer")){
                String serverName = in.readUTF();
                ServerUtil.updateServerName(serverName);
            } else if(subchannel.equals("giveAchievement")){
                String data = in.readUTF();
                String playerName = data.split(":")[0];

                if(StringUtils.isValidInteger(data.split(":")[1])){
                    int achievementID = Integer.parseInt(data.split(":")[1]);

                    Player player = Bukkit.getPlayer(playerName);
                    if(player != null){
                        ChestUser.getUser(player).achieve(achievementID);
                    }
                }
            }
        }
    }
}
