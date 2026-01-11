package com.petruth.urlshortener;

import com.petruth.urlshortener.service.UrlSafetyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlSafetyServiceTest {

    private UrlSafetyService service;

    @BeforeEach
    void setUp() {
        service = new UrlSafetyService();
    }

    @Test
    void isSafeUrl_ShouldReturnTrue_ForValidHttpUrl() {
        assertTrue(service.isSafeUrl("http://example.com"));
        assertTrue(service.isSafeUrl("https://example.com"));
        assertTrue(service.isSafeUrl("https://www.google.com/search?q=test"));
    }

    @Test
    void isSafeUrl_ShouldReturnFalse_ForDangerousProtocols() {
        assertFalse(service.isSafeUrl("javascript:alert('xss')"));
        assertFalse(service.isSafeUrl("data:text/html,<script>alert('xss')</script>"));
        assertFalse(service.isSafeUrl("vbscript:msgbox('xss')"));
        assertFalse(service.isSafeUrl("file:///etc/passwd"));
    }

    @Test
    void isSafeUrl_ShouldReturnFalse_ForBlacklistedDomains() {
        assertFalse(service.isSafeUrl("https://bit.ly/abc123"));
        assertFalse(service.isSafeUrl("http://tinyurl.com/test"));
        assertFalse(service.isSafeUrl("https://goo.gl/maps"));
    }

    @Test
    void isSafeUrl_ShouldReturnFalse_ForLocalhost() {
        assertFalse(service.isSafeUrl("http://localhost:8080"));
        assertFalse(service.isSafeUrl("http://127.0.0.1:8080"));
    }

    @Test
    void isSafeUrl_ShouldReturnFalse_ForPrivateNetworks() {
        assertFalse(service.isSafeUrl("http://192.168.1.1"));
        assertFalse(service.isSafeUrl("http://10.0.0.1"));
        assertFalse(service.isSafeUrl("http://172.16.0.1"));
    }

    @Test
    void isSafeUrl_ShouldReturnFalse_ForIpAddresses() {
        assertFalse(service.isSafeUrl("http://8.8.8.8"));
        assertFalse(service.isSafeUrl("https://1.1.1.1"));
    }

    @Test
    void isSafeUrl_ShouldReturnFalse_ForInvalidUrls() {
        assertFalse(service.isSafeUrl("not-a-url"));
        assertFalse(service.isSafeUrl("htp://invalid"));
        assertFalse(service.isSafeUrl(""));
    }

    @Test
    void getSafetyMessage_ShouldReturnAppropriateMessage_ForDangerousProtocols() {
        String message = service.getSafetyMessage("javascript:alert('xss')");
        assertNotNull(message);
        assertTrue(message.contains("Dangerous URL protocol"));
    }

    @Test
    void getSafetyMessage_ShouldReturnAppropriateMessage_ForUrlShorteners() {
        String message = service.getSafetyMessage("https://bit.ly/test");
        assertNotNull(message);
        assertTrue(message.contains("URL shorteners cannot be shortened"));
    }

    @Test
    void getSafetyMessage_ShouldReturnAppropriateMessage_ForPrivateUrls() {
        String message = service.getSafetyMessage("http://localhost:8080");
        assertNotNull(message);
        assertTrue(message.contains("Internal/private URLs cannot be shortened"));
    }

    @Test
    void getSafetyMessage_ShouldReturnNull_ForSafeUrls() {
        String message = service.getSafetyMessage("https://example.com");
        assertNull(message);
    }

    @Test
    void isSafeUrl_ShouldHandleCaseSensitivity() {
        assertFalse(service.isSafeUrl("JAVASCRIPT:alert('xss')"));
        assertFalse(service.isSafeUrl("HTTP://BIT.LY/test"));
        assertFalse(service.isSafeUrl("http://LOCALHOST:8080"));
    }

    @Test
    void isSafeUrl_ShouldHandleUrlsWithPaths() {
        assertTrue(service.isSafeUrl("https://example.com/path/to/resource"));
        assertTrue(service.isSafeUrl("https://example.com/path?query=value"));
        assertTrue(service.isSafeUrl("https://example.com/path#fragment"));
    }

    @Test
    void isSafeUrl_ShouldRejectFtpProtocol() {
        assertFalse(service.isSafeUrl("ftp://example.com"));
    }
}