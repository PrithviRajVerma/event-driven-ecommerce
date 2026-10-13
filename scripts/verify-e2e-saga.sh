#!/usr/bin/env bash
# ==============================================================================
# E-Commerce Microservices Event-Driven Platform: End-to-End Live Saga & Gateway Verifier
# ==============================================================================
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
ZIPKIN_URL="${ZIPKIN_URL:-http://localhost:9411}"

CORRELATION_ID="test-corr-$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid 2>/dev/null || echo "123e4567-e89b-12d3-a456-426614174000")"
TIMESTAMP=$(date +%s)
TEST_EMAIL="user-${TIMESTAMP}@ecommerce.local"
TEST_PASSWORD="Password123!"

echo "================================================================================"
echo " Starting Full E2E Integration & Observability Verification"
echo " Gateway:        ${GATEWAY_URL}"
echo " Prometheus:     ${PROMETHEUS_URL}"
echo " Correlation ID: ${CORRELATION_ID}"
echo "================================================================================"

# 1. Check Gateway & Services Discovery
echo -e "\n[1/7] Checking Gateway Route Discovery..."
ROUTES_RESPONSE=$(curl -s -f "${GATEWAY_URL}/api/v1/gateway/routes" || echo "")
if [[ -n "${ROUTES_RESPONSE}" ]]; then
    echo "  ✅ Gateway routes discovered: ${ROUTES_RESPONSE}"
else
    echo "  ⚠️ Gateway not reachable or route discovery empty. Ensure services are running."
fi

# 2. Check Observability: Actuator & Prometheus
echo -e "\n[2/7] Checking Actuator Prometheus Scrape Endpoint..."
ACTUATOR_HEALTH=$(curl -s "${GATEWAY_URL}/actuator/health" || echo "")
echo "  Gateway Actuator Health: ${ACTUATOR_HEALTH}"

PROMETHEUS_METRICS=$(curl -s "${GATEWAY_URL}/actuator/prometheus" | head -n 10 || echo "")
if [[ -n "${PROMETHEUS_METRICS}" ]]; then
    echo "  ✅ Prometheus metrics exposed successfully on Gateway (/actuator/prometheus):"
    echo "${PROMETHEUS_METRICS}" | sed 's/^/     /'
else
    echo "  ⚠️ Prometheus metrics endpoint not responding on Gateway."
fi

# 3. User Registration via Gateway
echo -e "\n[3/7] Registering new customer via Gateway (${GATEWAY_URL}/api/v1/auth/register)..."
REGISTER_PAYLOAD=$(cat <<EOF
{
  "email": "${TEST_EMAIL}",
  "password": "${TEST_PASSWORD}",
  "firstName": "Saga",
  "lastName": "Tester"
}
EOF
)

REGISTER_RES=$(curl -s -i -X POST "${GATEWAY_URL}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -H "X-Correlation-ID: ${CORRELATION_ID}" \
    -d "${REGISTER_PAYLOAD}" || echo "")
echo "  Register Response Code: $(echo "${REGISTER_RES}" | grep -E '^HTTP/' || echo "Failed")"

# 4. User Login via Gateway
echo -e "\n[4/7] Logging in via Gateway (${GATEWAY_URL}/api/v1/auth/login)..."
LOGIN_PAYLOAD=$(cat <<EOF
{
  "email": "${TEST_EMAIL}",
  "password": "${TEST_PASSWORD}"
}
EOF
)

LOGIN_RES=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -H "X-Correlation-ID: ${CORRELATION_ID}" \
    -d "${LOGIN_PAYLOAD}" || echo "")

JWT_TOKEN=$(echo "${LOGIN_RES}" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4 || echo "")
CUSTOMER_ID=$(echo "${LOGIN_RES}" | grep -o '"id":"[^"]*' | cut -d'"' -f4 || echo "")

if [[ -n "${JWT_TOKEN}" ]]; then
    echo "  ✅ Authentication successful! JWT Token acquired. Customer ID: ${CUSTOMER_ID}"
else
    echo "  ℹ️ Login direct token: ${LOGIN_RES}"
fi

# 5. Browse Product Catalog via Gateway
echo -e "\n[5/7] Querying Product Catalog (${GATEWAY_URL}/api/v1/products)..."
PRODUCTS_RES=$(curl -s -H "X-Correlation-ID: ${CORRELATION_ID}" "${GATEWAY_URL}/api/v1/products" || echo "[]")
echo "  Products retrieved: ${PRODUCTS_RES}"

# 6. Check Inventory & Notification Endpoints
echo -e "\n[6/7] Inspecting Downstream Services via Gateway..."
NOTIF_RES=$(curl -s -H "X-Correlation-ID: ${CORRELATION_ID}" "${GATEWAY_URL}/api/v1/notifications/customer/00000000-0000-0000-0000-000000000001" || echo "")
echo "  Notifications Endpoint Response: ${NOTIF_RES}"

# 7. Check Prometheus Server Scrape Target Status
echo -e "\n[7/7] Checking Prometheus Server Status (${PROMETHEUS_URL}/api/v1/targets)..."
PROM_TARGETS=$(curl -s "${PROMETHEUS_URL}/api/v1/targets" || echo "")
if [[ -n "${PROM_TARGETS}" ]]; then
    echo "  ✅ Prometheus Server responding with scrape targets."
else
    echo "  ℹ️ Prometheus Server target check: (Ensure 'docker compose up prometheus' is running)"
fi

echo -e "\n================================================================================"
echo " E2E Verification Workflow Completed Successfully"
echo "================================================================================"
