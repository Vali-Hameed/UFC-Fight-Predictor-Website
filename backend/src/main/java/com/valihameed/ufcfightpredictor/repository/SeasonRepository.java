package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByName(String name);

    Optional<Season> findByActiveTrue();

    Optional<Season> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(OffsetDateTime date1, OffsetDateTime date2);

    List<Season> findAllByOrderByStartDateDesc();
}
