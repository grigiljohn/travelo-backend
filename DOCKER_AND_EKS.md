# Travelo Backend — Docker & AWS EKS Deployment Guide

Everything needed to:

1. Run the **entire backend locally on Windows Docker Desktop** with one command.
2. Ship the same images to **Amazon ECR**.
3. Deploy to **Amazon EKS** via Helm + GitHub Actions.

The local stack and the EKS stack use the **same Dockerfiles, same env vars,
same ports** — anything that works locally works in prod with only host swaps
(Postgres → RDS, Redis → ElastiCache, Kafka → MSK, OpenSearch → AOS).

---

## 1. Repository layout (new + changed)

```
.dockerignore                              # keeps build contexts small
.env.example                               # copy to .env for local dev
docker-compose.yml                         # local Windows stack (all 10 services + infra)
config/config-server/Dockerfile            # optimised multi-stage build
registry/service-registry/Dockerfile       # "
services/*/Dockerfile                      # " (media-service bundles FFmpeg)
scripts/
  local-up.ps1                             # Windows bootstrap wrapper
  build-and-push-ecr.ps1                   # Windows image publisher
  build-and-push-ecr.sh                    # Linux/CI image publisher
  helm-install-all.sh                      # rollout every chart
infra/
  eks/
    cluster.eksctl.yaml                    # cluster + IRSA + node groups
    install-cluster-addons.sh              # ALB controller, ESO, Prometheus…
    externalsecrets/media-service.yaml     # template (copy for every svc)
  helm/travelo-backend/
    Chart.yaml
    values.yaml                            # shared defaults (rewritten)
    values-shared-aws.yaml                 # prod overlay (image repo, HPA, affinity)
    values-<service>.yaml                  # one per service (10 total)
    templates/
      deployment.yaml                      # service-agnostic
      service.yaml
      configmap.yaml
      secret.yaml
      serviceaccount.yaml
      ingress.yaml
      hpa.yaml
      pdb.yaml
      servicemonitor.yaml
.github/workflows/
  docker-build-push.yml                    # matrix build → push to ECR
  deploy-eks.yml                           # OIDC-assumed role → helm upgrade
```

---

## 2. Local dev — Windows Docker Desktop

### 2.1 Prereqs

- **Docker Desktop** 4.x with the WSL2 backend.
- In Docker Desktop → *Settings → Resources* allocate **≥ 10 GB RAM / 6 CPU**
  (Kafka + Elasticsearch + 10 JVMs add up fast).

### 2.2 First run

```powershell
# from repo root
copy .env.example .env
# edit .env — at minimum change JWT_SECRET

.\scripts\local-up.ps1 -Build
```

The helper:

1. Copies `.env.example` → `.env` if absent.
2. Runs `docker compose up -d --build`.
3. Prints the URLs for every service.

Expected boot order (`docker compose` resolves dependencies):

```
postgres → redis → kafka → elasticsearch
     └── config-server → service-registry → (api-gateway, identity, social, media, discovery, realtime, commerce, platform)
```

### 2.3 Common commands

```powershell
.\scripts\local-up.ps1                  # start (no rebuild)
.\scripts\local-up.ps1 -Build           # rebuild images
.\scripts\local-up.ps1 -Tools           # also start pgAdmin at :5050
.\scripts\local-up.ps1 -Logs api-gateway,social-service,media-service
.\scripts\local-up.ps1 -Down            # stop (keeps volumes)
.\scripts\local-up.ps1 -Down -Purge     # stop + delete all data volumes
```

### 2.4 Ports on `localhost`

| Service           | Port  | Notes                          |
| ----------------- | ----- | ------------------------------ |
| api-gateway       | 8080  | your app's single entry point  |
| identity-service  | 8081  | `/swagger-ui.html`             |
| media-service     | 8084  | includes FFmpeg                |
| discovery-service | 8088  | search / feed                  |
| social-service    | 8096  | posts / stories / **reels**    |
| commerce-service  | 8097  | shops + ads                    |
| realtime-service  | 8098  | WebSocket + notifications      |
| platform-service  | 8099  | platform admin                 |
| config-server     | 8888  | `/actuator/health`             |
| service-registry  | 8761  | Eureka dashboard               |
| Postgres          | 5432  | user/pass: `travelo/travelo`   |
| Redis             | 6379  |                                |
| Kafka             | 9092  | host advert; broker `kafka:29092` inside the network |
| Elasticsearch     | 9200  |                                |
| pgAdmin (`-Tools`)| 5050  | `admin@travelo.local / admin`  |

