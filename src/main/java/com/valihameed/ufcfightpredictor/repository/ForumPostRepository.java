package com.valihameed.ufcfightpredictor.repository;

import com.valihameed.ufcfightpredictor.models.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    List<ForumPost> findByThreadId(Long threadId);
}
