# QuickLink – Deployment & Ad Integration Guide

## What's in this deliverable

| File | Purpose |
|---|---|
| `Dockerfile` | Multi-stage build → slim runtime image |
| `.dockerignore` | Keeps secrets and IDE junk out of the build context |
| `docker-compose.yml` | Local development stack (app + Postgres) |
| `render.yaml` | Render IaC blueprint (deploy with one click) |
| `application.properties` | Updated config – reads `PORT` env var for Render |
| `ads/banner.html` | Thymeleaf ad-fragment (drop into any template) |
| `ads.css` | Scoped ad styles |
| `AdInterceptor.java` | Adds `showAds` flag to every page model automatically |
| `WebMvcConfig.java` | Registers the interceptor |

---

## 1. Docker – Local Development

```bash
# 1. Make sure env.properties exists at the repo root (not committed)
# 2. Build & run everything
docker compose up --build

# The app is at http://localhost:8080
# Local Postgres is at localhost:5432 (user: dev, pass: devpassword)
```

The `DB_URL` in `docker-compose.yml` **overrides** whatever is in `env.properties` so you talk to the local Postgres by default. Comment that line out if you want to hit Supabase instead while running in Docker.

---

## 2. Render Deployment

### 2a. Connect the repo

1. Go to **Render Dashboard → New → Blueprint**.
2. Point it at your GitHub repo root (where `render.yaml` lives).
3. Render reads the blueprint and creates the **urlshortener** web service automatically.

### 2b. Set environment variables

Open the service's **Environment** tab and add every variable listed below. Render encrypts them at rest and injects them at runtime — they never appear in logs or the image.

| Variable | Where to get the value |
|---|---|
| `DB_URL` | Supabase → Settings → Connection → Transaction pooler URI (port 6543). Append `?sslmode=require&prepareThreshold=0&preparedStatementCacheQueries=0&preparedStatementCacheSizeMiB=0` |
| `DB_USERNAME` | Supabase pooler user |
| `DB_PASSWORD` | Supabase pooler password |
| `GITHUB_CLIENT_ID` | GitHub → Settings → Developer Settings → OAuth Apps |
| `GITHUB_CLIENT_SECRET` | Same app |
| `GOOGLE_CLIENT_ID` | Google Cloud Console → APIs & Services → Credentials |
| `GOOGLE_CLIENT_SECRET` | Same credential |
| `MICROSOFT_CLIENT_ID` | Azure Portal → App Registrations |
| `MICROSOFT_CLIENT_SECRET` | Same app → Certificates & secrets |
| `MICROSOFT_TENANT_ID` | Azure Portal → App Registrations → Overview |
| `STRIPE_SECRET_KEY` | Stripe Dashboard → Developers → API Keys (live key for production) |
| `STRIPE_WEBHOOK_SECRET` | Stripe Dashboard → Developers → Webhooks → Signing secret (create a webhook pointing at `https://your-render-url/payment/webhook`) |
| `STRIPE_PRICE_ID` | Stripe Dashboard → Products → select your $5/mo price → API ID |
| `APP_URL` | Your Render URL, e.g. `https://urlshortener.onrender.com` (no trailing slash) |

### 2c. OAuth redirect URLs

Each provider needs to know where to send the user back. Add these **exact** URLs in each provider's dashboard:

| Provider | Redirect URL |
|---|---|
| GitHub | `https://urlshortener.onrender.com/login/oauth2/code/github` |
| Google | `https://urlshortener.onrender.com/login/oauth2/code/google` |
| Microsoft | `https://urlshortener.onrender.com/login/oauth2/code/microsoft` |

Replace the domain with your actual Render URL.

### 2d. Health check

Render pings `/actuator/health`. The app already exposes this via Spring Actuator. The custom `databaseHealthIndicator` bean (in `HealthCheckConfig`) will report `DOWN` if Supabase is unreachable, which causes Render to restart the instance automatically.

---

## 3. Ad Integration

### 3a. File placement

```
src/main/resources/
├── static/css/
│   └── ads.css                   ← copy ads.css here
└── templates/
    └── ads/
        └── banner.html           ← copy ads/banner.html here

src/main/java/com/petruth/urlshortener/config/
├── AdInterceptor.java            ← copy here
└── WebMvcConfig.java             ← copy here
```

### 3b. Include the ad CSS in your base layouts

Add this `<link>` to the `<head>` of every template that should show ads (or create a shared layout fragment):

```html
<link rel="stylesheet" href="/css/ads.css">
```

### 3c. Drop the ad fragment into pages

Anywhere inside `<body>` — typically right after the `<nav>` and before the main content:

```html
<!-- Show the ad banner (hidden automatically for premium users) -->
<div th:replace="~{ads/banner}"></div>
```

The `AdInterceptor` already sets `showAds` on every page model. The fragment itself uses Spring Security's `sec:authorize` to hide itself for premium users server-side, so no ad HTML is ever sent to them.

### 3d. Swap in a real ad network

Open `ads/banner.html` and replace the placeholder `<div class="ql-ad__placeholder">` with your ad-network script tag. Example for **Google AdSense**:

```html
<div class="ql-ad__slot" id="ql-ad-banner-slot">
  <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-XXXXXXX" crossorigin="anonymous"></script>
  <ins class="adsbygoogle"
       style="display:block;"
       data-ad-client="ca-pub-XXXXXXX"
       data-ad-slot="XXXXXXX"
       data-ad-format="auto"
       data-full-width-responsive="true"></ins>
  <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
</div>
```

### 3e. How premium suppression works

1. `AdInterceptor` checks the authenticated user's `premium` flag after every page request.
2. It sets `showAds = false` in the Thymeleaf model.
3. The `banner.html` fragment uses `sec:authorize` to conditionally render — premium users receive **zero** ad HTML in the response.
4. No client-side JS is needed to hide ads; it's all server-side.

---

## 4. Key design decisions

**Why `optional:` on `spring.config.import`?** Inside the Docker image there is no `env.properties` file — all values come from environment variables injected by Render (or `docker-compose`). The `optional:` prefix tells Spring to skip the file gracefully instead of crashing.

**Why `PORT` env var?** Render assigns a random high port per deploy and expects the app to listen on it. The original hard-coded `8080` would cause a health-check failure. The fallback `${PORT:8080}` keeps local (`java -jar`) runs working without changes.

**Why scoped ad CSS?** All ad selectors live under `.ql-ad-*`. This prevents any ad-network injected styles from leaking into the rest of the UI, and makes it trivial to remove ads entirely in the future (delete one CSS file and one fragment).
