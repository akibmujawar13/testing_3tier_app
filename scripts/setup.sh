#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; load_env
echo '========================================'; echo ' Observability Commerce Platform Setup'; echo '========================================'
need java; need mvn; need node; need npm; need psql
JAVA_VERSION="$(java -version 2>&1 | head -1)"; [[ "$JAVA_VERSION" == *'"1.8.'* ]] || { echo "ERROR: Java 8 is required; found: $JAVA_VERSION"; exit 1; }
echo '[OK] Java 8, Maven, Node.js, npm, and PostgreSQL client detected.'
"$ROOT/scripts/build.sh"; echo 'Setup completed successfully. Run scripts/seed-db.sh, then scripts/start.sh.'
