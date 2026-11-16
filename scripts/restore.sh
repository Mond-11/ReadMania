#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Скрипт відновлення БД ReadMania з резервної копії (pg_restore) - вимога SRS R4, R5
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

BACKUP_DIR="${BACKUP_DIR:-${PROJECT_ROOT}/backups}"
DB_CONTAINER="${DB_CONTAINER:-readmania-db}"
DB_NAME="${DB_NAME:-readmania}"
DB_USER="${DB_USER:-readmania}"

RESTORE_FILE="${1:-}"

# Якщо файл не вказано — шукаємо останній доступний дамп
if [ -z "${RESTORE_FILE}" ]; then
    if [ -L "${BACKUP_DIR}/readmania-latest.dump" ] && [ -e "${BACKUP_DIR}/readmania-latest.dump" ]; then
        RESTORE_FILE="${BACKUP_DIR}/readmania-latest.dump"
    elif [ -d "${BACKUP_DIR}" ]; then
        RESTORE_FILE="$(find "${BACKUP_DIR}" -maxdepth 1 -name 'readmania-*.dump' ! -name 'readmania-latest.dump' -type f | sort -r | head -n 1)"
    fi
fi

if [ -z "${RESTORE_FILE}" ] || [ ! -f "${RESTORE_FILE}" ]; then
    echo "❌ Помилка: файл резервної копії не знайдено." >&2
    echo "   Використання: $0 [шлях_до_файлу_дампу.dump]" >&2
    exit 1
fi

if [ ! -s "${RESTORE_FILE}" ]; then
    echo "❌ Помилка: файл дампу '${RESTORE_FILE}' порожній." >&2
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🚀 Початок відновлення БД ReadMania з файлу: ${RESTORE_FILE}"

# Перевірка наявності Docker
if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Помилка: docker не встановлено або недоступний у PATH." >&2
    exit 1
fi

# Перевірка доступності контейнера БД
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    echo "❌ Помилка: контейнер БД '${DB_CONTAINER}' не запущений." >&2
    echo "   Запустіть БД командою: ./run-docker.sh up або docker compose up -d db" >&2
    exit 1
fi

# Перевірка готовності PostgreSQL
if ! docker exec "${DB_CONTAINER}" pg_isready -U "${DB_USER}" -d "${DB_NAME}" >/dev/null 2>&1; then
    echo "❌ Помилка: PostgreSQL у контейнері '${DB_CONTAINER}' не готовий до підключення." >&2
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🔄 Відновлення схеми та даних через pg_restore..."
docker exec -i "${DB_CONTAINER}" pg_restore -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists --no-owner < "${RESTORE_FILE}" || {
    EXIT_CODE=$?
    # pg_restore може повертати код 1 у разі незначних попереджень (warnings), перевіримо стан
    if [ "${EXIT_CODE}" -ne 0 ] && [ "${EXIT_CODE}" -ne 1 ]; then
        echo "❌ Помилка під час виконання pg_restore (код: ${EXIT_CODE})." >&2
        exit "${EXIT_CODE}"
    fi
}

echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ Базу даних успішно відновлено з ${RESTORE_FILE}!"
