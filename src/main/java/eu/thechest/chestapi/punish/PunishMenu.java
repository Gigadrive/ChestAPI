package eu.thechest.chestapi.punish;

import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.items.ItemUtil;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.Rank;
import eu.thechest.chestapi.util.PlayerUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.UUID;

public class PunishMenu implements Listener {
    public static void openFor(Player p,UUID victim){
        openFor(p,victim,PunishMenuPage.MAIN);
    }

    public static void openFor(Player p,UUID victim,PunishMenuPage page){
        ChestUser u = ChestUser.getUser(p);
        if(!u.hasPermission(Rank.MOD)) return;
        Inventory inv = null;
        String victimName = PlayerUtilities.getNameFromUUID(victim);
        Rank victimRank = PlayerUtilities.getRankFromUUID(victim);

        if(page == PunishMenuPage.MAIN){
            inv = Bukkit.createInventory(null,9*3, "[SN] Main");

            ItemStack i = new ItemStack(Material.SKULL_ITEM,1,(short)3);
            SkullMeta iM = (SkullMeta)i.getItemMeta();
            iM.setOwner(victimName);
            iM.setDisplayName(victimRank.getColor() + victimName);
            i.setItemMeta(iM);

            inv.setItem(10,ItemUtil.hideFlags(i));
            inv.setItem(12,ItemUtil.hideFlags(ItemUtil.namedItem(Material.CLAY_BRICK, ChatColor.GREEN + u.getTranslatedMessage("General"),null)));
            inv.setItem(13,ItemUtil.hideFlags(ItemUtil.namedItem(Material.DIAMOND_SWORD, ChatColor.RED + u.getTranslatedMessage("Hacking"),null)));
            inv.setItem(14,ItemUtil.hideFlags(ItemUtil.namedItem(Material.BOOK_AND_QUILL, ChatColor.GOLD + u.getTranslatedMessage("Chat Behavior"),null)));
            inv.setItem(16,ItemUtil.hideFlags(ItemUtil.namedItem(Material.BARRIER,ChatColor.DARK_RED + u.getTranslatedMessage("Close"),null)));
        } else if(page == PunishMenuPage.GENERAL){
            inv = Bukkit.createInventory(null,9*4, "[SN] " + u.getTranslatedMessage("General"));

            ItemStack i = new ItemStack(Material.SKULL_ITEM,1,(short)3);
            SkullMeta iM = (SkullMeta)i.getItemMeta();
            iM.setOwner(victimName);
            iM.setDisplayName(victimRank.getColor() + victimName);
            i.setItemMeta(iM);

            inv.setItem(4,ItemUtil.hideFlags(i));

            inv.setItem(10,b(p,Material.ITEM_FRAME,0,"Banned Modifications",new String[]{},"2mon",PunishmentType.BAN));
            inv.setItem(11,b(p,Material.DIAMOND_CHESTPLATE,0,"Teaming",new String[]{"Teaming in a solo gamemode"},"2w",PunishmentType.BAN));
            inv.setItem(12,b(p,Material.DIAMOND_CHESTPLATE,0,"Exceeding Team Limit",new String[]{"Teaming with 3 people in Survival Games"},"2w",PunishmentType.BAN));
            inv.setItem(13,b(p,Material.LAVA_BUCKET,0,"Compromised Account",new String[]{"(Only use on hacked accounts)"},"Permanent",PunishmentType.BAN));
            inv.setItem(14,b(p,Material.EXP_BOTTLE,0,"Multiaccounting",new String[]{"Avoiding a punishment with another account"},"Permanent",PunishmentType.BAN));
            inv.setItem(15,b(p,Material.PAPER,0,"Inappropriate Name",new String[]{"Having an inappropriate Name"},"Permanent",PunishmentType.BAN));
            inv.setItem(16,b(p,Material.GOLD_CHESTPLATE,0,"Inappropriate Skin/Cape",new String[]{"Having an inappropriate skin/cape"},"Permanent",PunishmentType.BAN));

            inv.setItem(19,b(p,Material.NETHER_BRICK_ITEM,0,"Scamming/Phishing",new String[]{"Advertising illegal/scamming websites"},"Permanent",PunishmentType.BAN));
            inv.setItem(20,b(p,Material.TNT,0,"Team Griefing",new String[]{"Sabotaging team mates in a game","Killing team mates in DeathMatch"},"1w",PunishmentType.BAN));
            inv.setItem(21,b(p,Material.HOPPER,0,"Inappropriate Building",new String[]{"Building inappropriate things in Build & Guess"},"3w",PunishmentType.BAN));
            inv.setItem(22,b(p,Material.REDSTONE_TORCH_ON,0,"Exploiting",new String[]{"Glitching out of map to win a game","Using a game bug for advantages"},"3mon",PunishmentType.BAN));

            inv.setItem(31,ItemUtil.hideFlags(ItemUtil.namedItem(Material.BARRIER,ChatColor.DARK_RED + u.getTranslatedMessage("Close"),null)));
        } else if(page == PunishMenuPage.HACKING){
            inv = Bukkit.createInventory(null,9*4, "[SN] " + u.getTranslatedMessage("Hacking"));

            ItemStack i = new ItemStack(Material.SKULL_ITEM,1,(short)3);
            SkullMeta iM = (SkullMeta)i.getItemMeta();
            iM.setOwner(victimName);
            iM.setDisplayName(victimRank.getColor() + victimName);
            i.setItemMeta(iM);

            inv.setItem(4,ItemUtil.hideFlags(i));

            inv.setItem(10,b(p,Material.IRON_SWORD,0,"Kill Aura / Force field",new String[]{"Attacking all players in reach as fast as possible"},"Permanent",PunishmentType.BAN));
            inv.setItem(11,b(p,Material.BOW,0,"Aimbot",new String[]{"Hiting arrows almost every time"},"Permanent",PunishmentType.BAN));
            inv.setItem(12,b(p,Material.DIAMOND,0,"X-Ray",new String[]{"Being able to see resources through the ground"},"Permanent",PunishmentType.BAN));
            inv.setItem(13,b(p,Material.LEATHER_BOOTS,0,"Sprint / Speed",new String[]{"Having increased speed"},"Permanent",PunishmentType.BAN));
            inv.setItem(14,b(p,Material.WEB,0,"NoSlowdown",new String[]{"Being able to run with a bow","Being able to run while eating"},"Permanent",PunishmentType.BAN));
            inv.setItem(15,b(p,Material.LEATHER_BOOTS,0,"Water Walking",new String[]{"Walking on water"},"Permanent",PunishmentType.BAN));
            inv.setItem(16,b(p,Material.IRON_SWORD,0,"NoSwing",new String[]{"Attacking without swinging arm","Breaking blocks without swinging arm"},"Permanent",PunishmentType.BAN));

            inv.setItem(19,b(p,Material.FEATHER,0,"Flying",new String[]{"Flying through the air"},"Permanent",PunishmentType.BAN));
            inv.setItem(20,b(p,Material.STONE_SWORD,0,"Other Hacks",new String[]{},"Permanent",PunishmentType.BAN));

            inv.setItem(31,ItemUtil.hideFlags(ItemUtil.namedItem(Material.BARRIER,ChatColor.DARK_RED + u.getTranslatedMessage("Close"),null)));
        } else if(page == PunishMenuPage.CHAT){
            inv = Bukkit.createInventory(null,9*4, "[SN] " + u.getTranslatedMessage("Chat Behavior"));

            ItemStack i = new ItemStack(Material.SKULL_ITEM,1,(short)3);
            SkullMeta iM = (SkullMeta)i.getItemMeta();
            iM.setOwner(victimName);
            iM.setDisplayName(victimRank.getColor() + victimName);
            i.setItemMeta(iM);

            inv.setItem(4,ItemUtil.hideFlags(i));

            inv.setItem(10,b(p,Material.BOOK_AND_QUILL,0,"Foul language",new String[]{"Swearing in chat","Avoiding chat filter"},"6h",PunishmentType.MUTE));
            inv.setItem(11,b(p,Material.BOOK_AND_QUILL,0,"Disrespectful behavior",new String[]{"Being rude to other players"},"12h",PunishmentType.MUTE));
            inv.setItem(12,b(p,Material.BOOK_AND_QUILL,0,"Negative Behavior",new String[]{"Verbally attacking other players","Racism","Stalking other players"},"2w",PunishmentType.MUTE));
            inv.setItem(13,b(p,Material.BOOK_AND_QUILL,0,"Advertising",new String[]{"Advertising other websites or server","(See Scamming/Phishing in General)"},"1mon",PunishmentType.MUTE));
            inv.setItem(14,b(p,Material.BOOK_AND_QUILL,0,"Verbal Abuse",new String[]{"Telling other players to do something to themselves that could potentially harm them","\"kill yourself\""},"1w",PunishmentType.MUTE));
            inv.setItem(15,b(p,Material.BOOK_AND_QUILL,0,"Spam",new String[]{"Spamming in chat","Sending the same message over and over again"},"3d",PunishmentType.MUTE));
            inv.setItem(16,b(p,Material.BOOK_AND_QUILL,0,"Player Discouragement",new String[]{"\"ez\"","\"rekt\""},"1mon",PunishmentType.MUTE));

            inv.setItem(19,b(p,Material.BOOK_AND_QUILL,0,"Hackusation",new String[]{"Accusing another player of hacking"},"1h",PunishmentType.MUTE));
            inv.setItem(20,b(p,Material.BOOK_AND_QUILL,0,"Other",new String[]{},"Permanent",PunishmentType.MUTE));

            inv.setItem(31,ItemUtil.hideFlags(ItemUtil.namedItem(Material.BARRIER,ChatColor.DARK_RED + u.getTranslatedMessage("Close"),null)));
        }

        if(inv != null) p.openInventory(inv);
    }

