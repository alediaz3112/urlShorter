package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlLogRepository extends JpaRepository<UrlLog, Long> {
}
