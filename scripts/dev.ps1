# Levanta MySQL en el puerto 3307 y arranca el backend con el perfil local.
# Uso: .\scripts\dev.ps1
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path ".env")) {
    Write-Error "Falta el archivo .env. Copia .env.example y completa las claves."
    exit 1
}

# Spring no lee .env: lo parseamos y exportamos al proceso.
Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }

    $parts = $line -split "=", 2
    if ($parts.Count -ne 2) { return }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    if ($value.Length -ge 2) {
        $first = $value[0]
        $last = $value[$value.Length - 1]
        if (($first -eq "'" -and $last -eq "'") -or ($first -eq '"' -and $last -eq '"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }
    }
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE = "local" }
if (-not $env:DB_URL) { $env:DB_URL = "jdbc:mysql://localhost:3307/tecnofactory" }
if (-not $env:DB_USER) { $env:DB_USER = "root" }

# No arranca el backend hasta que MySQL esté healthy.
docker compose up -d --wait db
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& .\mvnw.cmd spring-boot:run
exit $LASTEXITCODE