    private static ItemStack b(Player p, Material icon, int iconDurability, String name, String[] examples, String duration, PunishmentType punishmentType){
        ChestUser u = ChestUser.getUser(p);
        ItemStack i = new ItemStack(icon,1,(short)iconDurability);
        ItemMeta iM = i.getItemMeta();
        iM.setDisplayName(ChatColor.GREEN.toString() + ChatColor.BOLD.toString() + u.getTranslatedMessage(name));
        ArrayList<String> iL = new ArrayList<String>();
        if(examples != null && examples.length > 0){
            iL.add(ChatColor.WHITE + u.getTranslatedMessage("Examples") + ":");
            for(String s : examples){
                iL.add(ChatColor.DARK_GREEN + "- " + ChatColor.WHITE.toString() + ChatColor.ITALIC.toString() + u.getTranslatedMessage(s));
            }

            iL.add(" ");
        }
        iL.add(ChatColor.WHITE + u.getTranslatedMessage("Type of Punishment") + ": " + ChatColor.RED + punishmentType.toString());
        if(punishmentType != PunishmentType.KICK) iL.add(ChatColor.WHITE + u.getTranslatedMessage("Duration") + ": " + ChatColor.GOLD + duration);
        iM.setLore(iL);
        i.setItemMeta(iM);

        return ItemUtil.hideFlags(i);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player){
            Player p = (Player)e.getWhoClicked();
            ChestUser u = ChestUser.getUser(p);
            Inventory inv = e.getInventory();
            int slot = e.getRawSlot();

            if(inv.getName().equals("[SN] Main")){
                e.setCancelled(true);

                int headSlot = 10;
                if(inv.getItem(headSlot) != null && inv.getItem(headSlot).getItemMeta() != null && inv.getItem(headSlot).getType() == Material.SKULL_ITEM && inv.getItem(headSlot).getDurability() == (short)3 && ((SkullMeta)inv.getItem(headSlot).getItemMeta()).getOwner() != null){
                    String victimName = ((SkullMeta)inv.getItem(headSlot).getItemMeta()).getOwner();

                    if(victimName != null && !victimName.isEmpty()){
                        UUID victim = PlayerUtilities.getUUIDFromName(victimName);

                        if(victim != null){
                            if(slot == 12){
                                openFor(p,victim,PunishMenuPage.GENERAL);
                            } else if(slot == 13){
                                openFor(p,victim,PunishMenuPage.HACKING);
                            } else if(slot == 14){
                                openFor(p,victim,PunishMenuPage.CHAT);
                            } else if(slot == 16){
                                p.closeInventory();
                            }
                        }
                    }
                }
            } else if(inv.getName().equals("[SN] " + u.getTranslatedMessage("General"))){
                e.setCancelled(true);

                int headSlot = 4;
                if(inv.getItem(headSlot) != null && inv.getItem(headSlot).getItemMeta() != null && inv.getItem(headSlot).getType() == Material.SKULL_ITEM && inv.getItem(headSlot).getDurability() == (short)3 && ((SkullMeta)inv.getItem(headSlot).getItemMeta()).getOwner() != null){
                    String victimName = ((SkullMeta)inv.getItem(headSlot).getItemMeta()).getOwner();

                    if(victimName != null && !victimName.isEmpty()){
                        UUID victim = PlayerUtilities.getUUIDFromName(victimName);

                        if(victim != null){
                            if(slot == 10){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "BANNEDMODS");
                            } else if(slot == 11){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "TEAMING");
                            } else if(slot == 12){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "EXCEEDINGTEAM");
                            } else if(slot == 13){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "COMPROMISED");
                            } else if(slot == 14){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "MULTIACCOUNTING");
                            } else if(slot == 15){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "INAPPROPRIATENAME");
                            } else if(slot == 16){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "INAPPROPRIATESKIN");
                            } else if(slot == 19){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "SCAM");
                            } else if(slot == 20){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "TEAMGRIEFING");
                            } else if(slot == 21){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "INAPPROPRIATEBUILDING");
                            } else if(slot == 22){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "EXPLOITING");
                            } else if(slot == 31){
                                openFor(p,victim);
                            }
                        }
                    }
                }
            } else if(inv.getName().equals("[SN] " + u.getTranslatedMessage("Hacking"))){
                e.setCancelled(true);

                int headSlot = 4;
                if(inv.getItem(headSlot) != null && inv.getItem(headSlot).getItemMeta() != null && inv.getItem(headSlot).getType() == Material.SKULL_ITEM && inv.getItem(headSlot).getDurability() == (short)3 && ((SkullMeta)inv.getItem(headSlot).getItemMeta()).getOwner() != null){
                    String victimName = ((SkullMeta)inv.getItem(headSlot).getItemMeta()).getOwner();

                    if(victimName != null && !victimName.isEmpty()){
                        UUID victim = PlayerUtilities.getUUIDFromName(victimName);

                        if(victim != null){
                            if(slot == 10){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "KILLAURA");
                            } else if(slot == 11){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "AIMBOT");
                            } else if(slot == 12){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "XRAY");
                            } else if(slot == 13){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "SPEED");
                            } else if(slot == 14){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "NOSLOWDOWN");
                            } else if(slot == 15){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "WATERWALKING");
                            } else if(slot == 16){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "NOSWING");
                            } else if(slot == 19){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "FLY");
                            } else if(slot == 20){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "OTHERHACKS");
                            } else if(slot == 31){
                                openFor(p,victim);
                            }
                        }
                    }
                }
            } else if(inv.getName().equals("[SN] " + u.getTranslatedMessage("Chat Behavior"))){
                e.setCancelled(true);

                int headSlot = 4;
                if(inv.getItem(headSlot) != null && inv.getItem(headSlot).getItemMeta() != null && inv.getItem(headSlot).getType() == Material.SKULL_ITEM && inv.getItem(headSlot).getDurability() == (short)3 && ((SkullMeta)inv.getItem(headSlot).getItemMeta()).getOwner() != null){
                    String victimName = ((SkullMeta)inv.getItem(headSlot).getItemMeta()).getOwner();

                    if(victimName != null && !victimName.isEmpty()){
                        UUID victim = PlayerUtilities.getUUIDFromName(victimName);

                        if(victim != null){
                            if(slot == 10){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "FOULLANGUAGE");
                            } else if(slot == 11){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "DISRESPECTFUL");
                            } else if(slot == 12){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "NEGATIVE");
                            } else if(slot == 13){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "ADVERTISING");
                            } else if(slot == 14){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "VERBALABUSE");
                            } else if(slot == 15){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "SPAM");
                            } else if(slot == 16){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "DISCOURAGEMENT");
                            } else if(slot == 19){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "HACKUSATION");
                            } else if(slot == 20){
                                p.closeInventory();
                                ChestAPI.executeBungeeCommand("BungeeConsole","handlepunishment " + p.getName() + " " + victimName + " " + "OTHERCHAT");
                            } else if(slot == 31){
                                openFor(p,victim);
                            }
                        }
                    }
                }
            }
        }
    }

    public enum PunishMenuPage {
        MAIN,HACKING,CHAT,GENERAL
    }

    public enum PunishmentType {
        BAN,MUTE,KICK
    }
}
