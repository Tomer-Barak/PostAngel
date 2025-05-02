#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Complete release script for PostAngel v0.2.0
.DESCRIPTION
    This script:
    1. Builds a release APK
    2. Creates a new tag v0.2.0
    3. Commits and pushes the current state
    4. Creates a GitHub release with the APK file
#>

# Configuration
$VersionName = "0.2.0"
$VersionCode = 2
$RepoOwner = "Tomer-Barak"
$RepoName = "PostAngel"
$GithubRepoUrl = "https://github.com/$RepoOwner/$RepoName"
$ApkPath = "app/build/outputs/apk/release/app-release.apk"
$TagName = "v$VersionName"
$ReleaseNotes = "PostAngel v0.2.0 release featuring:
- Added support for custom APIs and models
- Added Markdown support
"

# Check if git is installed
Write-Host "Checking if git is installed..." -ForegroundColor Yellow
if (-not (Get-Command "git" -ErrorAction SilentlyContinue)) {
    Write-Host "Git is not installed. Please install git and try again." -ForegroundColor Red
    exit 1
}

# Step 1: Build the APK
Write-Host "Building release APK..." -ForegroundColor Yellow
./gradlew assembleRelease
if (-not $?) {
    Write-Host "Failed to build APK. Exiting." -ForegroundColor Red
    exit 1
}

# Check if APK exists
if (-not (Test-Path $ApkPath)) {
    Write-Host "APK not found at $ApkPath. Build may have failed." -ForegroundColor Red
    exit 1
}

# Step 2: Create a new tag
Write-Host "Creating a new tag: $TagName..." -ForegroundColor Yellow
git tag $TagName
if (-not $?) {
    Write-Host "Failed to create tag. Exiting." -ForegroundColor Red
    exit 1
}

# Step 3: Commit and push changes
Write-Host "Committing and pushing changes..." -ForegroundColor Yellow
git add .
git commit -m "Release v$VersionName - Added custom APIs, models, and Markdown support"
git push origin main
git push origin $TagName
if (-not $?) {
    Write-Host "Failed to push changes. Exiting." -ForegroundColor Red
    exit 1
}

# Step 4: Create a GitHub release
Write-Host "Creating a GitHub release..." -ForegroundColor Yellow
Write-Host "`nAPK successfully built at: $ApkPath" -ForegroundColor Green
Write-Host "`nNow follow these steps to create a GitHub release:" -ForegroundColor Cyan
Write-Host "1. Go to: $GithubRepoUrl/releases/new" -ForegroundColor White
Write-Host "2. Enter tag version: $TagName" -ForegroundColor White
Write-Host "3. Enter release title: PostAngel $VersionName" -ForegroundColor White
Write-Host "4. Enter release description with the following notes:" -ForegroundColor White
Write-Host "   $ReleaseNotes" -ForegroundColor White
Write-Host "5. Upload the APK from: $ApkPath" -ForegroundColor White

Write-Host "`nRelease process completed successfully!" -ForegroundColor Green