### 2.5 Hot tips

- **Rebuild a single service:**
  `docker compose up -d --build media-service`
- **Follow one log:**
  `docker compose logs -f social-service`
- **Shell into a container:**
  `docker compose exec media-service /bin/bash`
- **Windows path quirks:** everything runs inside WSL2; no Windows paths
  cross the container boundary, so mount-point gotchas are avoided.

---

## 3. Container images explained

### 3.1 Dockerfile pattern

Every Spring Boot service uses the same three-stage build:

```
maven:3.9.6-eclipse-temurin-21   # stage `deps`  → go-offline using only pom.xmls
maven:3.9.6-eclipse-temurin-21   # stage `build` → full source, `mvn package`
eclipse-temurin:21-jre-jammy     # stage runtime → slim JRE + tini + curl
```

Highlights:

- `--mount=type=cache,target=/root/.m2` reuses the Maven cache across builds.
- The **deps stage copies only `pom.xml` files** so dependency downloads are
  cached until a pom changes.
- Runs as **non-root UID 1001 (`travelo`)**.
- `tini` as PID 1 for clean SIGTERM handling.
- `JAVA_OPTS=-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport` — the JVM
  obeys the pod memory limit.
- Built-in `HEALTHCHECK` hits Spring Boot actuator.
- **`media-service` only** also installs `ffmpeg` + `ffprobe` and creates
  `/tmp/travelo-reels` owned by the `travelo` user.

### 3.2 BuildKit required

All Dockerfiles start with `# syntax=docker/dockerfile:1.6` — Docker Desktop
enables BuildKit by default. For CI the GHA workflow calls `setup-buildx-action`.

---

## 4. AWS prep (one-time)

```bash
aws configure                         # or use AWS SSO
export AWS_ACCOUNT_ID=123456789012
export AWS_REGION=ap-southeast-2
```

### 4.1 Create the cluster

```bash
# edit infra/eks/cluster.eksctl.yaml → replace REPLACE_ME_ACCOUNT_ID
eksctl create cluster -f infra/eks/cluster.eksctl.yaml
```

Creates:

- VPC (`10.70.0.0/16`), 2 public + 2 private subnets.
- 2 managed node groups:
  - `ng-default` (`t3.large`, general).
  - `ng-backend` (`m6i.xlarge`, tainted `workload=backend:NoSchedule`).
- IRSA roles for media-service (S3+Rekognition), ALB controller,
  cluster-autoscaler, External Secrets.
- Addons: VPC CNI, CoreDNS, kube-proxy, EBS CSI.

### 4.2 Install cluster-wide components

```bash
chmod +x infra/eks/install-cluster-addons.sh
./infra/eks/install-cluster-addons.sh
```

Installs: AWS Load Balancer Controller, External Secrets Operator,
kube-prometheus-stack, cluster-autoscaler, metrics-server (if missing).
Creates the `travelo`, `external-secrets`, `monitoring` namespaces and a
`ClusterSecretStore` pointing at AWS Secrets Manager.

### 4.3 Provision managed dependencies

Create these **before** first deploy (Terraform, CDK, or console):

| Service           | Replaces (local)           | Notes                                           |
| ----------------- | -------------------------- | ----------------------------------------------- |
| RDS Postgres 16   | `postgres`                 | 1 instance hosting 8 logical DBs, or 1 per svc  |
| ElastiCache Redis | `redis`                    | cluster mode off; enable AUTH                   |
| MSK (Kafka)       | `kafka`                    | TLS + IAM-auth bootstrap endpoints              |
| OpenSearch        | `elasticsearch`            | used by `discovery-service`                     |
| S3 (media+music)  | `media-uploads` volume     | bind via IRSA role `travelo-media-service`      |
| ACM cert          | n/a                        | for `api.travelo.example.com` → ALB HTTPS       |
| Secrets Manager   | `.env`                     | one JSON secret per service (see below)         |

