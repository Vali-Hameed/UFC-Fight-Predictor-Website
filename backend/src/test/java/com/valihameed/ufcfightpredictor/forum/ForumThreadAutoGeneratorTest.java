package com.valihameed.ufcfightpredictor.forum;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.models.ForumThread;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.ForumThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ForumThreadAutoGeneratorTest {

    @Mock private EventRepository eventRepository;
    @Mock private FightRepository fightRepository;
    @Mock private ForumThreadRepository forumThreadRepository;

    private ForumThreadAutoGenerator underTest;

    @BeforeEach
    void setUp() {
        underTest = new ForumThreadAutoGenerator(
                eventRepository,
                fightRepository,
                forumThreadRepository
        );
    }

    @Test
    void generatesMissingThreadsOnStartup() {
        // Given
        Event event1 = new Event();
        event1.setId(100L);
        event1.setName("UFC 300");

        Fight fight1 = new Fight();
        fight1.setId(10L);
        fight1.setEventId(100L);
        fight1.setFighter1Name("Conor McGregor");
        fight1.setFighter2Name("Dustin Poirier");

        Fight fight2WithoutNames = new Fight();
        fight2WithoutNames.setId(11L);

        given(eventRepository.findAll()).willReturn(List.of(event1));
        given(fightRepository.findAll()).willReturn(List.of(fight1, fight2WithoutNames));
        
        // Simulating that thread for event1 doesn't exist, and thread for fight1 doesn't exist
        given(forumThreadRepository.existsByEventIdAndFightIdIsNull(100L)).willReturn(false);
        given(forumThreadRepository.existsByFightId(10L)).willReturn(false);

        // When
        underTest.generateMissingThreadsOnStartup();

        // Then
        ArgumentCaptor<ForumThread> threadCaptor = ArgumentCaptor.forClass(ForumThread.class);
        verify(forumThreadRepository, times(2)).save(threadCaptor.capture());

        List<ForumThread> savedThreads = threadCaptor.getAllValues();
        
        ForumThread eventThread = savedThreads.get(0);
        assertThat(eventThread.getEventId()).isEqualTo(100L);
        assertThat(eventThread.getFightId()).isNull();
        assertThat(eventThread.getTitle()).isEqualTo("UFC 300 Discussion");

        ForumThread fightThread = savedThreads.get(1);
        assertThat(fightThread.getEventId()).isEqualTo(100L);
        assertThat(fightThread.getFightId()).isEqualTo(10L);
        assertThat(fightThread.getTitle()).isEqualTo("Conor McGregor vs Dustin Poirier Discussion");
    }

    @Test
    void doesNotGenerateThreadsWhenAlreadyExist() {
        // Given
        Event event1 = new Event();
        event1.setId(100L);

        Fight fight1 = new Fight();
        fight1.setId(10L);
        fight1.setFighter1Name("A");
        fight1.setFighter2Name("B");

        given(eventRepository.findAll()).willReturn(List.of(event1));
        given(fightRepository.findAll()).willReturn(List.of(fight1));
        
        // Simulating that threads already exist
        given(forumThreadRepository.existsByEventIdAndFightIdIsNull(100L)).willReturn(true);
        given(forumThreadRepository.existsByFightId(10L)).willReturn(true);

        // When
        underTest.generateMissingThreadsOnStartup();

        // Then
        verify(forumThreadRepository, times(0)).save(any(ForumThread.class));
    }
}
