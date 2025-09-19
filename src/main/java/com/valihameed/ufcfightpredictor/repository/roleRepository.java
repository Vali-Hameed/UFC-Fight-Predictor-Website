package com.valihameed.ufcfightpredictor.repository;


import com.valihameed.ufcfightpredictor.users.role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
    public interface roleRepository extends JpaRepository<role, Long> {
        Optional<role> findByName(String name);
    }

