package com.example.urlshortener.service;

import org.springframework.stereotype.Service;

@Service
public class GooGlService {
    public String shortenUrl(String originalUrl) {
        return "http://goo.gl/" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
