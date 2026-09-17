#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; load_env; need java; need npm; need psql; mkdir -p "$RUN" "$LOGS"
if pid_running "$RUN/backend.pid" || pid_running "$RUN/frontend.pid"; then echo 'ERROR: application is already running. Use scripts/status.sh.'; exit 1; fi
export PGPASSWORD="$DB_PASSWORD"; psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -c 'SELECT 1' >/dev/null || { echo 'ERROR: PostgreSQL connectivity failed. Run scripts/seed-db.sh after creating the DB.'; exit 1; }
[ -f "$ROOT/backend/target/commerce-platform-1.0.0.jar" ] || (cd "$ROOT/backend" && mvn -q -DskipTests package)
nohup java -jar "$ROOT/backend/target/commerce-platform-1.0.0.jar" >"$LOGS/backend.log" 2>&1 & echo $! >"$RUN/backend.pid"
for i in {1..30}; do curl -fsS "http://localhost:${BACKEND_PORT}/actuator/health" >/dev/null && break; sleep 1; done
curl -fsS "http://localhost:${BACKEND_PORT}/actuator/health" >/dev/null || { echo 'Backend did not become healthy; see logs/backend.log'; exit 1; }
[ -d "$ROOT/frontend/node_modules" ] || (cd "$ROOT/frontend" && npm install)
nohup npm --prefix "$ROOT/frontend" run dev -- -p "$FRONTEND_PORT" >"$LOGS/frontend.log" 2>&1 & echo $! >"$RUN/frontend.pid"
for i in {1..30}; do curl -fsS "http://localhost:${FRONTEND_PORT}" >/dev/null && break; sleep 1; done
curl -fsS "http://localhost:${FRONTEND_PORT}" >/dev/null || { echo 'Frontend did not become available; see logs/frontend.log'; exit 1; }; echo "Running: frontend http://localhost:$FRONTEND_PORT | backend http://localhost:$BACKEND_PORT"
