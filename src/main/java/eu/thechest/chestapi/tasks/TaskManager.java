package eu.thechest.chestapi.tasks;

import eu.thechest.chestapi.ChestAPI;
import org.bukkit.scheduler.BukkitTask;

/**
 * Created by zeryt on 19.02.2017.
 */
public class TaskManager {
    private static boolean init = false;

    public static BukkitTask ANNOUNCEMENT_TASK;

    public static void init(){
        if(!init){
            AutoAnnouncementTask.init();
            ANNOUNCEMENT_TASK = new AutoAnnouncementTask().runTaskTimer(ChestAPI.getInstance(), 1L, 10*60*20);

            init = true;
        }
    }
}
