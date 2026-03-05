# MeghaConnect - Build & Deploy Script
# Builds Angular frontend + Spring Boot backend into single deployable JAR

param(
    [switch]$SkipFrontend,
    [switch]$SkipBackend,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MeghaConnect - Build & Deploy Script  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Project paths
$ProjectRoot = $PSScriptRoot
$FrontendDir = Join-Path $ProjectRoot "frontend"
$BackendDir = Join-Path $ProjectRoot "backend"
$StaticDir = Join-Path $BackendDir "src\main\resources\static"
$DistDir = Join-Path $FrontendDir "dist\frontend"

# Clean previous builds
if ($Clean) {
    Write-Host "[CLEAN] Removing previous builds..." -ForegroundColor Yellow
    
    if (Test-Path (Join-Path $FrontendDir "dist")) {
        Remove-Item -Path (Join-Path $FrontendDir "dist") -Recurse -Force
        Write-Host "  Cleaned Angular dist folder" -ForegroundColor Green
    }
    
    if (Test-Path $StaticDir) {
        Get-ChildItem -Path $StaticDir -Exclude ".gitkeep" | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "  Cleaned backend static folder" -ForegroundColor Green
    }
    
    Set-Location $BackendDir
    mvn clean | Out-Null
    Write-Host "  Cleaned Maven target folder" -ForegroundColor Green
    Set-Location $ProjectRoot
    Write-Host ""
}

# Build Angular Frontend
if (-not $SkipFrontend) {
    Write-Host "[STEP 1] Building Angular Frontend (Production)..." -ForegroundColor Yellow
    Set-Location $FrontendDir
    
    ng build --configuration production
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Angular build FAILED!" -ForegroundColor Red
        Set-Location $ProjectRoot
        exit 1
    }
    
    if (-not (Test-Path $DistDir)) {
        Write-Host "Dist folder not found!" -ForegroundColor Red
        Set-Location $ProjectRoot
        exit 1
    }
    
    $distSize = (Get-ChildItem -Path $DistDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Host "Angular build completed: $([math]::Round($distSize, 2)) MB" -ForegroundColor Green
    Set-Location $ProjectRoot
    Write-Host ""
}

# Copy Frontend to Backend Static
if (-not $SkipFrontend) {
    Write-Host "[STEP 2] Copying Frontend to Backend Static Folder..." -ForegroundColor Yellow
    
    if (-not (Test-Path $StaticDir)) {
        New-Item -Path $StaticDir -ItemType Directory -Force | Out-Null
    }
    
    Get-ChildItem -Path $StaticDir -Exclude ".gitkeep" | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    
    # Copy from browser subfolder (Angular 17+ esbuild output structure)
    $browserDir = Join-Path $DistDir "browser"
    if (Test-Path $browserDir) {
        Copy-Item -Path "$browserDir\*" -Destination $StaticDir -Recurse -Force
        Write-Host "Copied from browser subfolder (Angular 17+ structure)" -ForegroundColor Cyan
    } else {
        # Fallback for older Angular versions (direct dist output)
        Copy-Item -Path "$DistDir\*" -Destination $StaticDir -Recurse -Force
        Write-Host "Copied from dist root (legacy Angular structure)" -ForegroundColor Cyan
    }
    
    $staticFiles = (Get-ChildItem -Path $StaticDir -Recurse -File | Measure-Object).Count
    Write-Host "Copied $staticFiles files to static folder" -ForegroundColor Green
    Write-Host ""
}

# Build Spring Boot JAR
if (-not $SkipBackend) {
    Write-Host "[STEP 3] Building Spring Boot JAR (with embedded frontend)..." -ForegroundColor Yellow
    Set-Location $BackendDir
    
    mvn clean package -DskipTests
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Maven build FAILED!" -ForegroundColor Red
        Set-Location $ProjectRoot
        exit 1
    }
    
    $jarFile = Get-ChildItem -Path (Join-Path $BackendDir "target") -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
    
    if ($jarFile) {
        $jarSize = $jarFile.Length / 1MB
        Write-Host "JAR created: $($jarFile.Name)" -ForegroundColor Green
        Write-Host "JAR size: $([math]::Round($jarSize, 2)) MB" -ForegroundColor Green
        Write-Host "Location: $($jarFile.FullName)" -ForegroundColor Cyan
    } else {
        Write-Host "JAR file not found!" -ForegroundColor Red
        Set-Location $ProjectRoot
        exit 1
    }
    
    Set-Location $ProjectRoot
    Write-Host ""
}

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BUILD COMPLETED SUCCESSFULLY!  " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Deployment Package Ready:" -ForegroundColor Yellow
Write-Host ""

if (Test-Path (Join-Path $BackendDir "target")) {
    $jarFile = Get-ChildItem -Path (Join-Path $BackendDir "target") -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
    if ($jarFile) {
        Write-Host "JAR File: backend\target\$($jarFile.Name)" -ForegroundColor White
        Write-Host "Includes: Backend API + Frontend UI (embedded)" -ForegroundColor White
        Write-Host ""
        Write-Host "How to Deploy:" -ForegroundColor Yellow
        Write-Host "1. Copy JAR to target machine" -ForegroundColor Gray
        Write-Host "2. Ensure Java 8+ and MySQL 8+ are installed" -ForegroundColor Gray
        Write-Host "3. CREATE DATABASE meghaconnect;" -ForegroundColor Gray
        Write-Host "4. Run:" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   java -jar $($jarFile.Name) `` " -ForegroundColor Cyan
        Write-Host "     --spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect ``" -ForegroundColor Cyan
        Write-Host "     --spring.datasource.username=root ``" -ForegroundColor Cyan
        Write-Host "     --spring.datasource.password=yourpassword ``" -ForegroundColor Cyan
        Write-Host "     --app.jwt.secret=MeghaConnect2026SecureJwtSecretKeyForProductionUseOnly12345" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "5. Access: http://localhost:8080" -ForegroundColor Gray
        Write-Host ""
    }
}

Write-Host "Default Logins: hcm/hcm123, admin/admin123, deo1/deo123" -ForegroundColor Yellow
Write-Host "See: docs\DEPLOYMENT.md for detailed instructions" -ForegroundColor Gray
Write-Host ""
