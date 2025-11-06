package com.swiftbeard.product_recommender_engine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebScrapingService
 */
class WebScrapingServiceTest {

    private WebScrapingService webScrapingService;

    @BeforeEach
    void setUp() {
        webScrapingService = new WebScrapingService();
    }

    @Test
    void testGetContentPreview_withLongContent() {
        // Given
        String longContent = "a".repeat(1000);

        // When
        String preview = webScrapingService.getContentPreview(longContent);

        // Then
        assertNotNull(preview);
        assertTrue(preview.length() <= 503); // 500 + "..."
        assertTrue(preview.endsWith("..."));
    }

    @Test
    void testGetContentPreview_withShortContent() {
        // Given
        String shortContent = "This is a short content";

        // When
        String preview = webScrapingService.getContentPreview(shortContent);

        // Then
        assertEquals(shortContent, preview);
        assertFalse(preview.endsWith("..."));
    }

    @Test
    void testGetContentPreview_withNullContent() {
        // When
        String preview = webScrapingService.getContentPreview(null);

        // Then
        assertEquals("", preview);
    }

    @Test
    void testGetContentPreview_withEmptyContent() {
        // When
        String preview = webScrapingService.getContentPreview("");

        // Then
        assertEquals("", preview);
    }

    @Test
    void testScrapeWebsite_withInvalidProtocol() {
        // Given
        String invalidUrl = "ftp://example.com";

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            webScrapingService.scrapeWebsite(invalidUrl);
        });
    }

    @Test
    void testScrapeWebsite_withMalformedUrl() {
        // Given
        String malformedUrl = "not-a-valid-url";

        // When/Then
        assertThrows(MalformedURLException.class, () -> {
            webScrapingService.scrapeWebsite(malformedUrl);
        });
    }

    // Note: Integration tests for actual web scraping would require a test server
    // or mocking the Jsoup connection, which is beyond the scope of basic unit tests
}
