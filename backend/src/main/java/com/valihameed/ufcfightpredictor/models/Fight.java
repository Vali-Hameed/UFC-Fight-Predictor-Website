package com.valihameed.ufcfightpredictor.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fights", indexes = {
        @Index(name = "idx_fight_event_id", columnList = "event_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "fighter1_name")
    private String fighter1Name;

    @Column(name = "fighter2_name")
    private String fighter2Name;

    @Column(name = "weight_class")
    private String weightClass;

    @Column(name = "is_main_event")
    private Boolean isMainEvent;

    @Column(name = "fight_order")
    private Integer fightOrder;

    private String status;

    @Column(name = "result_winner")
    private String resultWinner;

    @Column(name = "result_method")
    private String resultMethod;

    @Column(name = "result_round")
    private Integer resultRound;

    @Column(name = "result_time")
    private String resultTime;

    @Column(name = "current_round")
    private Integer currentRound;

    @Column(name = "current_clock")
    private String currentClock;

    @Column(name = "live_status")
    private String liveStatus;

    @Column(name = "card_tier")
    private String cardTier;
}
