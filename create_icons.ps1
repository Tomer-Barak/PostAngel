# This script resizes the BarakBot.png image to create properly sized launcher icons
# Add System.Drawing assembly for image processing
Add-Type -AssemblyName System.Drawing

# Define the source image path
$sourcePath = "c:\Users\tomer\My Drive\Stuff\BarakBot_Social_Media\icon.png"

# Define the mipmap directories and sizes for different densities
$mipmapSizes = @{
    'mipmap-mdpi' = 48;
    'mipmap-hdpi' = 72;
    'mipmap-xhdpi' = 96;
    'mipmap-xxhdpi' = 144;
    'mipmap-xxxhdpi' = 192;
}

# Check if source image exists
if (-not (Test-Path $sourcePath)) {
    Write-Error "Source image not found: $sourcePath"
    exit 1
}

# Load the source image
$sourceImage = [System.Drawing.Image]::FromFile($sourcePath)

# Define base path for Android resources
$basePath = "c:\Users\tomer\My Drive\Stuff\BarakBot_Social_Media\app\src\main\res"

# Create foreground icons for each density
foreach ($mipmap in $mipmapSizes.GetEnumerator()) {
    $dirName = $mipmap.Key
    $iconSize = $mipmap.Value
    
    # Create path for the directory
    $dirPath = Join-Path -Path $basePath -ChildPath $dirName
    
    # Ensure directory exists
    if (-not (Test-Path $dirPath)) {
        New-Item -Path $dirPath -ItemType Directory -Force | Out-Null
    }
    
    # Create the foreground icon at this size
    $foregroundPath = Join-Path -Path $dirPath -ChildPath "ic_launcher_foreground.png"
    
    # Create a blank bitmap with the right size and transparent background
    $bitmap = New-Object System.Drawing.Bitmap $iconSize, $iconSize
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    
    # Set high quality mode
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    
    # Calculate padding (20% for adaptive icon padding)
    $padding = [int]($iconSize * 0.2)
    $drawSize = $iconSize - (2 * $padding)
    
    # Clear with transparent background
    $graphics.Clear([System.Drawing.Color]::Transparent)
    
    # Draw the source image onto this bitmap with padding
    $graphics.DrawImage($sourceImage, $padding, $padding, $drawSize, $drawSize)
    
    # Save as PNG to preserve transparency
    $bitmap.Save($foregroundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # Release resources
    $graphics.Dispose()
    $bitmap.Dispose()
    
    Write-Output "Created foreground icon: $foregroundPath"
    
    # Also create regular launcher icon for backward compatibility
    $launcherPath = Join-Path -Path $dirPath -ChildPath "ic_launcher.png"
    $bitmap = New-Object System.Drawing.Bitmap $iconSize, $iconSize
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    
    # Set high quality mode
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    
    # Clear with transparent background
    $graphics.Clear([System.Drawing.Color]::Transparent)
    
    # Draw the source image onto this bitmap (without padding for legacy icon)
    $graphics.DrawImage($sourceImage, 0, 0, $iconSize, $iconSize)
    
    # Save as PNG
    $bitmap.Save($launcherPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # Release resources
    $graphics.Dispose()
    $bitmap.Dispose()
    
    Write-Output "Created launcher icon: $launcherPath"
    
    # Create round launcher icon
    $roundPath = Join-Path -Path $dirPath -ChildPath "ic_launcher_round.png"
    $bitmap = New-Object System.Drawing.Bitmap $iconSize, $iconSize
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    
    # Set high quality mode
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    
    # Create a circular mask for the round icon
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(0, 0, $iconSize, $iconSize)
    $graphics.SetClip($path)
    
    # Clear with transparent background
    $graphics.Clear([System.Drawing.Color]::Transparent)
    
    # Draw the source image onto this bitmap
    $graphics.DrawImage($sourceImage, 0, 0, $iconSize, $iconSize)
    
    # Save as PNG
    $bitmap.Save($roundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # Release resources
    $graphics.Dispose()
    $bitmap.Dispose()
    
    Write-Output "Created round icon: $roundPath"
}

# Cleanup
$sourceImage.Dispose()
Write-Output "Icon generation complete!"
