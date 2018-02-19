package eu.thechest.chestapi.event;

import eu.thechest.chestapi.maps.Map;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by zeryt on 27.05.2017.
 */
public class VotingEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Map finalMap;

    public VotingEndEvent(Map finalMap){
        this.finalMap = finalMap;
    }

    public Map getFinalMap(){
        return this.finalMap;
    }

    public HandlerList getHandlers(){
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}
