#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Complete release script for PostAngel v0.1.0
.DESCRIPTION
    This script:
    1. Deletes all existing GitHub tags
    2. Builds a release APK
    3. Creates a new tag v0.1.0
    4. Commits and pushes the current state
    5. Creates a GitHub release with the APK file
#>

# Configuration
$VersionName = "0.1.0"
$VersionCode = 1
$RepoOwner = "Tomer-Barak"
$RepoName = "PostAngel"
$GithubRepoUrl = "https://github.com/$RepoOwner/$RepoName"
$ApkPath = "app/build/outputs/apk/release/app-release.apk"
$TagName = "v$VersionName"
$ReleaseNotes = "Initial v0.1.0 release of PostAngel"

# Check if git is installed
Write-Host "Checking if git is installed..." -ForegroundColor Yellow
if (-not (Get-Command "git" -ErrorAction SilentlyContinue)) {
    Write-Host "Git is not installed. Please install git and try again." -ForegroundColor Red
    exit 1
}

# Step 1: Delete all existing tags
Write-Host "Deleting all existing GitHub tags..." -ForegroundColor Yellow
$confirmation = Read-Host "This will delete ALL existing tags from the remote repository. Are you sure? (y/N)"
if ($confirmation -eq "y") {
    # Get all remote tags
    $remoteTags = git ls-remote --tags origin
    if (-not $?) {
        Write-Host "Failed to get remote tags. Ensure you have proper access to the repository." -ForegroundColor Red
        exit 1
    }
    
    # Extract the tag names
    $tagNames = $remoteTags | ForEach-Object {
        if ($_ -match 'refs/tags/(.+)') {
            $matches[1]
        }
    }
    
    # Delete each tag locally and remotely
    foreach ($tag in $tagNames) {
        if ($tag -notlike '*^{}') { # Skip annotated tag objects
            Write-Host "Deleting tag: $tag" -ForegroundColor White
            git tag -d $tag 2>$null
            git push origin --delete $tag 2>$null
        }
    }
    
    Write-Host "All tags have been deleted" -ForegroundColor Green
} else {
    Write-Host "Tag deletion canceled" -ForegroundColor Yellow
}

# Step 2: Build the APK
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

# Step 3: Commit any pending changes
Write-Host "Committing pending changes..." -ForegroundColor Yellow
git add .
git commit -m "Release v$VersionName"
if (-not $?) {
    Write-Host "Failed to commit changes. There might be no changes to commit or git config issue." -ForegroundColor Yellow
    $continueAnyway = Read-Host "Continue anyway? (y/N)"
    if ($continueAnyway -ne "y") {
        exit 1
    }
}

# Step 4: Create new tag
Write-Host "Creating new tag v$VersionName..." -ForegroundColor Yellow
git tag -a "v$VersionName" -m "Version $VersionName"
if (-not $?) {
    Write-Host "Failed to create tag. Exiting." -ForegroundColor Red
    exit 1
}

# Step 5: Push changes and tag
Write-Host "Pushing changes and tag to GitHub..." -ForegroundColor Yellow
git push
git push origin "v$VersionName"
if (-not $?) {
    Write-Host "Failed to push to GitHub. Exiting." -ForegroundColor Red
    exit 1
}

# Step 6: Instructions for GitHub release
Write-Host "`nAPK successfully built at: $ApkPath" -ForegroundColor Green
Write-Host "`nNow follow these steps to create a GitHub release:" -ForegroundColor Cyan
Write-Host "1. Go to: $GithubRepoUrl/releases/new?tag=v$VersionName" -ForegroundColor White
Write-Host "2. Enter release title: PostAngel $VersionName" -ForegroundColor White
Write-Host "3. Enter release description with the following notes:" -ForegroundColor White
Write-Host "   $ReleaseNotes" -ForegroundColor White
Write-Host "4. Upload the APK from: $ApkPath" -ForegroundColor White
Write-Host "5. Click 'Publish release'" -ForegroundColor White

Write-Host "`nWould you like to open the GitHub release page now? (Y/n)" -ForegroundColor Yellow
$response = Read-Host
if ($response -ne "n") {
    Start-Process "$GithubRepoUrl/releases/new?tag=v$VersionName"
}

Write-Host "`nRelease process completed successfully!" -ForegroundColor Green
