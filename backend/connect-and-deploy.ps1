# PowerShell скрипт для подключения и деплоя
$SERVER = "45.134.12.54"
$USER = "root"
$PASSWORD = "NWI605OWxT1O"
$REMOTE_DIR = "/opt/zhivoy-backend"

Write-Host "=== Connecting to VPS and deploying Zhivoy Backend ===" -ForegroundColor Green

# Функция для выполнения команд через SSH с паролем
function Invoke-SSHCommand {
    param(
        [string]$Command,
        [string]$Server,
        [string]$User,
        [string]$Password
    )
    
    # Используем sshpass если доступен, иначе используем expect-подобный подход
    # Для Windows можно использовать plink (PuTTY) или установить sshpass через WSL
    
    # Попробуем через WSL если доступен
    if (Get-Command wsl -ErrorAction SilentlyContinue) {
        $wslCommand = "echo '$Password' | sshpass -p '$Password' ssh -o StrictHostKeyChecking=no ${User}@${Server} '$Command'"
        wsl bash -c $wslCommand
    } else {
        Write-Host "WSL not found. Please run commands manually:" -ForegroundColor Yellow
        Write-Host "ssh ${User}@${Server}" -ForegroundColor Cyan
        Write-Host "Password: $Password" -ForegroundColor Cyan
        Write-Host "Then run: $Command" -ForegroundColor Cyan
    }
}

# Проверка подключения
Write-Host "`nTesting connection..." -ForegroundColor Yellow
$testCmd = "echo 'Connection OK' && uname -a"
Invoke-SSHCommand -Command $testCmd -Server $SERVER -User $USER -Password $PASSWORD

Write-Host "`n=== Manual deployment steps ===" -ForegroundColor Green
Write-Host "Since interactive password is required, please run these commands manually:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Connect to server:" -ForegroundColor Cyan
Write-Host "   ssh root@45.134.12.54" -ForegroundColor White
Write-Host "   Password: NWI605OWxT1O" -ForegroundColor White
Write-Host ""
Write-Host "2. Run deployment script:" -ForegroundColor Cyan
Write-Host "   bash <(cat <<'DEPLOY_SCRIPT'" -ForegroundColor White
Get-Content deploy.sh | Write-Host
Write-Host "DEPLOY_SCRIPT" -ForegroundColor White
Write-Host "   )" -ForegroundColor White











