#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; load_env; need mvn; need npm
(cd "$ROOT/backend" && mvn -q -DskipTests package)
(cd "$ROOT/frontend" && npm install && npm run build)
echo "Build completed."
