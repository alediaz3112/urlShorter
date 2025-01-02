package com.example.urlshortener.controller;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.service.BitlyService;
import com.example.urlshortener.service.GooGlService;
import com.example.urlshortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UrlController {

    @Autowired
    private UrlService service;

    @Autowired
    private BitlyService bitlyService;

    @Autowired
    private GooGlService gooGlService;

    @PostMapping("/shorten")
    public ResponseEntity<Url> shortenUrl(@RequestBody Url request) {
        Url createdUrl = service.createShortUrl(request.getOriginalUrl(), request.getShortUrl());
        return ResponseEntity.created(URI.create("/api/" + createdUrl.getShortUrl())).body(createdUrl);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addUrl(@RequestBody Url request, @RequestParam("method") char method) {
        if (request.getOriginalUrl() == null || request.getOriginalUrl().isEmpty()) {
            return ResponseEntity.badRequest().body("La URL original no puede estar vacía.");
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
            Url createdUrl = service.createShortUrl(request.getOriginalUrl(), shortenedUrl);
            return ResponseEntity.ok(createdUrl);
        } catch (Exception e) {
            // En caso de error, almacenar en cache
            service.cacheUrl(request.getOriginalUrl(), shortenedUrl);
            return ResponseEntity.status(202).body("La solicitud fue aceptada, pero almacenada en cache temporalmente.");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Url> deleteUrl(@PathVariable Long id) {
        Optional<Url> url = service.getOriginalUrl(service.getOriginalUrl(String.valueOf(id)).get().getShortUrl());
        if (url.isPresent()) {
            service.deleteUrl(id);
            return ResponseEntity.ok(url.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> resolveUrl(@PathVariable String shortUrl) {
        Optional<Url> url = service.getOriginalUrl(shortUrl);
        if (url.isPresent() && url.get().isActive()) {
            return ResponseEntity.status(302).location(URI.create(url.get().getOriginalUrl())).build();
        }
        return ResponseEntity.notFound().build();
    }
}
