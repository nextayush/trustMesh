#!/usr/bin/env bash

# run.sh
# Master startup script for the Quantum-Provenance project.
# Ensures JAVA_HOME, file permissions, and line endings are correct,
# then delegates to the orchestrator.

set -euo pipefail

# Locate project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================================="
echo "   Quantum-Provenance Master Entrypoint                  "
echo "========================================================="

# 0. Auto-detect JAVA_HOME if not set
if [ -z "${JAVA_HOME:-}" ]; then
    echo "JAVA_HOME not set — auto-detecting from 'java' on PATH..."
    if command -v java >/dev/null 2>&1; then
        # Use java itself to report java.home (works on all OSes)
        DETECTED_JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 | grep 'java.home' | awk -F'= ' '{print $2}' | tr -d '\r')"
        if [ -n "${DETECTED_JAVA_HOME}" ]; then
            export JAVA_HOME="${DETECTED_JAVA_HOME}"
            echo "JAVA_HOME set to: ${JAVA_HOME}"
        else
            echo "WARNING: Could not auto-detect JAVA_HOME. Maven may fail."
        fi
    else
        echo "ERROR: 'java' not found on PATH. Please install JDK 21+ and set JAVA_HOME."
        exit 1
    fi
else
    echo "JAVA_HOME: ${JAVA_HOME}"
fi

# 1. Normalize line endings (CRLF -> LF) to prevent Windows line-ending errors in Bash
echo "Ensuring UNIX-style line endings for helper scripts..."
if command -v sed >/dev/null 2>&1; then
    find "${PROJECT_ROOT}/infra/scripts" -type f -name "*.sh" -exec sed -i 's/\r$//' {} + 2>/dev/null || true
    sed -i 's/\r$//' "${PROJECT_ROOT}/maven/bin/mvn" 2>/dev/null || true
fi

# 2. Grant execution permissions
echo "Granting execution permissions..."
chmod +x "${PROJECT_ROOT}/infra/scripts/"*.sh 2>/dev/null || true
chmod +x "${PROJECT_ROOT}/maven/bin/mvn" 2>/dev/null || true

# 3. Invoke orchestrator
echo "Delegating execution to infra/scripts/run-all.sh..."
echo "---------------------------------------------------------"
bash "${PROJECT_ROOT}/infra/scripts/run-all.sh"
