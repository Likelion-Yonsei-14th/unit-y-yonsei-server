package com.likelion.yonsei.daedongje.domain.image.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Properties {

    private String bucket;
    private String region;
    private String imageBaseUrl;
    private long presignedUrlExpirationMinutes;

    public Duration getPresignedUrlExpiration() {
        return Duration.ofMinutes(presignedUrlExpirationMinutes);
    }

    public String getNormalizedImageBaseUrl() {
        if (imageBaseUrl == null || imageBaseUrl.isBlank()) {
            return "https://" + bucket + ".s3." + region + ".amazonaws.com";
        }

        if (imageBaseUrl.endsWith("/")) {
            return imageBaseUrl.substring(0, imageBaseUrl.length() - 1);
        }

        return imageBaseUrl;
    }
}