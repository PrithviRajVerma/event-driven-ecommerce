#!/usr/bin/env bash
set -e

echo "=========================================================="
echo " Deploying Event-Driven E-Commerce Platform to Kubernetes"
echo "=========================================================="

echo "[1/7] Creating Namespace..."
kubectl apply -f k8s/00-namespace.yaml

echo "[2/7] Creating Secrets..."
kubectl apply -f k8s/01-secrets.yaml

echo "[3/7] Creating ConfigMaps..."
kubectl apply -f k8s/02-configmap.yaml

echo "[4/7] Deploying Infrastructure (Databases, Kafka, Redis)..."
kubectl apply -f k8s/03-infrastructure.yaml

echo "[5/7] Deploying Observability Stack (Zipkin, Prometheus, Grafana)..."
kubectl apply -f k8s/04-observability.yaml

echo "[6/7] Deploying Microservices..."
kubectl apply -f k8s/05-microservices.yaml

echo "[7/7] Configuring Ingress Rules..."
kubectl apply -f k8s/06-ingress.yaml

echo ""
echo "Deployment applied successfully!"
echo "To watch pod status, run:"
echo "  kubectl get pods -n ecommerce -w"
echo ""
echo "To forward API Gateway locally, run:"
echo "  kubectl port-forward svc/api-gateway -n ecommerce 8080:8080"
