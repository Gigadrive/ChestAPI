package eu.thechest.chestapi.items;

import eu.thechest.chestapi.mysql.MySQLManager;
import javafx.scene.paint.Color;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;

/**
 * Created by zeryt on 04.04.2017.
 */
public class VaultItem {
    public static HashMap<Integer,VaultItem> STORAGE = new HashMap<Integer,VaultItem>();

    public static VaultItem getItem(int id){
        if(STORAGE.containsKey(id)){
            return STORAGE.get(id);
        } else {
            return null;
        }
    }

    public static void init(){
        STORAGE.clear();

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `lobbyShop_availableItems`");
            ResultSet rs = ps.executeQuery();
            rs.beforeFirst();

            while(rs.next()){
                if(!STORAGE.containsKey(rs.getInt("id"))){
                    if(ItemCategory.getCategory(rs.getString("category")) != null){
                        STORAGE.put(rs.getInt("id"), new VaultItem(rs.getInt("id"),rs.getString("name"),rs.getString("description"), ItemCategory.valueOf(rs.getString("category")), ItemRarity.valueOf(rs.getString("rarity")),rs.getInt("icon"),rs.getInt("iconDurability"),rs.getBoolean("canDropInRandomCrate"),rs.getInt("armorColor.red"),rs.getInt("armorColor.green"),rs.getInt("armorColor.blue"),rs.getString("skinURL"),ChatColor.getByChar(rs.getString("fameTitleColor"))));
                    }
                }
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private int id;
    private String name;
    private String description;
    private ItemCategory category;
    private ItemRarity rarity;
    private int icon;
    private int iconDurability;
    private boolean canDropInRandomCrate;
    private int armorColorRed;
    private int armorColorGreen;
    private int armorColorBlue;
    private String skinURL;
    private ChatColor fameTitleColor;

    public VaultItem(int id, String name, String description, ItemCategory category, ItemRarity rarity, int icon, int iconDurability, boolean canDropInRandomCrate, int armorColorRed, int armorColorGreen, int armorColorBlue, String skinURL, ChatColor fameTitleColor){
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.icon = icon;
        this.iconDurability = iconDurability;
        this.canDropInRandomCrate = canDropInRandomCrate;
        this.armorColorRed = armorColorRed;
        this.armorColorGreen = armorColorGreen;
        this.armorColorBlue = armorColorBlue;
        this.skinURL = skinURL;
        this.fameTitleColor = fameTitleColor;
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

    public ItemCategory getCategory(){
        return this.category;
    }

    public ItemRarity getRarity(){
        return this.rarity;
    }

    public ItemStack getItem(){
        ItemStack i = new ItemStack(icon);
        i.setDurability((short)iconDurability);

        if(i.getType() == Material.LEATHER_HELMET || i.getType() == Material.LEATHER_CHESTPLATE || i.getType() == Material.LEATHER_LEGGINGS || i.getType() == Material.LEATHER_BOOTS){
            if(armorColorRed > -1 && armorColorGreen > -1 && armorColorBlue > -1){
                LeatherArmorMeta m = (LeatherArmorMeta)i.getItemMeta();

                m.setColor(org.bukkit.Color.fromRGB(armorColorRed,armorColorGreen,armorColorBlue));

                i.setItemMeta(m);
            }
        } else if(i.getType() == Material.SKULL_ITEM && iconDurability == 3){
            if(getSkinURL() != null && !getSkinURL().isEmpty()){
                i = ItemUtil.profiledSkullCustom(getSkinURL());
            }
        }

        return i;
    }

    public boolean canDropInRandomCrate(){
        return this.canDropInRandomCrate;
    }

    public String getSkinURL(){
        return this.skinURL;
    }

    public ChatColor getFameTitleColor(){
        return this.fameTitleColor;
    }
}
