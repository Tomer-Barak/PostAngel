# Fix Android Studio Gradle project script

Write-Host "Fixing Android Studio Gradle project..." -ForegroundColor Green

# Check java installation
$javaInstalled = $false
try {
    $javaVersion = cmd /c "java -version 2>&1"
    if ($javaVersion -match 'version') {
        $javaInstalled = $true
        Write-Host "Java is installed: $javaVersion" -ForegroundColor Green
    }
} catch {
    Write-Host "Java not found in PATH. Please install Java JDK." -ForegroundColor Red
    Write-Host "Download from: https://adoptium.net/temurin/releases/" -ForegroundColor Yellow
    exit 1
}

# Fix gradlew permissions
Write-Host "Making gradlew executable..." -ForegroundColor Green
if (Test-Path "z:\BarakBot\Twitter\app\gradlew") {
    # On Windows, we need a bat file for gradlew
    if (-not (Test-Path "z:\BarakBot\Twitter\app\gradlew.bat")) {
        Write-Host "Creating gradlew.bat file..." -ForegroundColor Yellow
        
        # Create a simple batch file that calls the shell script using bash if available
        @"
@echo off
set SCRIPT_PATH=%~dp0gradlew
if exist "%SCRIPT_PATH%" (
    echo Using gradle wrapper...
    if exist "%LOCALAPPDATA%\Android\Sdk\tools\bin\sdkmanager.bat" (
        echo Using Android SDK to run gradle wrapper...
        call "%LOCALAPPDATA%\Android\Sdk\tools\bin\sdkmanager.bat" --list > nul 2>&1
    )
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        echo Using Android Studio's Java...
        set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
        set PATH=%JAVA_HOME%\bin;%PATH%
    )
    java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain %*
) else (
    echo gradlew script not found!
)
"@ | Out-File -FilePath "z:\BarakBot\Twitter\app\gradlew.bat" -Encoding ASCII
    }
}

# Check for Android Studio's Java
$asJava = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
if (Test-Path $asJava) {
    Write-Host "Found Android Studio's Java at: $asJava" -ForegroundColor Green
    Write-Host "You can use this Java for running Gradle." -ForegroundColor Green
}

# Check if Android SDK is installed
$sdkManager = "$env:LOCALAPPDATA\Android\Sdk\tools\bin\sdkmanager.bat"
if (Test-Path $sdkManager) {
    Write-Host "Found Android SDK at: $env:LOCALAPPDATA\Android\Sdk" -ForegroundColor Green
} else {
    Write-Host "Android SDK not found in the default location." -ForegroundColor Yellow
    Write-Host "Make sure Android Studio is properly installed." -ForegroundColor Yellow
}

Write-Host "`nTo sync your project in Android Studio:" -ForegroundColor Cyan
Write-Host "1. Open Android Studio" -ForegroundColor White
Write-Host "2. Open your project at z:\BarakBot\Twitter\app" -ForegroundColor White
Write-Host "3. Wait for initial indexing to complete" -ForegroundColor White
Write-Host "4. Click on 'File > Sync Project with Gradle Files'" -ForegroundColor White
Write-Host "`nIf you keep getting errors:" -ForegroundColor Cyan
Write-Host "1. Try 'File > Invalidate Caches / Restart...'" -ForegroundColor White
Write-Host "2. Make sure you have the correct JDK set up in Android Studio settings" -ForegroundColor White

Write-Host "`nWould you like to try running gradlew.bat now? (y/n)" -ForegroundColor Cyan
$response = Read-Host
if ($response -eq "y" -or $response -eq "Y") {
    Write-Host "`nRunning: .\gradlew.bat tasks" -ForegroundColor Green
    cd "z:\BarakBot\Twitter\app"
    .\gradlew.bat tasks
}
