package eu.thechest.chestapi.event;

import eu.thechest.chestapi.user.ChestUser;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NickChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player p;

    public NickChangeEvent(Player p){
        this.p = p;
    }

    public Player getPlayer(){
        return this.p;
    }

    public ChestUser getUser(){
        return ChestUser.getUser(getPlayer());
    }

    public HandlerList getHandlers(){
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}
