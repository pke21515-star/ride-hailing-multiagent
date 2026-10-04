@echo off
chcp 65001 >nul
title 网约车多Agent协同调度系统 - 8871
cd /d "%~dp0"
:: --server.port=8871 显式指定端口（防止环境中 SERVER__PORT 等变量覆盖 yml 配置）
if exist "target\ride-hailing-multiagent-1.0.0.jar" (
  "%JAVA_HOME%\bin\java.exe" -jar target\ride-hailing-multiagent-1.0.0.jar --server.port=8871
) else (
  echo [首次运行] 正在打包...
  call mvn -q -o package -DskipTests
  "%JAVA_HOME%\bin\java.exe" -jar target\ride-hailing-multiagent-1.0.0.jar --server.port=8871
)
pause
