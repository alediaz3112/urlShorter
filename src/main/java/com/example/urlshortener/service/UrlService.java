package com.example.urlshortener.service;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.model.UrlLog;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.repository.UrlLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UrlService {

    @Autowired
    private UrlRepository repository;

    @Autowired
    private UrlLogRepository logRepository;

    public Url createShortUrl(String originalUrl, String customAlias) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setShortUrl(customAlias != null ? customAlias : generateShortAlias());
        url.setActive(true);
        Url savedUrl = repository.save(url);

        logAction(savedUrl.getId(), "CREATED");
        return savedUrl;
    }

    public Optional<Url> getOriginalUrl(String shortUrl) {
        return Optional.ofNullable(repository.findByShortUrl(shortUrl));
    }

    public void deleteUrl(Long id) {
        repository.deleteById(id);
        logAction(id, "DELETED");
    }

    private String generateShortAlias() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private void logAction(Long urlId, String action) {
        UrlLog log = new UrlLog();
        log.setUrlId(urlId);
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
    }

}
