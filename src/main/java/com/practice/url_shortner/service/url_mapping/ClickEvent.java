package com.practice.url_shortner.service.url_mapping;

/**
 * ClickEvent
 */
public class ClickEvent {

    private final String userAgent;
    private final String shortCode;

    public ClickEvent(String userAgent, String shortCode) {
        this.userAgent = userAgent;
        this.shortCode = shortCode;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getShortUrl() {
        return shortCode;
    }
}
