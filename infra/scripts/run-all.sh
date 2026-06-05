#!/usr/bin/env bash

# run-all.sh
# Orchestrates the startup of the entire Quantum-Provenance project:
# 1. Spins up Docker dependencies (Postgres, Kafka, MinIO, Vault, Anvil)
# 2. Configures Vault policies, MinIO buckets, and Kafka topics
# 3. Compiles Smart Contracts & deploys them to local Anvil node
# 4. Generates Web3j Java Wrappers
# 5. Compiles and packages Java microservices
# 6. Starts all microservices in the background with stdout logged to files

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_DIR="${PROJECT_ROOT}/logs"

mkdir -p "${LOG_DIR}"

echo "========================================================="
echo "  Starting Quantum-Provenance Master Orchestrator        "
echo "========================================================="

# Helper to check service health
check_health() {
    local service=$1
    local max_retries=12
    local count=0
    until [ "$(docker inspect -f '{{.State.Health.Status}}' "qp-${service}" 2>/dev/null)" == "healthy" ]; do
        count=$((count+1))
        if [ $count -gt $max_retries ]; then
            echo "WARNING: ${service} did not report healthy in time. Continuing..."
            return 1
        fi
        echo "Waiting for qp-${service} container... (${count}/${max_retries})"
        sleep 5
    done
    echo "qp-${service} is healthy!"
    return 0
}

# 1. Start Docker Containers
echo "1. Bootstrapping docker dependencies..."
cd "${PROJECT_ROOT}/infra/docker"
docker compose up -d

echo "Verifying service container health..."
check_health "postgres"
check_health "vault"
check_health "minio"

# 2. Configure Local Environment (Vault/MinIO/Kafka)
echo "2. Setting up services configurations..."
export VAULT_TOKEN="dev-root-token"
export VAULT_ADDR="http://localhost:8200"

# Enable engines
docker exec -e VAULT_TOKEN=dev-root-token qp-vault vault secrets enable transit 2>/dev/null || true
docker exec -e VAULT_TOKEN=dev-root-token qp-vault vault write -f transit/keys/blockchain-signing-key 2>/dev/null || true
docker exec -e VAULT_TOKEN=dev-root-token qp-vault vault write transit/keys/blockchain-signing-key-ecdsa type=ecdsa-p256 2>/dev/null || true

# Policy upload
docker exec -i -e VAULT_TOKEN=dev-root-token qp-vault vault policy write quantum-provenance-policy - <<EOF
path "secret/data/quantum-provenance/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
path "transit/encrypt/*" {
  capabilities = ["update"]
}
path "transit/decrypt/*" {
  capabilities = ["update"]
}
path "transit/sign/*" {
  capabilities = ["update"]
}
path "transit/verify/*" {
  capabilities = ["update"]
}
EOF

# MinIO buckets
docker exec qp-minio mc alias set local http://localhost:9000 minioadmin minioadminpassword
docker exec qp-minio mc mb local/quantum-provenance-manifests 2>/dev/null || true
docker exec qp-minio mc anonymous set download local/quantum-provenance-manifests

# Kafka topics
docker exec qp-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic shipment-events --partitions 3 --replication-factor 1 2>/dev/null || true
docker exec qp-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic blockchain-events --partitions 3 --replication-factor 1 2>/dev/null || true

# --- Helper: Run a Node.js command, tolerating the Windows libuv UV_HANDLE_CLOSING crash ---
# Node.js on Windows (MINGW64) can segfault during process exit cleanup even when all
# work completed successfully. This function runs the command with set +e, then verifies
# success by checking an expected output file/directory exists.
run_node_cmd() {
    local description="$1"
    local verify_path="$2"     # file/dir that should exist after success (empty = skip check)
    shift 2
    echo "  -> ${description}"
    set +e
    "$@" 2>&1
    local rc=$?
    set -e
    if [ $rc -ne 0 ]; then
        if [ -n "${verify_path}" ] && [ -e "${verify_path}" ]; then
            echo "  [WARN] Command exited with code ${rc} (likely Windows libuv crash). Output verified — continuing."
        else
            echo "  [ERROR] Command failed (exit ${rc}) and expected output '${verify_path}' not found."
            exit 1
        fi
    fi
}

# 3. Deploy Smart Contracts
echo "3. Compiling and deploying EVM smart contracts..."
cd "${PROJECT_ROOT}/contracts"
run_node_cmd "Installing npm dependencies" "${PROJECT_ROOT}/contracts/node_modules" npm install
run_node_cmd "Compiling Solidity contracts" "${PROJECT_ROOT}/contracts/artifacts" npx hardhat compile
run_node_cmd "Deploying core contracts" "${PROJECT_ROOT}/contracts/build-raw/deployed-addresses.json" npx hardhat run scripts/deploy-core.ts --network localhost
run_node_cmd "Deploying access contracts" "${PROJECT_ROOT}/contracts/build-raw/deployed-addresses.json" npx hardhat run scripts/deploy-access.ts --network localhost

# 4. Extract ABIs & Generate Web3j wrappers
echo "4. Generating Web3j Java bindings..."
cd "${PROJECT_ROOT}"
run_node_cmd "Extracting ABI and bytecode" "${PROJECT_ROOT}/contracts/build-raw/ShipmentRegistry.abi" node contracts/scripts/extract-abi-bin.js
./maven/bin/mvn exec:java -pl services/blockchain-service "-Dexec.mainClass=com.quantumprovenance.blockchain.web3j.WrapperGenerator"

# 5. Build Java Microservices
echo "5. Building and packaging all Java microservices..."
./maven/bin/mvn clean package -DskipTests "-Dnet.bytebuddy.experimental=true"

# 6. Run Microservices
echo "6. Launching Spring Boot microservices in the background..."

start_service() {
    local name=$1
    local port=$2
    local jar_path=$3
    
    echo "Starting ${name} on port ${port}..."
    nohup java -Dnet.bytebuddy.experimental=true -jar "${jar_path}" > "${LOG_DIR}/${name}.log" 2>&1 &
    echo $! > "${LOG_DIR}/${name}.pid"
}

start_service "crypto-service" "8081" "${PROJECT_ROOT}/services/crypto-service/target/crypto-service-1.0.0-SNAPSHOT.jar"
start_service "storage-service" "8082" "${PROJECT_ROOT}/services/storage-service/target/storage-service-1.0.0-SNAPSHOT.jar"
start_service "blockchain-service" "8083" "${PROJECT_ROOT}/services/blockchain-service/target/blockchain-service-1.0.0-SNAPSHOT.jar"
start_service "shipment-service" "8084" "${PROJECT_ROOT}/services/shipment-service/target/shipment-service-1.0.0-SNAPSHOT.jar"
start_service "api-gateway" "8080" "${PROJECT_ROOT}/services/api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar"

echo "========================================================="
echo "  All microservices launched!                            "
echo "  Logs: ${LOG_DIR}/                                     "
echo "  PIDs: check *.pid in ${LOG_DIR}                        "
echo "  Gateway Endpoint: http://localhost:8080                "
echo "  To stop all services: run 'kill \$(cat ${LOG_DIR}/*.pid)' "
echo "========================================================="
