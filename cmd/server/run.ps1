# One-command local run script for Payment Service (Windows)
# Usage: .\run.ps1

$ErrorActionPreference = "Stop"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Payment Service - One-Command Local Run" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Check if Java 21 is installed
Write-Host "Checking Java version... " -NoNewline
try {
    $javaVersion = (java -version 2>&1 | Select-String -Pattern 'version' | ForEach-Object { $_ -replace '.*"(\d+).*', '$1' })[0]
    if ([int]$javaVersion -ge 21) {
        Write-Host "✓ Java $javaVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ Java 21 or higher required (found Java $javaVersion)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Java not found" -ForegroundColor Red
    Write-Host "Please install Java 21 or higher"
    exit 1
}

# Navigate to project root
Set-Location (Join-Path $PSScriptRoot "..\..") -ErrorAction Stop

# Check if env.properties exists
if (-not (Test-Path "src\main\resources\env.properties")) {
    Write-Host "⚠ env.properties not found, creating from example..." -ForegroundColor Yellow

    if (Test-Path "src\main\resources\env.properties.example") {
        Copy-Item "src\main\resources\env.properties.example" "src\main\resources\env.properties"
        Write-Host "✓ Created env.properties" -ForegroundColor Green
    } else {
        Write-Host "Creating default env.properties..."
        @"
db.url=jdbc:sqlite:payment.db
db.username=
db.password=
"@ | Out-File -FilePath "src\main\resources\env.properties" -Encoding UTF8
        Write-Host "✓ Created default env.properties" -ForegroundColor Green
    }
}

# Clean and build
Write-Host ""
Write-Host "Building application..."
.\mvnw.cmd clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Build successful" -ForegroundColor Green
Write-Host ""

# Find the JAR file
$jarFile = Get-ChildItem -Path "target" -Filter "payment-service-*.jar" | Select-Object -First 1

if ($null -eq $jarFile) {
    Write-Host "✗ JAR file not found in target directory" -ForegroundColor Red
    exit 1
}

# Run the application
Write-Host "Starting Payment Service..."
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Ports:"
Write-Host "  - gRPC Service: http://localhost:9090"
Write-Host "  - Actuator (Health/Metrics): http://localhost:8081"
Write-Host ""
Write-Host "Health Check: http://localhost:8081/actuator/health"
Write-Host "Metrics: http://localhost:8081/actuator/metrics"
Write-Host ""
Write-Host "Press Ctrl+C to stop"
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

java -jar $jarFile.FullName

