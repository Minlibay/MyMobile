# PowerShell скрипт для загрузки файлов на VPS
$SERVER = "45.134.12.54"
$USER = "root"
$REMOTE_DIR = "/opt/zhivoy-backend"

Write-Host "=== Uploading Zhivoy Backend to VPS ===" -ForegroundColor Green

# Проверка наличия SCP (через OpenSSH)
if (-not (Get-Command scp -ErrorAction SilentlyContinue)) {
    Write-Host "SCP not found. Please install OpenSSH client." -ForegroundColor Red
    exit 1
}

# Создание директории на сервере
Write-Host "Creating remote directory..." -ForegroundColor Yellow
ssh "${USER}@${SERVER}" "mkdir -p ${REMOTE_DIR}"

# Загрузка файлов
Write-Host "Uploading files..." -ForegroundColor Yellow

$files = @(
    "app",
    "alembic",
    "alembic.ini",
    "pyproject.toml",
    "Dockerfile",
    "docker-compose.yml",
    "nginx.conf",
    "setup-server.sh"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "  Uploading $file..." -ForegroundColor Cyan
        if (Test-Path $file -PathType Container) {
            scp -r $file "${USER}@${SERVER}:${REMOTE_DIR}/"
        } else {
            scp $file "${USER}@${SERVER}:${REMOTE_DIR}/"
        }
    } else {
        Write-Host "  Warning: $file not found" -ForegroundColor Yellow
    }
}

Write-Host "`n=== Upload complete! ===" -ForegroundColor Green
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. SSH to server: ssh ${USER}@${SERVER}"
Write-Host "2. cd ${REMOTE_DIR}"
Write-Host "3. Run: bash setup-server.sh"



