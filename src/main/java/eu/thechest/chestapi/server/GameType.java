package eu.thechest.chestapi.server;

import org.bukkit.ChatColor;

/**
 * Created by zeryt on 11.02.2017.
 */
public enum GameType {
    NONE(null, ChatColor.GOLD + "[TheChestEU] ", ChatColor.GOLD,"TCE",null),
    SURVIVAL_GAMES("Survival Games", ChatColor.DARK_RED + "[Survival Games] ", ChatColor.DARK_RED,"SG","SurvivalGames"),
    SG_DUELS("Survival Games: Duels", ChatColor.DARK_RED + "[SGDuels] ", ChatColor.DARK_RED,"SGD","SGDuels"),
    KITPVP("KitPvP", ChatColor.YELLOW + "[KitPvP] ", ChatColor.YELLOW,"KPVP","KitPvP"),
    MUSICAL_GUESS("Musical Guess", ChatColor.BLUE + "[MusicalGuess] ", ChatColor.BLUE,"MG","MusicalGuess"),
    DEATHMATCH("Death Match", ChatColor.DARK_AQUA + "[DeathMatch] ", ChatColor.DARK_AQUA,"DM","DeathMatch"),
    BUILD_AND_GUESS("Build & Guess", ChatColor.DARK_PURPLE + "[BuildGuess] ", ChatColor.DARK_PURPLE,"BG","BuildAndGuess"),
    SOCCER("Soccer", ChatColor.AQUA + "[SoccerMC] ", ChatColor.AQUA,"SC","SoccerMC"),
    INFECTION_WARS("Infection Wars", ChatColor.DARK_GREEN + "[InfectionWars] ", ChatColor.DARK_GREEN,"INFW","InfectionWars"),
    TOBIKO("Tobiko", ChatColor.LIGHT_PURPLE + "[Tobiko] ", ChatColor.LIGHT_PURPLE,"TK","Tobiko");

    private String name;
    private String prefix;
    private ChatColor color;
    private String abbreviation;
    private String serverGroupName;

    GameType(String name, String prefix, ChatColor color, String abbreviation, String serverGroupName){
        this.name = name;
        this.prefix = prefix;
        this.color = color;
        this.abbreviation = abbreviation;
        this.serverGroupName = serverGroupName;
    }

    public String getName(){
        return this.name;
    }

    public String getPrefix(){
        return this.prefix;
    }

    public ChatColor getColor(){
        return this.color;
    }

    public String getAbbreviation(){
        return this.abbreviation;
    }

    public String getServerGroupName(){
        return serverGroupName;
    }

    public static GameType fromAbbreviation(String abbreviation){
        for(GameType g : values()){
            if(g.getAbbreviation() != null && g.getAbbreviation().equals(abbreviation)) return g;
        }

        return GameType.NONE;
    }

    public static GameType fromServerGroupname(String serverGroupName){
        for(GameType g : values()){
            if(g.getServerGroupName() != null && g.getServerGroupName().equals(serverGroupName)) return g;
        }

        return GameType.NONE;
    }
}
