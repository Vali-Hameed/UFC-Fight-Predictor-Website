package com.valihameed.ufcfightpredictor.forum;

import com.valihameed.ufcfightpredictor.models.ForumPost;
import com.valihameed.ufcfightpredictor.repository.ForumPostRepository;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.models.Notification;
import com.valihameed.ufcfightpredictor.models.ThreadSubscription;
import com.valihameed.ufcfightpredictor.repository.NotificationRepository;
import com.valihameed.ufcfightpredictor.repository.ThreadSubscriptionRepository;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.util.InputSanitizer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/forum/posts")
@AllArgsConstructor
public class ForumPostController {
    private final ForumPostRepository forumPostRepository;
    private final userRepository userRepository;
    private final ThreadSubscriptionRepository threadSubscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final InputSanitizer inputSanitizer;

    private ForumPost populateUsername(ForumPost post) {
        if (post.getUserId() != null) {
            userRepository.findById(post.getUserId()).ifPresentOrElse(
                u -> {
                    if (u.getUsername().startsWith("deleted_user_")) {
                        post.setUsername("deleted user");
                    } else {
                        post.setUsername(u.getUsername());
                    }
                },
                () -> post.setUsername("deleted user")
            );
        } else {
            post.setUsername("deleted user");
        }
        return post;
    }

    @GetMapping
    public List<ForumPost> list(@RequestParam Long threadId) {
        return forumPostRepository.findByThreadId(threadId).stream()
                .map(this::populateUsername)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ForumPostRequest request, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        
        if (currentUser.getBannedFromForumUntil() != null && currentUser.getBannedFromForumUntil().isAfter(OffsetDateTime.now())) {
            return ResponseEntity.status(403).body("You are banned from the forum until " + currentUser.getBannedFromForumUntil());
        }

        ForumPost post = ForumPost.builder()
            .threadId(request.getThreadId())
            .userId(currentUser.getId())
            .content(inputSanitizer.sanitize(request.getContent()))
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .isDeleted(false)
            .build();
            
        ForumPost savedPost = populateUsername(forumPostRepository.save(post));
        Set<Long> notifiedUsers = new HashSet<>();
        
        // 1. Mentions
        Pattern p = Pattern.compile("(?<=^|\\s)@([a-zA-Z0-9_]+)");
        Matcher m = p.matcher(request.getContent());
        while (m.find()) {
            String mentionedUsername = m.group(1);
            userRepository.findByUsername(mentionedUsername).ifPresent(mentionedUser -> {
                if (!mentionedUser.getId().equals(currentUser.getId()) && !notifiedUsers.contains(mentionedUser.getId())) {
                    Notification n = Notification.builder()
                        .userId(mentionedUser.getId())
                        .type("FORUM_MENTION")
                        .message("You were mentioned by " + currentUser.getUsername() + " in a forum thread.")
                        .link("/forum/" + request.getThreadId())
                        .read(false)
                        .createdAt(OffsetDateTime.now())
                        .build();
                    notificationRepository.save(n);
                    notifiedUsers.add(mentionedUser.getId());
                }
            });
        }

        // 2. Thread Subscriptions
        List<ThreadSubscription> subscriptions = threadSubscriptionRepository.findByThreadId(request.getThreadId());
        for (ThreadSubscription sub : subscriptions) {
            if (!sub.getUserId().equals(currentUser.getId()) && !notifiedUsers.contains(sub.getUserId())) {
                Notification n = Notification.builder()
                    .userId(sub.getUserId())
                    .type("FORUM_REPLY")
                    .message(currentUser.getUsername() + " replied to a thread you are subscribed to.")
                    .link("/forum/" + request.getThreadId())
                    .read(false)
                    .createdAt(OffsetDateTime.now())
                    .build();
                notificationRepository.save(n);
                notifiedUsers.add(sub.getUserId());
            }
        }
        
        return ResponseEntity.ok(savedPost);
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
                return ResponseEntity.ok(populateUsername(forumPostRepository.save(post)));
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