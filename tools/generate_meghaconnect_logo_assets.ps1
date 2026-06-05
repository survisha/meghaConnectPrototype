param(
    [string]$Root = (Resolve-Path ".").Path
)

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"

$blue = [System.Drawing.Color]::FromArgb(255, 4, 92, 219)
$darkBlue = [System.Drawing.Color]::FromArgb(255, 7, 31, 92)
$teal = [System.Drawing.Color]::FromArgb(255, 10, 196, 184)
$muted = [System.Drawing.Color]::FromArgb(255, 75, 85, 99)
$white = [System.Drawing.Color]::White
$transparent = [System.Drawing.Color]::FromArgb(0, 255, 255, 255)

function Ensure-Dir([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function New-GradientBrush([System.Drawing.RectangleF]$Rect) {
    return [System.Drawing.Drawing2D.LinearGradientBrush]::new($Rect, $blue, $teal, 0)
}

function Draw-RoundLine($Graphics, [float]$X1, [float]$Y1, [float]$X2, [float]$Y2, [float]$Width, $Brush) {
    $pen = [System.Drawing.Pen]::new($Brush, $Width)
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $Graphics.DrawLine($pen, $X1, $Y1, $X2, $Y2)
    $pen.Dispose()
}

function Draw-LogoMark($Graphics, [float]$X, [float]$Y, [float]$W, [float]$H) {
    $rect = [System.Drawing.RectangleF]::new($X, $Y, $W, $H)
    $grad = New-GradientBrush $rect
    $lineWidth = $W * 0.16

    Draw-RoundLine $Graphics ($X + $W * 0.18) ($Y + $H * 0.68) ($X + $W * 0.18) ($Y + $H * 0.43) $lineWidth $grad

    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.StartFigure()
    $path.AddBezier(
        ($X + $W * 0.18), ($Y + $H * 0.43),
        ($X + $W * 0.28), ($Y + $H * 0.31),
        ($X + $W * 0.38), ($Y + $H * 0.55),
        ($X + $W * 0.48), ($Y + $H * 0.64)
    )
    $path.AddBezier(
        ($X + $W * 0.48), ($Y + $H * 0.64),
        ($X + $W * 0.54), ($Y + $H * 0.70),
        ($X + $W * 0.61), ($Y + $H * 0.55),
        ($X + $W * 0.72), ($Y + $H * 0.43)
    )
    $pen = [System.Drawing.Pen]::new($grad, $lineWidth)
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $Graphics.DrawPath($pen, $path)
    $pen.Dispose()
    $path.Dispose()

    Draw-RoundLine $Graphics ($X + $W * 0.82) ($Y + $H * 0.68) ($X + $W * 0.82) ($Y + $H * 0.43) $lineWidth $grad
    Draw-RoundLine $Graphics ($X + $W * 0.72) ($Y + $H * 0.43) ($X + $W * 0.82) ($Y + $H * 0.43) $lineWidth $grad

    $head = $W * 0.12
    $Graphics.FillEllipse($grad, $X + $W * 0.12, $Y + $H * 0.04, $head, $head)
    $Graphics.FillEllipse($grad, $X + $W * 0.76, $Y + $H * 0.04, $head, $head)
    $grad.Dispose()
}

function Draw-LogoLockup($Graphics, [int]$Width, [int]$Height, [switch]$WhiteVariant, [switch]$IconOnly) {
    $Graphics.Clear($transparent)
    $Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $Graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    if ($IconOnly) {
        $side = [Math]::Min($Width, $Height) * 0.78
        Draw-LogoMark $Graphics (($Width - $side) / 2) (($Height - $side) / 2) $side $side
        return
    }

    $markW = $Width * 0.52
    $markH = $Height * 0.42
    Draw-LogoMark $Graphics (($Width - $markW) / 2) ($Height * 0.05) $markW $markH

    $brandSize = [Math]::Max(16, $Height * 0.115)
    $tagSize = [Math]::Max(8, $Height * 0.036)
    $brandFont = [System.Drawing.Font]::new("Segoe UI", $brandSize, [System.Drawing.FontStyle]::Bold)
    $tagFont = [System.Drawing.Font]::new("Segoe UI", $tagSize, [System.Drawing.FontStyle]::Regular)
    $brand = "MEGHACONNECT"
    $tag = "CONNECTING PEOPLE, EMPOWERING LIVES"
    $brandBrush = if ($WhiteVariant) { [System.Drawing.SolidBrush]::new($white) } else { New-GradientBrush ([System.Drawing.RectangleF]::new(0, 0, $Width, $Height)) }
    $tagBrush = if ($WhiteVariant) { [System.Drawing.SolidBrush]::new($white) } else { [System.Drawing.SolidBrush]::new($muted) }

    $brandSizeMeasured = $Graphics.MeasureString($brand, $brandFont)
    $brandY = $Height * 0.58
    $Graphics.DrawString($brand, $brandFont, $brandBrush, (($Width - $brandSizeMeasured.Width) / 2), $brandY)

    $tagSizeMeasured = $Graphics.MeasureString($tag, $tagFont)
    $tagY = $Height * 0.78
    $Graphics.DrawString($tag, $tagFont, $tagBrush, (($Width - $tagSizeMeasured.Width) / 2), $tagY)

    $brandFont.Dispose()
    $tagFont.Dispose()
    $brandBrush.Dispose()
    $tagBrush.Dispose()
}

function Save-Png([string]$Path, [int]$Width, [int]$Height, [switch]$WhiteVariant, [switch]$IconOnly, [switch]$WhiteBackground) {
    $bmp = [System.Drawing.Bitmap]::new($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    if ($WhiteBackground) {
        $g.Clear($white)
    }
    Draw-LogoLockup $g $Width $Height -WhiteVariant:$WhiteVariant -IconOnly:$IconOnly
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

function Save-Ico([string]$Path, [string[]]$PngPaths) {
    $out = [System.IO.File]::Open($Path, [System.IO.FileMode]::Create)
    $writer = [System.IO.BinaryWriter]::new($out)
    $writer.Write([UInt16]0)
    $writer.Write([UInt16]1)
    $writer.Write([UInt16]$PngPaths.Count)
    $offset = 6 + (16 * $PngPaths.Count)
    $bytes = @()
    foreach ($png in $PngPaths) {
        $data = [System.IO.File]::ReadAllBytes($png)
        $img = [System.Drawing.Image]::FromFile($png)
        $icoWidth = $img.Width
        $icoHeight = $img.Height
        if ($icoWidth -ge 256) { $icoWidth = 0 }
        if ($icoHeight -ge 256) { $icoHeight = 0 }
        $writer.Write([byte]$icoWidth)
        $writer.Write([byte]$icoHeight)
        $writer.Write([byte]0)
        $writer.Write([byte]0)
        $writer.Write([UInt16]1)
        $writer.Write([UInt16]32)
        $writer.Write([UInt32]$data.Length)
        $writer.Write([UInt32]$offset)
        $offset += $data.Length
        $bytes += ,$data
        $img.Dispose()
    }
    foreach ($data in $bytes) { $writer.Write($data) }
    $writer.Dispose()
    $out.Dispose()
}

function Write-Svg([string]$Path, [string]$TextColor, [string]$TagColor) {
@"
<svg role="img" aria-label="MeghaConnect logo" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 430">
  <title>MeghaConnect</title>
  <defs>
    <linearGradient id="mcGradient" x1="160" y1="35" x2="740" y2="330" gradientUnits="userSpaceOnUse">
      <stop stop-color="#045CDB"/>
      <stop offset="1" stop-color="#0AC4B8"/>
    </linearGradient>
    <linearGradient id="mcTextGradient" x1="130" y1="0" x2="770" y2="0" gradientUnits="userSpaceOnUse">
      <stop stop-color="#071F5C"/>
      <stop offset="1" stop-color="#0AC4B8"/>
    </linearGradient>
  </defs>
  <g fill="none" stroke="url(#mcGradient)" stroke-linecap="round" stroke-width="74">
    <path d="M210 238V136C210 136 270 115 326 170L396 240C420 264 455 264 480 240L550 170C606 115 670 136 670 136V238"/>
  </g>
  <circle cx="210" cy="70" r="42" fill="url(#mcGradient)"/>
  <circle cx="670" cy="70" r="42" fill="url(#mcGradient)"/>
  <text x="450" y="335" text-anchor="middle" font-family="Inter, Segoe UI, Arial, sans-serif" font-size="72" font-weight="850" letter-spacing="10" fill="$TextColor">MEGHACONNECT</text>
  <g fill="none" stroke="$TagColor" stroke-width="1.5" opacity="0.85">
    <path d="M115 383H220"/>
    <path d="M680 383H785"/>
  </g>
  <text x="450" y="392" text-anchor="middle" font-family="Inter, Segoe UI, Arial, sans-serif" font-size="22" font-weight="500" letter-spacing="7" fill="$TagColor">CONNECTING PEOPLE, EMPOWERING LIVES</text>
</svg>
"@ | Set-Content -LiteralPath $Path -Encoding UTF8
}

$frontendAssets = Join-Path $Root "frontend\src\asserts"
$frontendPublic = Join-Path $Root "frontend\public\asserts"
$mobileAssets = Join-Path $Root "mobile\assets"
$mobileWeb = Join-Path $Root "mobile\web"
$androidRes = Join-Path $Root "mobile\android\app\src\main\res"
$iosAssets = Join-Path $Root "mobile\ios\Runner\Assets.xcassets"

foreach ($dir in @($frontendAssets, $frontendPublic, $mobileAssets, $mobileWeb, (Join-Path $mobileWeb "icons"))) {
    Ensure-Dir $dir
}

foreach ($dir in @($frontendAssets, $frontendPublic)) {
    Write-Svg (Join-Path $dir "logo.svg") "url(#mcTextGradient)" "#4B5563"
    Write-Svg (Join-Path $dir "logo-dark.svg") "url(#mcTextGradient)" "#4B5563"
    Write-Svg (Join-Path $dir "logo-white.svg") "#FFFFFF" "#FFFFFF"
    Save-Png (Join-Path $dir "logo.png") 900 430
    Save-Png (Join-Path $dir "logo-small.png") 256 256 -IconOnly
    Save-Png (Join-Path $dir "logo-white.png") 900 430 -WhiteVariant
    Save-Png (Join-Path $dir "logo-dark.png") 900 430
    foreach ($s in 32, 64, 128, 256, 512) {
        Save-Png (Join-Path $dir "logo-$s.png") $s $s -IconOnly
    }
    Save-Ico (Join-Path $dir "favicon.ico") @((Join-Path $dir "logo-32.png"), (Join-Path $dir "logo-64.png"), (Join-Path $dir "logo-256.png"))
}

Write-Svg (Join-Path $mobileAssets "logo.svg") "url(#mcTextGradient)" "#4B5563"
Save-Png (Join-Path $mobileAssets "logo.png") 900 430
Save-Png (Join-Path $mobileAssets "logo-small.png") 512 512 -IconOnly
Save-Png (Join-Path $mobileAssets "logo-white.png") 900 430 -WhiteVariant

$webIconDir = Join-Path $mobileWeb "icons"
Save-Png (Join-Path $mobileWeb "favicon.png") 64 64 -IconOnly
Save-Png (Join-Path $webIconDir "Icon-192.png") 192 192 -IconOnly -WhiteBackground
Save-Png (Join-Path $webIconDir "Icon-512.png") 512 512 -IconOnly -WhiteBackground
Save-Png (Join-Path $webIconDir "Icon-maskable-192.png") 192 192 -IconOnly -WhiteBackground
Save-Png (Join-Path $webIconDir "Icon-maskable-512.png") 512 512 -IconOnly -WhiteBackground

$androidSizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}
foreach ($entry in $androidSizes.GetEnumerator()) {
    $dir = Join-Path $androidRes $entry.Key
    Ensure-Dir $dir
    Save-Png (Join-Path $dir "ic_launcher.png") $entry.Value $entry.Value -IconOnly -WhiteBackground
}
Save-Png (Join-Path $androidRes "drawable\ic_launcher_foreground.png") 432 432 -IconOnly
Save-Png (Join-Path $androidRes "drawable\launch_logo.png") 384 384 -IconOnly
Save-Png (Join-Path $androidRes "drawable-v21\launch_logo.png") 384 384 -IconOnly

$iosIconDir = Join-Path $iosAssets "AppIcon.appiconset"
$iosIcons = @{
    "Icon-App-20x20@1x.png" = 20
    "Icon-App-20x20@2x.png" = 40
    "Icon-App-20x20@3x.png" = 60
    "Icon-App-29x29@1x.png" = 29
    "Icon-App-29x29@2x.png" = 58
    "Icon-App-29x29@3x.png" = 87
    "Icon-App-40x40@1x.png" = 40
    "Icon-App-40x40@2x.png" = 80
    "Icon-App-40x40@3x.png" = 120
    "Icon-App-60x60@2x.png" = 120
    "Icon-App-60x60@3x.png" = 180
    "Icon-App-76x76@1x.png" = 76
    "Icon-App-76x76@2x.png" = 152
    "Icon-App-83.5x83.5@2x.png" = 167
    "Icon-App-1024x1024@1x.png" = 1024
}
foreach ($entry in $iosIcons.GetEnumerator()) {
    Save-Png (Join-Path $iosIconDir $entry.Key) $entry.Value $entry.Value -IconOnly -WhiteBackground
}

$launchDir = Join-Path $iosAssets "LaunchImage.imageset"
Save-Png (Join-Path $launchDir "LaunchImage.png") 168 185 -IconOnly
Save-Png (Join-Path $launchDir "LaunchImage@2x.png") 336 370 -IconOnly
Save-Png (Join-Path $launchDir "LaunchImage@3x.png") 504 555 -IconOnly
