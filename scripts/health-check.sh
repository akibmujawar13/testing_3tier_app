#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; load_env; failed=0
check(){ if curl -fsS "$2" >/dev/null; then echo "[OK] $1"; else echo "[FAIL] $1"; failed=1; fi; }
check 'Frontend' "http://localhost:$FRONTEND_PORT"; check 'Backend actuator' "http://localhost:$BACKEND_PORT/actuator/health"; check 'API health' "http://localhost:$BACKEND_PORT/api/v1/health"; check 'Database API health' "http://localhost:$BACKEND_PORT/api/v1/health/database"; check 'Mock payment' "http://localhost:$BACKEND_PORT/api/v1/health/external-services"
exit "$failed"
