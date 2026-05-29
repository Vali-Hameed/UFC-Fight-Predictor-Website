package com.valihameed.ufcfightpredictor.community;

import com.valihameed.ufcfightpredictor.models.CommunityVote;
import com.valihameed.ufcfightpredictor.repository.CommunityVoteRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/community-votes")
@AllArgsConstructor
public class CommunityVoteController {
    private final CommunityVoteRepository communityVoteRepository;

    @GetMapping("/{fightId}")
    public ResponseEntity<CommunityVote> get(@PathVariable Long fightId) {
        return communityVoteRepository.findByFightId(fightId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{fightId}")
    public ResponseEntity<CommunityVote> upsert(@PathVariable Long fightId, @RequestBody CommunityVoteRequest request) {
        Optional<CommunityVote> existing = communityVoteRepository.findByFightId(fightId);
        CommunityVote vote = existing.orElseGet(() -> CommunityVote.builder().fightId(fightId).build());
        vote.setFighter1Votes(request.getFighter1Votes());
        vote.setFighter2Votes(request.getFighter2Votes());
        vote.setLastUpdated(OffsetDateTime.now());
        return ResponseEntity.ok(communityVoteRepository.save(vote));
    }

    @Data
    public static class CommunityVoteRequest {
        private Integer fighter1Votes;
        private Integer fighter2Votes;
    }
}