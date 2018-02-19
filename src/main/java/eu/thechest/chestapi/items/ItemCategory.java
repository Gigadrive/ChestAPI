package eu.thechest.chestapi.items;

import eu.thechest.chestapi.lang.Translation;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by zeryt on 04.04.2017.
 */
public enum ItemCategory {
    ARROW_TRAILS("Arrow Trails", "Arrow Trail", ChatColor.AQUA, Material.ARROW, 0),
    VICTORY_EFFECTS("Victory Effects", "Victory Effect", ChatColor.YELLOW, Material.GOLD_INGOT, 0),
    KILL_EFFECTS("Kill Effects", "Kill Effect", ChatColor.RED, Material.REDSTONE, 0),
    LOBBY_HAT("Lobby Hats","Lobby Hat", ChatColor.DARK_GREEN, Material.LEATHER_HELMET, 0),
    PETS("Pets","Pet",ChatColor.LIGHT_PURPLE,Material.BONE,0),
    LOBBY_TRAILS("Lobby Trails","Lobby Trail",ChatColor.BLUE,Material.LEATHER_BOOTS,0),
    GADGET("Gadgets","Gadget",ChatColor.WHITE,Material.PISTON_STICKY_BASE,0),
    GOAL_EXPLOSIONS("Soccer Goal Explosions","Soccer Goal Explosion",ChatColor.DARK_AQUA, Material.SNOW_BALL, 0),
    FAME_TITLE("Fame Titles","Fame Title",ChatColor.GOLD,Material.NAME_TAG,0);

    private String name;
    private String nameSingular;
    private ChatColor color;
    private Material icon;
    private int iconDataValue;

    ItemCategory(String name, String nameSingular, ChatColor color, Material icon, int iconDataValue){
        this.name = name;
        this.nameSingular = nameSingular;
        this.color = color;
        this.icon = icon;
        this.iconDataValue = iconDataValue;
    }

    public String getName(){
        return this.name;
    }

    public String getNameSingular(){
        return this.nameSingular;
    }

    public ChatColor getColor(){
        return this.color;
    }

    public Material getIcon(){
        return this.icon;
    }

    public int getIconDataValue(){
        return this.iconDataValue;
    }

    public ItemStack toItemStack(){
        return toItemStack(null);
    }

    public ItemStack toItemStack(Translation t){
        ItemStack i = new ItemStack(icon);
        i.setDurability((short)iconDataValue);

        ItemMeta iM = i.getItemMeta();
        if(t == null){
            iM.setDisplayName(getColor() + name);
        } else {
            if(t.getPhrases().containsKey(name)){
                iM.setDisplayName(getColor() + t.getPhrases().get(name));
            } else {
                iM.setDisplayName(getColor() + name);
            }
        }

        i.setItemMeta(iM);
        return ItemUtil.hideFlags(ItemUtil.setUnbreakable(i,true));
    }

    public static ItemCategory getCategory(String s){
        for(ItemCategory c : values()){
            if(c.toString().equalsIgnoreCase(s)) return c;
        }

        return null;
    }
}
