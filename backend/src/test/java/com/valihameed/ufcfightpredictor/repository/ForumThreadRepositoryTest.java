package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.ForumThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ForumThreadRepositoryTest {

    @Autowired
    private ForumThreadRepository forumThreadRepository;

    @BeforeEach
    void setUp() {
        ForumThread thread1 = ForumThread.builder()
                .eventId(100L)
                .fightId(null)
                .title("UFC 300 Discussion")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        ForumThread thread2 = ForumThread.builder()
                .eventId(100L)
                .fightId(10L)
                .title("McGregor vs Chandler Discussion")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        ForumThread thread3 = ForumThread.builder()
                .eventId(200L)
                .fightId(null)
                .title("UFC Fight Night Discussion")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        forumThreadRepository.saveAll(List.of(thread1, thread2, thread3));
    }

    @Test
    void itShouldFindByEventId() {
        List<ForumThread> threads = forumThreadRepository.findByEventId(100L);

        assertThat(threads).hasSize(2);
    }

    @Test
    void itShouldFindByFightId() {
        List<ForumThread> threads = forumThreadRepository.findByFightId(10L);

        assertThat(threads).hasSize(1);
        assertThat(threads.get(0).getTitle()).isEqualTo("McGregor vs Chandler Discussion");
    }

    @Test
    void itShouldCheckIfExistsByEventIdAndFightIdIsNull() {
        boolean exists = forumThreadRepository.existsByEventIdAndFightIdIsNull(100L);
        boolean notExists = forumThreadRepository.existsByEventIdAndFightIdIsNull(999L);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void itShouldCheckIfExistsByFightId() {
        boolean exists = forumThreadRepository.existsByFightId(10L);
        boolean notExists = forumThreadRepository.existsByFightId(999L);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
