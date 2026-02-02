# QuickLink URL Shortener

A modern, production-ready URL shortening service built with Spring Boot, featuring OAuth authentication, premium subscriptions, and comprehensive analytics.

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com)

## ✨ Features

### Core Functionality
- **URL Shortening** – Generate short, memorable links from long URLs
- **Custom Short Codes** – Premium users can choose their own codes
- **Click Tracking** – Real-time analytics for every shortened link
- **QR Code Generation** – Instant QR codes for all links
- **Bulk Operations** – Shorten multiple URLs at once (premium)

### Authentication & Authorization
- **OAuth 2.0 / OpenID Connect** – Login with GitHub, Google, or Microsoft
- **Session Management** – Secure, persistent sessions across devices
- **Role-Based Access** – Free and premium tier controls

### Premium Features ($5/month)
- ✅ **No Ads** – Clean, distraction-free experience
- ✅ **Custom Short Codes** – Choose your own memorable URLs
- ✅ **Advanced Analytics** – Device, browser, and referrer tracking
- ✅ **Higher Limits** – 500 links per hour (vs 50 for free)
- ✅ **Priority Support** – Get help when you need it

### Analytics Dashboard
- **Click Metrics** – Total, daily, and weekly click counts
- **Time-Series Charts** – Visual click history over 7/30/90 days
- **Device Breakdown** – Desktop vs mobile vs tablet (premium)
- **Browser Stats** – Chrome, Firefox, Safari, Edge distributions (premium)
- **Top Referrers** – Traffic sources ranked by clicks (premium)

### Monetization
- **Google AdSense Integration** – Ad-supported free tier
- **Stripe Subscriptions** – Recurring premium payments
- **Server-Side Ad Suppression** – Premium users never see ads

---

## 🚀 Tech Stack

| Layer | Technology                                                  |
|-------|-------------------------------------------------------------|
| **Backend** | Spring Boot 4.x (Java 17), Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 16 (Supabase)                   |
| **Migrations** | Flyway                                                      |
| **Authentication** | OAuth2 (GitHub, Google, Microsoft)                          |
| **Payments** | Stripe Checkout & Subscriptions                             |
| **Ads** | Google AdSense                                              |
| **Frontend** | Thymeleaf, Bootstrap 5, Chart.js                            |
| **Deployment** | Docker, Render.com                                          |
| **DevOps** | Docker Compose, Render IaC (render.yaml)                    |

---

## 📦 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose (for local dev)
- Node.js 18+ (optional, for frontend tooling)

### 1. Clone & Configure

```bash
git clone https://github.com/petruhertea/quicklink.git
cd quicklink
cp .env.example .env
```

Edit `.env` and fill in:
- OAuth credentials (GitHub, Google, Microsoft)
- Stripe keys (secret key, webhook secret, price ID)
- Database password (for local Docker Compose only)

### 2. Run Locally with Docker Compose

```bash
docker compose up -d
```

The app will be available at **http://localhost:8080**

**Included services:**
- `app` – Spring Boot on port 8080
- `postgres` – PostgreSQL 16 on port 5432
- `pgadmin` – Database GUI at http://localhost:5050 (login: `admin@admin.com` / password from `.env`)

### 3. Access the Application

- **Home:** http://localhost:8080
- **Login:** http://localhost:8080/login (OAuth providers)
- **Dashboard:** http://localhost:8080/dashboard (after login)
- **Subscription:** http://localhost:8080/subscription

---

## 🌐 Deployment to Render

This repository includes a complete Infrastructure-as-Code setup for Render.

