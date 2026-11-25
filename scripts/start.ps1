param([int]$Port = 8080)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Installez un JDK 21 et rouvrez PowerShell.' }
if (Test-Path -LiteralPath '.\chargeslot.jar') {
    & java -jar '.\chargeslot.jar' "--server.port=$Port"
} else {
    & '.\mvnw.cmd' spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
}
exit $LASTEXITCODE
