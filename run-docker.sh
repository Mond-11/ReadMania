#!/usr/bin/env bash
set -e

# Скрипт для керування локальним середовищем ReadMania у Docker

COMMAND="${1:-up}"

case "$COMMAND" in
  up|start)
    echo "=================================================="
    echo "Запуск ReadMania (PostgreSQL 17 + Spring Boot Backend)..."
    echo "=================================================="
    docker compose up -d --build
    echo ""
    echo "✅ Контейнери запущені у фоновому режимі!"
    echo "Backend: http://localhost:8080"
    echo "PostgreSQL: localhost:5432 (db: readmania, user: readmania)"
    echo "Healthcheck: http://localhost:8080/actuator/health"
    echo ""
    echo "Для перегляду логів виконайте: ./run-docker.sh logs"
    ;;
  down|stop)
    echo "Зупинка контейнерів ReadMania..."
    docker compose down
    echo "Контейнери успішно зупинено."
    ;;
  restart)
    echo "Перезапуск контейнерів..."
    docker compose down
    docker compose up -d --build
    echo "Перезапуск завершено."
    ;;
  logs)
    docker compose logs -f
    ;;
  status|ps)
    docker compose ps
    ;;
  clean)
    echo "Повне очищення (контейнери + томи даних)..."
    docker compose down -v
    echo "Очищення завершено."
    ;;
  *)
    echo "Використання: $0 {up|down|restart|logs|status|clean}"
    exit 1
    ;;
esac
