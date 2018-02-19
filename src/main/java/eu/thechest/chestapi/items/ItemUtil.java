package eu.thechest.chestapi.items;

import eu.thechest.chestapi.util.GameProfileBuilder;
import eu.thechest.chestapi.util.StringUtils;
import net.minecraft.server.v1_8_R3.NBTTagByte;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagInt;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by zeryt on 11.02.2017.
 */
public class ItemUtil {
    public static ItemStack hideFlags(ItemStack iStack){
        return hideFlags(iStack, 63);
    }

    public static ItemStack hideFlags(ItemStack iStack, int flagValue){
        net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(iStack);
        NBTTagCompound tag = stack.hasTag() ? stack.getTag() : new NBTTagCompound();

        tag.set("HideFlags", new NBTTagByte((byte)flagValue));
        stack.setTag(tag);

        return CraftItemStack.asBukkitCopy(stack);
    }

    public static ItemStack setUnbreakable(ItemStack iStack, boolean unbreak){
        net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(iStack);
        NBTTagCompound tag = stack.hasTag() ? stack.getTag() : new NBTTagCompound();

        if(!tag.hasKey("Unbreakable")){
            tag.set("Unbreakable", new NBTTagInt(StringUtils.convertBooleanToInteger(unbreak)));
        }
        stack.setTag(tag);

        return CraftItemStack.asBukkitCopy(stack);
    }

    public static ItemStack addGlow(ItemStack item) {
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsItem.hasTag() ? nmsItem.getTag() : new NBTTagCompound();

        if(!tag.hasKey("ench")) {
            tag.set("ench", new NBTTagList());
        }

        nmsItem.setTag(tag);
        return CraftItemStack.asCraftMirror(nmsItem);
    }

    public static boolean isTool(Material material) {
        return material == Material.WOOD_SWORD || material == Material.STONE_SWORD || material == Material.GOLD_SWORD || material == Material.IRON_SWORD || material == Material.DIAMOND_SWORD || material == Material.WOOD_PICKAXE || material == Material.STONE_PICKAXE || material == Material.GOLD_PICKAXE || material == Material.IRON_PICKAXE || material == Material.DIAMOND_PICKAXE || material == Material.WOOD_AXE || material == Material.STONE_AXE || material == Material.GOLD_AXE || material == Material.IRON_AXE || material == Material.DIAMOND_AXE || material == Material.WOOD_SPADE || material == Material.STONE_SPADE || material == Material.GOLD_SPADE || material == Material.IRON_SPADE || material == Material.DIAMOND_SPADE || material == Material.WOOD_HOE || material == Material.STONE_HOE || material == Material.GOLD_HOE || material == Material.IRON_HOE || material == Material.DIAMOND_HOE;
    }

    public static ItemStack namedItem(Material m, String name, String[] lore){
        ItemStack is = new ItemStack(m);
        ItemMeta meta = is.getItemMeta();
        meta.setDisplayName(name);
        if(lore != null){
            meta.setLore(Arrays.asList(lore));
        }

        is.setItemMeta(meta);
        return is;
    }

    public static ItemStack namedItem(Material m, String name, String[] lore, int durability){
        ItemStack i = namedItem(m, name, lore);
        i.setDurability((short)durability);
        return i;
    }

    public static ItemStack namedItem(Material m, String name, String[] lore, int durability, int amount){
        ItemStack i = namedItem(m, name, lore, durability);
        i.setAmount(amount);
        return i;
    }

    public static boolean isHelmet(ItemStack i){
        Material m = i.getType();

        if(m == Material.LEATHER_HELMET || m == Material.IRON_HELMET || m == Material.CHAINMAIL_HELMET || m == Material.DIAMOND_HELMET){
            return true;
        } else {
            return false;
        }
    }

    public static boolean isChestplate(ItemStack i){
        Material m = i.getType();

        if(m == Material.LEATHER_CHESTPLATE || m == Material.IRON_CHESTPLATE || m == Material.CHAINMAIL_CHESTPLATE || m == Material.DIAMOND_CHESTPLATE){
            return true;
        } else {
            return false;
        }
    }

    public static boolean isLeggings(ItemStack i){
        Material m = i.getType();

        if(m == Material.LEATHER_LEGGINGS || m == Material.IRON_LEGGINGS || m == Material.CHAINMAIL_LEGGINGS || m == Material.DIAMOND_LEGGINGS){
            return true;
        } else {
            return false;
        }
    }

    public static boolean isBoots(ItemStack i){
        Material m = i.getType();

        if(m == Material.LEATHER_BOOTS || m == Material.IRON_BOOTS || m == Material.CHAINMAIL_BOOTS || m == Material.DIAMOND_BOOTS){
            return true;
        } else {
            return false;
        }
    }

    public static ItemStack profiledSkull(String s){
        return profiledSkullCustom("http://textures.minecraft.net/texture/" + s);
    }

    public static ItemStack profiledSkullCustom(String s){
        ItemStack skull = new ItemStack(Material.SKULL_ITEM);
        skull.setDurability((short)3);

        SkullMeta im = (SkullMeta)skull.getItemMeta();
        Field profileField = null;

        try {
            profileField = im.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(im, GameProfileBuilder.getProfile(UUID.randomUUID(), s, s));
        } catch(Exception e){}

        skull.setItemMeta(im);

        return skull;
    }
}