### Prerequisites
1. **Render Account** – Sign up at [render.com](https://render.com)
2. **Supabase Project** (recommended) or use Render's managed PostgreSQL
3. **OAuth Apps** – Create apps in GitHub, Google Cloud Console, Azure Portal
4. **Stripe Account** – Set up a product and get API keys
5. **Google AdSense** – Optional; skip if running ad-free

### Deploy Steps

1. **Fork this repository** to your GitHub account

2. **Create a New Web Service** in Render:
   - Connect your GitHub repo
   - Select **Docker** as runtime
   - Use `render.yaml` blueprint (auto-detected)

3. **Set Environment Variables** in Render dashboard:

   | Variable | Where to Get It |
   |----------|----------------|
   | `GITHUB_CLIENT_ID` | GitHub → Settings → Developer settings → OAuth Apps |
   | `GITHUB_CLIENT_SECRET` | Same as above |
   | `GOOGLE_CLIENT_ID` | Google Cloud Console → APIs & Services → Credentials |
   | `GOOGLE_CLIENT_SECRET` | Same as above |
   | `MICROSOFT_CLIENT_ID` | Azure Portal → App Registrations → Overview |
   | `MICROSOFT_CLIENT_SECRET` | Azure Portal → Certificates & secrets |
   | `STRIPE_SECRET_KEY` | Stripe Dashboard → Developers → API Keys |
   | `STRIPE_WEBHOOK_SECRET` | Stripe → Webhooks → Create endpoint for `/payment/webhook` |
   | `STRIPE_PRICE_ID` | Stripe → Products → Your $5/mo price → API ID |

4. **OAuth Redirect URLs** – Add these to each provider:
   - GitHub: `https://your-app.onrender.com/login/oauth2/code/github`
   - Google: `https://your-app.onrender.com/login/oauth2/code/google`
   - Microsoft: `https://your-app.onrender.com/login/oauth2/code/microsoft`

5. **Stripe Webhook** – Point to `https://your-app.onrender.com/payment/webhook`
   - Events: `checkout.session.completed`, `customer.subscription.created`, `customer.subscription.deleted`

6. **Deploy** – Render will build and deploy automatically

See **[DEPLOYMENT.md](./DEPLOYMENT.md)** for detailed instructions.

---

## 🔧 Local Development (without Docker)

### 1. Install Dependencies
- PostgreSQL 16+
- Java 17+
- Maven 3.8+

### 2. Create Database

```sql
CREATE DATABASE quicklink;
CREATE USER quicklink_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE quicklink TO quicklink_user;
```

### 3. Configure Application

Create `src/main/resources/env.properties`:

```properties
DB_URL=jdbc:postgresql://localhost:5432/quicklink
DB_USERNAME=quicklink_user
DB_PASSWORD=your_password
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
# ... etc (see .env.example)
```

### 4. Run

```bash
./mvnw spring-boot:run
```

---

## 📂 Project Structure

```
quicklink/
├── src/main/
│   ├── java/com/petruth/urlshortener/
│   │   ├── config/              # Security, WebMvc, Stripe
│   │   ├── controller/          # REST & page controllers
│   │   ├── model/               # JPA entities
│   │   ├── repository/          # Spring Data JPA
│   │   ├── service/             # Business logic
│   │   └── interceptor/         # Ad suppression for premium
│   └── resources/
│       ├── db/migration/        # Flyway SQL migrations
│       ├── templates/           # Thymeleaf HTML
│       ├── static/              # CSS, JS, ads.txt
│       └── application.properties
├── Dockerfile                   # Multi-stage production build
├── docker-compose.yml           # Local dev stack
├── render.yaml                  # Render IaC blueprint
├── DEPLOYMENT.md                # Detailed deployment guide
└── README.md                    # You are here
```

---

## 🔐 Security

- **HTTPS Enforced** – All production traffic over TLS
- **Content Security Policy** – Strict CSP headers for XSS protection
- **CSRF Protection** – Enabled for all state-changing operations
- **OAuth 2.0** – Industry-standard authentication
- **Secure Sessions** – HttpOnly, Secure, SameSite cookies
- **Rate Limiting** – 50 requests/hour (free), 500/hour (premium)
- **SQL Injection Prevention** – Parameterized queries via JPA

---

## 🗄️ Database Schema

### `users`
- `id` (BIGSERIAL) – Primary key
- `oauth_provider` (VARCHAR) – github / google / microsoft
- `oauth_id` (VARCHAR) – Provider-specific user ID
- `email` (VARCHAR)
- `name` (VARCHAR)
- `avatar_url` (VARCHAR)
- `premium` (BOOLEAN) – Premium subscription status
- `stripe_customer_id` (VARCHAR)
- `created_at` (TIMESTAMP)

### `urls`
- `id` (BIGSERIAL)
- `code` (VARCHAR, UNIQUE) – Short code
- `long_url` (TEXT) – Original URL
- `user_id` (BIGINT) – Foreign key to users
- `click_count` (INTEGER)
- `date_created` (TIMESTAMP)
- `expires_at` (TIMESTAMP, nullable)

### `clicks`
- `id` (BIGSERIAL)
- `url_id` (BIGINT) – Foreign key to urls
- `clicked_at` (TIMESTAMP)
- `referrer` (VARCHAR, nullable)
- `user_agent` (VARCHAR, nullable)
- `ip_address` (VARCHAR, nullable)

Managed by **Flyway** – migrations in `src/main/resources/db/migration/`

---

## 🎨 Frontend Architecture

### Pages
- `index.html` – Landing page with URL shortener
- `login.html` – OAuth provider selection
- `dashboard.html` – User's link management
- `analytics.html` – Detailed click analytics
- `subscription.html` – Premium upgrade flow
- `payment-success.html` – Post-checkout confirmation

### Ad Integration
- **Fragment:** `ads/banner.html` – Reusable ad unit
- **Suppression:** `sec:authorize="!hasAuthority('ROLE_PREMIUM')"` in templates
- **Interceptor:** `AdInterceptor.java` – Server-side premium check
- **Static File:** `ads.txt` – AdSense authorization (must be at root)

---

## 💳 Stripe Integration

### Checkout Flow
1. User clicks "Upgrade to Premium" → POST to `/payment/create-checkout-session`
2. Redirected to Stripe Checkout (hosted page)
3. On success → `checkout.session.completed` webhook fires
4. On `customer.subscription.created` → user's `premium` flag set to `true`
5. User redirected to `/payment/success`

### Webhook Events
- `checkout.session.completed` – Checkout finished (logs session)
- `customer.subscription.created` – Activate premium status
- `customer.subscription.deleted` – Revoke premium on cancellation

**Endpoint:** `/payment/webhook` (unauthenticated, CSRF-exempt, signature-verified)

---

## 📊 Analytics Implementation

### Click Recording
Every redirect through `/api/{code}` triggers:
1. Increment `urls.click_count`
2. Insert row into `clicks` table with timestamp, referrer, user-agent, IP

### Premium Analytics
- **Device detection** – Parse user-agent for mobile/desktop/tablet
- **Browser stats** – Extract browser name and version
- **Referrer tracking** – Full URL stored, grouped by domain
- **Time-series data** – Daily click aggregations for charts

**Tech:** Chart.js for visualizations, REST API at `/api/analytics/{code}`

---

## 🛠️ Development Commands

### Build
```bash
./mvnw clean package
```

### Run Tests
```bash
./mvnw test
```

### Database Migrations (create new)
```bash
# Flyway will auto-apply on startup
# Add new .sql file to src/main/resources/db/migration/V{N}__{description}.sql
```

### Docker Build (production)
```bash
docker build -t quicklink:latest .
docker run -p 8080:8080 --env-file .env quicklink:latest
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork** the repository
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Code Style
- Java: Follow Google Java Style Guide
- HTML/CSS: 2-space indentation
- JavaScript: Use semicolons, `const`/`let` over `var`

### Testing
- Add unit tests for new business logic
- Test OAuth flows with mock providers
- Verify Stripe webhook handling with Stripe CLI

---

## 📄 License

This project is licensed under the **MIT License** – see [LICENSE](LICENSE) for details.

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) – Application framework
- [Stripe](https://stripe.com) – Payment processing
- [Render](https://render.com) – Hosting platform
- [Supabase](https://supabase.com) – PostgreSQL hosting
- [Bootstrap](https://getbootstrap.com) – UI framework
- [Chart.js](https://www.chartjs.org) – Analytics visualizations

---

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/petruhertea/quicklink/issues)
- **Email:** petre.hertea@gmail.com
- **Documentation:** [DEPLOYMENT.md](./DEPLOYMENT.md)

---

**Made with ❤️ by the QuickLink team**
