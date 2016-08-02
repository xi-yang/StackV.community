/* 
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Alberto Jimenez
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.
 * 
 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */
package net.maxgigapop.mrs.db;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import net.maxgigapop.mrs.crypto.PasswordCryptoService;
import net.maxgigapop.mrs.users.VersaStackUser;

public class UsersDBAccess {

    private Connection connect = null;
    private Statement statement = null;
    private ResultSet resultSet = null;

    private static final String users_db = "jdbc:mysql://localhost/versastack_users";
    private static final String users_db_id = "max";
    private static final String users_db_pw = "max";

    public ArrayList<VersaStackUser> readUsersList() throws ClassNotFoundException, SQLException, IOException {
        ArrayList<VersaStackUser> users = new ArrayList<>();

        try {
            Class.forName("com.mysql.jdbc.Driver");

            connect = DriverManager.getConnection(users_db, users_db_id, users_db_pw);
            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM users");

            if (!resultSet.next()) {
//                System.out.println("No records found");
            } else {
                do {
                    VersaStackUser user = new VersaStackUser();

                    user.setID(resultSet.getInt("id"));
                    user.setPassword(resultSet.getString("password"));
                    user.setSalt(resultSet.getString("salt"));
                    user.setEmail(resultSet.getString("email"));
                    user.setLanguage(resultSet.getString("language"));
                    user.setLastLogin(resultSet.getString("last_login"));

                    users.add(user);
                } while (resultSet.next());
            }

        } catch (ClassNotFoundException | SQLException e) {
            throw e;
        } finally {
            close();
        }

        return users;
    }

    public VersaStackUser findUserByEmail(String email) throws ClassNotFoundException, SQLException {
        VersaStackUser u = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");

            connect = DriverManager.getConnection(users_db, users_db_id, users_db_pw);
            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM users WHERE email = \"" + email + "\" LIMIT 1");

            if (!resultSet.next()) {
//                System.out.println("No records found");
            } else {
                u = new VersaStackUser();
                u.setID(resultSet.getInt("id"));
                u.setPassword(resultSet.getString("password"));
                u.setSalt(resultSet.getString("salt"));
                u.setEmail(resultSet.getString("email"));
                u.setLanguage(resultSet.getString("language"));
                u.setLastLogin(resultSet.getString("last_login"));
            }

        } catch (ClassNotFoundException | SQLException e) {
            throw e;
        } finally {
            close();
        }

        return u;
    }

    public void addUser(String email, String password) throws ClassNotFoundException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            byte[] salt = PasswordCryptoService.generateSalt();
            byte[] encryptedpw = PasswordCryptoService.getEncryptedPassword(password, salt);
            String base64salt = DatatypeConverter.printBase64Binary(salt);
            String base64pw = DatatypeConverter.printBase64Binary(encryptedpw);

            Class.forName("com.mysql.jdbc.Driver");

            connect = DriverManager.getConnection(users_db, users_db_id, users_db_pw);
            statement = connect.createStatement();
            statement.executeUpdate("INSERT INTO users(email, password, salt) "
                    + "VALUES(\"" + email + "\",\"" + base64pw + "\",\"" + base64salt + "\")");
        } catch (ClassNotFoundException | SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw e;
        } finally {
            close();
        }
    }

    public boolean authenticateUser(VersaStackUser u, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = DatatypeConverter.parseBase64Binary(u.getSalt());
        byte[] actualPassword = DatatypeConverter.parseBase64Binary(u.getPassword());
        boolean valid;

        try {
            valid = PasswordCryptoService.authenticate(password, actualPassword, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            valid = false;
            throw e;
        }

        if (valid) {
            try {
                connect = DriverManager.getConnection(users_db, users_db_id, users_db_pw);
                statement = connect.createStatement();
                statement.executeUpdate("UPDATE users SET last_login=NULL WHERE id=\"" + u.getID() + "\"");
            } catch (SQLException e) {
                //report silently
                System.out.println("Error updating last_login for user id=" + u.getID());
            } finally {
                close();
            }
        }

        return valid;
    }

    private void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException ignore) {
        }

        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException ignore) {
        }

        try {
            if (connect != null) {
                connect.close();
            }
        } catch (SQLException ignore) {
        }
    }
}
