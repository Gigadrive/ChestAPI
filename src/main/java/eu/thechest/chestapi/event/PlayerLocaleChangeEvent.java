package eu.thechest.chestapi.event;

import eu.thechest.chestapi.lang.Translation;
import eu.thechest.chestapi.user.ChestUser;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by zeryt on 15.02.2017.
 */
public class PlayerLocaleChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player p;
    private Translation oldLang;
    private Translation newLang;

    public PlayerLocaleChangeEvent(Player p, Translation old, Translation newLang){
        this.p = p;
        this.oldLang = old;
        this.newLang = newLang;
    }

    public Player getPlayer(){
        return this.p;
    }

    public ChestUser getUser(){
        return ChestUser.getUser(getPlayer());
    }

    public Translation getOldLang(){
        return this.oldLang;
    }

    public Translation getNewLang(){
        return this.newLang;
    }

    public HandlerList getHandlers(){
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}
