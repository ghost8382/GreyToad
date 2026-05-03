package com.stock_tracker.grey_toad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "channels")
@SQLRestriction("deleted IS NOT TRUE")
@Getter
@Setter
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50) not null default 'TEAM'")
    private ChannelScope scope = ChannelScope.TEAM;

    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
}