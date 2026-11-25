$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
& '.\mvnw.cmd' --batch-mode --no-transfer-progress verify
exit $LASTEXITCODE
