package com.k3ras.agorabank.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String name; // USER, ADMIN
    
    @ManyToMany(mappedBy = "roles")
    private Set<AuthUser> users = new HashSet<>();
    
    public Role() {
    }
    
    public Role(String name) {
        this.name = name;
    }
    
    // Getters y Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Set<AuthUser> getUsers() {
        return users;
    }
    
    public void setUsers(Set<AuthUser> users) {
        this.users = users;
    }
}
