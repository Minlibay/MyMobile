#!/bin/bash
set -e

echo "=== Setting up Zhivoy Backend on VPS ==="

# Create app directory
APP_DIR="/opt/zhivoy-backend"
mkdir -p $APP_DIR
cd $APP_DIR

# Copy files (assuming we're running from backend directory)
echo "Copying backend files..."
# This will be done via scp from local machine

# Create .env file if not exists
if [ ! -f .env ]; then
    echo "Creating .env file..."
    cat > .env <<EOF
DATABASE_URL=postgresql://zhivoy:zhivoy_password@postgres:5432/zhivoy
JWT_SECRET=$(openssl rand -hex 32)
ACCESS_TOKEN_TTL=900
REFRESH_TOKEN_TTL=2592000
ADMIN_API_KEY=$(openssl rand -hex 32)
EOF
    echo ".env file created. Please review and update if needed."
fi

# Set permissions
chmod 600 .env

# Start services
echo "Starting Docker Compose services..."
docker-compose up -d

# Wait for database
echo "Waiting for database..."
sleep 10

# Run migrations
echo "Running database migrations..."
docker-compose exec -T api alembic upgrade head

# Setup Nginx
echo "Configuring Nginx..."
cp nginx.conf /etc/nginx/sites-available/zhivoy
ln -sf /etc/nginx/sites-available/zhivoy /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Test Nginx config
nginx -t

# Reload Nginx
systemctl reload nginx

echo ""
echo "NOTE: Let's Encrypt requires a domain name, not an IP address."
echo "To enable HTTPS later, add a domain and run:"
echo "  certbot --nginx -d yourdomain.com"

echo "=== Setup complete! ==="
echo "Backend should be available at https://45.134.12.54"
echo "API docs: https://45.134.12.54/docs"

