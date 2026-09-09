#!/usr/bin/env bash
# =====================================================================
# 一键执行「小程序权限改为员工级 + 菜单合并」迁移（2026-09-09）
# 范围：hr_0001 ~ hr_0006（租户段 1-3，逐库替换 {DB}）+ hrsystem（系统段 4，执行一次）
#
# 用法：
#   bash run_mp_employee_permission_hr0001_0006.sh
#
# 自定义：
#   - 连接参数：改下方 DB_HOST / DB_PORT / DB_USER / DB_PASS
#   - 库范围：改 START / END（保持 4 位宽度，如 hr_0007 ~ hr_0008 设 START=7 END=8）
#
# 说明：
#   脚本按迁移文件中的标记 "-- 4. 系统库 hrsystem" 自动拆分为两段，
#   租户段对每段 hr_XXXX 执行并把 {DB} 占位符替换为真实库名；
#   系统段仅在 hrsystem 执行一次。两段均幂等（列已存在则跳过 / DELETE 可重复）。
# =====================================================================
set -uo pipefail

DB_HOST="127.0.0.1"
DB_PORT="13306"
DB_USER="root"
DB_PASS="test123456"
START=1
END=6

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$SCRIPT_DIR/2026-09-09_mp_employee_permission.sql"
MARKER="-- 4. 系统库 hrsystem"

MYSQL="mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS"

[ -f "$SCRIPT" ] || { echo "❌ 找不到迁移脚本: $SCRIPT"; exit 1; }

# 按标记拆分为 租户段(1-3) 与 系统段(4)
TENANT_SQL="$(awk "/^${MARKER}/{exit} {print}" "$SCRIPT")"
SYSTEM_SQL="$(awk "/^${MARKER}/{f=1} f{print}" "$SCRIPT")"

echo "===== 租户库段：hr_$(printf '%04d' $START) ~ hr_$(printf '%04d' $END) ====="
for ((i=START; i<=END; i++)); do
  DB="hr_$(printf '%04d' $i)"
  echo ">>> 执行 $DB"
  echo "$TENANT_SQL" | sed "s/{DB}/$DB/g" | $MYSQL "$DB" || { echo "❌ $DB 执行失败，已停止"; exit 1; }
done

echo "===== 系统库段：hrsystem（执行一次）====="
echo "$SYSTEM_SQL" | $MYSQL hrsystem || { echo "❌ hrsystem 执行失败"; exit 1; }

echo "===== 校验：各租户菜单（应只剩 1021，无 1030/5000/5020）====="
for ((i=START; i<=END; i++)); do
  DB="hr_$(printf '%04d' $i)"
  RES="$(echo "SELECT GROUP_CONCAT(id ORDER BY id) FROM ${DB}.tbmenu WHERE id IN (1021,1030,5000,5020);" | $MYSQL -N "$DB" 2>/dev/null)"
  echo "  $DB -> 菜单ID: ${RES:-（空）}"
done

echo "===== 完成 ✅ ====="
