package com.example.urlshortener.repository;

import com.example.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    List<Url> findByOriginalUrlContaining(String query);

    Url findByShortUrl(String shortUrl); // Declaración del método para buscar por shortUrl
}

