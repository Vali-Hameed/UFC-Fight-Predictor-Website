package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
}
