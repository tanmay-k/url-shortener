package com.practice.url_shortner.service.url_mapping;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Async;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.practice.url_shortner.entity.ClickEventEntity;
import com.practice.url_shortner.repository.IClickEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventHandler {

	private final IClickEventRepository clickEventRepository;

	@Async("getAsyncExecutor")
	@EventListener
	public void handleClickEvent(ClickEvent clickEvent) {
		ClickEventEntity clickEventEntity = new ClickEventEntity();
		clickEventEntity.setShortCode(clickEvent.getShortCode());
		clickEventEntity.setUserAgent(clickEvent.getUserAgent());
		clickEventEntity.setClickedAt(LocalDateTime.now(ZoneId.of("UTC")));
		clickEventRepository.save(clickEventEntity);
		log.debug("Click event recorded for short code {}", clickEvent.getShortCode());
	}
}
