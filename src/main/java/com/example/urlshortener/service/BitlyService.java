package com.example.urlshortener.service;

import org.springframework.stereotype.Service;

@Service
public class BitlyService {
    public String shortenUrl(String originalUrl) {
        return "http://bit.ly/" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
