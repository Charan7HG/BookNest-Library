
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("backend", "frontend")]
    [string]$Mode
)

$projectRoot = $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"

if (-not (Test-Path $envFile)) {
    throw ".env file not found at $envFile"
}

# Load environment variables
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()

    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
        return
    }

    $separator = $line.IndexOf("=")

    if ($separator -le 0) {
        return
    }

    $key = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()

    # Remove surrounding quotes if present
    if (
        ($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))
    ) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    Set-Item -Path "Env:$key" -Value $value
}

if ($Mode -eq "backend") {

    $requiredVars = @(
        "DB_URL",
        "DB_USERNAME",
        "DB_PASSWORD",
        "JWT_SECRET",
        "APP_CORS_ALLOWED_ORIGIN"
    )

    foreach ($key in $requiredVars) {
        $item = Get-Item -Path "Env:$key" -ErrorAction SilentlyContinue

        if ($null -eq $item -or [string]::IsNullOrWhiteSpace($item.Value)) {
            throw "Required environment variable missing: $key"
        }
    }

    Push-Location (Join-Path $projectRoot "library-backend")

    try {
        Write-Host "Starting Library backend..."
        & .\mvnw.cmd spring-boot:run
    }
    finally {
        Pop-Location
    }
}
elseif ($Mode -eq "frontend") {

    $frontendPath = Join-Path $projectRoot "library-frontend"

    Push-Location $frontendPath

    try {
        Write-Host "Starting Library frontend..."
        npm run dev
    }
    finally {
        Pop-Location
    }
}
