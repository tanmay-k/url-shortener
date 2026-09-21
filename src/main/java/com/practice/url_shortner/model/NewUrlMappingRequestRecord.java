package com.practice.url_shortner.model;

import com.practice.url_shortner.validator.SafeUrl;

public record NewUrlMappingRequestRecord(@SafeUrl String longUrl) {
}
