package eu.thechest.chestapi.maps;

import java.util.UUID;

/**
 * Created by zeryt on 19.03.2017.
 */
public class MapRating {
    public UUID uuid;
    public int mapID;

    public MapRating(UUID uuid, int mapID){
        this.uuid = uuid;
        this.mapID = mapID;
    }
}
