package eu.thechest.chestapi.server;

import net.md_5.bungee.api.ChatColor;

/**
 * Created by zeryt on 19.02.2017.
 */
@Deprecated
public enum GameState {
    UNDEFINED(ChatColor.DARK_GRAY + "Undefined"), // If nothing else is set
    LOBBY(ChatColor.GREEN + "Lobby"), // Lobby phase, voting for maps etc.
    WARMUP(ChatColor.GOLD + "Warmup"), // Warmup phase. Example: SurvivalGames grace period
    INGAME(ChatColor.RED + "Ingame"), // Game is running
    ENDING(ChatColor.DARK_RED + "Ending"), // Game is finished, people are about to be sent back to the lobby
    JOINABLE(ChatColor.GREEN + "Online"); // Server is online and game is endless (KitPvP)

    private String display;

    GameState(String display){
        this.display = display;
    }

    public String getDisplay(){
        return this.display;
    }
}
