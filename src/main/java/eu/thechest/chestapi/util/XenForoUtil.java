package eu.thechest.chestapi.util;

import eu.thechest.chestapi.mysql.MySQLManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.print.attribute.standard.MediaSize;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Created by zeryt on 01.03.2017.
 */
public class XenForoUtil {
    public static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    public static final String API_HASH = "*************************";
    public static final String NAME_FIELD = "minecraftusername";
    public static final String UUID_FIELD = "minecraftuuid";
    public static final String SITE = "https://thechest.eu/";

    public static String registerUser(String user, String password, String email) {
        if (email.contains("=") || email.contains("&")) {
            return "Potential injection attack! This event has been recorded!";
        }

        if (!email.matches(EMAIL_PATTERN)) {
            return "Invalid email address!";
        }

        String uuid = "null";
        Player p = Bukkit.getPlayer(user);
        if (p != null) uuid = p.getUniqueId().toString();

        try {
            PreparedStatement y = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `xf_user` WHERE `username` = ?");
            y.setString(1,p.getName());
            ResultSet r = y.executeQuery();
            if(r.first()){
                r.close();
                return "You are already registered!";
            }
            r.close();

            String link = "api.php?action=register&hash=" + API_HASH + "&username=" + user.replace('&', '.') +
                    "&password=" + password + "&email=" + email.replace('&', '.').replace('=', '.') +
                    /*"&custom_fields=" + NAME_FIELD + "=" + user + "," + UUID_FIELD + "=" + uuid + */"&user_state=email_confirm";

            URL url = new URL(SITE + link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Open URL connection
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // User already exists?
                if (inputLine.contains("{\"error\":7,\"message\":\"Something went wrong when \\\"registering user\\\": \\\"" +
                        "User already exists\\\"\",\"user_error_id\":40,\"user_error_field\":\"username\",\"" +
                        "user_error_key\":\"usernames_must_be_unique\",\"user_error_phrase\":\"Usernames must be unique." +
                        " The specified username is already in use.\"}"))
                    return "A user under this username already exists!";

                // Email already in use?
                if (inputLine.contains("{\"error\":7,\"message\":\"Something went wrong when \\\"registering user\\\": \\\"" +
                        "Email already used\\\"\",\"user_error_id\":42,\"user_error_field\":\"email\",\"user_error_key\":\"" +
                        "email_addresses_must_be_unique\",\"user_error_phrase\":\"Email addresses must be unique. " +
                        "The specified email address is already in use.\"}"))
                    return "A user under this email address already exists!";
            }

            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `xf_user` WHERE `username` = ?");
            ps.setString(1,p.getName());
            ResultSet rs = ps.executeQuery();
            if(rs.first()){
                int userID = rs.getInt("user_id");

                PreparedStatement a = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `xf_user_field_value` (`user_id`,`field_id`,`field_value`) VALUES(?,?,?);");
                a.setInt(1,userID);
                a.setString(2,"minecraftuuid");
                a.setString(3,uuid);
                a.executeUpdate();
                a.close();

                PreparedStatement b = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `xf_user_field_value` (`user_id`,`field_id`,`field_value`) VALUES(?,?,?);");
                b.setInt(1,userID);
                b.setString(2,"minecraftusername");
                b.setString(3,p.getName());
                b.executeUpdate();
                b.close();
            }
            MySQLManager.getInstance().closeResources(rs,ps);
        } catch(Exception e){
            e.printStackTrace();
            return "An internal error has occured! The stack trace has been printed to the server's console!";
        }

        return "Successfully registered on the forums! A confirmation email should be sent to your email address shortly! Please change your password once you confirm your account!";
    }
}
