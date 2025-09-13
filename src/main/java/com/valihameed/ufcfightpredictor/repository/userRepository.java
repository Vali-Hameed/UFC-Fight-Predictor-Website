package com.valihameed.ufcfightpredictor.repository;


import com.valihameed.ufcfightpredictor.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface userRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