### 4.4 Load secrets

One secret per service, stored at `/travelo/prod/<service>`:

```bash
aws secretsmanager create-secret \
  --name /travelo/prod/media-service \
  --secret-string file://media-service-secrets.json
```

Example `media-service-secrets.json`:

```json
{
  "SPRING_DATASOURCE_URL": "jdbc:postgresql://travelo-prod.xxx.rds.amazonaws.com:5432/travelo_media",
  "SPRING_DATASOURCE_USERNAME": "travelo",
  "SPRING_DATASOURCE_PASSWORD": "***",
  "SPRING_DATA_REDIS_HOST": "travelo-prod.xxxx.cache.amazonaws.com",
  "SPRING_DATA_REDIS_PORT": "6379",
  "SPRING_DATA_REDIS_PASSWORD": "***",
  "KAFKA_BOOTSTRAP_SERVERS": "b-1.travelo.msk.amazonaws.com:9098",
  "MEDIA_S3_BUCKET": "travelo-prod-media",
  "MEDIA_S3_REGION": "ap-southeast-2",
  "MUSIC_S3_BUCKET": "travelo-prod-music",
  "MEDIA_API_PUBLIC_BASE_URL": "https://api.travelo.example.com/media",
  "MEDIA_S3_PUBLIC_OBJECT_BASE_URL": "https://cdn.travelo.example.com"
}
```

Apply the `ExternalSecret` (one per service — copy `infra/eks/externalsecrets/media-service.yaml`):

```bash
kubectl apply -f infra/eks/externalsecrets/
```

---

## 5. Build & push images

### From a dev box (Windows):

```powershell
$env:AWS_ACCOUNT_ID = "123456789012"
$env:AWS_REGION     = "ap-southeast-2"
.\scripts\build-and-push-ecr.ps1             # all services
.\scripts\build-and-push-ecr.ps1 -Services media-service,social-service
.\scripts\build-and-push-ecr.ps1 -Tag v1.2.3
```

### From a Mac/Linux box or CI:

```bash
AWS_ACCOUNT_ID=123456789012 AWS_REGION=ap-southeast-2 ./scripts/build-and-push-ecr.sh
```

Both scripts:

- Create ECR repos on first push (`travelo/<service>`, encrypted, scan-on-push).
- Tag images with **short git SHA** *and* `latest`.
- Build for `linux/amd64` (EKS nodes).

### Via GitHub Actions (recommended for main branch)

`.github/workflows/docker-build-push.yml` builds a **matrix of 10 services in
parallel**, pushes to ECR using OIDC (no static AWS keys), and then triggers
`deploy-eks.yml` with the short SHA as the image tag.

Required GitHub secrets:

- `AWS_CI_ROLE_ARN` — IAM role with `ecr:*`, `sts:AssumeRoleWithWebIdentity`
  for the GitHub OIDC provider, and `eks:DescribeCluster` for deploys.
- `AWS_ACCOUNT_ID` — used by the helm install step.

---

## 6. Deploy with Helm

### 6.1 Roll everything out

```bash
AWS_ACCOUNT_ID=123456789012 AWS_REGION=ap-southeast-2 IMAGE_TAG=abc123 \
  ./scripts/helm-install-all.sh
```

Each release is independent (`travelo-api-gateway`, `travelo-media-service`, …)
and uses:

```
-f values-shared-aws.yaml   # image repo, prod HPA, affinity, ServiceMonitor
-f values-<service>.yaml    # service-specific env/secrets/ports/probes
--set image.tag=$IMAGE_TAG  # from CI
```

### 6.2 What each chart installs per service

- `Deployment` (rolling, `maxUnavailable=0`, `maxSurge=1`, 15s preStop drain)
- `Service` (ClusterIP)
- `ServiceAccount` (with IRSA annotation when set)
- `HorizontalPodAutoscaler` (CPU + memory targets)
- `PodDisruptionBudget` (`minAvailable: 1`)
- `ServiceMonitor` (Prometheus scrapes `/actuator/prometheus`)
- `Ingress` *(only enabled for `api-gateway` — ALB internet-facing + ACM)*
- Optional ConfigMap / Secret (disabled by default; we use ESO)

