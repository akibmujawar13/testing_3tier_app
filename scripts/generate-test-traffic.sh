#!/usr/bin/env bash
set -euo pipefail; source "$(dirname "$0")/lib.sh"; load_env; need curl; base="http://localhost:$BACKEND_PORT/api/v1"; ok=0; fail=0
hit(){ if curl -sS -o /dev/null -w '%{http_code}' "$1" | grep -Eq '^(200|201|404|422|500|502|504)$'; then ok=$((ok+1)); else fail=$((fail+1)); fi; }
for ((i=1;i<=TRAFFIC_REQUESTS;i++)); do hit "$base/products?q=enterprise"; hit "$base/products/$((1+i%100))"; hit "$base/customers?q=customer"; hit "$base/orders/$((1+i%200))"; [ $((i%10)) -eq 0 ] && hit "$base/diagnostics/slow"; [ $((i%15)) -eq 0 ] && hit "$base/diagnostics/server-error"; sleep "0.$(printf '%03d' "$TRAFFIC_DELAY_MS")" || true; done
echo "Traffic complete: $ok observed responses, $fail failed transport requests. Requests=$TRAFFIC_REQUESTS"
