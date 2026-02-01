package com.petruth.urlshortener.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class PingScheduler {

    private static final String TARGET_URL = "https://quicklink-uln2.onrender.com/actuator/health";

    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void pingApp() {
        try {
            URL targetUrl = new URL(TARGET_URL);
            HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                System.out.println("Ping successful: " + responseCode);
            } else {
                System.out.println("Ping failed: " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("Ping failed: " + e.getMessage());
        }
    }
}
