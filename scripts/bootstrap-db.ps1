param(
    [string]$User = "root",
    [string]$Password = "",
    [string]$Host = "localhost",
    [int]$Port = 3306,
    [string]$Schema = "hospital_management_system"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    throw "The mysql CLI is not on the PATH. Install MySQL or add the bin directory to PATH."
}

if ([string]::IsNullOrEmpty($Password)) {
    $secure = Read-Host -AsSecureString "Enter MySQL password for user '$User'"
    $Password = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    )
}

$projectRoot = (Resolve-Path -Path (Join-Path $PSScriptRoot ".." )).Path
$schemaFile = Join-Path $projectRoot "sql\schema.sql"
$seedFile = Join-Path $projectRoot "sql\seed-data.sql"

foreach ($file in @($schemaFile, $seedFile)) {
    if (-not (Test-Path $file)) {
        throw "SQL file not found: $file"
    }
}

function Invoke-MysqlScript {
    param(
        [string]$ScriptPath,
        [bool]$UseSchema = $false
    )

    $arguments = @(
        "--host=$Host",
        "--port=$Port",
        "--user=$User",
        "--password=$Password"
    )

    if ($UseSchema) {
        $arguments += "--database=$Schema"
    }

    Write-Host "Running $($ScriptPath.Substring($projectRoot.Length + 1))"
    & mysql @arguments < $ScriptPath
}

Invoke-MysqlScript -ScriptPath $schemaFile
Invoke-MysqlScript -ScriptPath $seedFile -UseSchema $true

Write-Host "Database bootstrap complete for schema '$Schema'."
