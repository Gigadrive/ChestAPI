package eu.thechest.chestapi.lang;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.user.ChestUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by zeryt on 24.07.2017.
 */
public class TranslatedHologram {
    public static ArrayList<TranslatedHologram> STORAGE = new ArrayList<TranslatedHologram>();

    private TranslatedHoloLine[] lines;
    private Location loc;
    private HashMap<String,Hologram> holograms;

    public TranslatedHologram(TranslatedHoloLine[] lines, Location loc){
        if(lines != null && lines.length > 0 && loc != null){
            this.lines = lines;
            this.loc = loc;
            this.holograms = new HashMap<String,Hologram>();

            for(Translation t : Translation.LANGUAGES){
                if(t.getLanguageKey() != null && !t.getLanguageKey().isEmpty() && !t.getLanguageKey().equalsIgnoreCase("EN")){
                    Hologram h = HologramsAPI.createHologram(ChestAPI.getInstance(),loc);

                    for(TranslatedHoloLine line : lines){
                        if(line.item != null){
                            h.appendItemLine(line.item);
                        }

                        if(line.text != null){
                            if(t.getPhrases().containsKey(line.text)){
                                h.appendTextLine(t.getPhrases().get(line.text));
                            } else {
                                h.appendTextLine(line.text);
                            }
                        }
                    }

                    h.getVisibilityManager().setVisibleByDefault(false);
                    this.holograms.put(t.getLanguageKey(),h);
                }
            }

            Hologram h = HologramsAPI.createHologram(ChestAPI.getInstance(),loc);

            for(TranslatedHoloLine line : lines){
                if(line.item != null){
                    h.appendItemLine(line.item);
                }

                if(line.text != null){
                    h.appendTextLine(line.text);
                }
            }

            h.getVisibilityManager().setVisibleByDefault(false);
            this.holograms.put("EN",h);

            update();
            STORAGE.add(this);
        }
    }

    public TranslatedHoloLine[] getLines(){
        return this.lines;
    }

    public void setLines(TranslatedHoloLine[] lines){
        this.lines = lines;
        update();
    }

    public Location getLocation(){
        return this.loc;
    }

    public HashMap<String,Hologram> getHolograms(){
        return this.holograms;
    }

    public void unregister(){
        for(Hologram h : this.holograms.values()){
            h.delete();
        }

        if(STORAGE.contains(this)) STORAGE.remove(this);
    }

    public void update(){
        for(Player all : Bukkit.getOnlinePlayers()) update(all);
    }

    public void update(Player p){
        ChestUser u = ChestUser.getUser(p);

        for(String langKey : this.holograms.keySet()){
            Translation t = Translation.getLanguage(langKey);
            Hologram h = this.holograms.get(langKey);

            h.getVisibilityManager().setVisibleByDefault(false);

            if(u.getCurrentLanguage() == t || u.getCurrentLanguage().getLanguageKey().equalsIgnoreCase(t.getLanguageKey())){
                h.getVisibilityManager().showTo(p);
            } else {
                h.getVisibilityManager().hideTo(p);
            }
        }
    }
}
