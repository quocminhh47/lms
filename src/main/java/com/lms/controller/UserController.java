package com.lms.controller;

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

import com.lms.dto.common.PageResponse;
import com.lms.dto.user.CreateUserRequest;
import com.lms.dto.user.UpdateUserRequest;
import com.lms.dto.user.UserResponse;
import com.lms.entity.UserRole;
import com.lms.service.UserService;
import com.lms.util.PagingSupport;

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
	public PageResponse<UserResponse> list(
			@RequestParam(required = false) String email,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) UserRole role,
			@RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort) {
		if (email != null && !email.isBlank()) {
			UserResponse user = userService.getByEmail(email);
			return new PageResponse<>(java.util.List.of(user), 0, 1, 1, 1, true, true);
		}
		return userService.listPage(q, role, active, page, size, sort);
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
