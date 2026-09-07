#!/bin/bash

cd "$(dirname "$0")"

echo "=========================================="
echo "  HR系统海南项目 - 生产环境打包"
echo "=========================================="

echo "[1/3] 清理旧的构建文件..."
mvn clean

echo "[2/3] 使用prod配置打包..."
mvn package -DskipTests -Dspring.profiles.active=prod

echo "=========================================="
echo "[3/3] 打包完成!"
echo "JAR文件位置: target/hrsystem-0.0.1-SNAPSHOT.jar"
echo "=========================================="

echo ""
echo "按回车键关闭窗口..."
read
