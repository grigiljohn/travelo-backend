# Local Development Setup Guide

This guide helps you set up the Travelo backend services to run locally (outside Docker).

## Prerequisites

1. **Java 21** - Install from [Adoptium](https://adoptium.net/) or use your preferred JDK
2. **Maven 3.9+** - Install from [Maven website](https://maven.apache.org/download.cgi)
3. **PostgreSQL** - Install from [PostgreSQL website](https://www.postgresql.org/download/windows/)
4. **Redis** (optional, for auth-service) - Install from [Redis website](https://redis.io/download) or use Docker: `docker run -d -p 6379:6379 redis:alpine`
5. **Kafka** (optional, for event-driven services) - Install from [Kafka website](https://kafka.apache.org/downloads)

## Database Setup

### Option 1: Using psql Command Line (Recommended)

If you have PostgreSQL installed and `psql` is in your PATH:

**Windows (PowerShell):**
```powershell
# Set PostgreSQL password if needed
$env:PGPASSWORD = "your_postgres_password"

# Connect to PostgreSQL and create databases
psql -U postgres -h localhost -c "CREATE DATABASE travelo_auth;"
psql -U postgres -h localhost -c "CREATE DATABASE travelo_posts;"
psql -U postgres -h localhost -c "CREATE DATABASE travelo_media;"
psql -U postgres -h localhost -c "CREATE DATABASE travelo_ads;"
psql -U postgres -h localhost -c "CREATE DATABASE travelo_notifications;"
psql -U postgres -h localhost -c "CREATE DATABASE travelo_messaging;"
psql -U postgres -h localhost -c "CREATE DATABASE travelo_stories;"
psql -U postgres -h localhost -c "CREATE DATABASE travelo_reels;"
```

**Linux/Mac:**
```bash
psql -U postgres -h localhost -c "CREATE DATABASE travelo_auth;"
psql -U postgres -h localhost -c "CREATE DATABASE travelo_posts;"
# ... (repeat for all databases)
```

### Option 2: Using pgAdmin or DBeaver

1. Open pgAdmin or Dbeaver
2. Connect to your local PostgreSQL instance
3. Right-click on "Databases" → "Create" → "Database"
4. Create each database:
   - `travelo_auth`
   - `travelo_posts`
   - `travelo_media`
   - `travelo_ads`
   - `travelo_notifications`
   - `travelo_messaging`
   - `travelo_stories`
   - `travelo_reels`

### Option 3: Using SQL Script

1. Open the SQL script: `scripts/create-local-databases.sql`
2. Connect to PostgreSQL using your preferred tool
3. Execute the script

### Create Database User (if needed)

If you don't have a `travelo` user, create it:

```sql
CREATE USER travelo WITH PASSWORD 'travelo';
ALTER USER travelo CREATEDB;
```

Then grant privileges:
```sql
GRANT ALL PRIVILEGES ON DATABASE travelo_auth TO travelo;
GRANT ALL PRIVILEGES ON DATABASE travelo_posts TO travelo;
-- Repeat for all databases
```

## Running Services

### Start Infrastructure Services First

1. **PostgreSQL** - Should be running on `localhost:5432`
2. **Redis** (if needed) - Should be running on `localhost:6379`
3. **Kafka** (if needed) - Should be running on `localhost:9092`
4. **Eureka Service Registry** (optional):
   ```bash
   mvn spring-boot:run -pl registry/service-registry
   ```
5. **Config Server** (optional):
   ```bash
   mvn spring-boot:run -pl config/config-server
   ```

### Start Microservices

Each service can be started independently:

```bash
# Auth Service
mvn spring-boot:run -pl services/auth-service

# User Service
mvn spring-boot:run -pl services/user-service

# Post Service
mvn spring-boot:run -pl services/post-service

# Media Service
mvn spring-boot:run -pl services/media-service

# API Gateway
mvn spring-boot:run -pl services/api-gateway
```

### Service Ports

- **Config Server**: 8888
- **Service Registry (Eureka)**: 8761
- **API Gateway**: 8080
- **Auth Service**: 8081
- **User Service**: 8082
- **Post Service**: 8083
- **Media Service**: 8084
- **Feed Service**: 8085
- **Story Service**: 8086
- **Reel Service**: 8087
- **Search Service**: 8088
- **Notification Service**: 8089
- **Messaging Service**: 8090
- **WebSocket Service**: 8091
- **Analytics Service**: 8092
- **Ad Service**: 8093
- **Admin Service**: 8094
- **Gateway Helpers**: 8095

## Configuration

All services are configured to use `localhost` for:
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Elasticsearch: `localhost:9200`
- Eureka: `http://localhost:8761/eureka/`
- Config Server: `http://localhost:8888`

You can override these using environment variables:
- `POSTGRES_URL` - Database connection URL
- `POSTGRES_USERNAME` - Database username (default: `travelo`)
- `POSTGRES_PASSWORD` - Database password (default: `travelo`)
- `REDIS_HOST` - Redis host (default: `localhost`)
- `REDIS_PORT` - Redis port (default: `6379`)
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka bootstrap servers (default: `localhost:9092`)

## Troubleshooting

### Database Connection Errors

If you see errors like:
```
java.lang.NullPointerException: Cannot invoke "org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert..."
```

**Solution:**
1. Ensure PostgreSQL is running: `pg_isready -h localhost -p 5432`
2. Verify the database exists: `psql -U postgres -h localhost -l`
3. Check credentials in `application.yml` or environment variables
4. Ensure the database user has proper permissions

### Port Already in Use

If a port is already in use, either:
1. Stop the service using that port
2. Change the port in `application.yml`:
   ```yaml
   server:
     port: 8081  # Change to available port
   ```

### Service Discovery Issues

If services can't find each other:
1. Ensure Eureka is running on `localhost:8761`
2. Check service registration at: `http://localhost:8761`
3. Verify service names match in `application.yml`

## Quick Start Script

For convenience, you can create a startup script:

**start-services.ps1** (PowerShell):
```powershell
# Start infrastructure
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run -pl registry/service-registry"
Start-Sleep -Seconds 10

# Start services
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run -pl services/auth-service"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run -pl services/user-service"
# ... add more services as needed
```

## Next Steps

1. Create all required databases
2. Start infrastructure services (PostgreSQL, Redis, etc.)
3. Start Eureka and Config Server (optional)
4. Start your microservices
5. Access Swagger UI at: `http://localhost:<service-port>/swagger-ui.html`

