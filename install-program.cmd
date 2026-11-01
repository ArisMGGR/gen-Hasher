@echo off
rem Builds the Windows installer and runs it. Double-click this file, or run it from a terminal.
rem Needs the same tools as build-installer.ps1: JDK 21+, Maven and WiX Toolset 3 on PATH.
setlocal
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File build-installer.ps1
if errorlevel 1 (
    echo.
    echo Building the installer failed; see the messages above.
    pause
    exit /b 1
)

rem The newest installer, in case older versions are still in the folder.
set "INSTALLER="
for /f "delims=" %%f in ('dir /b /o-d target\installer\*.exe') do if not defined INSTALLER set "INSTALLER=target\installer\%%f"
if not defined INSTALLER (
    echo No installer was found in target\installer.
    pause
    exit /b 1
)

rem Windows Installer refuses to install over the same version (error 1638), so remove any installed Hasher first.
rem Its saved settings are kept: they live in the registry, not in the install folder.
powershell -NoProfile -Command "Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*', 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue | Where-Object DisplayName -eq 'Hasher' | ForEach-Object { Write-Host ('Removing the installed Hasher ' + $_.DisplayVersion + ' ...'); $p = Start-Process msiexec.exe -ArgumentList '/x', $_.PSChildName, '/qn', '/norestart' -Wait -PassThru; if ($p.ExitCode) { exit $p.ExitCode } }"
if errorlevel 1 (
    echo.
    echo Removing the installed Hasher failed with error %errorlevel%. Uninstall it from Settings, Apps, then try again.
    pause
    exit /b 1
)

echo.
echo Running %INSTALLER% ...
start "" /wait "%INSTALLER%"
if errorlevel 1 (
    echo.
    echo The installer failed with error %errorlevel%.
    pause
    exit /b 1
)
echo Done. Open Hasher from the Start menu or the desktop shortcut.
pause
