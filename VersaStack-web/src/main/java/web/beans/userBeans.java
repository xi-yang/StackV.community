package web.beans;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;



public class userBeans {

    String login_db_user = "login_view";
    String login_db_pass = "loginuser";
    String reg_db_user = "login_view";
    String reg_db_pass = "loginuser";
    String front_db_user = "front_view";
    String front_db_pass = "frontuser";

    String username = "";
    String firstName = "";
    String lastName = "";
    String id = "";
    String password_hash = "";
    String active_usergroup = "";
    ArrayList<Integer> service_list = new ArrayList<>();
    ArrayList<Integer> group_list = new ArrayList<>();
    HashMap<String, String> model_map = new HashMap<>();
    String[] current_model = {"",""};

    boolean loggedIn = false;

    public userBeans() {
        
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
    
    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

    public String getActiveUsergroup() {
        return active_usergroup;
    }

    public boolean isAllowed(int serv_id) {
        return service_list.contains(serv_id);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void logOut() {
        loggedIn = false;
    }
    
    /**
     * Authenticates user against login database.
     * @param user username
     * @param pass unencrypted password
     * @return true if authentication is successful; false otherwise.
     */
    public boolean login(String user, String pass) {
        user = user.trim();
        pass = pass.trim();
        try {
            // Database Connection
            Connection log_conn, front_conn;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Properties log_connectionProps = new Properties();
            log_connectionProps.put("user", login_db_user);
            log_connectionProps.put("password", login_db_pass);
            log_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/login",
                    log_connectionProps);

            // Grab User Salt
            PreparedStatement prep = log_conn.prepareStatement("SELECT salt FROM Login.cred L "
                    + "WHERE L.username = ?");
            prep.setString(1, user);
            ResultSet rs1 = prep.executeQuery();
            if (!rs1.next()) {
                loggedIn = false;
            } else {
                String salt = rs1.getString(1);
                String digest_str = shaEnc(pass, salt);

                // Registration Authentication 
                prep = log_conn.prepareStatement("SELECT username, password_hash FROM Login.cred L "
                        + "WHERE L.username = ? AND L.password_hash = ?");
                prep.setString(1, user);
                prep.setString(2, digest_str);
                rs1 = prep.executeQuery();

                // Authenticated
                if (rs1.next()) {
                    log_conn.close();

                    Properties front_connectionProps = new Properties();
                    front_connectionProps.put("user", front_db_user);
                    front_connectionProps.put("password", front_db_pass);
                    front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                            front_connectionProps);

                    username = user;
                    password_hash = digest_str;

                    //Pull Userdata
                    prep = front_conn.prepareStatement("SELECT first_name, last_name, user_id,"
                            + " active_usergroup FROM Frontend.user_info U WHERE U.username = ?");
                    prep.setString(1, user);
                    ResultSet rs2 = prep.executeQuery();
                    if (rs2.next()) {
                        firstName = rs2.getString(1);
                        lastName = rs2.getString(2);
                        id = rs2.getString(3);
                        active_usergroup = rs2.getString(4);
                        loggedIn = true;
                    } else {
                        throw new IllegalStateException("SQL Inconsistency!");
                    }

                    //Collect Usergroups
                    prep = front_conn.prepareStatement("SELECT usergroup_id FROM user_belongs WHERE user_id = ?");
                    prep.setString(1, id);
                    rs2 = prep.executeQuery();
                    while (rs2.next()) {
                        group_list.add(rs2.getInt("usergroup_id"));
                    }

                    //Verify Active Group
                    if (!group_list.contains(Integer.parseInt(active_usergroup))) {
                        if (group_list.isEmpty()) {
                            // @@NOTICE: REPLACE ID WITH DEFAULT USER GROUP IN FUTURE
                            prep = front_conn.prepareStatement("UPDATE user_info SET `active_usergroup` = 2 WHERE `user_id` = ?");
                            prep.setString(1, id);
                        } else {
                            prep = front_conn.prepareStatement("UPDATE user_info SET `active_usergroup` = ? WHERE `user_id` = ?");
                            prep.setString(1, String.valueOf(group_list.get(0)));
                            prep.setString(2, id);
                        }
                        prep.executeUpdate();

                        prep = front_conn.prepareStatement("SELECT active_usergroup FROM user_info WHERE user_id = ?");
                        prep.setString(1, id);
                        rs2 = prep.executeQuery();
                        while (rs2.next()) {
                            active_usergroup = rs2.getString("active_usergroup");
                        }
                    }

                    //Pull ACLs
                    service_list = new ArrayList<>();
                    prep = front_conn.prepareStatement("SELECT DISTINCT A.service_id FROM acl A JOIN acl_entry_group G, acl_entry_user U WHERE"
                            + "(A.acl_id = G.acl_id AND G.usergroup_id = ?) OR (A.acl_id = U.acl_id AND U.user_id = ?)");
                    prep.setString(1, active_usergroup);
                    prep.setString(2, id);
                    rs2 = prep.executeQuery();
                    while (rs2.next()) {
                        service_list.add(rs2.getInt("service_id"));
                    }

                    front_conn.close();

                } else {
                    loggedIn = false;
                }
            }
        } catch (SQLException | ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedEncodingException |
                NoSuchAlgorithmException ex) {
            loggedIn = false;
            Logger.getLogger(userBeans.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Cannot log_connect the database!", ex);
        }

        return loggedIn;
    }

    /**
     * Registers new user into frontend database.
     * Parameters self-explanatory.
     * @param user
     * @param pass
     * @param first_name
     * @param last_name
     * @param usergroup_id
     * @param email
     * @return error code:<br />
     *  0 - success.<br />
     *  3 - duplicate username.<br />
     */
    public int register(String user, String pass, String first_name,
            String last_name, String usergroup_id, String email) {
        user = user.trim();
        pass = pass.trim();
        first_name = first_name.trim();
        last_name = last_name.trim();
        email = email.trim();

        try {
            // Database Connection
            Connection log_conn, front_conn;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Properties log_connectionProps = new Properties();
            log_connectionProps.put("user", reg_db_user);
            log_connectionProps.put("password", reg_db_pass);
            log_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Login",
                    log_connectionProps);

            //Check for duplicate username
            PreparedStatement prep = log_conn.prepareStatement("SELECT username"
                    + " FROM Login.cred L WHERE L.username = ?");
            prep.setString(1, user);
            ResultSet rs1 = prep.executeQuery();
            if (rs1.next()) {
                return 3;
            }

            // Encrypt password
            SecureRandom random = new SecureRandom();
            String salt = new BigInteger(320, random).toString(32);

            String pass_enc;
            try {
                pass_enc = shaEnc(pass, salt);
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
                Logger.getLogger(userBeans.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException("SHA Encryption failure!", ex);
            }

            // Registration into Login DB
            prep = log_conn.prepareStatement("INSERT INTO Login.cred "
                    + "(`username`, `password_hash`, `salt`) VALUES (?, ?, ?)");
            prep.setString(1, user);
            prep.setString(2, pass_enc);
            prep.setString(3, salt);
            prep.executeUpdate();

            log_conn.close();

            // Regisgration into Frontend DB
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("INSERT INTO Frontend.user_info "
                    + "(`username`, `email`, `active_usergroup`, `first_name`, `last_name`)"
                    + " VALUES (?, ?, ?, ?, ?)");
            prep.setString(1, user);
            prep.setString(2, email);
            prep.setInt(3, Integer.parseInt(usergroup_id));
            prep.setString(4, first_name);
            prep.setString(5, last_name);
            prep.executeUpdate();

            int user_id;
            prep = front_conn.prepareStatement("SELECT user_id"
                    + " FROM Frontend.user_info I WHERE I.username = ?");
            prep.setString(1, user);
            ResultSet rs2 = prep.executeQuery();
            if (rs2.next()) {
                user_id = rs2.getInt("user_id");

                prep = front_conn.prepareStatement("INSERT INTO Frontend.user_belongs (`user_id`, `usergroup_id`)"
                        + " VALUES (?, ?)");
                prep.setInt(1, user_id);
                prep.setInt(2, Integer.parseInt(usergroup_id));
                prep.executeUpdate();
            }

            front_conn.close();

        } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(userBeans.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Cannot log_connect the database!", ex);
        }

        return 0;
    }

    /**
     * Updates user information. Only processes fields entered.
     * Parameters self-explanatory.
     * @param username
     * @param pass
     * @param first_name
     * @param last_name
     * @param email
     * @param activegroup 
     */
    public void update(String username, String pass, String first_name,
            String last_name, String email, String activegroup) {
        try {
            if (!pass.isEmpty()) {
                pass = pass.trim();

                // Database Connection
                Connection log_conn;
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                Properties log_connectionProps = new Properties();
                log_connectionProps.put("user", reg_db_user);
                log_connectionProps.put("password", reg_db_pass);
                log_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Login",
                        log_connectionProps);

                SecureRandom random = new SecureRandom();
                String salt = new BigInteger(320, random).toString(32);

                String pass_enc;
                try {
                    pass_enc = shaEnc(pass, salt);
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
                    Logger.getLogger(userBeans.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IllegalStateException("SHA Encryption failure!", ex);
                }

                // Update into Login DB
                PreparedStatement prep = log_conn.prepareStatement("UPDATE Login.cred SET `password_hash` = ?, `salt` = ? WHERE `username` = ?");
                prep.setString(1, pass_enc);
                prep.setString(2, salt);
                prep.setString(3, username);
                prep.executeUpdate();
                password_hash = pass_enc;

                log_conn.close();
            }

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                    front_connectionProps);

            if (!first_name.isEmpty()) {
                first_name = first_name.trim();

                PreparedStatement prep = front_conn.prepareStatement("UPDATE Frontend.user_info SET `first_name` = ? WHERE `username` = ?");
                prep.setString(1, first_name);
                prep.setString(2, username);
                prep.executeUpdate();
                firstName = first_name;
            }
            if (!last_name.isEmpty()) {
                last_name = last_name.trim();

                PreparedStatement prep = front_conn.prepareStatement("UPDATE Frontend.user_info SET `last_name` = ? WHERE `username` = ?");
                prep.setString(1, last_name);
                prep.setString(2, username);
                prep.executeUpdate();
                lastName = last_name;
            }
            if (!email.isEmpty()) {
                email = email.trim();

                PreparedStatement prep = front_conn.prepareStatement("UPDATE Frontend.user_info SET `email` = ? WHERE `username` = ?");
                prep.setString(1, email);
                prep.setString(2, username);
                prep.executeUpdate();
            }
            
            PreparedStatement prep = front_conn.prepareStatement("UPDATE Frontend.user_info SET `active_usergroup` = ? WHERE `username` = ?");
            prep.setString(1, activegroup);
            prep.setString(2, username);
            prep.executeUpdate();
            active_usergroup = activegroup;

            refreshACL();

        } catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(userBeans.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Cannot log_connect the database!", ex);
        }
    }

