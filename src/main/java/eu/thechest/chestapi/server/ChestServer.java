package eu.thechest.chestapi.server;

import eu.thechest.chestapi.mysql.MySQLManager;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

/**
 * Created by zeryt on 19.02.2017.
 */
@Deprecated
public class ChestServer {
    public static HashMap<String, ChestServer> STORAGE = new HashMap<String, ChestServer>();

    public static ChestServer getServer(String name){
        if(!STORAGE.containsKey(name)) new ChestServer(name);

        if(STORAGE.containsKey(name)){
            return STORAGE.get(name);
        } else {
            return null;
        }
    }

    private String name;
    private String ip;
    private int port;

    private String[] data;

    public ChestServer(String name){
        if(!STORAGE.containsKey(name)){
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `servers` WHERE `name`=?");
                ps.setString(1, name);

                ResultSet rs = ps.executeQuery();

                if(rs.first()){
                    this.name = rs.getString("name");
                    this.ip = rs.getString("ip");
                    this.port = rs.getInt("port");

                    STORAGE.put(this.name, this);
                }

                ps.close();
                rs.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public String getName(){
        return this.name;
    }

    public String getIP(){
        return this.ip;
    }

    public int getPort(){
        return this.port;
    }

    public String[] getData(){
        return data;
    }

    public void fetchData() {
        new Thread(new Runnable(){
            public void run(){
                try {

                    /*Socket socket = new Socket();
                    OutputStream os;
                    DataOutputStream dos;
                    InputStream is;
                    InputStreamReader isr;

                    socket.setSoTimeout(2500);
                    socket.connect(new InetSocketAddress(ip, port));

                    os = socket.getOutputStream();
                    dos = new DataOutputStream(os);

                    is = socket.getInputStream();
                    isr = new InputStreamReader(is, Charset.forName("UTF-16BE"));

                    dos.write(new byte[]{(byte) 0xFE, (byte) 0x01});

                    int packetID = is.read();

                    if (packetID == -1) {
                        System.out.println("Invalid Packet ID! (End Of Stream)");
                    }
                    if (packetID != 0xFF) {
                        System.out.println("Invalid Packet Id! " + packetID);
                    }

                    int length = isr.read();

                    if (length == -1) {
                        System.out.println("End Of Stream");
                    }

                    if (length == 0) {
                        System.out.println("Invalid length");
                    }

                    char[] chars = new char[length];

                    if (isr.read(chars, 0, length) != length) {
                        System.out.println("End Of Stream");
                    }

                    String string = new String(chars);
                    data = string.split("\0");*/
                    PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `servers` WHERE `name` = ?");
                    ps.setString(1,name);
                    ResultSet rs = ps.executeQuery();

                    if(rs.first()){
                        if(rs.getBoolean("online") == true){
                            data = new String[]{"","","",GameState.valueOf(rs.getString("gamestate")).getDisplay(),String.valueOf(rs.getInt("players.online")),String.valueOf(rs.getInt("players.max")),rs.getString("map")};
                        } else {
                            data = null;
                        }
                    }

                    MySQLManager.getInstance().closeResources(rs,ps);
                } catch (Exception e) {
                    data = null;
                }
            }
        }).start();
    }

    public String parseData(String[] data, Connection connection) {
        if(connection == Connection.ONLINE_PLAYERS) {
            return data[4];
        } else if(connection == Connection.MOTD) {
            return data[3];
        } else if(connection == Connection.MAX_PLAYERS) {
            return data[5];
        } else if(connection == Connection.MAP) {
            return data[6];
        } else {
            System.err.println("Connection value not handled!");
        }
        return null;
    }

    public enum Connection {
        ONLINE_PLAYERS, MAX_PLAYERS, MOTD, MAP
    }
}
