package eu.thechest.chestapi.lang;

import eu.thechest.chestapi.mysql.MySQLManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by zeryt on 11.02.2017.
 */
public class Translation {
    private String langKey;
    private HashMap<String,String> PHRASES;

    public static ArrayList<Translation> LANGUAGES = new ArrayList<Translation>();

    public static Translation getLanguage(String langKey){
        if(langKey == null) return null;

        for(Translation t : LANGUAGES){
            if(t.getLanguageKey().equalsIgnoreCase(langKey)) return t;
        }

        return new Translation(langKey);
    }

    public Translation(String langKey){
        this.langKey = langKey.toUpperCase();
        this.PHRASES = new HashMap<String,String>();

        if(!langKey.equals("EN")){
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `translations`");
                ResultSet rs = ps.executeQuery();

                rs.beforeFirst();

                while(rs.next()){
                    String en = rs.getString("EN");
                    String phrase = rs.getString(this.langKey.toUpperCase());

                    if(en != null && phrase != null){
                        this.PHRASES.put(en,phrase);
                    }
                }

                MySQLManager.getInstance().closeResources(rs,ps);

                LANGUAGES.add(this);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public String getLanguageKey(){
        return this.langKey;
    }

    public HashMap<String,String> getPhrases(){
        return this.PHRASES;
    }
}
