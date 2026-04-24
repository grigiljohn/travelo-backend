#!/usr/bin/env bash
# Deploy (or upgrade) every Travelo service into the `travelo` namespace.
#
# Usage:
#   AWS_ACCOUNT_ID=123456789012 AWS_REGION=ap-southeast-2 IMAGE_TAG=abcdef123456 \
#     ./scripts/helm-install-all.sh
#
# Flags:
#   SERVICES="api-gateway social-service"  # subset; defaults to all
#   NAMESPACE=travelo
#   CHART=./infra/helm/travelo-backend
#   SHARED=./infra/helm/travelo-backend/values-shared-aws.yaml

set -euo pipefail

: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID}"
: "${AWS_REGION:=ap-southeast-2}"
: "${IMAGE_TAG:=latest}"
: "${NAMESPACE:=travelo}"
: "${CHART:=./infra/helm/travelo-backend}"
: "${SHARED:=./infra/helm/travelo-backend/values-shared-aws.yaml}"

REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/travelo"

ALL=(
  config-server
  api-gateway
  identity-service
  social-service
  media-service
  discovery-service
  realtime-service
  commerce-service
  platform-service
)
# service-registry (Eureka) is optional on k8s. Uncomment to include:
# ALL+=(service-registry)

IFS=' ' read -ra SELECTED <<<"${SERVICES:-${ALL[*]}}"

kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

for svc in "${SELECTED[@]}"; do
  values_file="${CHART}/values-${svc}.yaml"
  if [[ ! -f "${values_file}" ]]; then
    echo "skip ${svc}: ${values_file} not found"; continue
  fi
  release="travelo-${svc}"
  echo
  echo "==> helm upgrade --install ${release}"
  helm upgrade --install "${release}" "${CHART}" \
    --namespace "${NAMESPACE}" \
    --values "${SHARED}" \
    --values "${values_file}" \
    --set image.repository="${REGISTRY}" \
    --set image.tag="${IMAGE_TAG}" \
    --wait --timeout 10m
done

echo
echo "Deployed tag=${IMAGE_TAG} into ns/${NAMESPACE}. Check rollout:"
echo "  kubectl -n ${NAMESPACE} get pods,svc,ingress"
