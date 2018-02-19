package eu.thechest.chestapi.achievement;

import org.bukkit.entity.Player;

import java.sql.Timestamp;

/**
 * Created by zeryt on 18.02.2017.
 */
public class AchievementUnlock {
    private int id;
    private Player p;
    private Achievement a;
    private Timestamp time;

    public AchievementUnlock(int id, Player p, int achievementID, Timestamp time) {
        this.id = id;
        this.p = p;
        this.a = Achievement.getAchievement(achievementID);
        this.time = time;
    }

    public int getID(){
        return this.id;
    }

    public Player getPlayer(){
        return this.p;
    }

    public Achievement getAchievement(){
        return this.a;
    }

    public Timestamp getTimeAdded(){
        return this.time;
    }
}
