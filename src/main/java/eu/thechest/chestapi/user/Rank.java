package eu.thechest.chestapi.user;

import eu.thechest.chestapi.util.StringUtils;
import org.bukkit.ChatColor;

/**
 * Created by zeryt on 11.02.2017.
 */
public enum Rank {
    USER(0,"User",null,ChatColor.GRAY,'k',1),
    PRO(1,"Pro","Pro",ChatColor.GOLD,'j',2),
    PRO_PLUS(2,"Pro+","Pro+",ChatColor.DARK_AQUA,'i',3),
    TITAN(3,"Titan","Titan",ChatColor.AQUA,'h',3),
    VIP(4,"VIP","VIP",ChatColor.DARK_PURPLE,'g',5),
    STAFF(5,"Staff Member","Staff",ChatColor.YELLOW,'f',5),
    BUILD_TEAM(6,"Build Team","Builder",ChatColor.BLUE,'e',5),
    MOD(7,"Moderator","Mod",ChatColor.GREEN,'d',10),
    SR_MOD(8,"Senior Mod","SrMod",ChatColor.DARK_GREEN,'c',10),
    CM(9,"Community Manager","CM",ChatColor.RED,'b',50),
    ADMIN(10,"Admin","Admin",ChatColor.DARK_RED,'a',50);

    private int id;
    private String name;
    private String prefix;
    private ChatColor color;
    private char scoreboardChar;
    private int votingPower;

    Rank(int id,String name,String prefix,ChatColor color,char scoreboardChar,int votingPower){
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.color = color;
        this.scoreboardChar = scoreboardChar;
        this.votingPower = votingPower;
    }

    public int getID(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public String getPrefix(){
        if(prefix == null){
            return null;
        } else {
            return StringUtils.limitString(this.prefix,7);
        }
    }

    public ChatColor getColor(){
        return this.color;
    }

    public char getScoreboardChar(){
        return this.scoreboardChar;
    }

    public int getVotingPower(){
        return this.votingPower;
    }

    public static Rank fromChar(char scoreboardChar){
        for(Rank r : values()){
            if(r.getScoreboardChar() == scoreboardChar) return r;
        }

        return null;
    }

    public static Rank fromID(int id){
        for(Rank r : values()){
            if(r.getID() == id) return r;
        }

        return null;
    }
}
