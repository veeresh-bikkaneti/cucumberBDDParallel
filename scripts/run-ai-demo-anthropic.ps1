#Requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (-not $env:AI_HEALING_PROVIDER) { $env:AI_HEALING_PROVIDER = "anthropic" }
if (-not $env:AI_HEALING_DEMO_LIVE) { $env:AI_HEALING_DEMO_LIVE = "true" }
if ([string]::IsNullOrEmpty($env:AI_HEALING_API_KEY) -and [string]::IsNullOrEmpty($env:ANTHROPIC_API_KEY)) {
  Write-Error "Set AI_HEALING_API_KEY or ANTHROPIC_API_KEY"
  exit 1
}
Set-Location (Join-Path $PSScriptRoot "..")
& .\mvnw.cmd -pl examples/ai-healing-demo -am test -Plive-ai-demo
