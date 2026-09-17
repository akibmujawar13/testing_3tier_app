#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; for name in backend frontend; do p="$RUN/$name.pid"; if pid_running "$p"; then kill "$(cat "$p")"; echo "Stopped $name."; else echo "$name not running."; fi; rm -f "$p"; done
