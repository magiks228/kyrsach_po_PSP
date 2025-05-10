package org.strah.model.users;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

import org.strah.utils.PasswordUtil;

@Entity
@Table(name = "users")
public class AppUser extends User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected AppUser() { super(); }

    public AppUser(String login, String pwd, String fullName, String roleTitle){
        this(login, pwd, fullName, Role.fromTitle(roleTitle));
    }

    public AppUser(String login, String pwd, String fullName, Role role){
        super(login, pwd, fullName, role);
    }

    public Long getId() { return id; }
}
