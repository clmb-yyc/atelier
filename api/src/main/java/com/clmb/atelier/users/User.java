package com.clmb.atelier.users;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;
    private String username;
    private String password_hash;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPassword() {
        return password_hash;
    }

    public void setPassword(String password_hash) {
        this.password_hash = password_hash;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password_hash='" + password_hash + '\'' +
                '}';
    }
}
