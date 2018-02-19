package eu.thechest.chestapi.util;

/**
 * Created by zeryt on 06.05.2017.
 */
public class CrewTagData {
    public String tag;
    public boolean isLeader;

    public CrewTagData(String tag, boolean isLeader){
        this.tag = tag;
        this.isLeader = isLeader;
    }

    @Override
    public String toString(){
        return tag;
    }
}
