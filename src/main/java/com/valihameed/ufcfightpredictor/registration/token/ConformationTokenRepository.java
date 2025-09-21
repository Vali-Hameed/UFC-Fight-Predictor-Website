package com.valihameed.ufcfightpredictor.registration.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConformationTokenRepository extends JpaRepository<ConformationToken, Long> {

    Optional<ConformationToken> findByToken(String token);
}
