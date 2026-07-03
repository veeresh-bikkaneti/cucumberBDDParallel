#!/usr/bin/env bash
set -euo pipefail
: "${AI_HEALING_PROVIDER:=anthropic}"
: "${AI_HEALING_DEMO_LIVE:=true}"
export AI_HEALING_PROVIDER AI_HEALING_DEMO_LIVE
if [[ -z "${AI_HEALING_API_KEY:-}" && -z "${ANTHROPIC_API_KEY:-}" ]]; then
  echo "Set AI_HEALING_API_KEY or ANTHROPIC_API_KEY" >&2
  exit 1
fi
cd "$(dirname "$0")/.."
./mvnw -pl examples/ai-healing-demo -am test -Plive-ai-demo