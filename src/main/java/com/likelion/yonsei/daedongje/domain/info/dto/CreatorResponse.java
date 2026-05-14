package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.yonsei.daedongje.domain.info.entity.Creator;

public record CreatorResponse(
        Long id,
        @JsonProperty("part_name")
        String partName,
        @JsonProperty("department_name")
        String departmentName,
        @JsonProperty("student_id")
        String studentId,
        String name,
        @JsonProperty("display_order")
        Integer displayOrder
) {
    public static CreatorResponse from(Creator creator) {
        return new CreatorResponse(
                creator.getId(),
                creator.getPartName(),
                creator.getDepartmentName(),
                creator.getStudentId(),
                creator.getName(),
                creator.getDisplayOrder()
        );
    }
}
