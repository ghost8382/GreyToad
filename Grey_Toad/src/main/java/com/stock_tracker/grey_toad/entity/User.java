package com.stock_tracker.grey_toad.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "users")
@SQLRestriction("deleted IS NOT TRUE")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "USER"; // USER | LEADER | ADMIN — controls system permissions

    // Free-text display label, e.g. "Wsparcie merytoryczne", "Frontend Lead"
    private String jobTitle;

    @Column(columnDefinition = "TEXT")
    private String quote;

    @Column(nullable = false, columnDefinition = "varchar(255) not null default 'AVAILABLE'")
    private String status = "AVAILABLE"; // AVAILABLE | BREAK | DINNER | OUT_OF_OFFICE | MEETING

    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
}
