#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Скрипт створення резервної копії БД ReadMania (PostgreSQL) - вимога SRS R5
# ==============================================================================
# Формат: стиснений кастомний дамп pg_dump (-Fc) для швидкого та надійного відновлення.
# Зберігання: ротація локальних копій з видаленням старших за 14 днів (RETENTION_DAYS).
# Опційно: копіювання у віддалене S3/хмарне сховище за допомогою rclone.
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

BACKUP_DIR="${BACKUP_DIR:-${PROJECT_ROOT}/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
DB_CONTAINER="${DB_CONTAINER:-readmania-db}"
DB_NAME="${DB_NAME:-readmania}"
DB_USER="${DB_USER:-readmania}"
OFFSITE_REMOTE="${OFFSITE_REMOTE:-}"

TIMESTAMP="$(date +%Y-%m-%d_%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/readmania-${TIMESTAMP}.dump"
LATEST_LINK="${BACKUP_DIR}/readmania-latest.dump"

mkdir -p "${BACKUP_DIR}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🚀 Початок створення резервної копії БД ReadMania..."

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

# Виконання pg_dump у форматі custom (-Fc)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 📦 Створення дампу бази даних '${DB_NAME}'..."
docker exec "${DB_CONTAINER}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" -Fc > "${BACKUP_FILE}"

# Перевірка валідності створеного файлу
if [ ! -s "${BACKUP_FILE}" ]; then
    echo "❌ Помилка: створений файл дампу '${BACKUP_FILE}' порожній або не існує." >&2
    rm -f "${BACKUP_FILE}"
    exit 1
fi

BACKUP_SIZE="$(du -h "${BACKUP_FILE}" | cut -f1)"
echo "✅ Резервну копію успішно створено: ${BACKUP_FILE} (розмір: ${BACKUP_SIZE})"

# Оновлення посилання на останній дамп
ln -sf "readmania-${TIMESTAMP}.dump" "${LATEST_LINK}"

# Ротація старих резервних копій (зберігання RETENTION_DAYS днів)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🧹 Очищення локальних резервних копій, старших за ${RETENTION_DAYS} днів..."
DELETED_COUNT=0
while IFS= read -r old_file; do
    if [ -n "${old_file}" ]; then
        rm -f "${old_file}"
        echo "   Видалено застарілу копію: ${old_file}"
        DELETED_COUNT=$((DELETED_COUNT + 1))
    fi
done < <(find "${BACKUP_DIR}" -maxdepth 1 -name 'readmania-*.dump' ! -name 'readmania-latest.dump' -type f -mtime +"${RETENTION_DAYS}")

if [ "${DELETED_COUNT}" -eq 0 ]; then
    echo "   Застарілих локальних копій для видалення не знайдено."
fi

# Опційне копіювання у віддалене сховище
if [ -n "${OFFSITE_REMOTE}" ]; then
    if command -v rclone >/dev/null 2>&1; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ☁️  Копіювання дампу у віддалене сховище: ${OFFSITE_REMOTE}..."
        rclone copy "${BACKUP_FILE}" "${OFFSITE_REMOTE}"
        rclone delete --min-age "${RETENTION_DAYS}d" "${OFFSITE_REMOTE}" || true
        echo "✅ Віддалену синхронізацію завершено."
    else
        echo "⚠️  Попередження: OFFSITE_REMOTE задано, але утиліту rclone не знайдено." >&2
    fi
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🎉 Резервне копіювання успішно завершено."
