package eu.thechest.chestapi.game;

import com.google.gson.Gson;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.maps.Map;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameType;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by zeryt on 27.03.2017.
 */
public class Game {
    private int id;
    private GameType gameType;
    private ArrayList<UUID> participants;
    private ArrayList<UUID> winners;
    private String server;
    private Map map;
    private boolean completed;
    private ArrayList<GameEvent> events;

    public Game(int id, GameType gameType, String server, Map map){
        this.id = id;
        this.gameType = gameType;
        this.participants = new ArrayList<UUID>();
        this.winners = new ArrayList<UUID>();
        this.server = server;
        this.map = map;
        this.completed = false;
        this.events = new ArrayList<GameEvent>();
    }

    public int getID(){
        return this.id;
    }

    public GameType getGameType(){
        return this.gameType;
    }

    public ArrayList<UUID> getParticipants(){
        return this.participants;
    }

    public ArrayList<UUID> getWinners(){
        return this.winners;
    }

    public String getServer(){
        return this.server;
    }

    public Map getMap(){
        return this.map;
    }

    public boolean isCompleted(){
        return this.completed;
    }

    public void setCompleted(boolean b){
        this.completed = b;
    }

    public ArrayList<GameEvent> getEvents(){
        return this.events;
    }

    //
    // ADD GAME EVENT METHODS START
    //

    public void addPlayerChatEvent(Player p, String msg){
        GameEvent e = new GameEvent();
        e.type = GameEventType.PLAYER_CHAT;

        e.p = p.getUniqueId().toString();
        e.msg = msg;

        this.events.add(e);
    }

    public void addSpectatorChatEvent(Player p, String msg){
        GameEvent e = new GameEvent();
        e.type = GameEventType.SPECTATOR_CHAT;

        e.p = p.getUniqueId().toString();
        e.msg = msg;

        this.events.add(e);
    }

    public void addPlayerDeathEvent(Player p){
        GameEvent e = new GameEvent();
        e.type = GameEventType.PLAYER_DEATH;

        e.p = p.getUniqueId().toString();

        this.events.add(e);
    }

    public void addTobikoHitEvent(Player p){
        GameEvent e = new GameEvent();
        e.type = GameEventType.TOBIKO_HIT;

        e.p = p.getUniqueId().toString();

        this.events.add(e);
    }

    public void addTobikoWinEvent(Player p){
        GameEvent e = new GameEvent();
        e.type = GameEventType.TOBIKO_WIN;

        e.p = p.getUniqueId().toString();

        this.events.add(e);
    }

    public void addTobikoLoseEvent(){
        GameEvent e = new GameEvent();
        e.type = GameEventType.TOBIKO_LOSE;

        this.events.add(e);
    }

    public void addMusicalGuessSongGuessEvent(Player p, int songID){
        GameEvent e = new GameEvent();
        e.type = GameEventType.MG_GUESS_SONG;

        e.p = p.getUniqueId().toString();
        e.songID = songID;

        this.events.add(e);
    }

    public void addPlayerDeathEvent(Player p, Player killer){
        GameEvent e = new GameEvent();
        e.type = GameEventType.PLAYER_KILL;

        e.p = p.getUniqueId().toString();
        e.killer = killer.getUniqueId().toString();

        this.events.add(e);
    }

    public void addSoccerGoalEvent(boolean isRedTeam){
        GameEvent e = new GameEvent();
        e.type = GameEventType.SOCCER_GOAL;

        e.isRedTeam = isRedTeam;

        this.events.add(e);
    }

    public void addSoccerGoalEvent(boolean isRedTeam, Player p){
        GameEvent e = new GameEvent();
        e.type = GameEventType.SOCCER_GOAL_BY_PLAYER;

        e.p = p.getUniqueId().toString();
        e.isRedTeam = isRedTeam;

        this.events.add(e);
    }

    public void addWordGuessedEvent(Player p, String word){
        GameEvent e = new GameEvent();
        e.type = GameEventType.WORD_GUESSED;

        e.p = p.getUniqueId().toString();
        e.word = word;

        this.events.add(e);
    }

    //
    // ADD GAME EVENT METHODS END
    //

    public void saveData(){
        ChestAPI.async(() -> {
            try {
                String participantsString = "";
                String winnersString = "";

                for(UUID uuid : getParticipants()){
                    if(participantsString == ""){
                        participantsString = uuid.toString();
                    } else {
                        participantsString = participantsString + "," + uuid.toString();
                    }
                }

                for(UUID uuid : getWinners()){
                    if(winnersString == ""){
                        winnersString = uuid.toString();
                    } else {
                        winnersString = winnersString + "," + uuid.toString();
                    }
                }

                String eventString = null;
                if(this.events.size() > 0){
                    Gson gson = new Gson();
                    eventString = gson.toJson(this.events);
                }

                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `games` SET `game` = ?, `server` = ?, `map` = ?, `participants` = ?, `winners` = ?, `isCompleted` = ?, `eventLog` = ? WHERE `id` = ?");
                ps.setString(1,getGameType().toString());
                ps.setString(2,getServer());
                if(getMap() == null){
                    ps.setInt(3,0);
                } else {
                    ps.setInt(3,getMap().getID());
                }
                ps.setString(4,participantsString);
                ps.setString(5,winnersString);
                ps.setBoolean(6,isCompleted());
                ps.setString(7,eventString);
                ps.setInt(8,getID());
                ps.executeUpdate();
                ps.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        });
    }
}
