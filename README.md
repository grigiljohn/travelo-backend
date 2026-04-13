# Travelo Backend Monorepo

Travelo is a social travel platform composed of event-driven microservices. This monorepo contains all server-side components, shared libraries, infrastructure as code, and operational tooling required to run the platform locally or in production.

## Structure

```
travelo-backend/
├─ pom.xml                     # top-level Maven parent (aggregator)
├─ README.md
├─ build/                      # CI/CD scripts and automation assets
├─ docs/                       # architecture notes, API contracts
├─ infra/                      # deployment assets (docker-compose, helm, terraform)
│  ├─ docker-compose.yml
│  └─ helm/
├─ infra-scripts/              # infrastructure provisioning scripts
├─ libs/                       # shared libraries
│  ├─ commons/                 # reusable helpers, response models
│  ├─ security/                # shared security configuration
│  └─ events/                  # domain event contracts
├─ config/
│  └─ config-server/           # Spring Cloud Config Server
├─ registry/
│  └─ service-registry/        # Eureka service discovery
└─ services/                   # functional microservices
   ├─ api-gateway/             # Spring Cloud Gateway entry point
   ├─ auth-service/            # OAuth2 / identity foundation
   ├─ user-service/            # profile graph
   ├─ post-service/            # posts, captions, reactions
   ├─ media-service/           # media ingestion & processing
   ├─ feed-service/            # personalized feed aggregation
   ├─ story-service/           # ephemeral stories
   ├─ reel-service/            # short-form videos
   ├─ search-service/          # discovery and search
   ├─ notification-service/    # push + in-app notifications
   ├─ messaging-service/       # direct messages
   ├─ websocket-service/       # realtime socket connections
   ├─ analytics-service/       # event ingestion & analytics
   ├─ ad-service/              # promotions & billing hooks
   ├─ admin-service/           # moderation suite
   └─ gateway-helpers/         # auxiliary gateway-adjacent services
```

## Getting Started

1. **Prerequisites**
   - Java 21 (Temurin or compatible distribution)
   - Maven 3.9+
   - Docker / Docker Compose (for containerized runtime)

2. **Bootstrap the repo**
   ```powershell
   mvn -B clean verify
   ```

3. **Run locally with Docker Compose**
   ```powershell
   cd infra
   docker compose up --build
   ```
   The compose file spins up the config server, Eureka registry, API gateway, and a representative subset of core services. Additional services can be added following the existing pattern.

4. **Access endpoints**
   - Config Server: http://localhost:8888
   - Service Registry (Eureka UI): http://localhost:8761
   - API Gateway health: http://localhost:8080/health
   - Postgres (local development): `jdbc:postgresql://localhost:5432/travelo_posts` (`travelo` / `travelo`)
   - Swagger UI for each service: `http://localhost:<service-port>/swagger-ui/index.html`

## Development Notes

- Each microservice is self-contained with its own `Dockerfile`, Maven module, and health endpoint for smoke testing.
- Services register with the Eureka server and retrieve configuration via the Spring Cloud Config server by default.
- Shared code lives in `libs/`. Example utilities include `HealthResponse` for consistent API responses and reusable security helpers.
- Extend configuration for each service by adding files under `config/config-server/src/main/resources/configs` or by pointing to an external Git backend.
- CI/CD automation, Kubernetes manifests, and infrastructure scripts have dedicated directories (`build/`, `infra/helm/`, `infra-scripts/`) and are ready for environment-specific templating.

## Next Steps

- Add persistence layers (PostgreSQL, Redis, Kafka) and bind them via Compose or Helm charts.
- Introduce service-specific REST and messaging interfaces beyond the baseline health checks.
- Configure Spring Cloud Config to use a Git backend for versioned configuration management in production environments.

Happy building! 🚀
# travelo-backend
