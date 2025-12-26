package server;

import java.util.HashMap;

// управление пользователями
public class UserManager {
    private HashMap<String, User> users = new HashMap<>();

    public UserManager() {
        // тестовые пользователи
        users.put("user", new User("user", "123", false));  // обычный
        users.put("vip", new User("vip", "123", true));     // VIP
    }

    // проверка логина/пароля
    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }

    // проверка вип статуса
    public boolean isVip(String username) {
        User user = users.get(username);
        return user != null && user.isVip();
    }
}