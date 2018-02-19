package eu.thechest.chestapi.game;

import org.bukkit.entity.Player;

import java.sql.Timestamp;

/**
 * Created by zeryt on 28.04.2017.
 */
public class GameEvent {
    public String p;
    public String msg;
    public String killer;
    public boolean isRedTeam;
    public String word;
    public int songID;
    public GameEventType type;

    public Timestamp time;

    public GameEvent(){
        time = new Timestamp(System.currentTimeMillis());
    }
}
