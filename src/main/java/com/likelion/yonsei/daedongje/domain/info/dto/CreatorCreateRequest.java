package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatorCreateRequest(
        @JsonProperty("part_name")
        @Size(max = 100, message = "part_name은 100자를 넘을 수 없습니다.")
        String partName,

        @JsonProperty("department_name")
        @Size(max = 100, message = "department_name은 100자를 넘을 수 없습니다.")
        String departmentName,

        @JsonProperty("student_id")
        @Size(max = 20, message = "student_id는 20자를 넘을 수 없습니다.")
        String studentId,

        @Size(max = 50, message = "name은 50자를 넘을 수 없습니다.")
        String name,

        @JsonProperty("display_order")
        @NotNull(message = "display_order는 필수입니다.")
        @Min(value = 1, message = "display_order는 1 이상이어야 합니다.")
        Integer displayOrder
) {
}
