#!/bin/bash
# Скрипт для проверки статуса сервера и логов

echo "=== Проверка статуса контейнеров ==="
cd /opt/zhivoy-backend/backend
docker-compose ps

echo ""
echo "=== Логи API (последние 50 строк) ==="
docker-compose logs --tail=50 api

echo ""
echo "=== Проверка подключения к БД ==="
docker-compose exec -T postgres psql -U zhivoy -d zhivoy -c "\dt"

echo ""
echo "=== Проверка миграций ==="
docker-compose exec api alembic current

echo ""
echo "=== Проверка таблицы users ==="
docker-compose exec -T postgres psql -U zhivoy -d zhivoy -c "SELECT COUNT(*) FROM users;"









