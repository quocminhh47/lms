package com.lms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lms.dto.auth.AuthResponse;
import com.lms.dto.auth.LoginRequest;
import com.lms.dto.auth.RegisterRequest;
import com.lms.dto.user.UserResponse;
import com.lms.exception.UnauthorizedException;
import com.lms.security.LmsUserPrincipal;
import com.lms.service.AuthService;
import com.lms.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final UserService userService;

	public AuthController(AuthService authService, UserService userService) {
		this.authService = authService;
		this.userService = userService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	/**
	 * Current authenticated user. Identity comes from the security principal
	 * (JWT), not from a client-supplied id.
	 */
	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal LmsUserPrincipal principal) {
		if (principal == null) {
			throw new UnauthorizedException("Authentication required");
		}
		return userService.getById(principal.getId());
	}
}
