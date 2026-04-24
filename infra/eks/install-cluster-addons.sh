#!/usr/bin/env bash
# One-time cluster bootstrap after `eksctl create cluster`.
# Installs: AWS Load Balancer Controller, External Secrets Operator,
# kube-prometheus-stack, metrics-server (if missing).
#
# Prereqs: kubectl + helm 3 + cluster kubeconfig.

set -euo pipefail

: "${CLUSTER_NAME:=travelo-prod}"
: "${AWS_REGION:=ap-southeast-2}"
: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID}"

kubectl create namespace travelo          --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace external-secrets --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace monitoring       --dry-run=client -o yaml | kubectl apply -f -

echo "==> AWS Load Balancer Controller"
helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName="${CLUSTER_NAME}" \
  --set region="${AWS_REGION}" \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller \
  --set vpcId="$(aws eks describe-cluster --name "${CLUSTER_NAME}" --region "${AWS_REGION}" --query 'cluster.resourcesVpcConfig.vpcId' --output text)"

echo "==> External Secrets Operator"
helm repo add external-secrets https://charts.external-secrets.io
helm repo update
helm upgrade --install external-secrets external-secrets/external-secrets \
  -n external-secrets \
  --set serviceAccount.create=false \
  --set serviceAccount.name=external-secrets \
  --set installCRDs=true

kubectl apply -f - <<EOF
apiVersion: external-secrets.io/v1beta1
kind: ClusterSecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: ${AWS_REGION}
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets
            namespace: external-secrets
EOF

echo "==> kube-prometheus-stack"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n monitoring \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
  --set prometheus.prometheusSpec.podMonitorSelectorNilUsesHelmValues=false \
  --set grafana.service.type=ClusterIP

echo "==> metrics-server (if not already shipped by EKS)"
kubectl get deploy -n kube-system metrics-server >/dev/null 2>&1 || {
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
}

echo "==> cluster-autoscaler"
helm repo add autoscaler https://kubernetes.github.io/autoscaler
helm repo update
helm upgrade --install cluster-autoscaler autoscaler/cluster-autoscaler \
  -n kube-system \
  --set autoDiscovery.clusterName="${CLUSTER_NAME}" \
  --set awsRegion="${AWS_REGION}" \
  --set rbac.serviceAccount.create=false \
  --set rbac.serviceAccount.name=cluster-autoscaler

echo ""
echo "Cluster addons installed. Next:"
echo "  1. Seed AWS Secrets Manager with per-service secrets:"
echo "       aws secretsmanager create-secret --name /travelo/prod/social-service --secret-string file://secrets.json"
echo "  2. Apply ExternalSecret manifests from infra/eks/externalsecrets/"
echo "  3. Push images:  ./scripts/build-and-push-ecr.sh"
echo "  4. Install chart: ./scripts/helm-install-all.sh"
