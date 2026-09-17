#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; need mvn; (cd "$ROOT/backend" && mvn test); echo 'Backend test suite passed.'
