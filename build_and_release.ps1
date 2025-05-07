# Build and Release Script for PostAngel
# This script will:
# 1. Clean the project
# 2. Build a release APK
# 3. Commit changes to git
# 4. Create a git tag
# 5. Push changes and tags to remote

# Set error action preference
$ErrorActionPreference = "Stop"

Write-Host "PostAngel v0.2.2 Build and Release Script" -ForegroundColor Cyan

# Step 1: Clean the project
Write-Host "Cleaning project..." -ForegroundColor Green
./gradlew clean
if (-not $?) {
    Write-Host "Error cleaning project!" -ForegroundColor Red
    exit 1
}

# Step 2: Build release APK
Write-Host "Building release APK..." -ForegroundColor Green
./gradlew assembleRelease
if (-not $?) {
    Write-Host "Error building release APK!" -ForegroundColor Red
    exit 1
}

# Check if APK was created
$apkPath = "./app/build/outputs/apk/release/app-release.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "Release APK not found at expected location: $apkPath" -ForegroundColor Red
    exit 1
} else {
    Write-Host "Release APK successfully built at: $apkPath" -ForegroundColor Green
}

# Step 3: Commit changes to git
Write-Host "Committing changes to git..." -ForegroundColor Green
$commitMessage = "Version 0.2.2: Added toggle mode button in Create Post + fixed initial mode display"
git add .
git commit -m "$commitMessage"
if (-not $?) {
    Write-Host "Error committing changes!" -ForegroundColor Red
    Write-Host "You may need to commit changes manually." -ForegroundColor Yellow
    exit 1
}

# Step 4: Create a git tag
Write-Host "Creating git tag v0.2.2..." -ForegroundColor Green
git tag -a v0.2.2 -m "Version 0.2.2"
if (-not $?) {
    Write-Host "Error creating git tag!" -ForegroundColor Red
    exit 1
}

# Step 5: Push changes and tags to remote
Write-Host "Do you want to push changes and tags to remote? (y/n)" -ForegroundColor Yellow
$response = Read-Host
if ($response -eq "y") {
    Write-Host "Pushing changes to remote..." -ForegroundColor Green
    git push
    git push --tags
    if (-not $?) {
        Write-Host "Error pushing to remote!" -ForegroundColor Red
        exit 1
    }
    Write-Host "Changes and tags successfully pushed to remote!" -ForegroundColor Green
} else {
    Write-Host "Skipping push to remote. Don't forget to push manually!" -ForegroundColor Yellow
}

Write-Host "Build and release process completed successfully!" -ForegroundColor Cyan
Write-Host "Release APK is available at: $apkPath" -ForegroundColor Green
