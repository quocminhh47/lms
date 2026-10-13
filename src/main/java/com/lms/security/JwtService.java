package com.lms.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.lms.configuration.JwtProperties;
import com.lms.entity.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private static final String CLAIM_ROLE = "role";

	private final JwtProperties jwtProperties;
	private final SecretKey secretKey;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.secretKey = Keys.hmacShaKeyFor(
				jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}

	public String createToken(UUID userId, String email, UserRole role) {
		Date now = new Date();
		Date expiresAt = new Date(now.getTime() + jwtProperties.getExpirationMs());

		return Jwts.builder()
				.subject(userId.toString())
				.claim("email", email)
				.claim(CLAIM_ROLE, role.name())
				.issuedAt(now)
				.expiration(expiresAt)
				.signWith(secretKey)
				.compact();
	}

	public long getExpirationMs() {
		return jwtProperties.getExpirationMs();
	}

	public UUID extractUserId(String token) {
		return UUID.fromString(parseClaims(token).getSubject());
	}

	public UserRole extractRole(String token) {
		String role = parseClaims(token).get(CLAIM_ROLE, String.class);
		return UserRole.valueOf(role);
	}

	public boolean isValid(String token) {
		try {
			parseClaims(token);
			return true;
		}
		catch (ExpiredJwtException ex) {
			return false;
		}
		catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
