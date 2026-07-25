package com.valihameed.ufcfightpredictor.forum;

import com.valihameed.ufcfightpredictor.models.ForumThread;
import com.valihameed.ufcfightpredictor.models.ThreadSubscription;
import com.valihameed.ufcfightpredictor.repository.ForumThreadRepository;
import com.valihameed.ufcfightpredictor.repository.ThreadSubscriptionRepository;
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
@RequestMapping("/api/v1/forum/threads")
@AllArgsConstructor
public class ForumThreadController {
    private final ForumThreadRepository forumThreadRepository;
    private final ThreadSubscriptionRepository threadSubscriptionRepository;
    private final InputSanitizer inputSanitizer;

    @GetMapping
    public List<ForumThread> list(@RequestParam(required = false) Long eventId, @RequestParam(required = false) Long fightId) {
        if (fightId != null) {
            return forumThreadRepository.findAll().stream().filter(thread -> fightId.equals(thread.getFightId())).toList();
        }
        if (eventId != null) {
            return forumThreadRepository.findByEventId(eventId);
        }
        return forumThreadRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumThread> get(@PathVariable Long id) {
        return forumThreadRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ForumThreadRequest request, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        if (!currentUser.isEnabled()) {
            return ResponseEntity.status(403).body("You must verify your email address before creating a thread.");
        }
        ForumThread thread = ForumThread.builder()
            .eventId(request.getEventId())
            .fightId(request.getFightId())
            .createdBy(currentUser.getId())
            .title(inputSanitizer.sanitize(request.getTitle()))
            .createdAt(OffsetDateTime.now())
            .build();
        return ResponseEntity.ok(forumThreadRepository.save(thread));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        forumThreadRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/subscribe")
    public ResponseEntity<Boolean> toggleSubscribe(@PathVariable Long id, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        java.util.Optional<ThreadSubscription> existing = threadSubscriptionRepository.findByUserIdAndThreadId(currentUser.getId(), id);
        if (existing.isPresent()) {
            threadSubscriptionRepository.delete(existing.get());
            return ResponseEntity.ok(false);
        } else {
            ThreadSubscription sub = ThreadSubscription.builder()
                .userId(currentUser.getId())
                .threadId(id)
                .createdAt(OffsetDateTime.now())
                .build();
            threadSubscriptionRepository.save(sub);
            return ResponseEntity.ok(true);
        }
    }

    @GetMapping("/{id}/subscription-status")
    public ResponseEntity<Boolean> checkSubscription(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(false);
        }
        user currentUser = (user) authentication.getPrincipal();
        boolean isSubscribed = threadSubscriptionRepository.findByUserIdAndThreadId(currentUser.getId(), id).isPresent();
        return ResponseEntity.ok(isSubscribed);
    }

    @Data
    public static class ForumThreadRequest {
        private Long eventId;
        private Long fightId;
        private String title;
    }
}