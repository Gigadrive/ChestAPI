package eu.thechest.chestapi.event;

import eu.thechest.chestapi.maps.Map;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by zeryt on 27.05.2017.
 */
public class FinalMapLoadedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Map finalMap;
    private World world;

    public FinalMapLoadedEvent(Map finalMap, World world){
        this.finalMap = finalMap;
        this.world = world;
    }

    public Map getFinalMap(){
        return this.finalMap;
    }

    public World getWorld(){
        return this.world;
    }

    public HandlerList getHandlers(){
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}
