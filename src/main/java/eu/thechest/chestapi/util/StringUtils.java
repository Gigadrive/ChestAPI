package eu.thechest.chestapi.util;

import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by zeryt on 11.02.2017.
 */
public class StringUtils {
    public static final String LINE_SEPERATOR = ChatColor.STRIKETHROUGH.toString() + "----------------------------------------------------";
    public static final String SCOREBOARD_LINE_SEPERATOR = ChatColor.DARK_GRAY + "---------------";
    public static final String SCOREBOARD_FOOTER_IP = ChatColor.YELLOW + ">> " + ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "TheChest.eu";

    public static boolean isValidInteger(String s){
        try {
            int i = Integer.parseInt(s);
            return true;
        } catch(Exception e){
            return false;
        }
    }

    public static Integer convertBooleanToInteger(boolean b){
        if(b){
            return 1;
        } else {
            return 0;
        }
    }

    public static boolean getChanceBoolean(int chanceTrue, int chanceFalse){
        ArrayList<Boolean> a = new ArrayList<Boolean>();

        for (int i = 0; i < chanceTrue; i++) a.add(Boolean.TRUE);
        for (int i = 0; i < chanceFalse; i++) a.add(Boolean.FALSE);

        Collections.shuffle(a);

        return a.get(0);
    }

    public static boolean convertIntegerToBoolean(Integer i){
        if(i == 1){
            return true;
        } else {
            return false;
        }
    }

    public static int randomInteger(int min, int max){
        Random rdm = new Random();
        int rdmNm = rdm.nextInt((max - min) + 1) + min;

        return rdmNm;
    }

    public static double randomDouble(double min, double max){
        Random r = new Random();
        double randomValue = min + (max - min) * r.nextDouble();

        return randomValue;
    }

    public static String placeZeroIfNeeded(long number) {
        return (number >=10)? Long.toString(number):String.format("0%s",Long.toString(number));
    }

    public static String limitString(String s, int limit){
        if(s.length() > limit){
            return s.substring(0,limit-1);
        } else {
            return s;
        }
    }

    public static String secondsToString(long pTime) {
        final long min = pTime/60;
        final long sec = pTime-(min*60);

        final String strMin = placeZeroIfNeeded(min);
        final String strSec = placeZeroIfNeeded(sec);
        return String.format("%s:%s",strMin,strSec);
    }

    public static double round(double value, int places) {
        /*if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();*/

        /*NumberFormat.getInstance().setMinimumFractionDigits(2);
        NumberFormat.getInstance().setMaximumFractionDigits(2);
        return Double.parseDouble(NumberFormat.getInstance().format(value));*/

        double p = Math.pow(10,places);
        return Math.round(value*p)/p;
    }

    // by Ugleh on Spigot Forums
    // https://www.spigotmc.org/posts/2440432
    public static ArrayList<String> getWordWrapLore(String string) {
        StringBuilder sb = new StringBuilder(string);

        int i = 0;
        while (i + 35 < sb.length() && (i = sb.lastIndexOf(" ", i + 35)) != -1) {
            sb.replace(i, i + 1, "\n");
        }

        ArrayList<String> a = new ArrayList<String>();
        for(String s : sb.toString().split("\n")) a.add(s);

        return a;
    }

    public static ArrayList<String> getWordWrapLore(String string, ChatColor prefixColor) {
        ArrayList<String> a = new ArrayList<String>();

        for(String s : getWordWrapLore(string)){
            a.add(prefixColor + s);
        }

        return a;
    }

