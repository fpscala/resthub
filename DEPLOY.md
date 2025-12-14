# NestHub Deployment Guide

## Prerequisites

1. Docker and Docker Compose installed
2. A domain name (for production SSL setup)
3. Telegram Bot Token from @BotFather

## Quick Start (Development)

1. **Copy environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit .env file:**
   - Add your Telegram Bot Token
   - Set the webhook URL (use ngrok for local testing: `ngrok http 8080`)

3. **Start services:**
   ```bash
   # Start development services (no SSL/Nginx)
   docker-compose up -d postgres redis minio backend-api frontend telegram-bot
   ```

4. **Initialize MinIO bucket:**
   - Open http://localhost:9001 in browser
   - Login with minioadmin/minioadmin123
   - Create bucket named `nesthub-uploads`

5. **Access applications:**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api
   - MinIO Console: http://localhost:9001
   - MinIO API: http://localhost:9000

## Production Deployment

1. **Prepare your domain:**
   - Point your domain's A record to your server's IP
   - Make sure ports 80 and 443 are open

2. **Create production .env file:**
   ```bash
   cp .env.example .env
   # Edit with your domain and production settings
   ```

3. **Start with SSL:**
   ```bash
   # Start base services first
   docker-compose up -d postgres redis minio backend-api frontend

   # Get initial SSL certificate
   docker-compose -f docker-compose.yml --profile production run --rm certbot \
     certonly --webroot --webroot-path=/var/www/certbot \
     --email your-email@example.com --agree-tos --no-eff-email \
     -d your-domain.com

   # Start Nginx with SSL
   docker-compose -f docker-compose.yml --profile production up -d nginx
   ```

4. **Update nginx configuration:**
   - Edit `nginx/conf.d/default.conf`
   - Replace `your-domain.com` with your actual domain
   - Restart Nginx: `docker-compose restart nginx`

5. **Setup Telegram webhook:**
   ```bash
   # The webhook will be automatically set when the bot starts
   # Make sure your domain is accessible from the internet
   ```

## Docker Compose Profiles

- **Default**: Development services (postgres, redis, minio, backend, frontend, bot)
- **Production**: Adds Nginx reverse proxy with SSL and Certbot

```bash
# Development
docker-compose up -d

# Production with SSL
docker-compose --profile production up -d
```

## Environment Variables

Key variables to configure in `.env`:

- `TELEGRAM_BOT_TOKEN`: Get from @BotFather
- `TELEGRAM_WEBHOOK_URL`: Your domain + /webhook (e.g., https://nesthub.com/webhook)
- `JWT_SECRET`: Random secret string for JWT tokens
- `DATABASE_PASSWORD`: Custom database password
- `AWS_ACCESS_KEY_ID`: S3 access key (minioadmin for MinIO)
- `AWS_SECRET_ACCESS_KEY`: S3 secret key (minioadmin123 for MinIO)

## Monitoring

Check service logs:
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend-api
docker-compose logs -f telegram-bot
```

## Common Issues

1. **SSL Certificate Issues:**
   - Make sure your domain points to the server
   - Wait for DNS propagation (can take up to 24 hours)
   - Check Certbot logs: `docker-compose logs certbot`

2. **Telegram Bot Not Responding:**
   - Verify bot token is correct
   - Check webhook URL is accessible
   - Check bot logs: `docker-compose logs telegram-bot`

3. **Database Connection Issues:**
   - Wait for PostgreSQL to be healthy
   - Check connection string in .env
   - Verify database exists

4. **Image Upload Issues:**
   - Ensure MinIO bucket `nesthub-uploads` exists
   - Check S3 credentials in .env
   - Verify CORS settings in MinIO

## Backup

Backup data volumes:
```bash
# Database
docker-compose exec postgres pg_dump -U nesthub_user nesthub > backup.sql

# MinIO data
docker run --rm -v nesthub_minio_data:/data -v $(pwd):/backup ubuntu tar czf /backup/minio-backup.tar.gz -C /data .
```

## Security Notes

- Change default passwords in production
- Use strong JWT secret
- Enable firewall
- Regularly update Docker images
- Monitor logs for suspicious activity