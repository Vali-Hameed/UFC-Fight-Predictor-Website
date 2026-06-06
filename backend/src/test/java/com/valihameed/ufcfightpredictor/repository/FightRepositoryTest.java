package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Fight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class FightRepositoryTest {

    @Autowired
    private FightRepository fightRepository;

    @BeforeEach
    void setUp() {
        Fight fight1 = Fight.builder()
                .eventId(100L)
                .fighter1Name("Conor McGregor")
                .fighter2Name("Dustin Poirier")
                .fightOrder(2)
                .isMainEvent(true)
                .build();

        Fight fight2 = Fight.builder()
                .eventId(100L)
                .fighter1Name("Justin Gaethje")
                .fighter2Name("Michael Chandler")
                .fightOrder(1)
                .isMainEvent(false)
                .build();

        Fight fight3 = Fight.builder()
                .eventId(200L)
                .fighter1Name("Islam Makhachev")
                .fighter2Name("Charles Oliveira")
                .fightOrder(1)
                .isMainEvent(true)
                .build();

        fightRepository.saveAll(List.of(fight1, fight2, fight3));
    }

    @Test
    void itShouldFindByEventIdOrderByFightOrderAsc() {
        List<Fight> fights = fightRepository.findByEventIdOrderByFightOrderAsc(100L);

        assertThat(fights).hasSize(2);
        assertThat(fights.get(0).getFighter1Name()).isEqualTo("Justin Gaethje"); // Order 1
        assertThat(fights.get(1).getFighter1Name()).isEqualTo("Conor McGregor");  // Order 2
    }

    @Test
    void itShouldFindByEventIdAndFighter1NameAndFighter2Name() {
        Optional<Fight> fight = fightRepository.findByEventIdAndFighter1NameAndFighter2Name(
                200L, "Islam Makhachev", "Charles Oliveira");

        assertThat(fight).isPresent();
        assertThat(fight.get().getIsMainEvent()).isTrue();
    }

    @Test
    void itShouldNotFindByEventIdAndFighter1NameAndFighter2NameWhenDoesNotExist() {
        Optional<Fight> fight = fightRepository.findByEventIdAndFighter1NameAndFighter2Name(
                200L, "Jon Jones", "Stipe Miocic");

        assertThat(fight).isNotPresent();
    }
}