    public static void sendJoinMessage(Player p){
        new BukkitRunnable(){
            @Override
            public void run() {
                if(ChestUser.isLoaded(p)){
                    ChestUser u = ChestUser.getUser(p);
                    if(u.isVanished()) return;
                    if(u.isNicked()){
                        for(Player all : Bukkit.getOnlinePlayers()){
                            if(ChestUser.isLoaded(all)){
                                ChestUser a = ChestUser.getUser(all);
                                if(all != p){
                                    if(a.hasPermission(Rank.VIP)){
                                        all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p has joined the server.").replace("%p",u.getRank().getColor() + p.getName() + ChatColor.GOLD));
                                    } else {
                                        all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p has joined the server.").replace("%p",p.getDisplayName() + ChatColor.GOLD));
                                    }
                                }
                            }
                        }
                    } else {
                        for(Player all : Bukkit.getOnlinePlayers()){
                            if(ChestUser.isLoaded(all)){
                                ChestUser a = ChestUser.getUser(all);
                                if(all != p){
                                    if(a.hasPermission(Rank.VIP)){
                                        all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p has joined the server.").replace("%p",u.getRank().getColor() + p.getName() + ChatColor.GOLD));
                                    } else {
                                        all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p has joined the server.").replace("%p",p.getDisplayName() + ChatColor.GOLD));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskLater(ChestAPI.getInstance(),10);
    }

    public static void sendQuitMessage(Player p){
        if(ChestUser.isLoaded(p)){
            ChestUser u = ChestUser.getUser(p);
            if(u.isVanished()) return;
            for(Player all : Bukkit.getOnlinePlayers()){
                if(ChestUser.isLoaded(all)){
                    ChestUser a = ChestUser.getUser(all);
                    if(all != p){
                        if(a.hasPermission(Rank.VIP)){
                            all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p has left the server.").replace("%p",u.getRank().getColor() + p.getName() + ChatColor.GOLD));
                        } else {
                            all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p has left the server.").replace("%p",p.getDisplayName() + ChatColor.GOLD));
                        }
                    }
                }
            }
        }
    }

    public static boolean containsIgnoreCase(String msg, String toFilter){
        return Pattern.compile(Pattern.quote(toFilter), Pattern.CASE_INSENSITIVE).matcher(msg).find();
    }

    public static boolean isURL(String s){
        if(s != null && !s.isEmpty()){
            String URLFinder = new StringBuilder()
                    .append("((?:(http|https|Http|Https|rtsp|Rtsp):")
                    .append("\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)")
                    .append("\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_")
                    .append("\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?")
                    .append("((?:(?:[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}\\.)+")
                    .append("(?:")   // plus top level domain
                    .append("(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])")
                    .append("|(?:biz|b[abdefghijmnorstvwyz])")
                    .append("|(?:cat|com|coop|c[acdfghiklmnoruvxyz])")
                    .append("|d[ejkmoz]")
                    .append("|(?:edu|e[cegrstu])")
                    .append("|f[ijkmor]")
                    .append("|(?:gov|g[abdefghilmnpqrstuwy])")
                    .append("|h[kmnrtu]")
                    .append("|(?:info|int|i[delmnoqrst])")
                    .append("|(?:jobs|j[emop])")
                    .append("|k[eghimnrwyz]")
                    .append("|l[abcikrstuvy]")
                    .append("|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])")
                    .append("|(?:name|net|n[acefgilopruz])")
                    .append("|(?:org|om)")
                    .append("|(?:pro|p[aefghklmnrstwy])")
                    .append("|qa")
                    .append("|r[eouw]")
                    .append("|s[abcdeghijklmnortuvyz]")
                    .append("|(?:tel|travel|t[cdfghjklmnoprtvwz])")
                    .append("|u[agkmsyz]")
                    .append("|v[aceginu]")
                    .append("|w[fs]")
                    .append("|y[etu]")
                    .append("|z[amw]))")
                    .append("|(?:(?:25[0-5]|2[0-4]") // Ip-Adressen (IP addresses)
                    .append("[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]")
                    .append("|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]")
                    .append("[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}")
                    .append("|[1-9][0-9]|[0-9])))")
                    .append("(?:\\:\\d{1,5})?)") // Port-Nummern (Port numbers)
                    .append("(\\/(?:(?:[a-zA-Z0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~")  // Query-Ports
                    .append("\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?")
                    .append("(?:\\b|$)")
                    .toString();

            return Pattern.compile(URLFinder).matcher(s).find();
        } else {
            return false;
        }
    }

    public static String convertMCVersion(int i){
        switch(i){
            case 47: return "1.8.X";
            case 107: return "1.9";
            case 108: return "1.9.1";
            case 109: return "1.9.2";
            case 110: return "1.9.4";
            case 210: return "1.10.X";
            case 315: return "1.11";
            case 316: return "1.11.2";
            case 335: return "1.12";
            case 338: return "1.12.1";
        }

        return null;
    }
}
