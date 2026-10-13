package com.lms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.user.CreateUserRequest;
import com.lms.dto.user.UpdateUserRequest;
import com.lms.dto.user.UserResponse;
import com.lms.service.UserService;

import jakarta.validation.Valid;

/**
 * User management. Public registration lives only at {@code POST /api/auth/register}.
 * No endpoint here changes a user's role except admin-only creation.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
		return userService.create(request);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
	public UserResponse getById(@PathVariable UUID id) {
		return userService.getById(id);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<UserResponse> list(@RequestParam(required = false) String email) {
		if (email != null && !email.isBlank()) {
			return List.of(userService.getByEmail(email));
		}
		return userService.list();
	}

	/**
	 * Profile update. Role is not part of the request. Only ADMIN may change
	 * {@code active}; users may update their own name and avatar.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or (#id == authentication.principal.id and #request.active() == null)")
	public UserResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateUserRequest request) {
		return userService.update(id, request);
	}
}
