#!/bin/bash

echo "Building NestHub Frontend with new authentication system..."

# Clean any previous build
rm -rf .next build

# Build with production optimizations
npm run build

echo "Build completed!"
echo "Now run: docker build -t fpscala/nesthub-frontend:latest ."

# Optional: Build Docker image locally
# Uncomment the next line if you want to build Docker image immediately
# docker build -t fpscala/nesthub-frontend:latest .