package com.lms.mapper;

import org.springframework.stereotype.Component;

import com.lms.dto.section.SectionResponse;
import com.lms.entity.Section;

@Component
public class SectionMapper {

	public SectionResponse toResponse(Section section) {
		return new SectionResponse(
				section.getId(),
				section.getCourse().getId(),
				section.getTitle(),
				section.getSortOrder());
	}
}
