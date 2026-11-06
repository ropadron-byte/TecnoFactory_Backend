#!/usr/bin/env bash
# Levanta MySQL en el puerto 3307 y arranca el backend con el perfil local.
# Uso: ./scripts/dev.sh [argumentos extra para spring-boot:run]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [ ! -f .env ]; then
    echo "ERROR: falta el archivo .env. Copia .env.example y completa las claves." >&2
    exit 1
fi

# Spring no lee .env: exportamos sus variables al proceso Java.
set -a
# shellcheck disable=SC1091
source .env
set +a

# MySQL se publica en el 3307 para no chocar con el 3306 de otras instalaciones.
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
export DB_URL="${DB_URL:-jdbc:mysql://localhost:3307/tecnofactory}"
export DB_USER="${DB_USER:-root}"

# No arrancar con los valores de ejemplo de .env.example.
for variable in DB_PASSWORD MIGRATOR_PASSWORD APP_DB_PASSWORD JWT_SECRET ADMIN_PASSWORD; do
    valor="${!variable:-}"
    if [ -z "$valor" ] || [[ "$valor" == *CAMBIAR* ]] || [[ "$valor" == *cambia_esta_clave* ]]; then
        echo "ERROR: $variable falta o conserva un valor de ejemplo. Edita .env antes de continuar." >&2
        exit 1
    fi
done

# No arranca el backend hasta que MySQL esté healthy.
docker compose up -d --wait db

exec ./mvnw spring-boot:run "$@"
