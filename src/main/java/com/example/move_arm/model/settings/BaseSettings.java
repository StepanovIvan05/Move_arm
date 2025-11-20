package com.example.move_arm.model.settings;

// Этот класс готов стать @MappedSuperclass в JPA
public abstract class BaseSettings {
    
    // @Id
    // @GeneratedValue
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}