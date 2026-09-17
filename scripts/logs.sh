#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; mkdir -p "$LOGS"; tail -n "${1:-100}" -f "$LOGS/backend.log" "$LOGS/frontend.log"
