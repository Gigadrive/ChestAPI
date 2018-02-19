package eu.thechest.chestapi.items;

import org.bukkit.ChatColor;

/**
 * Created by zeryt on 04.04.2017.
 */
public enum ItemRarity {
    COMMON("Common",ChatColor.GRAY,155),
    RARE("Rare",ChatColor.DARK_AQUA,110),
    EPIC("Epic", ChatColor.DARK_PURPLE,75),
    LEGENDARY("Legendary",ChatColor.GOLD,40),
    MYTHIC("Mythic",ChatColor.DARK_RED,1),
    LIMITED("Limited",ChatColor.GREEN,0);

    private String name;
    private ChatColor color;
    private int chanceAmount;

    ItemRarity(String name, ChatColor color, int chanceAmount){
        this.name = name;
        this.color = color;
        this.chanceAmount = chanceAmount;
    }

    public String getName(){
        return this.name;
    }

    public ChatColor getColor(){
        return this.color;
    }

    public int getChanceAmount(){
        return this.chanceAmount;
    }
}
