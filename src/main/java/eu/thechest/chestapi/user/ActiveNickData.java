package eu.thechest.chestapi.user;

import com.mojang.authlib.GameProfile;

public class ActiveNickData {
    public String nick;
    public String skin;

    public ActiveNickData(String nick, String skin){
        this.nick = nick;
        this.skin = skin;
    }
}
