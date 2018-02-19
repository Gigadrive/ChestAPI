package eu.thechest.chestapi.achievement;

import eu.thechest.chestapi.mysql.MySQLManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Created by zeryt on 18.02.2017.
 */
public class Achievement {
    private static ArrayList<Achievement> STORAGE = new ArrayList<Achievement>();
    private static boolean init = false;

    private static boolean loaded(int id){
        for(Achievement a : STORAGE){
            if(a.getID() == id) return true;
        }

        return false;
    }

    public static Achievement getAchievement(int id){
        for(Achievement a : STORAGE){
            if(a.getID() == id){
                return a;
            }
        }

        new Achievement(id);

        for(Achievement a : STORAGE){
            if(a.getID() == id){
                return a;
            }
        }

        return null;
    }

    public static ArrayList<Achievement> getAchievements(){
        return STORAGE;
    }

    public static void init(){
        if(!init){
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `achievements` ORDER BY `category` ASC, `title` ASC");

                ResultSet rs = ps.executeQuery();
                rs.beforeFirst();
                while(rs.next()){
                    int id = rs.getInt("id");

                    if(!loaded(id)) new Achievement(id);
                }

                init = true;
                MySQLManager.getInstance().closeResources(rs,ps);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private int id;
    private String title;
    private String description;
    private Timestamp added;
    private String category;

    public Achievement(int id){
        if(loaded(id)) return;

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `achievements` WHERE `id` = ?");
            ps.setInt(1,id);
            ResultSet rs = ps.executeQuery();

            if(rs.first()){
                this.id = id;
                this.title = rs.getString("title");
                this.description = rs.getString("description");
                this.added = rs.getTimestamp("time_added");
                this.category = rs.getString("category");

                STORAGE.add(this);
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public int getID(){
        return this.id;
    }

    public String getTitle(){
        return this.title;
    }

    public String getDescription(){
        return this.description;
    }

    public Timestamp getTimeAdded(){
        return this.added;
    }

    public String getCategory(){
        return this.category;
    }
}
