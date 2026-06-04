package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
	List<Event> findByEventDateBetweenAndStatus(java.time.OffsetDateTime start, java.time.OffsetDateTime end, String status);
	Optional<Event> findByName(String name);
	List<Event> findByStatusOrderByEventDateDesc(String status, Pageable pageable);
	List<Event> findByStatusOrderByEventDateAsc(String status, Pageable pageable);
}
