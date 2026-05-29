package com.valihameed.ufcfightpredictor.controllers;

import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.util.InputSanitizer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class userController {
	private final userRepository userRepository;
	private final InputSanitizer inputSanitizer;

	@GetMapping("/me")
	public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
		user currentUser = (user) authentication.getPrincipal();
		return ResponseEntity.ok(UserProfileResponse.from(currentUser));
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
			return ResponseEntity.ok(UserProfileResponse.from(target));
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
		userRepository.save(currentUser);
		return ResponseEntity.ok(UserProfileResponse.from(currentUser));
	}

	@Data
	public static class UpdateProfileRequest {
		private String firstName;
		private String lastName;
		private String profileImageUrl;
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
		private OffsetDateTime updatedAt;

		public static UserProfileResponse from(user source) {
			UserProfileResponse response = new UserProfileResponse();
			response.id = source.getId();
			response.username = source.getUsername();
			response.firstName = source.getFirstName();
			response.lastName = source.getLastName();
			response.profileImageUrl = source.getProfileImageUrl();
			response.role = source.getRole() != null ? source.getRole().getName() : null;
			response.enabled = source.isEnabled();
			return response;
		}

		public static UserProfileResponse publicView(user source) {
			UserProfileResponse response = from(source);
			response.firstName = null;
			response.lastName = null;
			response.profileImageUrl = null;
			return response;
		}
	}
}
