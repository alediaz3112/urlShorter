package com.example.urlshortener.controller;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.service.BitlyService;
import com.example.urlshortener.service.GooGlService;
import com.example.urlshortener.service.UrlService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.instrument.Counter;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UrlController {

    private final Counter shortenUrlCounter;
    private final Counter addUrlCounter;
    private final Counter deleteUrlCounter;
    private final Counter resolveUrlCounter;
    private final Counter getByOriginalUrlCounter;
    private final Counter updateUrlCounter;

    @Autowired
    public UrlController(MeterRegistry meterRegistry) {
        this.shortenUrlCounter = Counter.builder("url.shorten.requests")
                .description("Number of shortenUrl requests")
                .register(meterRegistry);
        this.addUrlCounter = Counter.builder("url.add.requests")
                .description("Number of addUrl requests")
                .register(meterRegistry);
        this.deleteUrlCounter = Counter.builder("url.delete.requests")
                .description("Number of deleteUrl requests")
                .register(meterRegistry);
        this.resolveUrlCounter = Counter.builder("url.resolve.requests")
                .description("Number of resolveUrl requests")
                .register(meterRegistry);
        this.getByOriginalUrlCounter = Counter.builder("url.getByOriginalUrl.requests")
                .description("Number of getByOriginalUrl requests")
                .register(meterRegistry);
        this.updateUrlCounter = Counter.builder("url.update.requests")
                .description("Number of updateUrl requests")
                .register(meterRegistry);
    }

    @Autowired
    private UrlService service;

    @Autowired
    private BitlyService bitlyService;

    @Autowired
    private GooGlService gooGlService;

    @PostMapping("/shorten")
    public ResponseEntity<Url> shortenUrl(@RequestBody Url request) {
        shortenUrlCounter.increment();
        Url createdUrl = service.createShortUrl(request.getOriginalUrl(), request.getShortUrl(), request.getRetentionDays());
        return ResponseEntity.created(URI.create("/api/" + createdUrl.getShortUrl())).body(createdUrl);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addUrl(@RequestBody Url request, @RequestParam("method") char method) {
        addUrlCounter.increment();
        if (request.getOriginalUrl() == null || request.getOriginalUrl().isEmpty()) {
            return ResponseEntity.badRequest().body("La URL original no puede estar vacía.");
        }

        if (request.getRetentionDays() < 0) {
            return ResponseEntity.badRequest().body("El tiempo de permanencia debe ser 0 o un número positivo.");
        }

        String shortenedUrl;
        try {
            if (method == 'b') {
                shortenedUrl = bitlyService.shortenUrl(request.getOriginalUrl());
            } else if (method == 'g') {
                shortenedUrl = gooGlService.shortenUrl(request.getOriginalUrl());
            } else {
                return ResponseEntity.badRequest().body("Método no válido. Use 'b' para Bitly o 'g' para Goo.gl.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar la solicitud: " + e.getMessage());
        }

        // Intentar guardar en la base de datos
        try {
            Url createdUrl = service.createShortUrl(request.getOriginalUrl(), shortenedUrl, request.getRetentionDays());
            return ResponseEntity.ok(createdUrl);
        } catch (Exception e) {
            // En caso de error, almacenar en cache
            service.cacheUrl(request.getOriginalUrl(), shortenedUrl);
            return ResponseEntity.status(202).body("La solicitud fue aceptada, pero almacenada en cache temporalmente.");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Url> deleteUrl(@PathVariable Long id) {
        deleteUrlCounter.increment();
        Optional<Url> url = service.getOriginalUrl(service.getOriginalUrl(String.valueOf(id)).get().getShortUrl());
        if (url.isPresent()) {
            service.deleteUrl(id);
            return ResponseEntity.ok(url.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> resolveUrl(@PathVariable String shortUrl) {
        resolveUrlCounter.increment();
        Optional<Url> url = service.getOriginalUrl(shortUrl);
        if (url.isPresent() && url.get().isActive()) {
            return ResponseEntity.status(302).location(URI.create(url.get().getOriginalUrl())).build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/getByUrl")
    public ResponseEntity<?> getByOriginalUrl(@RequestParam("query") String query) {
        getByOriginalUrlCounter.increment();
        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body("El parámetro 'query' no puede estar vacío.");
        }
        List<Url> results = service.findByOriginalUrlContaining(query);
        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(results);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUrl(@PathVariable Long id, @RequestParam("newUrl") String newUrl) {
        updateUrlCounter.increment();
        if (newUrl == null || newUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("La nueva URL no puede estar vacía.");
        }

        Optional<Url> optionalUrl = service.getUrlById(id);
        if (optionalUrl.isPresent()) {
            Url url = optionalUrl.get();
            url.setOriginalUrl(newUrl);
            Url updatedUrl = service.updateUrl(url);
            return ResponseEntity.ok(updatedUrl);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


}