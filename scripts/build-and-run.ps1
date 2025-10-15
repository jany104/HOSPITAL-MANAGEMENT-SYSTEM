param(
    [string]$ConnectorPath = ""
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path -Path (Join-Path $PSScriptRoot ".." )).Path
Set-Location $projectRoot

$outDir = Join-Path $projectRoot "out"
if (Test-Path $outDir) {
    Remove-Item $outDir -Recurse -Force
}

Write-Host "Compiling sources..."
$sourceDir = Join-Path $projectRoot "src\hospital\management\system"
$sources = Get-ChildItem -Path $sourceDir -Filter *.java | ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) {
    throw "No Java source files found under $sourceDir"
}
javac -d $outDir $sources

$assetsSource = Join-Path $projectRoot "src\hospital\management\system\illustrations"
if (Test-Path $assetsSource) {
    $assetsDestination = Join-Path $outDir "hospital\management\system\illustrations"
    if (!(Test-Path $assetsDestination)) {
        New-Item -ItemType Directory -Path $assetsDestination -Force | Out-Null
    }
    Get-ChildItem -Path $assetsSource -File | Where-Object { $_.Extension -in '.svg', '.png' } | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination $assetsDestination -Force
    }
}

if ([string]::IsNullOrWhiteSpace($ConnectorPath)) {
    $ConnectorPath = Join-Path $projectRoot "mysql-connector-java-8.0.28.jar"
}

if (!(Test-Path $ConnectorPath)) {
    throw "MySQL Connector/J JAR not found. Specify the path via -ConnectorPath."
}

Write-Host "Running application..."
$classpath = '"' + $outDir + ';' + $ConnectorPath + '"'
java -cp $classpath hospital.management.system.Login
