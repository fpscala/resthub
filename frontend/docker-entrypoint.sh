#!/bin/sh

# Set default values if environment variables are not set
export NEXT_PUBLIC_API_URL="${NEXT_PUBLIC_API_URL:-http://localhost:8080}"
export NEXT_PUBLIC_S3_BUCKET_URL="${NEXT_PUBLIC_S3_BUCKET_URL:-http://localhost:9000}"

echo "=== Runtime Configuration ==="
echo "NEXT_PUBLIC_API_URL: $NEXT_PUBLIC_API_URL"
echo "NEXT_PUBLIC_S3_BUCKET_URL: $NEXT_PUBLIC_S3_BUCKET_URL"
echo "==========================="

# Substitute environment variables in runtime-config.js
envsubst < /app/public/runtime-config.js.template > /app/public/runtime-config.js

echo "Generated runtime-config.js:"
cat /app/public/runtime-config.js
echo "==========================="

# Execute the original command
exec "$@"