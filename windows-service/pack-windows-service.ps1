$ErrorActionPreference = "Stop"

# Determine project root
$scriptDir = $PSScriptRoot
if ($scriptDir -match "windows-service$") {
    $projectRoot = Split-Path -Parent $scriptDir
}
else {
    $projectRoot = $scriptDir
}

# Change to project root
Push-Location $projectRoot

try {
    # Verify we're in the right place
    if (-not (Test-Path "build.gradle.kts")) {
        throw "build.gradle.kts not found"
    }

    # Create bin directory
    $binDir = Join-Path $projectRoot "windows-service\bin"
    New-Item -ItemType Directory -Path $binDir -Force | Out-Null

    # Build JAR
    & .\gradlew.bat clean bootJar
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed"
    }

    # Copy JAR
    $jarPath = "build\libs\netmon2-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jarPath)) {
        throw "JAR file not found"
    }
    Copy-Item -Path $jarPath -Destination "$binDir\netmon2-0.0.1-SNAPSHOT.jar" -Force

    # Copy .env 
    Copy-Item -Path ".env" -Destination "windows-service\.env" -Force

    Write-Host "Packaging completed successfully"
}
catch {
    Write-Error $_.Exception.Message
    exit 1
}
finally {
    Pop-Location
}
