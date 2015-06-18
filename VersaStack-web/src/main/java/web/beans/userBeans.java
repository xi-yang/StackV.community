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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class userBeans {

    String login_db_user = "root";
    String login_db_pass = "takehaya";
    String reg_db_user = "root";
    String reg_db_pass = "takehaya";
    String front_db_user = "root";
    String front_db_pass = "takehaya";

    String username = "";
    String firstName = "";
    String lastName = "";
    String id = "";
    String password_hash = "";
    String usergroup = "";
    ArrayList<Integer> service_list = new ArrayList<>();

    boolean loggedIn = false;

    //TEMP
    public String ret_list() {
        String ret_string = "Size: " + service_list.size() + ". Elements:";
        for (Integer serv_id : service_list) {
            ret_string = ret_string + " " + serv_id;
        }
        return ret_string;
    }

    public userBeans() {

    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getUsergroup() {
        return usergroup;
    }

    public boolean isAllowed(int serv_id) {
        return service_list.contains(serv_id);
    }

    // Login
    public boolean isLoggedIn() {
        return loggedIn;
    }

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
            log_conn = DriverManager.getConnection("jdbc:mysql://localhost:8889/Login",
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
                    front_conn = DriverManager.getConnection("jdbc:mysql://localhost:8889/Frontend",
                            front_connectionProps);

                    username = user;
                    password_hash = digest_str;

                    //Pull Userdata
                    prep = front_conn.prepareStatement("SELECT first_name, last_name, user_id,"
                            + " usergroup_id FROM Frontend.user_info U WHERE U.username = ?");
                    prep.setString(1, user);
                    ResultSet rs2 = prep.executeQuery();
                    if (rs2.next()) {
                        firstName = rs2.getString(1);
                        lastName = rs2.getString(2);
                        id = rs2.getString(3);
                        usergroup = rs2.getString(4);
                        loggedIn = true;
                    } else {
                        throw new IllegalStateException("SQL Inconsistency!");
                    }

                    //Pull ACLs
                    service_list = new ArrayList<>();
                    prep = front_conn.prepareStatement("SELECT DISTINCT A.service_id FROM acl A JOIN acl_entry_group G, acl_entry_user U WHERE"
                            + "(A.acl_id = G.acl_id AND G.usergroup_id = ?) OR (A.acl_id = U.acl_id AND U.user_id = ?)");
                    prep.setString(1, usergroup);
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

    public void logOut() {
        loggedIn = false;
    }

    // Registration
    /*  Error codes:
     - 0: success
     - 3: duplicate username
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
            log_conn = DriverManager.getConnection("jdbc:mysql://localhost:8889/Login",
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
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:8889/Frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("INSERT INTO Frontend.user_info "
                    + "(`username`, `email`, `usergroup_id`, `first_name`, `last_name`)"
                    + " VALUES (?, ?, ?, ?, ?)");
            prep.setString(1, user);
            prep.setString(2, email);
            prep.setInt(3, Integer.parseInt(usergroup_id));
            prep.setString(4, first_name);
            prep.setString(5, last_name);
            prep.executeUpdate();

            front_conn.close();

        } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(userBeans.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Cannot log_connect the database!", ex);
        }

        return 0;
    }

    // Update
    public void update(String username, String pass, String first_name,
            String last_name, String email) {
        try {
            if (!pass.isEmpty()) {
                pass = pass.trim();

                // Database Connection
                Connection log_conn;
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                Properties log_connectionProps = new Properties();
                log_connectionProps.put("user", reg_db_user);
                log_connectionProps.put("password", reg_db_pass);
                log_conn = DriverManager.getConnection("jdbc:mysql://localhost:8889/Login",
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

                log_conn.close();
            }
            
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:8889/Frontend",
                    front_connectionProps);
            
            if (!first_name.isEmpty()) {
                first_name = first_name.trim();
                
                PreparedStatement prep = front_conn.prepareStatement("UPDATE Frontend.user_info SET `first_name` = ? WHERE `username` = ?");
                prep.setString(1, first_name);
                prep.setString(2, username);
                prep.executeUpdate();
            }
            if (!last_name.isEmpty()) {
                last_name = last_name.trim();
                
                PreparedStatement prep = front_conn.prepareStatement("UPDATE Frontend.user_info SET `last_name` = ? WHERE `username` = ?");
                prep.setString(1, last_name);
                prep.setString(2, username);
                prep.executeUpdate();
            }
            if (!email.isEmpty()) {
                email = email.trim();
                
                PreparedStatement prep = front_conn.prepareStatement("UPDATE Frontend.user_info SET `email` = ? WHERE `username` = ?");
                prep.setString(1, email);
                prep.setString(2, username);
                prep.executeUpdate();
            }
        } catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(userBeans.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Cannot log_connect the database!", ex);
        }
    }

    // Utility Functions
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
}
