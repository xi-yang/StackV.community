package net.maxgigapop.mrs.users;

public class VersaStackUser {

    private int id;
    private String password;
    private String email;
    private String language;
    private String lastLogin;
    private String salt;

    public int getID() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getLanguage() {
        return language;
    }

    public String getLastLogin() {
        return lastLogin;
    }
    
    public String getSalt() {
        return salt;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public void setSalt(String salt) {
        this.salt = salt;
    }
}
