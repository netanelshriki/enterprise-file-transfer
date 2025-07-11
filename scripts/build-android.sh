#!/bin/bash


set -e  # Exit on any error

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "🚀 Building SecureTransfer Android APK"
echo "======================================"

print_status "Checking prerequisites..."

if [ ! -d "$ANDROID_HOME" ] && [ ! -d "$ANDROID_SDK_ROOT" ]; then
    print_error "Android SDK not found"
    print_status "Please set ANDROID_HOME or ANDROID_SDK_ROOT environment variable"
    print_status "Download Android SDK from: https://developer.android.com/studio"
    exit 1
fi
print_success "Android SDK found"

if ! java -version 2>&1 | grep -q "21"; then
    print_error "Java 21 is required but not found"
    print_status "Please install Java 21 LTS (OpenJDK or Oracle)"
    exit 1
fi
print_success "Java 21 found"

cd android

chmod +x gradlew

print_status "Cleaning previous builds..."
./gradlew clean

print_status "Building debug APK..."
./gradlew assembleDebug
if [ $? -eq 0 ]; then
    print_success "Debug APK created: build/outputs/apk/debug/android-debug.apk"
else
    print_error "Debug APK build failed"
    exit 1
fi

print_status "Building release APK..."
./gradlew assembleRelease
if [ $? -eq 0 ]; then
    print_success "Release APK created: build/outputs/apk/release/android-release.apk"
    print_warning "Note: Release APK is not signed. Configure signing for production use."
else
    print_warning "Release APK build failed (may need signing configuration)"
    print_status "Debug APK is still available for testing"
fi

print_status "Running unit tests..."
./gradlew test
if [ $? -eq 0 ]; then
    print_success "All tests passed"
else
    print_warning "Some tests failed - check test reports"
fi

echo ""
echo "🎉 Android Build Summary"
echo "======================="

if [ -f "build/outputs/apk/debug/android-debug.apk" ]; then
    print_success "Debug APK: build/outputs/apk/debug/android-debug.apk"
    
    APK_SIZE=$(du -h build/outputs/apk/debug/android-debug.apk | cut -f1)
    print_status "APK Size: $APK_SIZE"
fi

if [ -f "build/outputs/apk/release/android-release.apk" ]; then
    print_success "Release APK: build/outputs/apk/release/android-release.apk"
    
    APK_SIZE=$(du -h build/outputs/apk/release/android-release.apk | cut -f1)
    print_status "APK Size: $APK_SIZE"
fi

echo ""
print_status "Build completed!"
print_status "To install debug APK on connected device: ./gradlew installDebug"
print_status "To run on emulator: ./gradlew installDebug && adb shell am start -n com.securetransfer.android/.MainActivity"
