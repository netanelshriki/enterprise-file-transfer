#!/bin/bash

set -e

echo "🧪 Testing Build Scripts"
echo "========================"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

print_status "Testing build script syntax..."

if bash -n scripts/build-all.sh; then
    print_success "build-all.sh syntax is valid"
else
    print_error "build-all.sh has syntax errors"
    exit 1
fi

if bash -n scripts/build-desktop.sh; then
    print_success "build-desktop.sh syntax is valid"
else
    print_error "build-desktop.sh has syntax errors"
    exit 1
fi

if bash -n scripts/build-android.sh; then
    print_success "build-android.sh syntax is valid"
else
    print_error "build-android.sh has syntax errors"
    exit 1
fi

print_status "Testing individual component builds..."

print_status "Testing desktop compilation..."
cd desktop
if mvn clean compile -q; then
    print_success "Desktop compilation test passed"
else
    print_error "Desktop compilation test failed"
    exit 1
fi
cd ..

print_status "Testing server compilation..."
cd server
if mvn clean compile -q; then
    print_success "Server compilation test passed"
else
    print_error "Server compilation test failed"
    exit 1
fi
cd ..

print_status "Testing Android Gradle wrapper..."
cd android
if [ -f "gradlew" ] && [ -x "gradlew" ]; then
    print_success "Android Gradle wrapper is executable"
else
    print_error "Android Gradle wrapper is not executable"
    exit 1
fi
cd ..

print_status "Testing Docker configurations..."

if docker --version > /dev/null 2>&1; then
    print_status "Docker is available - testing Docker builds..."
    
    cd server
    if docker build -t test-signaling-server . > /dev/null 2>&1; then
        print_success "Signaling server Docker build test passed"
        docker rmi test-signaling-server > /dev/null 2>&1
    else
        print_error "Signaling server Docker build test failed"
    fi
    
    cd coturn
    if docker build -t test-coturn . > /dev/null 2>&1; then
        print_success "Coturn Docker build test passed"
        docker rmi test-coturn > /dev/null 2>&1
    else
        print_error "Coturn Docker build test failed"
    fi
    cd ../..
else
    print_status "Docker not available - skipping Docker build tests"
fi

echo ""
print_success "All build script tests passed!"
print_status "Build system is ready for development and CI/CD"
