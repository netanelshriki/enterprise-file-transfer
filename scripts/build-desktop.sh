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

usage() {
    echo "Usage: $0 {windows|linux-deb|linux-appimage|mac-dmg|all}"
    echo ""
    echo "Options:"
    echo "  windows       Build Windows MSI installer"
    echo "  linux-deb     Build Linux DEB package"
    echo "  linux-appimage Build Linux AppImage"
    echo "  mac-dmg       Build macOS DMG package"
    echo "  all           Build all available packages for current platform"
    echo ""
    exit 1
}

if [ $# -eq 0 ]; then
    usage
fi

PLATFORM=$1

echo "🚀 Building SecureTransfer Desktop - $PLATFORM"
echo "=============================================="

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

cd desktop

print_status "Compiling desktop application..."
mvn clean compile
if [ $? -eq 0 ]; then
    print_success "Desktop compilation successful"
else
    print_error "Desktop compilation failed"
    exit 1
fi

case "$PLATFORM" in
    "windows")
        print_status "Building Windows MSI installer..."
        mvn jpackage:jpackage -Pwindows-msi
        if [ $? -eq 0 ]; then
            print_success "Windows MSI installer created: target/installer/SecureTransfer-1.0.0.msi"
        else
            print_error "Windows MSI installer failed"
            exit 1
        fi
        ;;
    "linux-deb")
        print_status "Building Linux DEB package..."
        mvn jpackage:jpackage -Plinux-deb
        if [ $? -eq 0 ]; then
            print_success "Linux DEB package created: target/installer/securetransfer_1.0.0-1_amd64.deb"
        else
            print_error "Linux DEB package failed"
            exit 1
        fi
        ;;
    "linux-appimage")
        print_status "Building Linux AppImage..."
        mvn jpackage:jpackage -Plinux-appimage
        if [ $? -eq 0 ]; then
            print_success "Linux AppImage created: target/installer/SecureTransfer.AppImage"
        else
            print_error "Linux AppImage failed"
            exit 1
        fi
        ;;
    "mac-dmg")
        print_status "Building macOS DMG package..."
        mvn jpackage:jpackage -Pmac-dmg
        if [ $? -eq 0 ]; then
            print_success "macOS DMG package created: target/installer/SecureTransfer-1.0.0.dmg"
        else
            print_error "macOS DMG package failed"
            exit 1
        fi
        ;;
    "all")
        print_status "Building all available packages for current platform..."
        
        case "$OSTYPE" in
            msys*|win32*|cygwin*)
                print_status "Windows detected - building MSI..."
                mvn jpackage:jpackage -Pwindows-msi
                ;;
            linux-gnu*)
                print_status "Linux detected - building DEB and AppImage..."
                mvn jpackage:jpackage -Plinux-deb
                mvn jpackage:jpackage -Plinux-appimage
                ;;
            darwin*)
                print_status "macOS detected - building DMG..."
                mvn jpackage:jpackage -Pmac-dmg
                ;;
            *)
                print_warning "Unknown OS type: $OSTYPE"
                print_status "Please specify a specific platform instead of 'all'"
                exit 1
                ;;
        esac
        
        print_success "All available packages built successfully"
        ;;
    *)
        print_error "Unknown platform: $PLATFORM"
        usage
        ;;
esac

print_success "Desktop build completed!"
print_status "Check target/installer/ directory for your installer files"
print_status "For development testing, use: mvn javafx:run"
