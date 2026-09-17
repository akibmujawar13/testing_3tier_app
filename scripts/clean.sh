#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; "$ROOT/scripts/stop.sh"; rm -rf "$ROOT/backend/target" "$ROOT/frontend/.next"; echo 'Build output cleaned; database retained.'
