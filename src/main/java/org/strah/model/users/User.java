package org.strah.model.users;

import java.io.Serializable;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import org.strah.model.users.Role;
import org.strah.utils.PasswordUtil;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class User implements Serializable {

    protected User() {}

    @NaturalId
    @Column(unique = true, nullable = false)
    protected String login;

    @Column(name = "full_name", nullable = false)
    protected String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    protected Role role;

    protected String password;


    /* ---------- пароль ---------- */
    @Column(name = "hash_pwd", length = 64, nullable = false)
    protected String hashPwd;        // hex‑SHA‑256

    @Column(name = "salt", length = 32, nullable = false)
    protected String salt;           // hex‑16 байт


    // Конструктор класса User
    public User(String login, String rawPassword, String fullName, Role role){
        this.login = login;
        this.fullName = fullName;
        this.role = role;

        this.salt = PasswordUtil.newSalt();
        this.hashPwd = PasswordUtil.hash(salt, rawPassword);
    }


    // Геттеры и сеттеры (инкапсуляция)
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRoleEnum() { return role; }          // enum
    public String getRole()   { return role.getTitle(); } // строковое представление

    public void setRole(Role r) { this.role = r; }

    /* ---------- password helpers ---------- */

    public boolean checkPassword(String rawPwd){
        return PasswordUtil.matches(salt, hashPwd, rawPwd);
    }

    public void setNewPassword(String rawPwd){
        this.salt = PasswordUtil.newSalt();
        this.hashPwd = PasswordUtil.hash(salt, rawPwd);
    }

    // Переопределение метода toString()
    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", getRole(), fullName, login);
    }
}
