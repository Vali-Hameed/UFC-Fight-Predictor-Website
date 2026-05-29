package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "community_votes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fight_id", unique = true)
    private Long fightId;

    @Column(name = "fighter1_votes")
    private Integer fighter1Votes;

    @Column(name = "fighter2_votes")
    private Integer fighter2Votes;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
