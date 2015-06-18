package web.beans;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class serviceBeans {

    String login_db_user = "root";
    String login_db_pass = "takehaya";
    String front_db_user = "root";
    String front_db_pass = "takehaya";

    public serviceBeans() {

    }
    
    // TODO::   Implement skeleton, replace this with pre-condition/post-condition
    //          documentation.
    public int driverInstall(String driverID) {
        
        
        return 0;
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
