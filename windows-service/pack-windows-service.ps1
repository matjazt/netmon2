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

    # Download SvcWatchDog
    $zipUrl = "https://github.com/matjazt/SvcWatchDog/releases/download/v1.1.0/SvcWatchDog.v1.1.0.zip"
    $zipPath = Join-Path $env:TEMP "SvcWatchDog.v1.1.0.zip"
    $extractPath = Join-Path $env:TEMP "SvcWatchDog_Extract"
    
    Write-Host "Downloading SvcWatchDog..."
    Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath
    
    # Extract zip
    if (Test-Path $extractPath) {
        Remove-Item $extractPath -Recurse -Force
    }
    Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force
    
    # Find and copy exe
    $exeFile = Get-ChildItem -Path $extractPath -Filter "*.exe" -Recurse | Select-Object -First 1
    if (-not $exeFile) {
        throw "No exe file found in downloaded zip"
    }
    Copy-Item -Path $exeFile.FullName -Destination "windows-service\service\NetMon2Service.exe" -Force
    
    # Cleanup
    Remove-Item $zipPath -Force
    Remove-Item $extractPath -Recurse -Force

    Write-Host "Packaging completed successfully"
}
catch {
    Write-Error $_.Exception.Message
    exit 1
}
finally {
    Pop-Location
}
