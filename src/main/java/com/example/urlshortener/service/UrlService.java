package com.example.urlshortener.service;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.model.UrlLog;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.repository.UrlLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UrlService {

    @Autowired
    private UrlRepository repository;

    @Autowired
    private UrlLogRepository logRepository;

    @Autowired
    private RedisTemplate<String, Url> redisTemplate;

    @Value("${url.cache.key}")
    private String cacheKey; // Llave principal para almacenar URLs en el cache.

    private static final String CACHE_KEY = "pendingUrls";

    public Url createShortUrl(String originalUrl, String customAlias) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setShortUrl(customAlias != null ? customAlias : generateShortAlias());
        url.setActive(true);

        try {
            Url savedUrl = repository.save(url);
            logAction(savedUrl.getId(), "CREATED_DIRECT");
            return savedUrl;
        } catch (Exception e) {
            // En caso de fallo, guardar en cache
            redisTemplate.opsForList().rightPush(CACHE_KEY, url);
            logAction(null, "CREATED_CACHED");
            return url;
        }
    }

    public Optional<Url> getOriginalUrl(String shortUrl) {
        return Optional.ofNullable(repository.findByShortUrl(shortUrl));
    }

    public void deleteUrl(Long id) {
        repository.deleteById(id);
        logAction(id, "DELETED");
    }

    @Scheduled(fixedRate = 5000)
    public void syncCacheToDatabase() {
        List<Url> pendingUrls = redisTemplate.opsForList().range(CACHE_KEY, 0, -1);
        if (pendingUrls != null && !pendingUrls.isEmpty()) {
            for (Url url : pendingUrls) {
                try {
                    Url savedUrl = repository.save(url);
                    redisTemplate.opsForList().remove(CACHE_KEY, 1, url);
                    logAction(savedUrl.getId(), "SAVED_FROM_CACHE");
                } catch (Exception e) {
                    logAction(null, "FAILED_TO_SAVE_FROM_CACHE");
                }
            }
        }
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

    public void cacheUrl(String originalUrl, String shortenedUrl) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setShortUrl(shortenedUrl);
        url.setActive(true);

        // Guardar la URL en el cache
        redisTemplate.opsForList().rightPush(cacheKey, url);

        // Registrar en el log que se almacenó en cache
        logAction(null, "CACHED - URL almacenada temporalmente: " + shortenedUrl);
    }

    public void flushCacheToDb() {
        while (redisTemplate.opsForList().size(cacheKey) > 0) {
            // Recuperar y guardar las URLs almacenadas en el cache
            Url cachedUrl = redisTemplate.opsForList().leftPop(cacheKey);
            if (cachedUrl != null) {
                try {
                    Url savedUrl = repository.save(cachedUrl);
                    logAction(savedUrl.getId(), "FLUSHED - URL guardada desde el cache: " + savedUrl.getShortUrl());
                } catch (Exception e) {
                    logAction(null, "ERROR - Fallo al guardar URL desde cache: " + cachedUrl.getShortUrl());
                }
            }
        }
    }
}