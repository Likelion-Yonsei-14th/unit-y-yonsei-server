package com.likelion.yonsei.daedongje.domain.info.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SatisfactionReviewCreateRequest(
        @NotNull(message = "Satisfaction rating is required.")
        @Min(value = 1, message = "Satisfaction rating must be at least 1.")
        @Max(value = 5, message = "Satisfaction rating must be at most 5.")
        Integer rating,

        @Size(max = 1000, message = "Review content must be 1000 characters or less.")
        String content
) {
}
