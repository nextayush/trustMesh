#!/usr/bin/env bash

# regenerate-web3j-wrappers.sh
# Compiles solidity contracts and generates type-safe Java bindings using Web3j CLI (via Docker)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "============================================="
echo "  Generating Web3j Java Wrappers for EVM     "
echo "============================================="

# 1. Compile Contracts
echo "Compiling smart contracts using Hardhat..."
cd "${PROJECT_ROOT}/contracts"
npx hardhat compile

# 2. Extract ABI & Bytecode BIN
echo "Extracting ABI and Bytecode from Hardhat artifacts..."
node scripts/extract-abi-bin.js

# 3. Create output directory if not exists
mkdir -p "${PROJECT_ROOT}/contracts/generated"

# 4. Generate Wrappers using Web3j Docker CLI
generate_wrapper() {
    local contract_name=$1
    echo "Generating wrapper for ${contract_name}..."
    docker run --rm \
        -v "${PROJECT_ROOT}/contracts:/contracts" \
        web3j/web3j:4.9.4 \
        solidity generate \
        -a "/contracts/build-raw/${contract_name}.abi" \
        -b "/contracts/build-raw/${contract_name}.bin" \
        -o /contracts/generated \
        -p com.quantumprovenance.contracts.generated
}

generate_wrapper "ShipmentRegistry"
generate_wrapper "ProvenanceAnchor"
generate_wrapper "ABACPolicyEngine"

echo "============================================="
echo "  Java Wrappers successfully generated in:  "
echo "  contracts/generated/                       "
echo "============================================="
