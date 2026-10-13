package com.lms.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "certificates",
		uniqueConstraints = {
				@UniqueConstraint(name = "uq_certificates_code", columnNames = "certificate_code"),
				@UniqueConstraint(
						name = "uq_certificates_user_course",
						columnNames = { "user_id", "course_id" })
		})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certificate {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@Column(name = "certificate_code", nullable = false, unique = true)
	private String certificateCode;

	@Column(name = "pdf_url")
	private String pdfUrl;

	@Setter(AccessLevel.NONE)
	@Column(name = "issued_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime issuedAt;
}
