# Run this script to use Android Studio's Java to run Gradle commands

Write-Host "Running Gradle command with Android Studio's Java..." -ForegroundColor Green

# Find Android Studio's Java
$asJavaPath = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
$androidStudioPaths = @(
    "C:\Program Files\Android\Android Studio\jbr\bin\java.exe",
    "${env:ProgramFiles}\Android\Android Studio\jbr\bin\java.exe",
    "${env:ProgramFiles(x86)}\Android\Android Studio\jbr\bin\java.exe",
    "$env:LOCALAPPDATA\Android\Android Studio\jbr\bin\java.exe"
)

$javaPath = $null
foreach ($path in $androidStudioPaths) {
    if (Test-Path $path) {
        $javaPath = $path
        $javaHomePath = Split-Path (Split-Path $path -Parent) -Parent
        Write-Host "Found Android Studio's Java at: $javaPath" -ForegroundColor Green
        break
    }
}

if ($javaPath -eq $null) {
    Write-Host "Android Studio's Java not found. Let's check for system Java..." -ForegroundColor Yellow
    try {
        $javaVersion = cmd /c "java -version 2>&1"
        if ($javaVersion -match 'version') {
            Write-Host "System Java is installed: $javaVersion" -ForegroundColor Green
            $javaPath = "java"
        }
    } catch {
        Write-Host "Java not found! Please install Java or Android Studio." -ForegroundColor Red
        exit 1
    }
}

# Ensure the gradle wrapper jar exists
if (-not (Test-Path "gradle\wrapper\gradle-wrapper.jar")) {
    Write-Host "gradle-wrapper.jar not found! Running download script..." -ForegroundColor Yellow
    .\download-wrapper.ps1
}

# Run the Gradle task
$command = "tasks"
if ($args.Count -gt 0) {
    $command = $args -join " "
}

Write-Host "Running: gradle $command" -ForegroundColor Cyan

if ($javaPath -eq "java") {
    # Use system Java
    & java -cp "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain $args
} else {
    # Use Android Studio's Java
    & $javaPath -cp "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain $args
}

# Report status
if ($LASTEXITCODE -eq 0) {
    Write-Host "Gradle command completed successfully." -ForegroundColor Green
} else {
    Write-Host "Gradle command failed with exit code: $LASTEXITCODE" -ForegroundColor Red
}
