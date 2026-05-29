package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.ScrapeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapeLogRepository extends JpaRepository<ScrapeLog, Long> {
}
