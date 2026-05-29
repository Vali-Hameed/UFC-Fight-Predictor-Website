package com.valihameed.ufcfightpredictor.forum;

import com.valihameed.ufcfightpredictor.models.ForumPost;
import com.valihameed.ufcfightpredictor.repository.ForumPostRepository;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.util.InputSanitizer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/forum/posts")
@AllArgsConstructor
public class ForumPostController {
    private final ForumPostRepository forumPostRepository;
    private final InputSanitizer inputSanitizer;

    @GetMapping
    public List<ForumPost> list(@RequestParam Long threadId) {
        return forumPostRepository.findByThreadId(threadId);
    }

    @PostMapping
    public ResponseEntity<ForumPost> create(@RequestBody ForumPostRequest request, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        ForumPost post = ForumPost.builder()
            .threadId(request.getThreadId())
            .userId(currentUser.getId())
            .content(inputSanitizer.sanitize(request.getContent()))
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .isDeleted(false)
            .build();
        return ResponseEntity.ok(forumPostRepository.save(post));
    }

    @PatchMapping("/{id}/delete")
    public ResponseEntity<ForumPost> softDelete(@PathVariable Long id, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        return forumPostRepository.findById(id)
            .filter(post -> post.getUserId().equals(currentUser.getId()) || currentUser.getRole().getName().equals("ROLE_ADMIN"))
            .map(post -> {
                post.setIsDeleted(true);
                post.setContent("[deleted]");
                post.setUpdatedAt(OffsetDateTime.now());
                return ResponseEntity.ok(forumPostRepository.save(post));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        forumPostRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class ForumPostRequest {
        private Long threadId;
        private String content;
    }
}