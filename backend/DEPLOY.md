# Деплой Zhivoy Backend на VPS

## Быстрый старт

### 1. Подключись к серверу
```bash
ssh root@45.134.12.54
```

### 2. Выполни скрипт установки зависимостей
```bash
# Скопируй содержимое deploy.sh и выполни на сервере
bash <(curl -s https://raw.githubusercontent.com/.../deploy.sh)
# ИЛИ загрузи файл и выполни:
# scp backend/deploy.sh root@45.134.12.54:/tmp/
# ssh root@45.134.12.54 "bash /tmp/deploy.sh"
```

### 3. Загрузи файлы бекенда на сервер

**Вариант A: Через SCP (с локальной машины)**
```bash
# Создай директорию на сервере
ssh root@45.134.12.54 "mkdir -p /opt/zhivoy-backend"

# Загрузи файлы
scp -r backend/app root@45.134.12.54:/opt/zhivoy-backend/
scp -r backend/alembic root@45.134.12.54:/opt/zhivoy-backend/
scp backend/alembic.ini root@45.134.12.54:/opt/zhivoy-backend/
scp backend/pyproject.toml root@45.134.12.54:/opt/zhivoy-backend/
scp backend/Dockerfile root@45.134.12.54:/opt/zhivoy-backend/
scp backend/docker-compose.yml root@45.134.12.54:/opt/zhivoy-backend/
scp backend/nginx.conf root@45.134.12.54:/opt/zhivoy-backend/
scp backend/setup-server.sh root@45.134.12.54:/opt/zhivoy-backend/
```

**Вариант B: Через Git (если репозиторий публичный)**
```bash
ssh root@45.134.12.54
cd /opt
git clone <your-repo-url> zhivoy-backend
cd zhivoy-backend/backend
```

### 4. Настрой и запусти

```bash
ssh root@45.134.12.54
cd /opt/zhivoy-backend

# Создай .env файл
cat > .env <<EOF
DATABASE_URL=postgresql://zhivoy:zhivoy_password@postgres:5432/zhivoy
JWT_SECRET=$(openssl rand -hex 32)
ACCESS_TOKEN_TTL=900
REFRESH_TOKEN_TTL=2592000
ADMIN_API_KEY=$(openssl rand -hex 32)
EOF

# Запусти сервисы
docker-compose up -d

# Дождись запуска БД (10 секунд)
sleep 10

# Выполни миграции
docker-compose exec -T api alembic upgrade head

# Настрой Nginx
cp nginx.conf /etc/nginx/sites-available/zhivoy
ln -sf /etc/nginx/sites-available/zhivoy /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
```

### 5. Проверь работу

```bash
# Проверь здоровье API
curl http://45.134.12.54/health

# Проверь документацию
curl http://45.134.12.54/docs
```

## Настройка HTTPS (опционально, нужен домен)

Если у тебя есть домен, указывающий на IP 45.134.12.54:

```bash
ssh root@45.134.12.54
certbot --nginx -d yourdomain.com --non-interactive --agree-tos --email your@email.com --redirect
systemctl reload nginx
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

# Обновление кода
cd /opt/zhivoy-backend
# Загрузи новые файлы
docker-compose restart api
```












