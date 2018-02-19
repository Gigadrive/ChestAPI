package eu.thechest.chestapi.event;

import eu.thechest.chestapi.user.ChestUser;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by zeryt on 01.04.2017.
 */
public class FameTitlePlateDamageEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private ArmorStand a;
    private Player p;
    private double damage;

    public FameTitlePlateDamageEvent(Player p, ArmorStand a, double damage){
        this.a = a;
        this.p = p;
        this.damage = damage;
    }

    public ArmorStand getPlate(){
        return this.a;
    }

    public Player getPlayer(){
        return this.p;
    }

    public ChestUser getUser(){
        return ChestUser.getUser(getPlayer());
    }

    public double getDamage(){
        return this.damage;
    }

    public HandlerList getHandlers(){
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}