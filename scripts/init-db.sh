#!/usr/bin/env bash
set -euo pipefail

# 初始化 living-frame 数据库。所有表结构、演示数据都复用项目内的 SQL 文件。
# 用法:
#   MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USER=root MYSQL_PASSWORD=xxx \
#     ./scripts/init-db.sh
#   DRY_RUN=1 ./scripts/init-db.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_DIR="$ROOT_DIR/living-frame-admin/src/main/resources/db"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-living_frame}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

for file in "$DB_DIR/00-create-database.sql" "$DB_DIR/schema.sql" "$DB_DIR/data.sql"; do
    if [[ ! -f "$file" ]]; then
        echo "找不到数据库初始化文件: $file" >&2
        exit 1
    fi
done

if [[ ! "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "数据库名不合法: $MYSQL_DATABASE" >&2
    exit 1
fi

TEMP_FILE="$(mktemp)"
trap 'rm -f "$TEMP_FILE"' EXIT

{
    cat "$DB_DIR/00-create-database.sql"
    printf '\nUSE `%s`;\n' "$MYSQL_DATABASE"
    cat "$DB_DIR/schema.sql"
    cat "$DB_DIR/data.sql"
} > "$TEMP_FILE"

if [[ "${DRY_RUN:-0}" == "1" ]]; then
    cat "$TEMP_FILE"
    exit 0
fi

if ! command -v "$MYSQL_BIN" >/dev/null 2>&1; then
    echo "找不到 mysql 客户端: $MYSQL_BIN" >&2
    exit 1
fi

export MYSQL_PWD="$MYSQL_PASSWORD"
"$MYSQL_BIN" \
    --host="$MYSQL_HOST" \
    --port="$MYSQL_PORT" \
    --user="$MYSQL_USER" \
    --default-character-set=utf8mb4 \
    < "$TEMP_FILE"

echo "数据库初始化完成: $MYSQL_DATABASE"
