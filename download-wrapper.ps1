# PowerShell script to download gradle-wrapper.jar
$wrapperPath = "gradle/wrapper"
$jarUrl = "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"

# Create directory if it doesn't exist
if (!(Test-Path $wrapperPath)) {
    New-Item -ItemType Directory -Path $wrapperPath -Force
}

# Download the jar file
$outputPath = Join-Path $wrapperPath "gradle-wrapper.jar"
Write-Host "Downloading gradle-wrapper.jar to $outputPath"
Invoke-WebRequest -Uri $jarUrl -OutFile $outputPath

Write-Host "Download complete!"
