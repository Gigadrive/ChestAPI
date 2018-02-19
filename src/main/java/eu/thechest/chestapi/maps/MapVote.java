package eu.thechest.chestapi.maps;

import org.bukkit.entity.Player;

/**
 * Created by zeryt on 27.03.2017.
 */
public class MapVote {
    public Player p;
    public Map map;

    public MapVote(Player p, Map map){
        this.p = p;
        this.map = map;
    }
}
