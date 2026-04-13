# Deploying Travelo Backend on Railway

This monorepo runs **multiple JVM services** (Spring Cloud: config-server, Eureka, API gateway, identity, social, media, discovery, realtime, commerce, platform) plus **PostgreSQL**, **Redis**, **Kafka**, and **Elasticsearch** in local Docker. On Railway you deploy **one Docker image per Railway service** and wire them with **environment variables** and **private networking** (same project) or **public HTTPS URLs**.

Spring Boot is configured to bind to **`PORT`** (Railway injects it at runtime) while keeping local defaults via `IDENTITY_SERVICE_PORT`, `SOCIAL_SERVICE_PORT`, etc.

---

## 1. Architecture choices on Railway

| Approach | When to use |
|----------|-------------|
| **Full stack** (recommended for production parity) | One Railway service per JVM app + managed Postgres + Redis + (optional) Kafka/ES plugins or add-ons. Use **Private Networking** so `lb://social-service` resolves via Eureka on the internal network. |
| **Minimal** (smoke / demo) | Deploy **config-server** → **service-registry** → **identity** + **Postgres** + **Redis** → **api-gateway**, then add others incrementally. Some features stay off until their dependencies exist. |

Railway does **not** run `docker-compose.yml` as-is. Recreate each dependency as a Railway **plugin** (Postgres, Redis) or **template** (Kafka is heavier; consider Confluent Cloud or a dedicated Kafka Railway template).

---

## 2. Repository files for Railway

| Path | Purpose |
|------|---------|
| `infra/railway/RAILWAY.md` | This guide |
| `railway.json` (repo root) | Default config-as-code for the API gateway image: **`build.builder` = `DOCKERFILE`**, root `Dockerfile`, **`deploy.startCommand` = null** (use image `ENTRYPOINT`). |
| `infra/railway/config-as-code/*.toml.example` | Optional per-service overrides: copy to `railway.toml` **or** paste into **Settings → Config-as-code** (adjust `dockerfilePath` per service). |
| `infra/railway/env/*.env.example` | Variable templates for each service |

**Config-as-code:** Railway reads **`railway.json` or `railway.toml`** from the **linked repo root** by default. For multiple services from one repo, create **multiple Railway services**, each with the same GitHub repo; leave **Root directory** empty unless you know you must set **Config as code** to an absolute path such as **`/railway.json`**. Per-service `dockerfilePath` can stay in the dashboard if you do not duplicate a config file per service.

---

## 3. Required environment variables (patterns)

### All JVM services (recommended)

```bash
# Railway sets this automatically — Spring picks it up via application.yml
# PORT=<assigned>

# JVM heap for small plans (512 MB–1 GB RAM)
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=30
```

### Config server

```bash
SPRING_PROFILES_ACTIVE=native
```

Public URL will be like `https://config-server-production-xxxx.up.railway.app` — other services need it only if you keep `SPRING_CONFIG_IMPORT=optional:configserver:...` pointed at this URL (HTTPS).

### Service registry (Eureka)

Override config import to your deployed config-server (if used):

```bash
SPRING_CONFIG_IMPORT=optional:configserver:https://<your-config-host>
```

Eureka peers/clients must use the **public** Eureka URL unless everything is on Railway private networking (then use internal hostnames from Railway docs).

### Identity service

```bash
POSTGRES_URL=jdbc:postgresql://<host>:<port>/<database>
POSTGRES_USERNAME=...
POSTGRES_PASSWORD=...
REDIS_HOST=<redis private host>
REDIS_PORT=6379
SPRING_CONFIG_IMPORT=optional:configserver:https://<config-host>
```

Use Railway **Postgres** plugin variables: map `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE` into `POSTGRES_*` and JDBC URL as in `infra/railway/env/identity-service.env.example`.

### API gateway

```bash
JWT_SECRET=<same 32+ byte secret as identity-service>
SPRING_CONFIG_IMPORT=optional:configserver:https://<config-host>
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=https://<eureka-host>/eureka/
# Route overrides if not using Eureka names (see application.yml)
IDENTITY_SERVICE_URI=https://<identity-service-public-url>
SOCIAL_SERVICE_URI=https://<social-service-public-url>
REALTIME_SERVICE_URI=https://<realtime-service-public-url>
DISCOVERY_SERVICE_URI=https://<discovery-service-public-url>
# ... match docker-compose hostnames to HTTPS URLs
```

When Eureka + `lb://` work on Railway private network, many `*_URI` overrides become optional.

### Social / realtime / media / etc.

Follow each service’s `application.yml` and `docker-compose.yml` for `POSTGRES_*`, `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_*`, `SPRING_CONFIG_IMPORT`, and inter-service URLs.

---

## 4. Health checks

Railway **Deploy → Healthcheck path** (or `railway.json` `deploy.healthcheckPath` / `railway.toml` `[deploy] healthcheckPath`):

| Service | Path |
|---------|------|
| api-gateway, identity, social, media, discovery, realtime, commerce, platform | `/actuator/health` |
| config-server | `/actuator/health` |
| service-registry | `/actuator/health` |

---

## 5. Dockerfile build context

All Dockerfiles use:

```dockerfile
WORKDIR /workspace
COPY . .
RUN mvn -B -pl services/<module> -am -f pom.xml clean package -DskipTests
```

