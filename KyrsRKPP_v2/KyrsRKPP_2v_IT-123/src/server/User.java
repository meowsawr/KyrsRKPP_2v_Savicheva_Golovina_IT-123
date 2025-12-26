package server;

public class User {
    private String username;
    private String password;
    private boolean isVip;  // true=вип, false=обычный

    public User(String username, String password, boolean isVip) {
        this.username = username;
        this.password = password;
        this.isVip = isVip;
    }

    // геттеры
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isVip() { return isVip; }
}