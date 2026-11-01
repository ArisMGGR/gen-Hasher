# Builds the Windows installer: target\installer\Hasher-<version>.exe
# Needs JDK 21+ (for jpackage), Maven and WiX Toolset 3 on PATH.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# The app version comes from pom.xml, so there is one place to bump it.
$version = ([xml](Get-Content pom.xml -Raw)).project.version
if (-not $version) { throw 'No <version> in pom.xml' }

mvn -B -ntp clean package
if ($LASTEXITCODE) { throw 'Maven build failed' }

# jpackage bundles everything in --input, so give it a folder holding only the app jar.
$stage = 'target\jpackage-input'
New-Item -ItemType Directory -Force $stage | Out-Null
Copy-Item target\hasher.jar $stage

jpackage --type exe `
    --name Hasher `
    --app-version $version `
    --vendor Hasher `
    --description 'Hash a string with MD5, SHA-1, SHA-2 and SHA-3' `
    --input $stage `
    --main-jar hasher.jar `
    --main-class hasher.HasherApp `
    --icon src\main\packaging\hasher.ico `
    --add-modules java.base,java.desktop,java.prefs `
    --win-per-user-install `
    --win-menu `
    --win-menu-group Hasher `
    --win-shortcut `
    --win-upgrade-uuid fcaeadc1-81a5-469f-bbeb-557411dfacdd `
    --dest target\installer
if ($LASTEXITCODE) { throw 'jpackage failed' }

Get-ChildItem target\installer\*.exe
