package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.repository.ShortenedUrlRepository;
import com.petruth.urlshortener.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LinkExpiryNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(LinkExpiryNotificationService.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final ShortenedUrlRepository shortenedUrlRepository;
    private final UserRepository userRepository;
    private final ExtensionTokenService tokenService;
    private final BrevoEmailService brevoEmailService;

    @Value("${APP_URL}")
    private String appUrl;

    public LinkExpiryNotificationService(
            ShortenedUrlRepository shortenedUrlRepository,
            UserRepository userRepository,
            ExtensionTokenService tokenService,
            BrevoEmailService brevoEmailService) {
        this.shortenedUrlRepository = shortenedUrlRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.brevoEmailService = brevoEmailService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendExpiryNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(3);

        List<ShortenedUrl> expiring =
                shortenedUrlRepository.findLinksExpiringBefore(now, threshold);

        log.info("Expiry notification job: {} links to notify", expiring.size());

        for (ShortenedUrl url : expiring) {
            try {
                User user = url.getUser();

                String extensionToken = tokenService.generateToken(
                        url.getId(), user.getId());
                String extendUrl = appUrl + "/links/extend?token=" + extensionToken;
                String analyticsUrl = appUrl + "/analytics/" + url.getCode();

                brevoEmailService.send(
                        user.getEmail(),
                        user.getName(),
                        "Your link \"/" + url.getCode() + "\" expires in 3 days",
                        buildEmailHtml(
                                user.getName(),
                                url.getCode(),
                                url.getLongUrl(),
                                url.getExpiresAt().format(DATE_FMT),
                                extendUrl,
                                analyticsUrl
                        )
                );

                url.setExpiryNotificationSentAt(LocalDateTime.now());
                shortenedUrlRepository.save(url);

            } catch (Exception e) {
                log.error("Failed to send expiry notification for link {}: {}",
                        url.getCode(), e.getMessage());
            }
        }
    }

    private String buildEmailHtml(String name, String code, String longUrl,
                                  String expiryDate, String extendUrl,
                                  String analyticsUrl) {
        String displayUrl = longUrl.length() > 60
                ? longUrl.substring(0, 57) + "..."
                : longUrl;

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:system-ui,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="background:#f5f5f5;padding:40px 20px;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;
                                    overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                        <tr>
                          <td style="background:linear-gradient(135deg,#667eea,#764ba2);
                                     padding:32px;text-align:center;">
                            <h1 style="color:white;margin:0;font-size:24px;">
                              Quick<span style="opacity:.85">Link</span>
                            </h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px;">
                            <p style="color:#333;font-size:16px;">Hi %s,</p>
                            <p style="color:#333;font-size:16px;">
                              Your shortened link
                              <strong style="color:#667eea;">/%s</strong>
                              is expiring on <strong>%s</strong>.
                            </p>
                            <div style="background:#f8f9fa;border-radius:8px;
                                        padding:16px;margin:20px 0;
                                        border-left:4px solid #667eea;">
                              <p style="margin:0;font-size:13px;color:#666;">
                                Redirects to:
                              </p>
                              <p style="margin:4px 0 0;font-size:14px;
                                        color:#333;word-break:break-all;">
                                %s
                              </p>
                            </div>
                            <p style="color:#333;font-size:16px;">
                              Want to keep this link alive?
                            </p>
                            <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                              <tr>
                                <td style="background:linear-gradient(135deg,#667eea,#764ba2);
                                           border-radius:8px;padding:14px 28px;">
                                  <a href="%s" style="color:white;text-decoration:none;
                                                      font-weight:600;font-size:16px;">
                                    Extend by 30 days
                                  </a>
                                </td>
                                <td width="16"></td>
                                <td style="border:2px solid #667eea;border-radius:8px;
                                           padding:12px 28px;">
                                  <a href="%s" style="color:#667eea;text-decoration:none;
                                                      font-weight:600;font-size:16px;">
                                    View Analytics
                                  </a>
                                </td>
                              </tr>
                            </table>
                            <p style="color:#999;font-size:13px;">
                              The "Extend" button works for 7 days and doesn't require
                              signing in. You can also manage this link from your
                              <a href="%s/dashboard" style="color:#667eea;">dashboard</a>.
                            </p>
                            <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                            <p style="color:#999;font-size:12px;margin:0;">
                              You're receiving this because you enabled expiry notifications.
                              You can turn them off in your
                              <a href="%s/dashboard" style="color:#667eea;">
                                notification settings
                              </a>.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(name, code, expiryDate, displayUrl,
                extendUrl, analyticsUrl, appUrl, appUrl);
    }
}