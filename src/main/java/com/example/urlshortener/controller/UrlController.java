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
    public ResponseEntity<Url> addUrl(@RequestBody Url request, @RequestParam("method") char method) {
        String shortenedUrl;
        if (method == 'b') {
            shortenedUrl = bitlyService.shortenUrl(request.getOriginalUrl());
        } else if (method == 'g') {
            shortenedUrl = gooGlService.shortenUrl(request.getOriginalUrl());
        } else {
            return ResponseEntity.badRequest().body(null);
        }

        Url createdUrl = service.createShortUrl(request.getOriginalUrl(), shortenedUrl);
        return ResponseEntity.ok(createdUrl);
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
