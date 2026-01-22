# Zhivoy Backend (FastAPI)

FastAPI + PostgreSQL backend for:
- User registration/login (login + password)
- Long-lived sessions via access+refresh token rotation
- Remote ads config (Yandex placements/ad unit ids)

## Local run (Docker)

```bash
cd backend
docker compose up --build
```

API will be available on `http://localhost:8080` (see `docker-compose.yml`).

## Environment

See `env.example`.


