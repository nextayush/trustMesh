#!/usr/bin/env bash

# setup-local-env.sh
# Bootstraps local development environment services using Docker Compose

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "============================================="
echo "  Setting up Quantum-Provenance Local Env    "
echo "============================================="

# Ensure Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "ERROR: Docker daemon is not running. Please start Docker and try again."
    exit 1
fi

echo "1. Starting Docker containers..."
cd "${PROJECT_ROOT}/infra/docker"
docker compose up -d

echo "Waiting for services to be healthy..."
# Simple polling loop to check container health
check_health() {
    local service=$1
    local max_retries=12
    local count=0
    until [ "$(docker inspect -f '{{.State.Health.Status}}' "qp-${service}" 2>/dev/null)" == "healthy" ]; do
        count=$((count+1))
        if [ $count -gt $max_retries ]; then
            echo "WARNING: ${service} did not report healthy in time. Continuing anyway..."
            return 1
        fi
        echo "Waiting for qp-${service}... (${count}/${max_retries})"
        sleep 5
    done
    echo "qp-${service} is healthy!"
    return 0
}

check_health "postgres"
check_health "vault"
check_health "minio"

echo "2. Configuring HashiCorp Vault..."
# Vault dev server automatically initializes and unseals with the token defined in docker-compose
export VAULT_TOKEN="dev-root-token"
export VAULT_ADDR="http://localhost:8200"

# Enable KV v2 secrets engine for PQC Private Key storage (often enabled by default at 'secret/')
echo "Enabling Vault Key-Value v2 Secrets Engine..."
docker exec qp-vault vault secrets enable -path=secret kv-v2 || echo "KV engine already enabled or failed"

# Enable Transit secrets engine for classic signing & encryption operations
echo "Enabling Vault Transit Secrets Engine..."
docker exec qp-vault vault secrets enable transit || echo "Transit engine already enabled or failed"

# Create a transit key for symmetric wrapping / classic signatures
echo "Creating transaction signing key in Vault..."
docker exec qp-vault vault write -f transit/keys/blockchain-signing-key || echo "Failed to create transit key"

# Configure a policy for the microservices
echo "Writing policy for microservices..."
docker exec -i qp-vault vault policy write quantum-provenance-policy - <<EOF
# Read/Write access to PQC private keys
path "secret/data/quantum-provenance/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

# Access transit engines for classic cryptographic operations
path "transit/encrypt/blockchain-signing-key" {
  capabilities = ["update"]
}
path "transit/decrypt/blockchain-signing-key" {
  capabilities = ["update"]
}
path "transit/sign/blockchain-signing-key" {
  capabilities = ["update"]
}
path "transit/verify/blockchain-signing-key" {
  capabilities = ["update"]
}
EOF

# Create access tokens for microservices
echo "Creating application token..."
APP_TOKEN=$(docker exec qp-vault vault token create -policy=quantum-provenance-policy -format=json | grep -o '"client_token": "[^"]*' | grep -o '[^"]*$')
echo "App Vault Token: ${APP_TOKEN}"
echo "Save this token to configure your application properties!"

echo "3. Creating MinIO Buckets..."
# We use docker minio client (mc) to configure buckets
docker exec qp-minio mc alias set local http://localhost:9000 minioadmin minioadminpassword
docker exec qp-minio mc mb local/quantum-provenance-manifests || echo "Bucket already exists"
docker exec qp-minio mc anonymous set download local/quantum-provenance-manifests

echo "4. Checking Kafka Topics..."
# Auto create topics for shipment transitions
docker exec qp-kafka kafka-topics.sh --bootstrap-server localhost:9092 --create --topic shipment-events --partitions 3 --replication-factor 1 || echo "Topic already exists"

echo "============================================="
echo "  Setup Complete! All local services ready.   "
echo "  Vault: http://localhost:8200               "
echo "  MinIO: http://localhost:9001               "
echo "  Anvil: http://localhost:8545               "
echo "============================================="
