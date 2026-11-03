package com.familyfinance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ff_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    public Role(String name) {
        this.name = name;
    }
}