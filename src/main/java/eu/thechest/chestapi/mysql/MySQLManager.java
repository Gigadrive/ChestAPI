package eu.thechest.chestapi.mysql;

import eu.thechest.chestapi.ChestAPI;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;

/**
 * Created by zeryt on 11.02.2017.
 */
public class MySQLManager {
    private static MySQLManager instance;
    private static Connection con;

    private static String host;
    private static String user;
    private static String password;
    private static String database;
    private static int port;

    public static MySQLManager getInstance(){
        return instance;
    }

    static {
        if(instance == null) instance = new MySQLManager();
    }

    public MySQLManager(){
        load();
    }

    public Connection getConnection(){
        checkConnection();

        return this.con;
    }

    public void load(){
        FileConfiguration config = ChestAPI.getInstance().getConfig();

        this.host = config.getString("mysql.host");
        this.user = config.getString("mysql.user");
        this.password = config.getString("mysql.password");
        this.database = config.getString("mysql.database");
        this.port = config.getInt("mysql.port");
    }

    private void openConnection(){
        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            con = DriverManager.getConnection(url,user,password);
        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    private void checkConnection(){
        try {
            if(this.con == null || !this.con.isValid(2)){
                openConnection();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void closeResources(ResultSet rs, PreparedStatement ps){
        try {
            if(rs != null) rs.close();
            if(ps != null) ps.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void unload(){
        try {
            if(con != null && this.con.isValid(2)){
                con.close();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
