#!/bin/bash
# =====================================================================
# Pruebas funcionales y de estres - Biblioteca Universitaria
# Requisitos: curl, bash (Git Bash). No requiere jq ni ab.
# =====================================================================

BASE_URL="http://localhost:8181/api/v1"
ADMIN_EMAIL="admin@kinal.edu.gt"
ADMIN_PASS="Admin123!"
USER_EMAIL="lector@kinal.edu.gt"
USER_PASS="Lector123!"

# Extrae un campo JSON anidado (soporta .data.token, .data.id, etc.)
json_field() {
  echo "$1" | grep -o "\"$2\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -1 | sed 's/.*:[[:space:]]*"//; s/"$//'
}

echo "    INICIANDO PRUEBAS FUNCIONALES Y ESTRES"

# 1. REGISTRO Y LOGIN
echo -e "\n[1] Registrando usuario LECTOR de prueba..."
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Usuario Prueba\",\"email\":\"$USER_EMAIL\",\"password\":\"$USER_PASS\"}"
echo ""

echo -e "\n[2] Autenticando ADMIN..."
ADMIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}")
ADMIN_TOKEN=$(json_field "$ADMIN_RESP" "token")

if [ -z "$ADMIN_TOKEN" ]; then
  echo " --> Error al obtener el token de ADMIN. Respuesta:"
  echo "$ADMIN_RESP"
  exit 1
fi
echo "  Token Admin: ${ADMIN_TOKEN:0:25}..."

# 2. CREAR LIBRO (ADMIN)
echo -e "\n[3] Creando libro (ADMIN)..."
LIBRO_RESP=$(curl -s -X POST "$BASE_URL/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"isbn":"978-0134685991","titulo":"Effective Java","autor":"Joshua Bloch","categoria":"Programacion","stockTotal":10,"stockDisponible":10}')
echo "$LIBRO_RESP"
LIBRO_ID=$(json_field "$LIBRO_RESP" "id")
echo "  Libro creado con id=$LIBRO_ID"

# 3. CATALOGO
echo -e "\n[4] Consultando catalogo..."
curl -s -X GET "$BASE_URL/libros" -H "Authorization: Bearer $ADMIN_TOKEN" | head -c 300
echo ""

# 4. CONTROL DE ACCESO (403)
echo -e "\n[5] Autenticando LECTOR..."
USER_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_EMAIL\",\"password\":\"$USER_PASS\"}")
USER_TOKEN=$(json_field "$USER_RESP" "token")

echo -e "\n[6] LECTOR intentando crear libro (debe dar 403)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/libros" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"isbn":"123-4567890123","titulo":"Prohibido","autor":"Anon","categoria":"Test","stockTotal":1,"stockDisponible":1}')
if [ "$HTTP_STATUS" -eq 403 ]; then
  echo "--> Seguridad OK: 403 Forbidden"
else
  echo "--> Advertencia: se esperaba 403, llego $HTTP_STATUS"
fi

# 5. PRUEBA DE ESTRES (concurrencia)
echo -e "\n[7] PRUEBA DE ESTRES: 100 peticiones en 10 hilos..."
echo "    (usando curl en paralelo; ab/wrk no disponibles)"
seq 1 100 | xargs -P 10 -I {} curl -s -o /dev/null -w "%{http_code}\n" \
  -X GET "$BASE_URL/libros" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | sort | uniq -c

echo -e "\n --> PRUEBAS COMPLETADAS"
