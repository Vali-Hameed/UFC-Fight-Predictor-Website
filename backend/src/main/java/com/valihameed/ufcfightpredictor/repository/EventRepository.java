package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
	java.util.List<com.valihameed.ufcfightpredictor.models.Event> findByEventDateBetweenAndStatus(java.time.OffsetDateTime start, java.time.OffsetDateTime end, String status);
}

