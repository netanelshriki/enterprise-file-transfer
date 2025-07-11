@echo off
REM SecureTransfer - Build All Platforms Script (Windows)
REM This script builds desktop applications and Android APK

setlocal enabledelayedexpansion

echo 🚀 Building SecureTransfer - All Platforms
echo ==========================================

REM Check prerequisites
echo [INFO] Checking prerequisites...

REM Check Java 21
java -version 2>&1 | findstr "21" >nul
if errorlevel 1 (
    echo [ERROR] Java 21 is required but not found
    echo [INFO] Please install Java 21 LTS (OpenJDK or Oracle)
    exit /b 1
)
echo [SUCCESS] Java 21 found

REM Check Maven
where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is required but not found
    echo [INFO] Please install Maven 3.8+
    exit /b 1
)
echo [SUCCESS] Maven found

REM Check Android SDK (optional)
set ANDROID_BUILD=true
if not defined ANDROID_HOME (
    if not defined ANDROID_SDK_ROOT (
        echo [WARNING] Android SDK not found - skipping Android build
        echo [INFO] Set ANDROID_HOME or ANDROID_SDK_ROOT to build Android APK
        set ANDROID_BUILD=false
    )
)
if "%ANDROID_BUILD%"=="true" (
    echo [SUCCESS] Android SDK found
)

REM Build Desktop Applications
echo [INFO] Building desktop applications...
cd desktop

echo [INFO] Compiling desktop application...
call mvn clean compile
if errorlevel 1 (
    echo [ERROR] Desktop compilation failed
    exit /b 1
)
echo [SUCCESS] Desktop compilation successful

REM Build Windows MSI installer
echo [INFO] Building Windows MSI installer...
call mvn jpackage:jpackage -Pwindows-msi
if errorlevel 1 (
    echo [WARNING] Windows MSI installer failed
) else (
    echo [SUCCESS] Windows MSI installer created
)

cd ..

REM Build Android APK
if "%ANDROID_BUILD%"=="true" (
    echo [INFO] Building Android APK...
    cd android
    
    echo [INFO] Building debug APK...
    call gradlew.bat assembleDebug
    if errorlevel 1 (
        echo [ERROR] Android debug APK failed
        cd ..
        exit /b 1
    )
    echo [SUCCESS] Android debug APK created
    
    echo [INFO] Building release APK...
    call gradlew.bat assembleRelease
    if errorlevel 1 (
        echo [WARNING] Android release APK failed (may need signing configuration)
    ) else (
        echo [SUCCESS] Android release APK created
    )
    
    cd ..
) else (
    echo [WARNING] Skipping Android build - SDK not configured
)

REM Build Signaling Server
echo [INFO] Building signaling server...
cd server

call mvn clean package
if errorlevel 1 (
    echo [ERROR] Signaling server build failed
    exit /b 1
)
echo [SUCCESS] Signaling server JAR created

cd ..

REM Summary
echo.
echo 🎉 Build Summary
echo ================

echo [SUCCESS] Desktop application compiled successfully

if exist "desktop\target\installer\SecureTransfer-1.0.0.msi" (
    echo [SUCCESS] Windows MSI: desktop\target\installer\SecureTransfer-1.0.0.msi
)

if "%ANDROID_BUILD%"=="true" (
    if exist "android\build\outputs\apk\debug\android-debug.apk" (
        echo [SUCCESS] Android Debug APK: android\build\outputs\apk\debug\android-debug.apk
    )
    
    if exist "android\build\outputs\apk\release\android-release.apk" (
        echo [SUCCESS] Android Release APK: android\build\outputs\apk\release\android-release.apk
    )
)

if exist "server\target\server-1.0.0.jar" (
    echo [SUCCESS] Signaling Server: server\target\server-1.0.0.jar
)

echo.
echo [INFO] Build completed! Check the target directories for your installers.
echo [INFO] For development, use:
echo [INFO]   Desktop: cd desktop ^&^& mvn javafx:run
echo [INFO]   Android: cd android ^&^& gradlew.bat installDebug
echo [INFO]   Server:  cd server ^&^& java -jar target\server-1.0.0.jar

pause
