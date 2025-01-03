package com.example.urlshortener.controller;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.service.BitlyService;
import com.example.urlshortener.service.GooGlService;
import com.example.urlshortener.service.UrlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @MockBean
    private BitlyService bitlyService;

    @MockBean
    private GooGlService gooGlService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testShortenUrl() throws Exception {
        Url mockUrl = new Url();
        mockUrl.setId(1L);
        mockUrl.setOriginalUrl("https://example.com");
        mockUrl.setShortUrl("short123");

        Mockito.when(urlService.createShortUrl(anyString(), anyString(), anyInt())).thenReturn(mockUrl);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("short123"));
    }

    @Test
    void testGetByOriginalUrl() throws Exception {
        Url mockUrl = new Url();
        mockUrl.setId(1L);
        mockUrl.setOriginalUrl("https://example.com");
        mockUrl.setShortUrl("short123");

        Mockito.when(urlService.findByOriginalUrlContaining("example")).thenReturn(Arrays.asList(mockUrl));

        mockMvc.perform(get("/api/getByUrl")
                        .param("query", "example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalUrl").value("https://example.com"));
    }

    @Test
    void testUpdateUrl() throws Exception {
        Url mockUrl = new Url();
        mockUrl.setId(1L);
        mockUrl.setOriginalUrl("https://example.com");
        mockUrl.setShortUrl("short123");

        Url updatedUrl = new Url();
        updatedUrl.setId(1L);
        updatedUrl.setOriginalUrl("https://newexample.com");
        updatedUrl.setShortUrl("short123");

        Mockito.when(urlService.getUrlById(1L)).thenReturn(Optional.of(mockUrl));
        Mockito.when(urlService.updateUrl(any(Url.class))).thenReturn(updatedUrl);

        mockMvc.perform(put("/api/update/1")
                        .param("newUrl", "https://newexample.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://newexample.com"));
    }

    @Test
    void testDeleteUrl() throws Exception {
        Url mockUrl = new Url();
        mockUrl.setId(1L);
        mockUrl.setOriginalUrl("https://example.com");
        mockUrl.setShortUrl("short123");

        Mockito.when(urlService.getOriginalUrl("short123")).thenReturn(Optional.of(mockUrl));
        Mockito.doNothing().when(urlService).deleteUrl(1L);

        mockMvc.perform(delete("/api/delete/1"))
                .andExpect(status().isOk());
    }
}
