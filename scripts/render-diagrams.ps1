$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
New-Item -ItemType Directory -Path '.\.tools' -Force | Out-Null
$jarPath = Join-Path (Get-Location) '.tools\plantuml-1.2025.10.jar'
$expectedHash = '4A01EA09B317180FB8E7EEF712DFDCA725409D2EE1919E4B5ADFE9D8362B6FE5'
if (-not (Test-Path -LiteralPath $jarPath)) {
    Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/net/sourceforge/plantuml/plantuml/1.2025.10/plantuml-1.2025.10.jar' -OutFile $jarPath
}
if ((Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash -ne $expectedHash) { throw 'Le fichier PlantUML ne correspond pas au SHA-256 attendu.' }
& java '-Djava.awt.headless=true' -jar $jarPath -charset UTF-8 -checkonly 'docs/diagrams/*.puml'
if ($LASTEXITCODE -ne 0) { throw 'Erreur de syntaxe PlantUML.' }
& java '-Djava.awt.headless=true' -jar $jarPath -charset UTF-8 -tsvg 'docs/diagrams/*.puml'
if ($LASTEXITCODE -ne 0) { throw 'Echec du rendu SVG.' }
& java '-Djava.awt.headless=true' -jar $jarPath -charset UTF-8 -tpng 'docs/diagrams/*.puml'
exit $LASTEXITCODE
