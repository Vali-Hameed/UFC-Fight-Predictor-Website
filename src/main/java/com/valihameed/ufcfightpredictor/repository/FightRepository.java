package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Fight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FightRepository extends JpaRepository<Fight, Long> {
    List<Fight> findByEventId(Long eventId);
}
