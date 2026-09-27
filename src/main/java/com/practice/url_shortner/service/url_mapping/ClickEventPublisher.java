package com.practice.url_shortner.service.url_mapping;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ClickEventPublisher{
    private final ApplicationEventPublisher publisher;

    public ClickEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishClickEvent(String userAgent, String shortCode) {
        ClickEvent clickEvent = new ClickEvent(userAgent, shortCode);
        publisher.publishEvent(clickEvent);
    }
}