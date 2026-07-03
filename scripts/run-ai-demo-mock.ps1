#Requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
& .\mvnw.cmd -pl examples/ai-healing-demo -am test "-Dtest=MockAiHealingDemoTest"