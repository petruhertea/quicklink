package com.petruth.urlshortener.service;

import com.petruth.urlshortener.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WelcomeEmailService {

    private final BrevoEmailService brevoEmailService;

    @Value("${APP_URL}")
    private String appUrl;

    public WelcomeEmailService(BrevoEmailService brevoEmailService) {
        this.brevoEmailService = brevoEmailService;
    }

    public void sendWelcomeEmail(User user) {
        brevoEmailService.send(
                user.getEmail(),
                user.getName(),
                "Welcome to QuickLink 🔗",
                buildEmailHtml(user)
        );
    }

    private String buildEmailHtml(User user) {
        String dashboardUrl    = appUrl + "/dashboard";
        String bulkUrl         = appUrl + "/bulk-shorten";
        String subscriptionUrl = appUrl + "/subscription";

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#f5f5f5;
                         font-family:system-ui,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="background:#f5f5f5;padding:40px 20px;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:12px;
                                overflow:hidden;
                                box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#667eea,#764ba2);
                                 padding:40px 32px;text-align:center;">
                        <h1 style="color:white;margin:0;font-size:28px;
                                   font-weight:800;letter-spacing:-0.5px;">
                          Quick<span style="opacity:.8">Link</span>
                        </h1>
                        <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;
                                  font-size:15px;">
                          Your links, shortened and tracked
                        </p>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:36px 32px;">
                        <p style="color:#333;font-size:17px;margin:0 0 8px;">
                          Hi %s 👋
                        </p>
                        <p style="color:#555;font-size:15px;
                                  line-height:1.6;margin:0 0 24px;">
                          Welcome to QuickLink! Your account is ready.
                          Here's a quick look at what you can do right now.
                        </p>

                        <!-- Feature cards -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="margin-bottom:24px;">
                          <tr>
                            <td style="padding:16px;background:#f8f9fa;
                                       border-radius:8px;
                                       border-left:4px solid #667eea;">
                              <p style="margin:0 0 4px;font-weight:700;
                                        color:#333;font-size:15px;">
                                🔗 Shorten any URL instantly
                              </p>
                              <p style="margin:0;color:#666;font-size:14px;
                                        line-height:1.5;">
                                Paste a long URL on the home page and get a
                                short link in seconds. All your links are saved
                                to your dashboard.
                              </p>
                            </td>
                          </tr>
                          <tr><td height="10"></td></tr>
                          <tr>
                            <td style="padding:16px;background:#f8f9fa;
                                       border-radius:8px;
                                       border-left:4px solid #667eea;">
                              <p style="margin:0 0 4px;font-weight:700;
                                        color:#333;font-size:15px;">
                                📊 Track every click
                              </p>
                              <p style="margin:0;color:#666;font-size:14px;
                                        line-height:1.5;">
                                Each link has its own analytics page showing
                                click history, devices, browsers, and referrers.
                              </p>
                            </td>
                          </tr>
                          <tr><td height="10"></td></tr>
                          <tr>
                            <td style="padding:16px;background:#f8f9fa;
                                       border-radius:8px;
                                       border-left:4px solid #667eea;">
                              <p style="margin:0 0 4px;font-weight:700;
                                        color:#333;font-size:15px;">
                                🔔 Expiry notifications
                              </p>
                              <p style="margin:0;color:#666;font-size:14px;
                                        line-height:1.5;">
                                Set an expiration date on any link and we'll
                                email you 3 days before it expires — with a
                                one-click option to extend by 30 days straight
                                from the email, no login needed.
                              </p>
                            </td>
                          </tr>
                        </table>

                        <!-- Premium callout -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="margin-bottom:28px;">
                          <tr>
                            <td style="padding:20px 24px;
                                       background:linear-gradient(135deg,
                                         rgba(102,126,234,0.08),
                                         rgba(118,75,162,0.08));
                                       border-radius:10px;
                                       border:1px solid rgba(102,126,234,0.2);">
                              <p style="margin:0 0 6px;font-weight:700;
                                        color:#333;font-size:15px;">
                                ⭐ Unlock more with Premium — $5/month
                              </p>
                              <p style="margin:0 0 14px;color:#555;
                                        font-size:14px;line-height:1.5;">
                                Custom short codes, advanced analytics,
                                bulk shortening, higher rate limits, and
                                an ad-free experience.
                              </p>
                              <a href="%s"
                                 style="display:inline-block;
                                        background:linear-gradient(135deg,
                                          #667eea,#764ba2);
                                        color:white;text-decoration:none;
                                        padding:10px 22px;border-radius:6px;
                                        font-weight:600;font-size:14px;">
                                See Premium features
                              </a>
                            </td>
                          </tr>
                        </table>

                        <!-- CTAs -->
                        <table cellpadding="0" cellspacing="0">
                          <tr>
                            <td style="background:linear-gradient(135deg,
                                         #667eea,#764ba2);
                                       border-radius:8px;padding:13px 26px;">
                              <a href="%s"
                                 style="color:white;text-decoration:none;
                                        font-weight:600;font-size:15px;">
                                Go to Dashboard
                              </a>
                            </td>
                            <td width="12"></td>
                            <td style="border:2px solid #667eea;
                                       border-radius:8px;padding:11px 26px;">
                              <a href="%s"
                                 style="color:#667eea;text-decoration:none;
                                        font-weight:600;font-size:15px;">
                                Shorten Multiple URLs
                              </a>
                            </td>
                          </tr>
                        </table>

                        <hr style="border:none;border-top:1px solid #eee;
                                   margin:32px 0 20px;">
                        <p style="color:#aaa;font-size:12px;margin:0;
                                  line-height:1.6;">
                          You're receiving this because you just created a
                          QuickLink account. If this wasn't you, you can
                          safely ignore this email.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(user.getName(), subscriptionUrl, dashboardUrl, bulkUrl);
    }
}