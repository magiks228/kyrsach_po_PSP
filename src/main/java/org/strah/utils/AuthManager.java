package org.strah.utils;

import org.strah.model.users.*;

import java.util.HashMap;
import java.util.Map;

import org.strah.model.users.Role;

public class AuthManager implements Authenticable {

    private Map<String, User> users = new HashMap<>();

    public AuthManager() {
        users.put("admin",    new Admin("admin", "admin123", "Администратор"));
        users.put("employee", new Employee("employee", "emp123", "Сотрудник"));
        users.put("customer", new Customer("customer", "cust123", "Клиент"));
    }

    // реализация авторизации
    @Override
    public boolean authenticate(String login, String password) {
        User user = users.get(login);
        return user != null && user.checkPassword(password);
    }

    // метод, возвращающий роль пользователя после авторизации
    public Role getUserRole(String login) {
        User user = users.get(login);
        return user != null ? user.getRoleEnum() : null;
    }
}