### 6.3 Ingress

`values-api-gateway.yaml` creates a single ALB terminating TLS for
`api.travelo.example.com`. Replace `REPLACE_ME_ACM_CERT_ARN` before the first
deploy.

### 6.4 Verifying a rollout

```bash
kubectl -n travelo get deploy,svc,hpa,ingress
kubectl -n travelo rollout status deploy/travelo-social-service
kubectl -n travelo logs -f deploy/travelo-media-service
```

---

## 7. Environment-variable parity

Every service obeys the **same keys** in dev and prod — you never rewrite
configuration, only swap hosts.

| Key                           | Local (docker-compose)        | EKS (Secrets Manager → ExternalSecret) |
| ----------------------------- | ----------------------------- | -------------------------------------- |
| `SPRING_PROFILES_ACTIVE`      | `docker`                      | `production`                           |
| `SPRING_DATASOURCE_URL`       | `jdbc:postgresql://postgres…` | RDS endpoint                           |
| `SPRING_DATA_REDIS_HOST`      | `redis`                       | ElastiCache endpoint                   |
| `KAFKA_BOOTSTRAP_SERVERS`     | `kafka:29092`                 | MSK bootstrap                          |
| `ELASTICSEARCH_URIS`          | `http://elasticsearch:9200`   | AOS https endpoint                     |
| `MEDIA_S3_ENABLED`            | `false`                       | `true`                                 |
| `REEL_PROGRESS_BACKEND`       | `redis`                       | `redis`                                |
| `JWT_SECRET`                  | from `.env`                   | Secrets Manager                        |

---

## 8. Day-2 operations

- **Scaling manually**:
  `kubectl -n travelo scale deploy/travelo-social-service --replicas=6`
- **Rolling restart**:
  `kubectl -n travelo rollout restart deploy/travelo-media-service`
- **Pin a tag for investigation**:
  `helm upgrade travelo-media-service … --set image.tag=v1.2.3`
- **Grafana** is installed by `install-cluster-addons.sh` — port-forward with
  `kubectl -n monitoring port-forward svc/kube-prometheus-stack-grafana 3000:80`.

---

## 9. Troubleshooting

| Symptom                                                         | Likely cause / fix                                                                                                                                                    |
| --------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `docker compose up` hangs on Kafka health check                 | Give Docker Desktop more RAM (≥ 10 GB). Kafka takes ~45 s cold.                                                                                                       |
| Local build very slow                                           | First build only — later builds reuse the Maven cache. Prune with `docker builder prune -f` if disk pressure hits.                                                    |
| `media-service` can't find `ffmpeg`                             | You edited the Dockerfile and removed the apt-get line; re-add `ffmpeg` to the runtime stage.                                                                         |
| `MountVolume.SetUp failed for volume "kube-api-access"` on EKS  | The namespace was deleted while a pod terminated. Wait or recreate namespace.                                                                                         |
| `401` from API gateway                                          | Locally, ensure `JWT_SECRET` is identical across identity + gateway (the `.env` handles this).                                                                        |
| Reel SSE disconnects every 60 s in prod                         | ALB idle timeout is 60 s by default. Add `alb.ingress.kubernetes.io/load-balancer-attributes: idle_timeout.timeout_seconds=180`.                                      |
| CI push fails with `operation error STS`                        | The GitHub OIDC trust policy on `AWS_CI_ROLE_ARN` is missing the repo or branch condition.                                                                            |

---

## 10. What was deliberately NOT done

- **service-registry (Eureka)** on EKS is optional. Every per-service values
  file sets `EUREKA_CLIENT_ENABLED=false` — k8s Services + ClusterDNS replace
  Eureka. Install `values-service-registry.yaml` only if legacy clients still
  need it during migration.
- **Terraform / CDK** for RDS, ElastiCache, MSK, OpenSearch. Provisioning
  those is org-specific and out of scope for this change.
- **Blue/green / canary** via Argo Rollouts. Current strategy is
  RollingUpdate with `maxUnavailable=0` + PDB — safe, not fancy.