    // Utility Functions
    
    /**
     * Encrypts password using SHA-256 salted encryption.
     * @param pass unencrypted password
     * @param salt randomly-generated salt
     * @return encrypted password.
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException 
     */
    private static String shaEnc(String pass, String salt) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String salted_password = pass + salt;

        md.update(salted_password.getBytes("UTF-8"));
        byte[] digest = md.digest();

        String digest_str = "";
        for (byte by : digest) {
            digest_str += by;
        }

        return digest_str;
    }

    /**
     * Refreshes local ACL permissions list.
     * @throws SQLException 
     */
    private void refreshACL() throws SQLException {
        service_list.clear();

        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                front_connectionProps);

        service_list = new ArrayList<>();
        PreparedStatement prep = front_conn.prepareStatement("SELECT DISTINCT A.service_id FROM acl A JOIN acl_entry_group G, acl_entry_user U WHERE"
                + "(A.acl_id = G.acl_id AND G.usergroup_id = ?) OR (A.acl_id = U.acl_id AND U.user_id = ?)");
        prep.setString(1, active_usergroup);
        prep.setString(2, id);
        ResultSet rs1 = prep.executeQuery();
        while (rs1.next()) {
            service_list.add(rs1.getInt("service_id"));
        }
    }
    
    public String printModels() {
        return model_map.toString();
    }
    
    public void addModel(String name, String model) {
        model_map.put(name, model);
        current_model[0] = name;
        current_model[1] = model;
    }
    
    public void removeModel(String name) {
        model_map.remove(name);
    }
    
    public void setCurr(String filterName, String filterModel) {
        current_model[0] = filterName;
        current_model[1] = filterModel;
    }
    
    public String getModelName() {
        return current_model[0];
    }
    
    public String getTtlModel() {
        return current_model[1];               
    }
        
    public String getModels() {
        return new JSONObject(model_map).toJSONString();
    }
    
    public ArrayList<String> getModelNames() {
        return new ArrayList<>(model_map.keySet());
    }
}
