package com.valihameed.ufcfightpredictor.repository;


import com.valihameed.ufcfightpredictor.users.user;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface userRepository extends JpaRepository<user, Long> {
    Optional<user> findByUsername(String username);
    Optional<user> findByEmail(String email);
}
