#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; load_env; need psql
export PGPASSWORD="$DB_PASSWORD"; PSQL=(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -v ON_ERROR_STOP=1)
"${PSQL[@]}" -f "$ROOT/database/schema.sql"; "${PSQL[@]}" -f "$ROOT/database/seed.sql"; echo "Database schema and deterministic seed data loaded."
