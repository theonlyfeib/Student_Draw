param(
    [ValidateSet("exe", "msi", "app-image")]
    [string]$Type = "exe",

    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildRoot = Join-Path $projectRoot "build"
$classesDirectory = Join-Path $buildRoot "classes"
$inputDirectory = Join-Path $buildRoot "input"
$outputDirectory = Join-Path $projectRoot "dist"
$jarPath = Join-Path $inputDirectory "student-draw.jar"

$javac = Get-Command javac -ErrorAction Stop
$jar = Get-Command jar -ErrorAction Stop
$jpackage = Get-Command jpackage -ErrorAction Stop

if ($Type -ne "app-image") {
    $wixRoot = $env:WIX
    if (-not $wixRoot) {
        $wixRoot = @(
            "C:\Program Files (x86)\WiX Toolset v3.14\bin",
            "C:\Program Files\WiX Toolset v3.14\bin"
        ) | Where-Object {
            (Test-Path (Join-Path $_ "candle.exe")) -and (Test-Path (Join-Path $_ "light.exe"))
        } | Select-Object -First 1
    } elseif (-not (Test-Path (Join-Path $wixRoot "candle.exe"))) {
        $wixBin = Join-Path $wixRoot "bin"
        if ((Test-Path (Join-Path $wixBin "candle.exe")) -and
            (Test-Path (Join-Path $wixBin "light.exe"))) {
            $wixRoot = $wixBin
        }
    }

    if (-not $wixRoot -or
        -not (Test-Path (Join-Path $wixRoot "candle.exe")) -or
        -not (Test-Path (Join-Path $wixRoot "light.exe"))) {
        throw "WiX Toolset 3.14.1 is required for EXE/MSI installers. Set WIX to its bin folder, or run with -Type app-image."
    }
    $env:PATH = "$wixRoot;$env:PATH"
}

New-Item -ItemType Directory -Path $classesDirectory, $inputDirectory, $outputDirectory -Force |
    Out-Null

& $javac.Source -encoding UTF-8 -Xlint:all -d $classesDirectory (Join-Path $projectRoot "Main.java")
if ($LASTEXITCODE -ne 0) {
    throw "Java compilation failed."
}

& $jar.Source --create --file $jarPath --main-class Main -C $classesDirectory .
if ($LASTEXITCODE -ne 0) {
    throw "Application JAR creation failed."
}

$arguments = @(
    "--type", $Type,
    "--name", "Student Draw",
    "--app-version", $Version,
    "--vendor", "Student Draw",
    "--description", "Randomly draw students and track class events.",
    "--input", $inputDirectory,
    "--main-jar", "student-draw.jar",
    "--main-class", "Main",
    "--dest", $outputDirectory
)

if ($Type -ne "app-image") {
    $arguments += @(
        "--win-per-user-install",
        "--win-dir-chooser",
        "--win-menu",
        "--win-menu-group", "Student Draw",
        "--win-shortcut",
        "--win-upgrade-uuid", "5ae87bdd-8574-4fe5-bf9e-d317868f9924"
    )
}

& $jpackage.Source @arguments
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed to create the $Type package."
}

Write-Output "Package created in $outputDirectory"
