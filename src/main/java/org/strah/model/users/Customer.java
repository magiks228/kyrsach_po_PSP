package org.strah.model.users;
import org.strah.model.users.Role;

// Класс Customer наследуется от User
public class Customer extends User {

    // Конструктор класса Customer
    public Customer(String login, String pwd, String fullName) {
        super(login, pwd, fullName, Role.CLIENT);
    }

    // Реализация абстрактного метода
    @Override
    public String getRole() {
        return "Клиент";
    }
}
