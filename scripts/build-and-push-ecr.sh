#!/usr/bin/env bash
# Build every Travelo service image and push to Amazon ECR.
# Mirrors scripts/build-and-push-ecr.ps1 for Linux/macOS / CI runners.
#
# Usage:
#   AWS_ACCOUNT_ID=123456789012 AWS_REGION=ap-southeast-2 ./scripts/build-and-push-ecr.sh
#   ./scripts/build-and-push-ecr.sh media-service social-service

set -euo pipefail

cd "$(dirname "$0")/.."

: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID}"
: "${AWS_REGION:=ap-southeast-2}"
: "${IMAGE_TAG:=$(git rev-parse --short=12 HEAD 2>/dev/null || date -u +%Y%m%d%H%M%S)}"

REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

declare -A DOCKERFILES=(
  [config-server]="config/config-server/Dockerfile"
  [service-registry]="registry/service-registry/Dockerfile"
  [api-gateway]="services/api-gateway/Dockerfile"
  [identity-service]="services/identity-service/Dockerfile"
  [social-service]="services/social-service/Dockerfile"
  [media-service]="services/media-service/Dockerfile"
  [discovery-service]="services/discovery-service/Dockerfile"
  [realtime-service]="services/realtime-service/Dockerfile"
  [commerce-service]="services/commerce-service/Dockerfile"
  [platform-service]="services/platform-service/Dockerfile"
)

if [[ $# -eq 0 ]]; then
  SERVICES=(config-server service-registry api-gateway identity-service social-service media-service discovery-service realtime-service commerce-service platform-service)
else
  SERVICES=("$@")
fi

echo "==> AWS account: ${AWS_ACCOUNT_ID}"
echo "==> AWS region : ${AWS_REGION}"
echo "==> Registry   : ${REGISTRY}"
echo "==> Tag        : ${IMAGE_TAG}"
echo "==> Services   : ${SERVICES[*]}"

aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${REGISTRY}"

for svc in "${SERVICES[@]}"; do
  df="${DOCKERFILES[$svc]:-}"
  [[ -n "$df" ]] || { echo "Unknown service: $svc"; exit 1; }

  repo="travelo/${svc}"
  image_sha="${REGISTRY}/${repo}:${IMAGE_TAG}"
  image_latest="${REGISTRY}/${repo}:latest"

  echo
  echo "==> [$svc] ensuring ECR repo"
  aws ecr describe-repositories --repository-names "$repo" --region "${AWS_REGION}" >/dev/null 2>&1 \
    || aws ecr create-repository \
         --repository-name "$repo" \
         --region "${AWS_REGION}" \
         --image-scanning-configuration scanOnPush=true \
         --encryption-configuration encryptionType=AES256 >/dev/null

  echo "==> [$svc] docker build → $image_sha"
  docker buildx build \
    --platform linux/amd64 \
    --file "$df" \
    --tag "$image_sha" \
    --tag "$image_latest" \
    --load \
    .

  echo "==> [$svc] docker push"
  docker push "$image_sha"
  docker push "$image_latest"
done

echo
echo "All images pushed at tag '${IMAGE_TAG}'."
