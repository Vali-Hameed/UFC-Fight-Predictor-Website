package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.ThreadSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThreadSubscriptionRepository extends JpaRepository<ThreadSubscription, Long> {
    Optional<ThreadSubscription> findByUserIdAndThreadId(Long userId, Long threadId);
    List<ThreadSubscription> findByThreadId(Long threadId);
}
