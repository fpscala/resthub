#!/bin/sh

# Set default values if environment variables are not set
export BACKEND_API_URL="${BACKEND_API_URL:-http://localhost:8080}"
export NEXT_PUBLIC_S3_BUCKET_URL="${NEXT_PUBLIC_S3_BUCKET_URL:-http://localhost:9000}"
export NODE_ENV="${NODE_ENV:-production}"
export LOG_LEVEL="${LOG_LEVEL:-info}"

echo "=== Server Configuration ==="
echo "BACKEND_API_URL: $BACKEND_API_URL"
echo "NEXT_PUBLIC_S3_BUCKET_URL: $NEXT_PUBLIC_S3_BUCKET_URL"
echo "NODE_ENV: $NODE_ENV"
echo "LOG_LEVEL: $LOG_LEVEL"
echo "PORT: ${PORT:-3000}"
echo "=========================="

# Log startup
echo "[$(date)] Starting Next.js application..."

# No runtime-config.js needed anymore - using server-side only environment variables

# Execute the original command with logging
echo "[$(date)] Executing: $@"
exec "$@"