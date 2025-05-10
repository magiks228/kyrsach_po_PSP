package org.strah.model.users;
import org.strah.model.users.Role;

// Класс Employee наследуется от User
public class Employee extends User {

    // Конструктор класса Employee
    public Employee(String login, String pwd, String fullName){
        super(login, pwd, fullName, Role.STAFF);
    }

    // Реализация абстрактного метода
    @Override
    public String getRole() {
        return "Сотрудник";
    }
}
