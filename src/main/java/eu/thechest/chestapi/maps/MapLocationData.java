package eu.thechest.chestapi.maps;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Created by zeryt on 24.02.2017.
 */
public class MapLocationData {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public MapLocationType type;

    public MapLocationData(double x, double y, double z, MapLocationType type){
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0f;
        this.pitch = 0f;
        this.type = type;
    }

    public MapLocationData(double x, double y, double z, float yaw, float pitch, MapLocationType type){
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.type = type;
    }

    public Location toBukkitLocation(String world){
        return new Location(Bukkit.getWorld(world),x,y,z,yaw,pitch);
    }
}
