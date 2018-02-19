package eu.thechest.chestapi.game;

import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameType;
import eu.thechest.chestapi.user.Rank;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

/**
 * Created by zeryt on 10.07.2017.
 */
public class GamePerk {
    public static HashMap<Integer,GamePerk> STORAGE;

    public static void load(){
        if(STORAGE == null) STORAGE = new HashMap<Integer,GamePerk>();
        STORAGE.clear();

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `gamePerks` ORDER BY `name` ASC");
            ResultSet rs = ps.executeQuery();
            rs.beforeFirst();

            while(rs.next()){
                int id = rs.getInt("id");

                if(!STORAGE.containsKey(id)){
                    Rank r = null;
                    if(rs.getString("requiredRank") != null) r = Rank.valueOf(rs.getString("requiredRank"));
                    STORAGE.put(id,new GamePerk(id,rs.getString("name"),rs.getString("description"),GameType.valueOf(rs.getString("gamemode")),rs.getInt("price"),r,Material.getMaterial(rs.getInt("icon")),rs.getInt("iconDurability")));
                }
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static GamePerk get(int id){
        if(STORAGE.containsKey(id)){
            return STORAGE.get(id);
        } else {
            return null;
        }
    }

    private int id;
    private String name;
    private String description;
    private GameType gamemode;
    private int price;
    private Rank requiredRank;
    private Material icon;
    private int iconDurability;

    public GamePerk(int id, String name, String description, GameType gamemode, int price, Rank requiredRank, Material icon, int iconDurability){
        this.id = id;
        this.name = name;
        this.description = description;
        this.gamemode = gamemode;
        this.price = price;
        this.requiredRank = requiredRank;
        this.icon = icon;
        this.iconDurability = iconDurability;
    }

    public int getID(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public String getDescription(){
        return this.description;
    }

    public GameType getGamemode(){
        return this.gamemode;
    }

    public int getPrice(){
        return this.price;
    }

    public Rank getRequiredRank(){
        return this.requiredRank;
    }

    public ItemStack getIcon(){
        ItemStack i = new ItemStack(icon);
        i.setDurability((short)iconDurability);
        return i;
    }
}
