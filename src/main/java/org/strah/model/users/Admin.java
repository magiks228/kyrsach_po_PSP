package org.strah.model.users;
import org.strah.model.users.Role;

// Класс Admin наследуется от User
public class Admin extends User {

    // Конструктор класса Admin
    public Admin(String login, String pwd, String fullName){
        super(login, pwd, fullName, Role.ADMIN);
    }

    // Реализация абстрактного метода
    @Override
    public String getRole() {
        return "Администратор";
    }
}
