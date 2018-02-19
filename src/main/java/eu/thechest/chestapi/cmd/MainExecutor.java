package eu.thechest.chestapi.cmd;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.event.NickChangeEvent;
import eu.thechest.chestapi.items.ItemUtil;
import eu.thechest.chestapi.maps.Map;
import eu.thechest.chestapi.maps.MapLocationType;
import eu.thechest.chestapi.maps.MapRatingManager;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.punish.PunishMenu;
import eu.thechest.chestapi.server.GameType;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ActiveNickData;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.Rank;
import eu.thechest.chestapi.util.PlayerUtilities;
import eu.thechest.chestapi.util.StringUtils;
import eu.thechest.chestapi.util.XenForoUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.nicknamer.api.NickNamerAPI;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Created by zeryt on 11.02.2017.
 */
public class MainExecutor implements CommandExecutor {
    public static ArrayList<String> NICK_COOLDOWN = new ArrayList<String>();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("ratemap")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(MapRatingManager.MAP_TO_RATE != null){
                    Map m = MapRatingManager.MAP_TO_RATE;

                    if(!MapRatingManager.hasRatedMap(p.getUniqueId(),m)){
                        if(args.length == 1){
                            if(StringUtils.isValidInteger(args[0])){
                                int rating = Integer.parseInt(args[0]);

                                if(rating >= 0 && rating <= 5){
                                    MapRatingManager.rateMap(p.getUniqueId(),m,rating);
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("You've successfully rated the map %m!").replace("%m",ChatColor.YELLOW + m.getName() + ChatColor.GREEN) + " " + ChatColor.GRAY + "(" + rating + ")");
                                } else {
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Please specify a rating between 1 and 5."));
                                }
                            } else {
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Please specify a rating between 1 and 5."));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Please specify a rating between 1 and 5."));
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You've already rated this map."));
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("No map is currently open for rating."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("fix")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(args.length == 1){
                    Player p2 = ChestAPI.getPlayer(args[0]);

                    if(p2 != null){
                        if(p != p2){
                            if(p.canSee(p2)){
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("Fixing player.."));
                                p.hidePlayer(p2);

                                new BukkitRunnable(){
                                    @Override
                                    public void run() {
                                        p.showPlayer(p2);
                                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("Der Spieler %s wurde gefixt.",ChestAPI.getDisplayName(p2,p) + ChatColor.GREEN));
                                    }
                                }.runTaskLater(ChestAPI.getInstance(),10);
                            } else {
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player cannot be fixed."));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You can't fix yourself."));
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player is not online."));
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <" + u.getTranslatedMessage("Player") + ">");
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("nick")){
            int cooldown = 3;

            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.VIP)){
                    if(args.length == 1){
                        if(args[0].equalsIgnoreCase("off")){
                            if(u.isNicked()){
                                NickNamerAPI.getNickManager().removeNick(p.getUniqueId());
                                NickNamerAPI.getNickManager().removeSkin(p.getUniqueId());
                                u.setNickData(null);

                                ChestAPI.async(() -> {
                                    u.updateName();

                                    try {
                                        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `active_nicks` SET `active` = ? WHERE `uuid` = ? AND `active` = ?");
                                        ps.setBoolean(1,false);
                                        ps.setString(2,p.getUniqueId().toString());
                                        ps.setBoolean(3,true);
                                        ps.executeUpdate();
                                        ps.close();

                                        p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.RED + u.getTranslatedMessage("Your nickname has been cleared."));
                                        p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.RED + u.getTranslatedMessage("You are now visible to others again."));

                                        NickChangeEvent event = new NickChangeEvent(p);
                                        Bukkit.getPluginManager().callEvent(event);

                                        if(ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD) u.updateFameTitleAboveHead();

                                        NICK_COOLDOWN.add(p.getName());
                                        Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(),new Runnable(){
                                            public void run(){
                                                NICK_COOLDOWN.remove(p.getName());
                                            }
                                        },cooldown*20);
                                    } catch(Exception e){
                                        e.printStackTrace();
                                    }
                                });
                            } else {
                                p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.RED + u.getTranslatedMessage("You are not nicked."));
                            }
                        } else if(args[0].equalsIgnoreCase("hide")){
                            if(!u.enabledNickHide()){
                                u.setHideNick(true);
                                p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.RED + u.getTranslatedMessage("You will no longer see your nicked name."));
                            } else {
                                u.setHideNick(false);
                                p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.GREEN + u.getTranslatedMessage("You will now see your nicked name again."));
                            }
                        } else {
                            if(NICK_COOLDOWN.contains(p.getName())){
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Please wait a little bit before executing this command again."));
                            } else {
                                String nick = args[0];
                                String skin = args[0];

                                if(nick.equalsIgnoreCase("random")){
                                    Collections.shuffle(ChestAPI.NICKS);
                                    nick = ChestAPI.NICKS.get(0);

                                    Collections.shuffle(ChestAPI.NICK_SKINS);
                                    skin = ChestAPI.NICK_SKINS.get(0);
                                }

                                if(Bukkit.getPlayer(nick) == null){
                                    NICK_COOLDOWN.add(p.getName());
                                    Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(),new Runnable(){
                                        public void run(){
                                            NICK_COOLDOWN.remove(p.getName());
                                        }
                                    },cooldown*20);

                                    final String finalNick = nick;
                                    final String finalSkin = skin;

                                    ChestAPI.async(() -> {
                                        try {
                                            PreparedStatement s = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `active_nicks` WHERE (`nick` = ? AND `active` = ?) OR (`skin` = ? AND `active` = ?)");
                                            s.setString(1,finalNick);
                                            s.setBoolean(2,true);
                                            s.setString(3,finalSkin);
                                            s.setBoolean(4,true);
                                            ResultSet ss = s.executeQuery();

                                            if(!ss.first()){
                                                UUID uuid = PlayerUtilities.getUUIDFromName(finalNick);

                                                if(uuid == null || (PlayerUtilities.getRankFromUUID(uuid).getID() < Rank.VIP.getID())){
                                                    NickNamerAPI.getNickManager().setNick(p.getUniqueId(),finalNick);
                                                    NickNamerAPI.getNickManager().setSkin(p.getUniqueId(),finalSkin);

                                                    if(u.enabledNickHide()){
                                                        p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.GREEN + u.getTranslatedMessage("Your nickname has been changed."));
                                                    } else {
                                                        p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.GREEN + u.getTranslatedMessage("You are now visible as %n.").replace("%n",ChatColor.YELLOW + finalNick + ChatColor.GREEN));
                                                    }

                                                    NickChangeEvent event = new NickChangeEvent(p);
                                                    Bukkit.getPluginManager().callEvent(event);

                                                    u.setNickData(new ActiveNickData(finalNick,finalSkin));

                                                    if(ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD) u.updateFameTitleAboveHead();

                                                    u.updateName();

                                                    PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `active_nicks` (`uuid`,`nick`,`skin`) VALUES(?,?,?);");
                                                    ps.setString(1,p.getUniqueId().toString());
                                                    ps.setString(2,finalNick);
                                                    ps.setString(3,finalSkin);
                                                    ps.executeUpdate();
                                                    ps.close();

                                                    NICK_COOLDOWN.add(p.getName());
                                                    Bukkit.getScheduler().scheduleSyncDelayedTask(ChestAPI.getInstance(),new Runnable(){
                                                        public void run(){
                                                            NICK_COOLDOWN.remove(p.getName());
                                                        }
                                                    },cooldown*20);
                                                } else {
                                                    p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.RED + u.getTranslatedMessage("Invalid nick."));
                                                }
                                            }

                                            MySQLManager.getInstance().closeResources(ss,s);
                                        } catch(Exception e){
                                            e.printStackTrace();
                                            p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.RED + u.getTranslatedMessage("An error occurred."));
                                        }
                                    });
                                } else {

                                }
                            }
                        }
                    } else {
                        p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.YELLOW + "/" + label + " random");
                        p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.YELLOW + "/" + label + " off");
                        p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.YELLOW + "/" + label + " <" + u.getTranslatedMessage("Name") + ">");
                        p.sendMessage(ChatColor.DARK_PURPLE + "[NICK] " + ChatColor.YELLOW + "/" + label + " hide");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("aacremovalnotice")){
            boolean exec = false;

            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                exec = u.hasPermission(Rank.ADMIN);
            } else {
                exec = true;
            }

            if(exec){
                for(Player p : Bukkit.getOnlinePlayers()){
                    ChestUser u = ChestUser.getUser(p);

                    p.sendMessage(ChatColor.DARK_RED + "[TAC] " + ChatColor.RED.toString() + ChatColor.BOLD.toString() + u.getTranslatedMessage("A player has been removed from your game for cheating."));
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("punish")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.MOD)){
                    if(args.length == 1){
                        String name = args[0];
                        UUID uuid = PlayerUtilities.getUUIDFromName(name);

                        if(uuid != null){
                            Rank rank = PlayerUtilities.getRankFromUUID(uuid);

                            if(u.hasPermission(rank)){
                                PunishMenu.openFor(p,uuid);
                            } else {
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Unknown UUID."));
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <" + u.getTranslatedMessage("Player") + ">");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("victoryeffect")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    u.playVictoryEffect();
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("swing")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    u.swingArm();
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("vote")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u =  ChestUser.getUser(p);

                String c = "      ";

                p.sendMessage(ChatColor.DARK_GREEN + StringUtils.LINE_SEPERATOR);
                u.sendCenteredMessage(ChatColor.YELLOW.toString() + u.getTranslatedMessage("Get 50 coins per vote!"));
                p.sendMessage(" ");
                p.spigot().sendMessage(new ComponentBuilder(c).append("PlanetMinecraft: ").color(net.md_5.bungee.api.ChatColor.GREEN).append(" ").append("[Vote]").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://www.planetminecraft.com/server/thechesteu-unique-gamemodes-multi-language-system-friendly-staff/vote/")).create());
                p.spigot().sendMessage(new ComponentBuilder(c).append("Minecraft-Server.eu: ").color(net.md_5.bungee.api.ChatColor.GREEN).append(" ").append("[Vote]").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://minecraft-server.eu/vote/index/125814")).create());
                p.spigot().sendMessage(new ComponentBuilder(c).append("Minecraft-Server-List.com: ").color(net.md_5.bungee.api.ChatColor.GREEN).append(" ").append("[Vote]").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL,"http://minecraft-server-list.com/server/402468/vote/")).create());
                p.spigot().sendMessage(new ComponentBuilder(c).append("MinecraftServers.org: ").color(net.md_5.bungee.api.ChatColor.GREEN).append(" ").append("[Vote]").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL,"http://minecraftservers.org/vote/448926")).create());
                p.spigot().sendMessage(new ComponentBuilder(c).append("Minecraft-Server-List.org: ").color(net.md_5.bungee.api.ChatColor.GREEN).append(" ").append("[Vote]").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL,"http://www.minecraft-servers-list.org/index.php?a=in&u=Zeryther")).create());
                p.spigot().sendMessage(new ComponentBuilder(c).append("TopG.org: ").color(net.md_5.bungee.api.ChatColor.GREEN).append(" ").append("[Vote]").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL,"http://topg.org/Minecraft/in-467206")).create());
                p.sendMessage(ChatColor.DARK_GREEN + StringUtils.LINE_SEPERATOR);

                //p.spigot().sendMessage(new ComponentBuilder("MineVote.org: ").color(net.md_5.bungee.api.ChatColor.GREEN).append(" ").append("[Vote]").color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://www.minevote.org/server/?/109/")).create());
            }
        }

        if(cmd.getName().equalsIgnoreCase("bungeecmd")){
            if(!(sender instanceof Player)){
                if(args.length > 0){
                    StringBuilder sb = new StringBuilder("");
                    for (int i = 0; i < args.length; i++) {
                        sb.append(args[i]).append(" ");
                    }
                    String s = sb.toString();

                    ChestAPI.executeBungeeCommand("BungeeConsole",s);
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("fly")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(ServerSettingsManager.RUNNING_GAME != GameType.NONE && !u.hasPermission(Rank.ADMIN)){
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You can't use this command ingame."));
                    return false;
                }

                if(u.hasPermission(Rank.MOD)){
                    if(args.length == 0){
                        p.setAllowFlight(!p.getAllowFlight());

                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GRAY + u.getTranslatedMessage("Fly mode") + ": " + ChatColor.WHITE + Boolean.valueOf(p.getAllowFlight()).toString());
                    } else if(args.length == 1){
                        if(u.hasPermission(Rank.ADMIN)){
                            Player p2 = Bukkit.getPlayer(args[0]);

                            if(p2 != null){
                                ChestUser u2 = ChestUser.getUser(p2);

                                p2.setAllowFlight(!p2.getAllowFlight());
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GRAY + u.getTranslatedMessage("Fly mode") + ": " + ChatColor.WHITE + Boolean.valueOf(p2.getAllowFlight()).toString());
                                p2.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GRAY + u2.getTranslatedMessage("Fly mode") + ": " + ChatColor.WHITE + Boolean.valueOf(p2.getAllowFlight()).toString());
                            } else {
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player is not online."));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " [" + u.getTranslatedMessage("Player") + "]");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                if(args.length == 0){
                    sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
                } else if(args.length == 1) {
                    Player p2 = Bukkit.getPlayer(args[0]);

                    if(p2 != null){
                        ChestUser u2 = ChestUser.getUser(p2);

                        p2.setAllowFlight(!p2.getAllowFlight());
                        sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GRAY + "Fly mode" + ": " + ChatColor.WHITE + Boolean.valueOf(p2.getAllowFlight()).toString());
                        p2.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GRAY + u2.getTranslatedMessage("Fly mode") + ": " + ChatColor.WHITE + Boolean.valueOf(p2.getAllowFlight()).toString());
                    } else {
                        sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "That player is not online.");
                    }
                } else {
                    sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " [Player]");
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("vanish")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(args.length == 0){
                    if(u.hasPermission(Rank.MOD)){
                        u.toggleVanish();
                        u.saveSettings();

                        for(Player all : Bukkit.getOnlinePlayers()){
                            ChestUser a = ChestUser.getUser(all);

                            if(a.hasPermission(Rank.MOD)){
                                if(u.isVanished()){
                                    all.sendMessage(ChatColor.YELLOW + a.getTranslatedMessage("%p is now vanished.").replace("%p",p.getName()));
                                } else {
                                    all.sendMessage(ChatColor.YELLOW + a.getTranslatedMessage("%p is no longer vanished.").replace("%p",p.getName()));
                                }
                            }
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                    }
                } else {
                    if(u.hasPermission(Rank.ADMIN)){
                        Player p2 = Bukkit.getPlayer(args[0]);
                        ChestUser u2 = ChestUser.getUser(p2);

                        if(p2 != null){
                            if(!u2.hasPermission(u.getRank())){
                                if(u2.hasPermission(Rank.MOD)){
                                    u2.toggleVanish();

                                    for(Player all : Bukkit.getOnlinePlayers()){
                                        ChestUser a = ChestUser.getUser(all);

                                        if(a.hasPermission(Rank.MOD)){
                                            all.sendMessage(ChatColor.YELLOW + a.getTranslatedMessage("%p toggled %c's vanish status.").replace("%p",p.getName()).replace("%c",p2.getName()));
                                        }
                                    }
                                } else {
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player can't enter vanish mode."));
                                }
                            } else {
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't allowed to change the status of that player."));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player is not online."));
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                    }
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("giverandomchest")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    if(args.length == 1){
                        String name = args[0];
                        UUID uuid = PlayerUtilities.getUUIDFromName(name);

                        if(uuid != null){
                            ChestAPI.async(() -> {
                                int i = ChestAPI.giveRandomChest(uuid);

                                if(i > 0){
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("Success!"));
                                } else {
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("An error occured."));
                                }
                            });
                        } else {
                            sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Unknown UUID."));
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <Player>");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("switch")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    if(args.length == 1) {
                        Player p2 = Bukkit.getPlayer(args[0]);

                        if(p2 != null){
                            Location o = p.getLocation();

                            p.teleport(p2.getLocation());
                            p2.teleport(o);

                            for(Player all : Bukkit.getOnlinePlayers()){
                                ChestUser a = ChestUser.getUser(all);

                                if(a.hasPermission(Rank.ADMIN)) all.sendMessage(ChatColor.YELLOW + u.getTranslatedMessage("%p switched %c with %d.").replace("%p",p.getName()).replace("%c",p2.getName()).replace("%d",p.getName()));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player is not online."));
                        }
                    } else if(args.length == 2){
                        Player p1 = Bukkit.getPlayer(args[0]);
                        Player p2 = Bukkit.getPlayer(args[1]);

                        if(p2 != null && p1 != null){
                            Location o = p1.getLocation();

                            p1.teleport(p2.getLocation());
                            p2.teleport(o);

                            for(Player all : Bukkit.getOnlinePlayers()){
                                ChestUser a = ChestUser.getUser(all);

                                if(a.hasPermission(Rank.ADMIN)) all.sendMessage(ChatColor.YELLOW + u.getTranslatedMessage("%p switched %c with %d.").replace("%p",p.getName()).replace("%c",p2.getName()).replace("%d",p1.getName()));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player is not online."));
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <" + u.getTranslatedMessage("Player") + ">" + " [" + u.getTranslatedMessage("Player") + "]");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                if(args.length == 2){
                    Player p1 = Bukkit.getPlayer(args[0]);
                    Player p2 = Bukkit.getPlayer(args[1]);

                    if(p2 != null && p1 != null){
                        Location o = p1.getLocation();

                        p1.teleport(p2.getLocation());
                        p2.teleport(o);

                        for(Player all : Bukkit.getOnlinePlayers()){
                            ChestUser a = ChestUser.getUser(all);

                            if(a.hasPermission(Rank.ADMIN)) all.sendMessage(ChatColor.YELLOW + a.getTranslatedMessage("%p switched %c with %d.").replace("%p","CONSOLE").replace("%c",p2.getName()).replace("%d",p1.getName()));
                        }

                        System.out.println("Switched " + p1.getName() + " with " + p2.getName() + ".");
                    } else {
                        sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "That player is not online.");
                    }
                } else {
                    sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <" + "Player" + ">" + " <" + "Player" + ">");
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("smite")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    if(args.length == 1){
                        Player p2 = Bukkit.getPlayer(args[0]);

                        if(p2 != null){
                            p2.getWorld().strikeLightning(p2.getLocation());

                            for(Player all : Bukkit.getOnlinePlayers()){
                                ChestUser a = ChestUser.getUser(all);

                                if(a.hasPermission(Rank.ADMIN)) all.sendMessage(ChatColor.YELLOW + a.getTranslatedMessage("%p smited %c.").replace("%p",p.getName()).replace("%c",p2.getName()));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player is not online."));
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <" + u.getTranslatedMessage("Player") + ">");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                if(args.length == 1){
                    Player p2 = Bukkit.getPlayer(args[0]);

                    if(p2 != null){
                        p2.getWorld().strikeLightning(p2.getLocation());

                        for(Player all : Bukkit.getOnlinePlayers()){
                            ChestUser a = ChestUser.getUser(all);

                            if(a.hasPermission(Rank.ADMIN)) all.sendMessage(ChatColor.YELLOW + a.getTranslatedMessage("%p smited %c.").replace("%p","CONSOLE").replace("%c",p2.getName()));
                        }

                        System.out.println("Smited " + p2.getName() + ".");
                    } else {
                        sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "That player is not online.");
                    }
                } else {
                    sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <" + "Player" + ">");
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("coins")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GRAY + u.getTranslatedMessage("Coins") + ": " + ChatColor.WHITE + u.getCoins());
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("clearchat")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.SR_MOD)){
                    for(Player all : Bukkit.getOnlinePlayers()){
                        ChestUser a = ChestUser.getUser(all);

                        if(a.hasPermission(Rank.STAFF)){
                            all.sendMessage(" ");
                            all.sendMessage(ChatColor.GREEN + a.getTranslatedMessage("%p has cleared the chat.").replace("%p",p.getName()));
                            all.sendMessage(" ");
                        } else {
                            for(int i = 0; i < 250; i++){
                                all.sendMessage(" ");
                            }

                            all.sendMessage(ChatColor.RED + a.getTranslatedMessage("A moderator has cleared the chat."));
                            all.sendMessage(" ");
                        }
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                for(Player all : Bukkit.getOnlinePlayers()){
                    ChestUser a = ChestUser.getUser(all);

                    if(a.hasPermission(Rank.STAFF)){
                        all.sendMessage(" ");
                        all.sendMessage(ChatColor.GREEN + a.getTranslatedMessage("%p has cleared the chat.").replace("%p","CONSOLE"));
                        all.sendMessage(" ");
                    } else {
                        for(int i = 0; i < 70; i++){
                            all.sendMessage(" ");
                        }

                        all.sendMessage(ChatColor.RED + a.getTranslatedMessage("A moderator has cleared the chat."));
                        all.sendMessage(" ");
                    }
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("skull")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if((ServerSettingsManager.RUNNING_GAME == GameType.NONE && u.hasPermission(Rank.BUILD_TEAM)) || (ServerSettingsManager.RUNNING_GAME != GameType.NONE && u.hasPermission(Rank.ADMIN))){
                    if(args.length == 1){
                        String name = args[0];

                        ItemStack head = new ItemStack(Material.SKULL_ITEM);
                        head.setDurability((short)3);
                        SkullMeta m = (SkullMeta)head.getItemMeta();
                        m.setOwner(name);
                        head.setItemMeta(m);
                        p.getInventory().addItem(head);

                        p.sendMessage(ChatColor.GREEN + ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("There you go!"));
                    } else {
                        sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " [Name]");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }



        if(cmd.getName().equalsIgnoreCase("addmaplocation")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    if(args.length == 2){
                        if(StringUtils.isValidInteger(args[0])){
                            ChestAPI.async(() -> {
                                int mapID = Integer.parseInt(args[0]);
                                MapLocationType type = MapLocationType.valueOf(args[1]);

                                try {
                                    PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `map_locations` (`mapID`,`x`,`y`,`z`,`yaw`,`pitch`,`type`,`addedBy`) VALUES(?,?,?,?,?,?,?,?)");
                                    ps.setInt(1,mapID);
                                    ps.setDouble(2,p.getLocation().getX());
                                    ps.setDouble(3,p.getLocation().getY());
                                    ps.setDouble(4,p.getLocation().getZ());
                                    ps.setFloat(5,p.getLocation().getYaw());
                                    ps.setFloat(6,p.getLocation().getPitch());
                                    ps.setString(7,type.toString());
                                    ps.setString(8,p.getUniqueId().toString());
                                    ps.execute();
                                    ps.close();

                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("Location added. Remove via webinterface"));
                                } catch(Exception e){
                                    e.printStackTrace();
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("An error occured."));
                                }
                            });
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <Map ID>");
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <Map ID> <Location Type>");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("memory")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    Runtime runtime = Runtime.getRuntime();
                    long total = runtime.totalMemory();
                    long free = runtime.freeMemory();
                    long used = total - free;

                    sender.sendMessage("");
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "[===== " + ChatColor.YELLOW + "Memory info" + ChatColor.LIGHT_PURPLE + " =====]");
                    sender.sendMessage("");
                    sender.sendMessage(ChatColor.AQUA + "Total Memory: " + ChatColor.YELLOW + (total / 1048576L) + " MB");
                    sender.sendMessage(ChatColor.AQUA + "Used Memory: " + ChatColor.YELLOW + (used / 1048576L) + " MB");
                    sender.sendMessage(ChatColor.AQUA + "Free Memory: " + ChatColor.YELLOW + (free / 1048576L) + " MB");
                    sender.sendMessage("");
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "[===== " + ChatColor.YELLOW + "Memory info" + ChatColor.LIGHT_PURPLE + " =====]");
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                Runtime runtime = Runtime.getRuntime();
                long total = runtime.totalMemory();
                long free = runtime.freeMemory();
                long used = total - free;

                sender.sendMessage("");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "[===== " + ChatColor.YELLOW + "Memory info" + ChatColor.LIGHT_PURPLE + " =====]");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.AQUA + "Total Memory: " + ChatColor.YELLOW + (total / 1048576L) + " MB");
                sender.sendMessage(ChatColor.AQUA + "Used Memory: " + ChatColor.YELLOW + (used / 1048576L) + " MB");
                sender.sendMessage(ChatColor.AQUA + "Free Memory: " + ChatColor.YELLOW + (free / 1048576L) + " MB");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "[===== " + ChatColor.YELLOW + "Memory info" + ChatColor.LIGHT_PURPLE + " =====]");
            }
        }

        if(cmd.getName().equalsIgnoreCase("langdump")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "MCL: " + p.spigot().getLocale());
                if(u.getCurrentLanguage() == null){
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "CRL: " + "null");
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "CRL: " + u.getCurrentLanguage().getLanguageKey());
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("register")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(args.length < 1){
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Please specify your email address."));
                } else {
                    ChestAPI.async(() -> {
                        Random ran = new Random();
                        long x = (long) ran.nextInt(Integer.MAX_VALUE) + 999999L;
                        String registrationStatus = XenForoUtil.registerUser(sender.getName(), x + "", args[0]);
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.YELLOW + u.getTranslatedMessage(registrationStatus));

                        if(registrationStatus.equals("Successfully registered on the forums! A confirmation email should be sent to your email address shortly! Please change your password once you confirm your account!")){
                            u.achieve(28);

                            new BukkitRunnable(){
                                @Override
                                public void run() {
                                    try {
                                        PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `users` SET `requiresForumsRankUpdate` = ? WHERE `uuid` = ?");
                                        ps.setBoolean(1,true);
                                        ps.setString(2,p.getUniqueId().toString());
                                        ps.executeUpdate();
                                        ps.close();
                                    } catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }.runTaskLaterAsynchronously(ChestAPI.getInstance(),3*20);
                        }
                    });
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("getpos")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "WORLD: " + p.getLocation().getWorld().getName());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "X: " + p.getLocation().getX());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Y: " + p.getLocation().getY());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Z: " + p.getLocation().getZ());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "YAW: " + p.getLocation().getYaw());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "PITCH: " + p.getLocation().getPitch());
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("blockloc")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    p.getInventory().addItem(ItemUtil.namedItem(Material.BLAZE_ROD,ChatColor.RED + "BlockLoc",null));
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        if(cmd.getName().equalsIgnoreCase("gamemode")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    if(args.length == 1){
                        String mode = args[0];

                        GameMode g = null;

                        for(GameMode gg : GameMode.values()){
                            if(gg.toString().toLowerCase().startsWith(mode)){
                                g = gg;
                            }
                        }

                        if(mode.equals("0")){
                            g = GameMode.SURVIVAL;
                        } else if(mode.equals("1")){
                            g = GameMode.CREATIVE;
                        } else if(mode.equals("2")){
                            g = GameMode.ADVENTURE;
                        } else if(mode.equals("3")){
                            g = GameMode.SPECTATOR;
                        }

                        if(g != null){
                            p.setGameMode(g);

                            for(Player all : Bukkit.getOnlinePlayers()){
                                ChestUser a = ChestUser.getUser(all);

                                if(a.hasPermission(Rank.ADMIN)){
                                    all.sendMessage(ChatColor.YELLOW + a.getTranslatedMessage("%p changed %p's gamemode to " + g.toString()).replace("%p",p.getName()));
                                }
                            }

                            System.out.println("[" + p.getName() + ": Changed " + p.getName() + "'s gamemode to " + g.toString() + "]");
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <0|1|2|3|survival|creative|adventure|spectator> [" + u.getTranslatedMessage("Player") + "]");
                        }
                    } else if(args.length == 2){
                        String player = args[1];
                        String mode = args[0];

                        GameMode g = null;

                        for(GameMode gg : GameMode.values()){
                            if(gg.toString().toLowerCase().startsWith(mode)){
                                g = gg;
                            }
                        }

                        if(mode.equals("0")){
                            g = GameMode.SURVIVAL;
                        } else if(mode.equals("1")){
                            g = GameMode.CREATIVE;
                        } else if(mode.equals("2")){
                            g = GameMode.ADVENTURE;
                        } else if(mode.equals("3")){
                            g = GameMode.SPECTATOR;
                        }

                        if(g != null){
                            if(Bukkit.getPlayer(player) != null){
                                Player p2 = Bukkit.getPlayer(player);

                                p2.setGameMode(g);

                                for(Player all : Bukkit.getOnlinePlayers()){
                                    ChestUser a = ChestUser.getUser(all);

                                    if(a.hasPermission(Rank.ADMIN)){
                                        all.sendMessage(ChatColor.YELLOW + a.getTranslatedMessage("%p changed %s's gamemode to " + g.toString()).replace("%p",p.getName()).replace("%s",p2.getName()));
                                    }
                                }

                                System.out.println("[" + p.getName() + ": Changed " + p2.getName() + "'s gamemode to " + g.toString() + "]");
                            } else {
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("That player is not online."));
                            }
                        } else {
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " <0|1|2|3|survival|creative|adventure|spectator> [" + u.getTranslatedMessage("Player") + "]");
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "/" + label + " [" + u.getTranslatedMessage("Player") + "]");
                    }
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You must be a player to do this!");
            }
        }

        return false;
    }
}
