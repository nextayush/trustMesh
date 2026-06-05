#!/usr/bin/env bash

# run-integration-tests.sh
# Runs test suites across all modules in correct dependency order

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "============================================="
echo "  Starting Quantum-Provenance E2E Verification"
echo "============================================="

# 1. Run Solidity Tests
echo "Step 1: Running Smart Contract unit tests..."
cd "${PROJECT_ROOT}/contracts"
npm run test

# 2. Run Java Microservices compilations and tests
echo "Step 2: Building parent POM and compiling Java services..."
cd "${PROJECT_ROOT}"
./maven/bin/mvn compile

# 3. Cryptography Tests
echo "Step 3: Verifying Bouncy Castle Post-Quantum Cryptography (ML-KEM/Dilithium)..."
./maven/bin/mvn test -pl services/crypto-service

# 4. Storage Service integration tests (MinIO Upload/Download)
echo "Step 4: Verifying MinIO content-addressable storage adapter..."
./maven/bin/mvn test -pl services/storage-service -Dtest=MinIOStorageAdapterIT

# 5. Blockchain Service integration tests (EVM + Web3j + ABAC)
echo "Step 5: Verifying Web3j EVM on-chain transactions..."
./maven/bin/mvn test -pl services/blockchain-service -Dtest=ShipmentAnchorServiceIT

# 6. Shipment Service unit tests
echo "Step 6: Verifying Shipment orchestrator logic..."
./maven/bin/mvn test -pl services/shipment-service "-Dnet.bytebuddy.experimental=true"

# 7. Gateway Security tests
echo "Step 7: Verifying API Gateway security and ABAC policies..."
./maven/bin/mvn test -pl services/api-gateway

echo "============================================="
echo "  All verification steps completed successfully!"
echo "============================================="
