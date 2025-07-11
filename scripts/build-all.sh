#!/bin/bash


set -e  # Exit on any error

echo "🚀 Building SecureTransfer - All Platforms"
echo "=========================================="

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

print_status "Checking prerequisites..."

if ! java -version 2>&1 | grep -q "21"; then
    print_error "Java 21 is required but not found"
    print_status "Please install Java 21 LTS (OpenJDK or Oracle)"
    exit 1
fi
print_success "Java 21 found"

if ! command -v mvn &> /dev/null; then
    print_error "Maven is required but not found"
    print_status "Please install Maven 3.8+"
    exit 1
fi
print_success "Maven found"

ANDROID_BUILD=true
if [ ! -d "$ANDROID_HOME" ] && [ ! -d "$ANDROID_SDK_ROOT" ]; then
    print_warning "Android SDK not found - skipping Android build"
    print_status "Set ANDROID_HOME or ANDROID_SDK_ROOT to build Android APK"
    ANDROID_BUILD=false
else
    print_success "Android SDK found"
fi

print_status "Building desktop applications..."
cd desktop

print_status "Compiling desktop application..."
mvn clean compile
if [ $? -eq 0 ]; then
    print_success "Desktop compilation successful"
else
    print_error "Desktop compilation failed"
    exit 1
fi

print_status "Building platform-specific installers..."

case "$OSTYPE" in
    msys*|win32*|cygwin*)
        print_status "Building Windows MSI installer..."
        mvn jpackage:jpackage -Pwindows-msi
        if [ $? -eq 0 ]; then
            print_success "Windows MSI installer created"
        else
            print_warning "Windows MSI installer failed"
        fi
        ;;
    linux-gnu*)
        print_status "Building Linux DEB package..."
        mvn jpackage:jpackage -Plinux-deb
        if [ $? -eq 0 ]; then
            print_success "Linux DEB package created"
        else
            print_warning "Linux DEB package failed"
        fi
        
        print_status "Building Linux AppImage..."
        mvn jpackage:jpackage -Plinux-appimage
        if [ $? -eq 0 ]; then
            print_success "Linux AppImage created"
        else
            print_warning "Linux AppImage failed"
        fi
        ;;
    darwin*)
        print_status "macOS detected - building universal package..."
        mvn jpackage:jpackage -Pmac-dmg
        if [ $? -eq 0 ]; then
            print_success "macOS DMG package created"
        else
            print_warning "macOS DMG package failed"
        fi
        ;;
    *)
        print_warning "Unknown OS type: $OSTYPE - skipping native installers"
        ;;
esac

cd ..

if [ "$ANDROID_BUILD" = true ]; then
    print_status "Building Android APK..."
    cd android
    
    chmod +x gradlew
    
    print_status "Building debug APK..."
    ./gradlew assembleDebug
    if [ $? -eq 0 ]; then
        print_success "Android debug APK created"
    else
        print_error "Android debug APK failed"
        cd ..
        exit 1
    fi
    
    print_status "Building release APK..."
    ./gradlew assembleRelease
    if [ $? -eq 0 ]; then
        print_success "Android release APK created"
    else
        print_warning "Android release APK failed (may need signing configuration)"
    fi
    
    cd ..
else
    print_warning "Skipping Android build - SDK not configured"
fi

print_status "Building signaling server..."
cd server

mvn clean package
if [ $? -eq 0 ]; then
    print_success "Signaling server JAR created"
else
    print_error "Signaling server build failed"
    exit 1
fi

cd ..

echo ""
echo "🎉 Build Summary"
echo "================"

print_success "Desktop application compiled successfully"

if [ -f "desktop/target/installer/SecureTransfer-1.0.0.msi" ]; then
    print_success "Windows MSI: desktop/target/installer/SecureTransfer-1.0.0.msi"
fi

if [ -f "desktop/target/installer/securetransfer_1.0.0-1_amd64.deb" ]; then
    print_success "Linux DEB: desktop/target/installer/securetransfer_1.0.0-1_amd64.deb"
fi

if [ -f "desktop/target/installer/SecureTransfer.AppImage" ]; then
    print_success "Linux AppImage: desktop/target/installer/SecureTransfer.AppImage"
fi

if [ "$ANDROID_BUILD" = true ]; then
    if [ -f "android/build/outputs/apk/debug/android-debug.apk" ]; then
        print_success "Android Debug APK: android/build/outputs/apk/debug/android-debug.apk"
    fi
    
    if [ -f "android/build/outputs/apk/release/android-release.apk" ]; then
        print_success "Android Release APK: android/build/outputs/apk/release/android-release.apk"
    fi
fi

if [ -f "server/target/server-1.0.0.jar" ]; then
    print_success "Signaling Server: server/target/server-1.0.0.jar"
fi

echo ""
print_status "Build completed! Check the target directories for your installers."
print_status "For development, use:"
print_status "  Desktop: cd desktop && mvn javafx:run"
print_status "  Android: cd android && ./gradlew installDebug"
print_status "  Server:  cd server && java -jar target/server-1.0.0.jar"
