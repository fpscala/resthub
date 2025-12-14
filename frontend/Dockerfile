# Build stage
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Build the application with proper tracing
RUN npm run build

# Production stage
FROM node:18-alpine AS runner

WORKDIR /app

# Install curl, gettext (for envsubst), and create a non-root user
RUN apk add --no-cache curl gettext && \
    addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy built application
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# Copy runtime config template and entrypoint script
COPY --chown=nextjs:nodejs public/runtime-config.js.template /app/public/
COPY --chown=nextjs:nodejs docker-entrypoint.sh /app/docker-entrypoint.sh

# Set ownership
USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"
ENV NODE_ENV=production

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000 || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["node", "server.js"]