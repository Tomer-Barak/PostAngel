#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Creates a GitHub release with the APK file for PostMuse app
.DESCRIPTION
    This script builds a release APK and guides you through creating a GitHub release
#>

# Parameters
param(
    [Parameter(Mandatory=$true)]
    [string]$VersionName,
    
    [Parameter(Mandatory=$true)]
    [int]$VersionCode,
    
    [Parameter(Mandatory=$false)]
    [string]$ReleaseNotes = "New release of PostMuse"
)

# Configuration
$RepoOwner = "Tomer-Barak"
$RepoName = "PostMuse"
$GithubRepoUrl = "https://github.com/$RepoOwner/$RepoName"
$ApkPath = "app/build/outputs/apk/release/app-release.apk"
$TagName = "v$VersionName"

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

# Step 2: Instructions for GitHub release
Write-Host "`nAPK successfully built at: $ApkPath" -ForegroundColor Green
Write-Host "`nNow follow these steps to create a GitHub release:" -ForegroundColor Cyan
Write-Host "1. Go to: $GithubRepoUrl/releases/new" -ForegroundColor White
Write-Host "2. Enter tag version: $TagName" -ForegroundColor White
Write-Host "3. Enter release title: PostMuse $VersionName" -ForegroundColor White
Write-Host "4. Enter release description with the following notes:" -ForegroundColor White
Write-Host "   $ReleaseNotes" -ForegroundColor White
Write-Host "5. Upload the APK from: $ApkPath" -ForegroundColor White
Write-Host "6. Click 'Publish release'" -ForegroundColor White

Write-Host "`nWould you like to open the GitHub release page now? (Y/n)" -ForegroundColor Yellow
$response = Read-Host
if ($response -ne "n") {
    Start-Process "$GithubRepoUrl/releases/new"
}