**Build context must be the monorepo root** (`travelo-backend`), not the service folder. In Railway: set **Dockerfile path** to e.g. `services/api-gateway/Dockerfile` and **root directory** empty (repo root), so `COPY . .` includes `libs/`, `pom.xml`, and all modules.

---

## 6. Mobile / frontend public URL

After the **API gateway** is live, set your app’s base URL to:

`https://<gateway-production>.up.railway.app`

Use `TRAVELO_API_BASE_URL` (or per-service vars) in the Flutter app so paths like `/auth-service`, `/messaging-service`, `/post-service` match gateway routes.

---

## 7. Steps to run on Railway (checklist)

1. **Create a Railway project** and connect this GitHub repository.
2. **Add PostgreSQL** (and **Redis**) via Railway **New → Database**. Create databases / users to match Flyway expectations (see `infra/docker-compose.yml` for DB names like `travelo_auth`, `travelo_posts`, etc.) or use one DB with schemas — align with each service’s `application.yml`.
3. **Create the first service: config-server**  
   - Dockerfile: `config/config-server/Dockerfile`  
   - Build context: repo root  
   - Variables: `SPRING_PROFILES_ACTIVE=native`  
   - Generate a **public domain** and note the HTTPS URL.
4. **Create service-registry**  
   - Dockerfile: `registry/service-registry/Dockerfile`  
   - Set `SPRING_CONFIG_IMPORT=optional:configserver:https://<config-server-url>` (no trailing slash issues; use your real URL).  
   - Public domain for Eureka; note `https://<registry>/eureka/`.
5. **Deploy identity-service** (and wire Postgres + Redis references). Set `JWT_SECRET` (strong random string, shared with gateway).  
6. **Deploy api-gateway** last among “edge” pieces, with `JWT_SECRET` identical to identity, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`, and all `*_SERVICE_URI` / `SOCIAL_SERVICE_URI` / `REALTIME_SERVICE_URI` pointing at each service’s **public** URL (or private URLs if using Railway private networking exclusively).  
7. **Add remaining services** (media, discovery, social, realtime, commerce, platform) one by one; copy env patterns from `infra/docker-compose.yml` and `infra/railway/env/*.env.example`.  
8. **Kafka / Elasticsearch**: add Railway-compatible services or managed vendors; set `KAFKA_BOOTSTRAP_SERVERS` / Elasticsearch URLs in social & discovery configs.  
9. **Healthchecks**: set `/actuator/health` on each service; wait for **green** before depending on a service from the gateway.  
10. **Verify**: `curl https://<gateway>/actuator/health` and a login call through `/auth-service/...` as your mobile app uses.

---

## 8. Copy-paste config-as-code

The API gateway ships with repo-root **`railway.json`**. Rename an example under `infra/railway/config-as-code/` to **`railway.toml`** at the repo root **only if** that Railway service should use a different file (one service per file content — duplicate repo connections or use dashboard for other services).

---

## 9. Troubleshooting

### `Error: Unable to access jarfile target/*jar`

Railway **Railpack** auto-detected Java and tried to run something like `java -jar target/*jar` from the **repo root**, where no shaded JAR exists and globs are not expanded for `java`. An **empty** “Custom start command” in **Deploy** does **not** fix this — the bad command comes from the **Railpack builder**, not from that field.

**Fix (do these in order):**

0. Open **Settings → Build** (not only Deploy). Set **Builder** to **Dockerfile**. Set **Dockerfile path** to **`Dockerfile`**. Leave **Root directory** empty for this monorepo. Redeploy and read **Build** logs: you should see Docker build stages (`FROM maven…`, `RUN mvn…`), not only “Railpack”.
1. On **Deployment → details**, confirm **Config source** includes **`railway.json`** and that **Builder** shows **DOCKERFILE**. If there is no file icon / no `railway.json`, your service is not building from the branch that contains it, or **Config as code** is pointed at the wrong path — set it to **`/railway.json`** (repo root).
2. If you use a non-empty **Root directory**, Railway does not auto-pick repo-root config — set **Config as code** to **`/railway.json`**, or add a minimal `railway.json` in that directory with `"build": { "builder": "DOCKERFILE", "dockerfilePath": "Dockerfile" }` and a Dockerfile that matches your layout.
3. **Deploy → Custom start command** should stay **empty** (or explicitly cleared) so the image **`ENTRYPOINT`** runs: `java -jar /app/app.jar`. The repo’s `railway.json` sets `"startCommand": null` for the same reason.
4. Optional fallback: add service variable **`RAILWAY_DOCKERFILE_PATH=Dockerfile`**.
5. **Push** the branch Railway builds so **`railway.json`** and root **`Dockerfile`** are on GitHub.

Rebuild and redeploy.

| Symptom | Likely cause |
|---------|----------------|
| Boot fails “Cannot get config” | `SPRING_CONFIG_IMPORT` URL wrong or config-server not up; `optional:` should allow start — check logs. |
| Eureka never sees instances | Firewall / wrong `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`; use public Eureka URL from same project. |
| 502 from gateway | Downstream service URL wrong or service not registered. |
| OOM on deploy | Lower heap: `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65` and upgrade Railway RAM. |
| Wrong port | Railway always sets `PORT`; do not hardcode listener port in Dockerfile `ENTRYPOINT`. |

---

For questions specific to Railway networking and reference variables, see: [Railway Docs — Variables](https://docs.railway.app/develop/variables) and [Private Networking](https://docs.railway.app/reference/private-networking).
