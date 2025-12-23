package com.petruth.urlshortener.service;

import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Set;

@Service
public class UrlSafetyService {

    private static final Set<String> BLACKLISTED_DOMAINS = Set.of(
            "bit.ly",  // Prevent nested URL shorteners
            "tinyurl.com",
            "goo.gl",
            "ow.ly"
            // Add known malicious domains here
    );

    private static final Set<String> DANGEROUS_PROTOCOLS = Set.of(
            "javascript:",
            "data:",
            "vbscript:",
            "file:"
    );

    public boolean isSafeUrl(String urlString) {
        try {
            // Check for dangerous protocols
            String lowerUrl = urlString.toLowerCase().trim();
            for (String protocol : DANGEROUS_PROTOCOLS) {
                if (lowerUrl.startsWith(protocol)) {
                    return false;
                }
            }

            // Parse URL
            URL url = new URL(urlString);
            String protocol = url.getProtocol().toLowerCase();
            String host = url.getHost().toLowerCase();

            // Only allow HTTP and HTTPS
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return false;
            }

            // Check blacklist
            for (String blocked : BLACKLISTED_DOMAINS) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) {
                    return false;
                }
            }

            // Prevent localhost/internal network URLs
            if (host.equals("localhost") ||
                    host.equals("127.0.0.1") ||
                    host.startsWith("192.168.") ||
                    host.startsWith("10.") ||
                    host.startsWith("172.16.") ||
                    host.startsWith("172.17.") ||
                    host.startsWith("172.18.") ||
                    host.startsWith("172.19.") ||
                    host.startsWith("172.20.") ||
                    host.startsWith("172.21.") ||
                    host.startsWith("172.22.") ||
                    host.startsWith("172.23.") ||
                    host.startsWith("172.24.") ||
                    host.startsWith("172.25.") ||
                    host.startsWith("172.26.") ||
                    host.startsWith("172.27.") ||
                    host.startsWith("172.28.") ||
                    host.startsWith("172.29.") ||
                    host.startsWith("172.30.") ||
                    host.startsWith("172.31.")) {
                return false;
            }

            // Check for IP addresses (optional - prevents shortening IPs)
            if (host.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public String getSafetyMessage(String url) {
        if (!isSafeUrl(url)) {
            if (url.toLowerCase().startsWith("javascript:") ||
                    url.toLowerCase().startsWith("data:")) {
                return "Dangerous URL protocol detected";
            }

            try {
                String host = new URL(url).getHost().toLowerCase();
                for (String blocked : BLACKLISTED_DOMAINS) {
                    if (host.contains(blocked)) {
                        return "URL shorteners cannot be shortened";
                    }
                }

                if (host.equals("localhost") || host.startsWith("127.") ||
                        host.startsWith("192.168.") || host.startsWith("10.")) {
                    return "Internal/private URLs cannot be shortened";
                }
            } catch (Exception e) {
                // Fall through
            }

            return "This URL cannot be shortened for security reasons";
        }
        return null;
    }
}