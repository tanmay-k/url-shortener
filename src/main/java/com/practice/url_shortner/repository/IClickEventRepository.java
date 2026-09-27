package com.practice.url_shortner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.practice.url_shortner.entity.ClickEventEntity;

@Repository
public interface IClickEventRepository extends JpaRepository<ClickEventEntity, Long> {
}
