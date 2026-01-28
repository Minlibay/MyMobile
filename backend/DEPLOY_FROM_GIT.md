 # Деплой Alta Backend с Git

## Быстрая инструкция

### 1. Подключись к серверу
```bash
ssh root@45.134.12.54
# Пароль: NWI605OWxT1O
```

### 2. Установи зависимости (один раз)
```bash
bash <(curl -sSL https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/main/backend/full-deploy.sh)
```

Или выполни вручную:
```bash
# Обновление системы
apt-get update -y && apt-get upgrade -y

# Установка Docker
if ! command -v docker &> /dev/null; then
    apt-get install -y ca-certificates curl gnupg lsb-release
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    systemctl enable docker
    systemctl start docker
fi

# Установка Docker Compose
if ! command -v docker-compose &> /dev/null; then
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
fi

# Установка Nginx
if ! command -v nginx &> /dev/null; then
    apt-get install -y nginx
    systemctl enable nginx
    systemctl start nginx
fi
```

### 3. Клонируй репозиторий
```bash
cd /opt
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git com.volovod.com.volovod.altavolovod.alta-backend
cd com.volovod.com.volovod.altavolovod.alta-backend/backend
```

### 4. Создай .env файл
```bash
cat > .env <<EOF
DATABASE_URL=postgresql+psycopg://zhivoy:zhivoy_password@postgres:5432/zhivoy
POSTGRES_DB=zhivoy
POSTGRES_USER=zhivoy
POSTGRES_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -hex 32)
ACCESS_TTL_SECONDS=900
REFRESH_TTL_SECONDS=2592000
ADMIN_API_KEY=$(openssl rand -hex 32)
EOF

chmod 600 .env
```

### 5. Запусти сервисы
```bash
docker-compose up -d
```

### 6. Дождись запуска БД и выполни миграции
```bash
# Подожди 10 секунд
sleep 10

# Выполни миграции
docker-compose exec api alembic upgrade head
```

### 7. Настрой Nginx
```bash
# Скопируй конфигурацию
cp nginx.conf /etc/nginx/sites-available/com.volovod.com.volovod.altavolovod.alta

# Создай симлинк
ln -sf /etc/nginx/sites-available/com.volovod.com.volovod.altavolovod.alta /etc/nginx/sites-enabled/

# Удали дефолтный сайт
rm -f /etc/nginx/sites-enabled/default

# Проверь конфигурацию
nginx -t

# Перезагрузи Nginx
systemctl reload nginx
```

### 8. Проверь работу
```bash
# Проверь здоровье API
curl http://45.134.12.54/health

# Проверь документацию
curl http://45.134.12.54/docs
```

## Обновление кода

```bash
cd /opt/com.volovod.com.volovod.altavolovod.alta-backend/backend
git pull
docker-compose build api
docker-compose up -d
docker-compose exec api alembic upgrade head
```

## Полезные команды

```bash
# Логи API
docker-compose logs -f api

# Логи БД
docker-compose logs -f postgres

# Перезапуск
docker-compose restart api

# Остановка
docker-compose down

# Просмотр статуса
docker-compose ps
```

## Настройка HTTPS (опционально, нужен домен)

Если у тебя есть домен, указывающий на IP 45.134.12.54:

```bash
apt-get install -y certbot python3-certbot-nginx
certbot --nginx -d yourdomain.com --non-interactive --agree-tos --email your@email.com --redirect
systemctl reload nginx
```

