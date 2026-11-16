#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Демон автоматичного щоденного резервного копіювання PostgreSQL (SRS R5)
# ==============================================================================
# - Щоденне створення стисненого дампу pg_dump (-Fc)
# - Автоматичне видалення резервних копій, старших за 14 днів (RETENTION_DAYS)
# - Час щоденного запуску: BACKUP_TIME (за замовчуванням 03:00 UTC)
# ==============================================================================

BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
PGHOST="${PGHOST:-db}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-readmania}"
PGUSER="${PGUSER:-readmania}"
PGPASSWORD="${PGPASSWORD:-readmania}"
BACKUP_TIME="${BACKUP_TIME:-03:00}"
RUN_ON_STARTUP="${RUN_ON_STARTUP:-true}"

export PGPASSWORD

mkdir -p "${BACKUP_DIR}"

trap 'echo "[$(date "+%Y-%m-%d %H:%M:%S")] 🛑 Отримано сигнал зупинки демона резервного копіювання."; exit 0' SIGTERM SIGINT

do_backup() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🚀 [Automated Backup] Початок автоматичного резервного копіювання..."
    
    # Очікування доступності БД
    local retries=10
    while ! pg_isready -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" >/dev/null 2>&1; do
        retries=$((retries - 1))
        if [ "$retries" -le 0 ]; then
            echo "❌ [Automated Backup] Помилка: PostgreSQL (${PGHOST}:${PGPORT}) недоступний." >&2
            return 1
        fi
        echo "⏳ [Automated Backup] Очікування готовності PostgreSQL..."
        sleep 3
    done

    local timestamp
    timestamp="$(date +%Y-%m-%d_%H%M%S)"
    local backup_file="${BACKUP_DIR}/readmania-${timestamp}.dump"
    local latest_link="${BACKUP_DIR}/readmania-latest.dump"

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 📦 [Automated Backup] Виконання pg_dump у файл ${backup_file}..."
    pg_dump -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -Fc > "${backup_file}"

    if [ ! -s "${backup_file}" ]; then
        echo "❌ [Automated Backup] Помилка: створений файл дампу порожній або відсутній." >&2
        rm -f "${backup_file}"
        return 1
    fi

    local backup_size
    backup_size="$(du -h "${backup_file}" | cut -f1)"
    echo "✅ [Automated Backup] Резервну копію успішно створено: ${backup_file} (${backup_size})"

    ln -sf "readmania-${timestamp}.dump" "${latest_link}"

    # Очищення старих копій (зберігання RETENTION_DAYS днів, після чого видалення)
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🧹 [Automated Backup] Очищення копій, старших за ${RETENTION_DAYS} днів..."
    local deleted_count=0
    while IFS= read -r old_file; do
        if [ -n "${old_file}" ]; then
            rm -f "${old_file}"
            echo "   Видалено застарілий дамп: ${old_file}"
            deleted_count=$((deleted_count + 1))
        fi
    done < <(find "${BACKUP_DIR}" -maxdepth 1 -name 'readmania-*.dump' ! -name 'readmania-latest.dump' -type f -mtime +"${RETENTION_DAYS}")

    if [ "${deleted_count}" -eq 0 ]; then
        echo "   Застарілих копій для видалення не знайдено."
    fi

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🎉 [Automated Backup] Резервне копіювання завершено успішно."
}

calculate_sleep_seconds() {
    local now
    now="$(date +%s)"
    local today_target
    today_target="$(date -d "today ${BACKUP_TIME}:00" +%s 2>/dev/null || date -d "${BACKUP_TIME}" +%s)"
    
    local next_run
    if [ "${now}" -ge "${today_target}" ]; then
        next_run="$(date -d "tomorrow ${BACKUP_TIME}:00" +%s 2>/dev/null || date -d "tomorrow ${BACKUP_TIME}" +%s)"
    else
        next_run="${today_target}"
    fi

    local diff=$((next_run - now))
    echo "${diff}"
}

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🛡️  Запуск демона резервного копіювання БД (розклад: щоденно о ${BACKUP_TIME}, зберігання: ${RETENTION_DAYS} днів)..."

if [ "${RUN_ON_STARTUP}" = "true" ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ℹ️  Виконання початкового дампу при старті сервісу..."
    do_backup || true
fi

while true; do
    sleep_sec="$(calculate_sleep_seconds)"
    next_date="$(date -d "+${sleep_sec} seconds" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo "через ${sleep_sec} сек")"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ⏳ Наступний щоденний автоматичний дамп заплановано на: ${next_date} (через ${sleep_sec} с)"
    
    # Спимо інтервалами по 60 секунд для можливості швидкого перехоплення сигналів
    while [ "${sleep_sec}" -gt 0 ]; do
        sleep_chunk=60
        if [ "${sleep_sec}" -lt 60 ]; then
            sleep_chunk="${sleep_sec}"
        fi
        sleep "${sleep_chunk}"
        sleep_sec=$((sleep_sec - sleep_chunk))
    done

    do_backup || true
done
