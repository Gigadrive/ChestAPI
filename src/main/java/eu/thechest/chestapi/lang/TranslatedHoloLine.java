package eu.thechest.chestapi.lang;

import org.bukkit.inventory.ItemStack;

/**
 * Created by zeryt on 24.07.2017.
 */
public class TranslatedHoloLine {
    public ItemStack item;
    public String text;

    public TranslatedHoloLine(String text){
        this.text = text;
    }

    public TranslatedHoloLine(ItemStack item){
        this.item = item;
    }
}
