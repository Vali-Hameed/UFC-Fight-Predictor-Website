package com.valihameed.ufcfightpredictor.ml;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrewarmLogRepository extends JpaRepository<PrewarmLog, Long> {

}
