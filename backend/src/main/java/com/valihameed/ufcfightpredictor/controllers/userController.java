package com.valihameed.ufcfightpredictor.controllers;

import com.valihameed.ufcfightpredictor.models.Event;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.models.Leaderboard;
import com.valihameed.ufcfightpredictor.models.UserPrediction;
import com.valihameed.ufcfightpredictor.repository.EventRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.LeaderboardRepository;
import com.valihameed.ufcfightpredictor.repository.UserPredictionRepository;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.users.userService;
import com.valihameed.ufcfightpredictor.security.JwtService;
import com.valihameed.ufcfightpredictor.util.InputSanitizer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class userController {
	private final userRepository userRepository;
	private final InputSanitizer inputSanitizer;
	private final LeaderboardRepository leaderboardRepository;
	private final UserPredictionRepository userPredictionRepository;
	private final FightRepository fightRepository;
	private final EventRepository eventRepository;
	private final com.valihameed.ufcfightpredictor.repository.PredictionResultRepository predictionResultRepository;
	private final userService userService;
	private final JwtService jwtService;
	private final com.valihameed.ufcfightpredictor.repository.UserBadgeRepository userBadgeRepository;

	@GetMapping("/me")
	public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof user)) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
		}
		user currentUser = (user) authentication.getPrincipal();
		return ResponseEntity.ok(buildFullProfileResponse(currentUser));
	}

	@GetMapping("/{username}")
	public ResponseEntity<UserProfileResponse> byUsername(@PathVariable String username, Authentication authentication) {
		Optional<user> found = userRepository.findByUsername(username);
		if (found.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		user target = found.get();
		user currentUser = authentication != null && authentication.getPrincipal() instanceof user authenticatedUser
			? authenticatedUser
			: null;
			
		if (currentUser != null && currentUser.getId().equals(target.getId())) {
			return ResponseEntity.ok(buildFullProfileResponse(target));
		}
		
		if (target.isPublicProfile()) {
		    return ResponseEntity.ok(buildFullProfileResponse(target));
		}

		return ResponseEntity.ok(UserProfileResponse.publicView(target));
	}

	@PutMapping("/me")
	public ResponseEntity<UserProfileResponse> updateMe(@RequestBody UpdateProfileRequest request, Authentication authentication) {
		user currentUser = (user) authentication.getPrincipal();
		if (request.firstName != null) {
			currentUser.setFirstName(inputSanitizer.sanitize(request.firstName));
		}
		if (request.lastName != null) {
			currentUser.setLastName(inputSanitizer.sanitize(request.lastName));
		}
		if (request.profileImageUrl != null) {
			currentUser.setProfileImageUrl(inputSanitizer.sanitize(request.profileImageUrl));
		}
		if (request.publicProfile != null) {
		    currentUser.setPublicProfile(request.publicProfile);
		}
		if (request.optOutEmailNotifications != null) {
		    currentUser.setOptOutEmailNotifications(request.optOutEmailNotifications);
		}
		if (request.cosmeticTitle != null) {
		    if (!request.cosmeticTitle.isEmpty()) {
		        List<com.valihameed.ufcfightpredictor.models.UserBadge> badges = userBadgeRepository.findByUserId(currentUser.getId());
		        boolean valid = false;
		        if (request.cosmeticTitle.contains("Event Winner")) {
		            long eventWins = badges.stream().filter(b -> "EVENT_WINNER".equals(b.getBadgeType())).count();
		            if (request.cosmeticTitle.equals(eventWins + "x Event Winner")) {
		                valid = true;
		            }
		        } else {
		            valid = badges.stream().anyMatch(b -> b.getBadgeLabel() != null && b.getBadgeLabel().equals(request.cosmeticTitle));
		        }
		        if (valid) {
		            currentUser.setCosmeticTitle(request.cosmeticTitle);
		        } else {
		            return ResponseEntity.badRequest().build();
		        }
		    } else {
		        currentUser.setCosmeticTitle(null);
		    }
		}
		userRepository.save(currentUser);
		return ResponseEntity.ok(buildFullProfileResponse(currentUser));
	}

	@GetMapping("/me/available-titles")
	public ResponseEntity<List<AvailableTitleDto>> getAvailableTitles(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof user)) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
		}
		user currentUser = (user) authentication.getPrincipal();
		
		List<com.valihameed.ufcfightpredictor.models.UserBadge> badges = userBadgeRepository.findByUserId(currentUser.getId());
		List<AvailableTitleDto> titles = new java.util.ArrayList<>();
		
		long eventWins = badges.stream().filter(b -> "EVENT_WINNER".equals(b.getBadgeType())).count();
		if (eventWins > 0) {
		    AvailableTitleDto dto = new AvailableTitleDto();
		    dto.setId(eventWins + "x Event Winner");
		    dto.setLabel(eventWins + "x Event Winner");
		    dto.setType("EVENT_WINNER");
		    titles.add(dto);
		}
		
		badges.stream()
		    .filter(b -> b.getBadgeType().startsWith("SEASON_"))
		    .forEach(b -> {
		        AvailableTitleDto dto = new AvailableTitleDto();
		        dto.setId(b.getBadgeLabel());
		        dto.setLabel(b.getBadgeLabel());
		        dto.setType(b.getBadgeType());
		        titles.add(dto);
		    });
		    
		return ResponseEntity.ok(titles);
	}

	@PutMapping("/me/username")
	public ResponseEntity<UsernameChangeResponse> changeUsername(@RequestBody ChangeUsernameRequest request, Authentication authentication) {
		user currentUser = (user) authentication.getPrincipal();
		
		String newUsername = inputSanitizer.sanitize(request.getNewUsername()).trim();
		if (newUsername.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		userService.changeUsername(currentUser, newUsername);
		
		// Generate new token because subject (username) changed
		String newToken = jwtService.generateToken(currentUser.getUsername(), currentUser.getTokenVersion());
		
		UsernameChangeResponse response = new UsernameChangeResponse();
		response.setToken(newToken);
		response.setProfile(buildFullProfileResponse(currentUser));
		
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/me")
	public ResponseEntity<?> deleteMe(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof user)) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
		}
		user currentUser = (user) authentication.getPrincipal();
		
		currentUser.setUsername("deleted_user_" + currentUser.getId());
		currentUser.setEmail("deleted_" + currentUser.getId() + "@deleted.com");
		currentUser.setPassword("");
		currentUser.setFirstName("Deleted");
		currentUser.setLastName("User");
		currentUser.setLocked(true);
		currentUser.setEnabled(false);
		currentUser.setProfileImageUrl(null);
		currentUser.setPublicProfile(false);
		currentUser.setOptOutEmailNotifications(true);
		userRepository.save(currentUser);
		return ResponseEntity.ok(java.util.Map.of("message", "User deleted successfully"));
	}
	
	private UserProfileResponse buildFullProfileResponse(user target) {
	    UserProfileResponse response = UserProfileResponse.from(target);
	    
	    LeaderboardStatsDto stats = new LeaderboardStatsDto();
	    Optional<Leaderboard> lbOpt = leaderboardRepository.findByUserId(target.getId());
	    if (lbOpt.isPresent()) {
	        Leaderboard lb = lbOpt.get();
	        stats.setTotalPoints(lb.getTotalPoints() != null ? lb.getTotalPoints() : 0);
	        
	        int total = lb.getTotalPredictions() != null ? lb.getTotalPredictions() : 0;
	        int correct = lb.getCorrectPredictions() != null ? lb.getCorrectPredictions() : 0;
	        if (total > 0) {
	            stats.setWinRate((double) correct / total);
	        } else {
	            stats.setWinRate(0.0);
	        }
	    } else {
	        stats.setTotalPoints(0);
	        stats.setWinRate(0.0);
	    }

	    if (target.isPublicProfile() && stats.getTotalPoints() > 0) {
	        long usersAhead = leaderboardRepository.countUsersWithMorePoints(stats.getTotalPoints());
	        stats.setRank((int) usersAhead + 1);
	    }

	    response.setLeaderboardStats(stats);
	    
	    List<UserPrediction> predictions = userPredictionRepository.findByUserId(target.getId());
	    List<PredictionHistoryDto> history = predictions.stream().map(p -> {
	        PredictionHistoryDto dto = new PredictionHistoryDto();
	        dto.setFightId(p.getFightId());
	        dto.setPredictedWinner(p.getPredictedWinner());
	        dto.setPredictedMethod(p.getPredictedMethod());
	        dto.setPredictedRound(p.getPredictedRound());
	        dto.setSubmittedAt(p.getSubmittedAt());
	        dto.setLocked(p.getLocked());
	        
	        List<com.valihameed.ufcfightpredictor.models.PredictionResult> results = predictionResultRepository.findByUserPredictionId(p.getId());
	        if (!results.isEmpty()) {
	            dto.setPointsAwarded(results.get(0).getPointsAwarded());
	            dto.setIsWinnerCorrect(results.get(0).getIsWinnerCorrect());
	        } else {
	            dto.setPointsAwarded(0);
	            dto.setIsWinnerCorrect(false);
	        }
	        
	        fightRepository.findById(p.getFightId()).ifPresent(fight -> {
	            dto.setFighter1Name(fight.getFighter1Name());
	            dto.setFighter2Name(fight.getFighter2Name());
	            dto.setEventId(fight.getEventId());
	            dto.setResultWinner(fight.getResultWinner());
	            dto.setResultMethod(fight.getResultMethod());
	            dto.setResultRound(fight.getResultRound());
	            if (fight.getEventId() != null) {
	                eventRepository.findById(fight.getEventId()).ifPresent(event -> {
	                    dto.setEventName(event.getName());
	                });
	            }
	        });
	        return dto;
	    }).collect(Collectors.toList());
	    
	    response.setPredictionHistory(history);

	    // Populate badges
	    List<com.valihameed.ufcfightpredictor.models.UserBadge> badges = userBadgeRepository.findByUserId(target.getId());
	    response.setBadges(badges.stream().map(b -> {
	        BadgeDto bd = new BadgeDto();
	        bd.setId(b.getId());
	        bd.setBadgeType(b.getBadgeType());
	        bd.setBadgeLabel(b.getBadgeLabel());
	        bd.setAwardedAt(b.getAwardedAt());
	        return bd;
	    }).collect(Collectors.toList()));
	    
	    return response;
	}

	@Data
	public static class UpdateProfileRequest {
		private String firstName;
		private String lastName;
		private String profileImageUrl;
		private Boolean publicProfile;
		private Boolean optOutEmailNotifications;
		private String cosmeticTitle;
	}

	@Data
	public static class ChangeUsernameRequest {
		private String newUsername;
	}

	@Data
	public static class UsernameChangeResponse {
		private String token;
		private UserProfileResponse profile;
	}

	@Data
	public static class AvailableTitleDto {
	    private String id;
	    private String label;
	    private String type;
	}

	@Data
	public static class LeaderboardStatsDto {
	    private Integer rank;
	    private Integer totalPoints;
	    private Double winRate;
	}

	@Data
	public static class PredictionHistoryDto {
	    private Long fightId;
	    private String fighter1Name;
	    private String fighter2Name;
	    private Long eventId;
	    private String eventName;
	    private String predictedWinner;
	    private String predictedMethod;
	    private Integer predictedRound;
	    private String resultWinner;
	    private String resultMethod;
	    private Integer resultRound;
	    private OffsetDateTime submittedAt;
	    private Boolean locked;
	    private Integer pointsAwarded;
	    private Boolean isWinnerCorrect;
	}

	@Data
	public static class BadgeDto {
	    private Long id;
	    private String badgeType;
	    private String badgeLabel;
	    private OffsetDateTime awardedAt;
	}

	@Data
	public static class UserProfileResponse {
		private Long id;
		private String username;
		private String firstName;
		private String lastName;
		private String profileImageUrl;
		private String role;
		private boolean enabled;
		private boolean publicProfile;
		private boolean optOutEmailNotifications;
		private OffsetDateTime updatedAt;
		private LeaderboardStatsDto leaderboardStats;
		private List<PredictionHistoryDto> predictionHistory;
		private String cosmeticGlowColor;
		private String cosmeticTitle;
		private List<BadgeDto> badges;

		public static UserProfileResponse from(user source) {
			UserProfileResponse response = new UserProfileResponse();
			response.id = source.getId();
			response.username = source.getUsername();
			response.firstName = source.getFirstName();
			response.lastName = source.getLastName();
			response.profileImageUrl = source.getProfileImageUrl();
			response.role = source.getRole() != null ? source.getRole().getName() : null;
			response.enabled = source.isEnabled();
			response.publicProfile = source.isPublicProfile();
			response.optOutEmailNotifications = source.isOptOutEmailNotifications();
			response.cosmeticGlowColor = source.getCosmeticGlowColor();
			response.cosmeticTitle = source.getCosmeticTitle();
			return response;
		}

		public static UserProfileResponse publicView(user source) {
			UserProfileResponse response = from(source);
			response.firstName = null;
			response.lastName = null;
			response.profileImageUrl = null;
			response.leaderboardStats = null;
			response.predictionHistory = null;
			return response;
		}
	}
}
