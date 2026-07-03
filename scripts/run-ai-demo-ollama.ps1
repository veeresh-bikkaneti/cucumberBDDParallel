#Requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (-not $env:AI_HEALING_PROVIDER) { $env:AI_HEALING_PROVIDER = "ollama" }
$env:AI_HEALING_DEMO_LIVE = "true"
Set-Location (Join-Path $PSScriptRoot "..")
& .\mvnw.cmd -pl examples/ai-healing-demo -am test -Plive-ai-demo