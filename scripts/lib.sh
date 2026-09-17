#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; RUN="$ROOT/run"; LOGS="$ROOT/logs"
load_env(){ [ -f "$ROOT/.env" ] || cp "$ROOT/.env.example" "$ROOT/.env"; set -a; . "$ROOT/.env"; set +a; }
pid_running(){ [ -f "$1" ] && kill -0 "$(cat "$1")" 2>/dev/null; }
need(){ command -v "$1" >/dev/null 2>&1 || { echo "ERROR: $1 is required." >&2; exit 1; }; }
