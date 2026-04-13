# Travelo Backend Helm Chart

This Helm chart deploys Travelo backend microservices to Kubernetes.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- PostgreSQL database
- Redis (for services that require it)
- Kafka (for services that require it)
- Elasticsearch (for search-service)

## Installation

### 1. Install PostgreSQL

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install postgresql bitnami/postgresql \
  --set auth.database=travelo \
  --set auth.username=travelo \
  --set auth.password=travelo
```

### 2. Install Redis

```bash
helm install redis bitnami/redis \
  --set auth.password=redispassword
```

### 3. Install Kafka

```bash
helm repo add confluentinc https://confluentinc.github.io/cp-helm-charts/
helm install kafka confluentinc/cp-helm-charts
```

### 4. Install Elasticsearch (for search-service)

```bash
helm repo add elastic https://helm.elastic.co
helm install elasticsearch elastic/elasticsearch
```

### 5. Deploy Services

#### Post Service

```bash
helm install post-service ./infra/helm/travelo-backend \
  -f ./infra/helm/travelo-backend/values-post-service.yaml \
  --set environment=production \
  --set database.host=postgresql \
  --set database.name=travelo_posts \
  --set database.username=travelo \
  --set database.password=travelo \
  --set image.repository=your-registry/travelo \
  --set image.tag=latest
```

#### Media Service

```bash
helm install media-service ./infra/helm/travelo-backend \
  -f ./infra/helm/travelo-backend/values-media-service.yaml \
  --set environment=production \
  --set database.host=postgresql \
  --set database.name=travelo_media \
  --set image.repository=your-registry/travelo \
  --set image.tag=latest
```

#### Feed Service

```bash
helm install feed-service ./infra/helm/travelo-backend \
  -f ./infra/helm/travelo-backend/values-feed-service.yaml \
  --set environment=production \
  --set redis.host=redis \
  --set redis.password=redispassword \
  --set kafka.bootstrapServers=kafka:9092 \
  --set image.repository=your-registry/travelo \
  --set image.tag=latest
```

## Configuration

### Environment Variables

Services can be configured using environment variables or by overriding values in the Helm chart.

Key environment variables:
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers
- `SPRING_DATA_REDIS_HOST`: Redis host
- `SPRING_DATA_REDIS_PORT`: Redis port
- `SPRING_DATA_REDIS_PASSWORD`: Redis password

### Secrets

Database passwords and other sensitive data should be stored in Kubernetes secrets:

```bash
kubectl create secret generic post-service-secrets \
  --from-literal=datasource-password=your-password \
  --from-literal=redis-password=your-redis-password
```

Then reference in values:

```yaml
database:
  passwordSecret:
    name: post-service-secrets
    key: datasource-password
```

## Upgrading

```bash
helm upgrade post-service ./infra/helm/travelo-backend \
  -f ./infra/helm/travelo-backend/values-post-service.yaml \
  --set image.tag=new-version
```

## Rolling Back

```bash
helm rollback post-service
```

## Uninstalling

```bash
helm uninstall post-service
```

## Monitoring

### Prometheus Scraping

All services expose Prometheus metrics at `/actuator/prometheus`. To scrape these metrics:

1. Install Prometheus Operator
2. Create ServiceMonitor resources for each service

Example ServiceMonitor:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: post-service-metrics
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: travelo-backend
      app.kubernetes.io/component: post-service
  endpoints:
    - port: http
      path: /actuator/prometheus
```

### Health Checks

All services expose health checks at:
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

These are automatically configured in the deployment.

## Auto-scaling

Services support Horizontal Pod Autoscaling (HPA). Enable it in values:

```yaml
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
  targetMemoryUtilizationPercentage: 80
```

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -l app.kubernetes.io/component=post-service
```

### View Logs

```bash
kubectl logs -l app.kubernetes.io/component=post-service -f
```

### Check Health

```bash
kubectl port-forward svc/travelo-backend-post-service 8083:8083
curl http://localhost:8083/actuator/health
```

### Check Metrics

```bash
curl http://localhost:8083/actuator/prometheus | grep http_server_requests
```

