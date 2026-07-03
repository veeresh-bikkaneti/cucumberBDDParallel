#!/usr/bin/env bash
set -euo pipefail
: "${AI_HEALING_PROVIDER:=ollama}"
: "${AI_HEALING_DEMO_LIVE:=true}"
export AI_HEALING_PROVIDER AI_HEALING_DEMO_LIVE
cd "$(dirname "$0")/.."
./mvnw -pl examples/ai-healing-demo -am test -Plive-ai-demo