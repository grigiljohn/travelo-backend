# Running Travelo Backend in IntelliJ IDEA

## Prerequisites
- **Java 21** (Temurin or compatible)
- **IntelliJ IDEA** (Community or Ultimate)
- **Maven 3.9+** (usually bundled with IntelliJ)
- **Docker Desktop** (for running dependencies)

## Step-by-Step Setup

### 1. Open Project in IntelliJ
1. Open IntelliJ IDEA
2. **File → Open** → Select `C:\Workspace\travelo-backend`
3. Choose "Open as Project" when prompted
4. Wait for Maven import to complete (check bottom-right status bar)

### 2. Configure Java SDK
1. **File → Project Structure** (Ctrl+Alt+Shift+S)
2. Under **Project**:
   - Set **SDK**: Java 21
   - Set **Language level**: 21
3. Click **OK**

### 3. Start Dependencies (Choose One Option)

#### Option A: Using Docker Compose (Recommended)
1. Open terminal in IntelliJ (Alt+F12)
2. Navigate to infra directory:
   ```powershell
   cd infra
   ```
3. Start only the required services:
   ```powershell
   docker compose up config-server service-registry postgres
   ```
   Or start all services:
   ```powershell
   docker compose up
   ```

#### Option B: Run Services in IntelliJ
You can also run Config Server and Service Registry directly in IntelliJ:

**Config Server:**
- Navigate to: `config/config-server/src/main/java/com/travelo/configserver/ConfigServerApplication.java`
- Right-click → **Run 'ConfigServerApplication'**

**Service Registry:**
- Navigate to: `registry/service-registry/src/main/java/com/travelo/registry/ServiceRegistryApplication.java`
- Right-click → **Run 'ServiceRegistryApplication'**

**PostgreSQL:** Still needs to run via Docker or local installation

### 4. Configure Application for Local Development

Since you're running locally (not in Docker), you need to update the `application.yml` for post-service:

**File:** `services/post-service/src/main/resources/application.yml`

Update these values:
```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888  # Changed from config-server:8888
  datasource:
    url: jdbc:postgresql://localhost:5432/travelo_posts  # Changed from postgres:5432

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # Changed from service-registry:8761
```

### 5. Run Post Service in IntelliJ

1. Navigate to: `services/post-service/src/main/java/com/travelo/postservice/PostServiceApplication.java`
2. Right-click on the class → **Run 'PostServiceApplication'**
   - Or click the green ▶️ icon next to the `main` method
   - Or use shortcut: **Shift+F10**

### 6. Verify Services Are Running

- **Config Server**: http://localhost:8888
- **Service Registry (Eureka)**: http://localhost:8761
- **Post Service**: http://localhost:8083
- **Post Service Health**: http://localhost:8083/actuator/health
- **Post Service Swagger**: http://localhost:8083/swagger-ui/index.html

## Running Multiple Services

To run multiple services simultaneously:

1. **Run → Edit Configurations...**
2. Click **+** → **Application**
3. Configure each service:
   - **Name**: PostServiceApplication
   - **Main class**: `com.travelo.postservice.PostServiceApplication`
   - **Module**: `post-service`
   - **Use classpath of module**: `post-service`
4. Repeat for other services you want to run
5. Use **Run → Run 'PostServiceApplication'** or click the run button

## Troubleshooting

### Maven Import Issues
- **File → Invalidate Caches...** → **Invalidate and Restart**
- **View → Tool Windows → Maven** → Click refresh icon

### Port Already in Use
- Check if services are already running: `netstat -ano | findstr :8083`
- Stop conflicting processes or change port in `application.yml`

### Database Connection Issues
- Ensure PostgreSQL is running: `docker ps`
- Verify connection string in `application.yml`
- Check database exists: Connect to `localhost:5432` with user `travelo` / password `travelo`

### Eureka Connection Issues
- Ensure Service Registry is running first
- Check `application.yml` has correct Eureka URL (`localhost:8761` for local)
- Services will still run even if Eureka is down (due to `optional:` prefix)

### Config Server Connection Issues
- Config Server is optional (due to `optional:` prefix)
- Service will use local `application.yml` if Config Server is unavailable
- To disable: Remove or comment out the `spring.config.import` line

## Quick Start Script

You can create a run configuration that starts multiple services:

1. **Run → Edit Configurations...**
2. Click **+** → **Compound**
3. Name it "All Services"
4. Add all services you want to run
5. Click **OK**
6. Run the compound configuration to start all services at once

## Development Tips

- Use **Spring Boot DevTools** (if added) for hot reload
- Enable **Build → Build Project automatically** for faster iteration
- Use IntelliJ's **Database** tool window to connect to PostgreSQL
- Check **Run** tool window for service logs
- Use **Services** tool window (View → Tool Windows → Services) to manage multiple Spring Boot applications


