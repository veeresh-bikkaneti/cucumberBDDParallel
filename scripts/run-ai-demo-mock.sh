#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -pl examples/ai-healing-demo -am test -Dtest=MockAiHealingDemoTest