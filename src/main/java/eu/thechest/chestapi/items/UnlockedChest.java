package eu.thechest.chestapi.items;

import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.util.StringUtils;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Created by zeryt on 15.04.2017.
 */
public class UnlockedChest {
    public int id;
    public ArrayList<Integer> items;
    public String name;
    public String icon;
    public Timestamp unlocked;

    public UnlockedChest(int id, ArrayList<Integer> items, String icon, String name, Timestamp unlocked){
        this.id = id;
        this.items = items;
        this.unlocked = unlocked;
        this.icon = icon;
        this.name = name;
    }

    public static UnlockedChest get(int id){
        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `lobbyShop_unlockedCrates` WHERE `id` = ?");
            ps.setInt(1,id);

            ResultSet rs = ps.executeQuery();
            if(rs.first()){
                ArrayList<Integer> items = new ArrayList<Integer>();
                for(String s : rs.getString("items").split(",")){
                    if(StringUtils.isValidInteger(s)) items.add(Integer.parseInt(s));
                }

                return new UnlockedChest(rs.getInt("id"),items,rs.getString("icon"),rs.getString("name"),rs.getTimestamp("time"));
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public ItemStack iconToItemStack(){
        if(icon.contains("http://")){
            return ItemUtil.profiledSkullCustom(icon);
        } else {
            ItemStack i = new ItemStack(Integer.parseInt(icon.split(":")[0]));
            i.setDurability((short)Integer.parseInt(icon.split(":")[1]));
            return i;
        }
    }
}
