# Sistema de Gestion de Biblioteca Universitaria — API REST

Backend REST para la gestion de un prestamo de libros en una biblioteca universitaria.
No incluye frontend: la API completa queda documentada mediante la coleccion de
Postman incluida en [`postman/Biblioteca.postman_collection.json`](postman/Biblioteca.postman_collection.json).

| | |
|---|---|
| **Stack** | Java 17 · Spring Boot 3.5 · Spring Security · JPA/Hibernate · **MySQL 8.0** · Maven |
| **Autenticacion** | JWT (HS256), API stateless, sin sesiones |
| **Pruebas** | 70 pruebas JUnit 5 + MockMvc + prueba de saturacion real contra Tomcat |

---

## 1. Requisitos

- JDK 17 o superior (probado con JDK 21)
- MySQL 8.0 o superior
- Maven Wrapper incluido (`./mvnw`) — no requiere Maven instalado

---

## 2. Puesta en marcha

### 2.1 Base de datos

```bash
mysql -u IN5AM -e "CREATE DATABASE IF NOT EXISTS biblioteca_in5am CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

> **Obligatorio:** el usuario `IN5AM` tiene el grant `ALL PRIVILEGES ON \`%_in5am\`.*`,
> por lo que la base de datos **debe terminar en `_in5am`** o MySQL rechazara la
> creacion con `ERROR 1044 (42000): Access denied`.

El esquema **no lo genera Hibernate**: lo define
[`src/main/resources/db/schema.sql`](src/main/resources/db/schema.sql) y se ejecuta
automaticamente al arrancar, junto con la carga inicial
[`src/main/resources/db/data.sql`](src/main/resources/db/data.sql). Ambos scripts son
idempotentes (`IF NOT EXISTS` / `WHERE NOT EXISTS`), por lo que el servicio puede
reiniciarse tantas veces como se quiera sobre la misma base.

> MySQL 8.0.16+ **aplica de verdad** las restricciones `CHECK` del esquema
> (roles, estados e invariantes de stock). En versiones anteriores se ignorarian,
> pero la capa Java (`StockValidator`) sigue aplicando las mismas reglas.

### 2.2 Variables de entorno

Todas son opcionales en desarrollo local; en produccion deben definirse.

| Variable | Por defecto | Descripcion |
|---|---|---|
| `DB_HOST` | `localhost` | Host de MySQL |
| `DB_PORT` | `3306` | Puerto |
| `DB_NAME` | `biblioteca_in5am` | Base de datos (debe terminar en `_in5am`) |
| `DB_USERNAME` | `IN5AM` | Usuario |
| `DB_PASSWORD` | *(vacia)* | Contrasena |
| `DB_POOL_MAX` | `30` | Tamano maximo del pool HikariCP |
| `JWT_SECRET` | *(solo desarrollo)* | Secreto HMAC de **al menos 32 bytes** |
| `JWT_EXPIRATION_MS` | `86400000` | Vigencia del token (24 h) |
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `TOMCAT_THREADS_MAX` | `50` | Hilos de Tomcat (solo si se desactivan los hilos virtuales) |

### 2.3 Ejecutar

```bash
.\mvnw.cmd spring-boot:run
```

Ya verificado de extremo a extremo contra **MySQL 8.0.34** real: login, catalogo con
filtros, alta de libro, prestamo con plazo de 14 dias, descuento y devolucion de
stock, devolucion duplicada (409), atrasos, sancion automatica (REGLA 4/5),
autorizacion por rol (401/403) y baja logica.

#### Arrancar desde IntelliJ IDEA

Si ejecutas la aplicacion desde el IDE sin definir variables de entorno, la app usa
los valores por defecto de `application.yml` — y `DB_PASSWORD` por defecto es **vacio**.
Como el usuario `IN5AM` si tiene contrasena, MySQL rechaza la conexion con:

```
java.sql.SQLException: Access denied for user 'IN5AM'@'localhost' (using password: NO)
```

Para evitarlo: **Run → Edit Configurations… → `KennyEvafinalApplication` →
Environment variables** y define:

```
DB_NAME=biblioteca_in5am;DB_PASSWORD=_odmon5Am;DB_USERNAME=IN5AM
```

Si el puerto 8080 esta ocupado, agrega ademas `SERVER_PORT=8181`.

### 2.4 Ejecutar las pruebas

```bash
./mvnw test
```

Los tests arrancan el contexto completo sobre **H2 en modo MySQL** y ejecutan
`schema.sql` y `data.sql` tal cual: si el esquema o la carga inicial fallaran, las
pruebas fallarian antes de llegar a la logica de negocio.

---

## 3. Cuentas de prueba

Cargadas por `db/data.sql` con contrasena cifrada con BCrypt:

| Email | Contrasena | Rol | Estado |
|---|---|---|---|
| `admin@kinal.edu.gt` | `Admin123!` | ADMIN | ACTIVO |
| `bibliotecario@kinal.edu.gt` | `Biblio123!` | BIBLIOTECARIO | ACTIVO |
| `lector@kinal.edu.gt` | `Lector123!` | LECTOR | ACTIVO |
| `lector-atrasos@kinal.edu.gt` | `Lector123!` | LECTOR | ACTIVO (con prestamo vencido) |

> `lector-atrasos` existe para probar la **REGLA 4** (sancion automatica) y el
> endpoint `/prestamos/atrasados`.

---

## 4. Endpoints

Base: `http://localhost:8080/api/v1`

### Autenticacion (publico)

| Metodo | Ruta | Descripcion | Exitos |
|---|---|---|---|
| POST | `/auth/register` | Registro publico. **Fuerza `LECTOR` y `ACTIVO`** sin importar el payload | 201 |
| POST | `/auth/login` | Devuelve `{ token, tipoToken, expiracionEnSegundos, usuario }` | 200 |

### Catalogo de libros

| Metodo | Ruta | Rol requerido | Exitos |
|---|---|---|---|
| GET | `/libros?titulo=&categoria=&page=&size=&sort=` | Cualquier autenticado | 200 |
| GET | `/libros/{id}` | Cualquier autenticado | 200 |
| POST | `/libros` | ADMIN | 201 |
| PUT | `/libros/{id}` | ADMIN | 200 |
| DELETE | `/libros/{id}` | ADMIN (baja **logica**) | 204 |

### Prestamos

| Metodo | Ruta | Rol requerido | Exitos |
|---|---|---|---|
| POST | `/prestamos` | ADMIN o BIBLIOTECARIO | 201 |
| PATCH | `/prestamos/{id}/devolucion` | ADMIN o BIBLIOTECARIO | 200 |
| GET | `/prestamos/mis-prestamos` | **solo LECTOR** | 200 |
| GET | `/prestamos/atrasados` | ADMIN o BIBLIOTECARIO | 200 |

`/mis-prestamos` toma la identidad **del token**, nunca de un parametro: no existe
forma de consultar el historial de otro usuario.

### Formato de respuesta

Éxito:

```json
{ "success": true, "message": "Prestamo registrado correctamente", "data": { } }
```

Error (nunca lleva stack traces ni datos internos):

```json
{
  "timestamp": "2026-10-06T10:15:30",
  "success": false,
  "status": 400,
  "error": "BUSINESS_RULE_VIOLATION",
  "message": "El libro no tiene ejemplares disponibles",
  "path": "/api/v1/prestamos",
  "data": null
}
```

Codigos usados: `200` `201` `204` `400` `401` `403` `404` `409` `500`.

---

## 5. Reglas de negocio

| Regla | Descripcion | Como se implementa |
|---|---|---|
| 1 | No prestar si `stockDisponible = 0` | `PrestamoServiceImpl.registrar` |
| 2 | LECTOR: maximo 3 prestamos activos | `countByUsuarioIdAndEstadoIn` |
| 3 | `fechaDevolucionEsperada = fechaPrestamo + 14 dias` | Calculado en el backend; el cliente nunca la envia |
| 4 | LECTOR con prestamo vencido pasa a `SANCIONADO` | `SancionService`, en **transaccion propia** |
| 5 | `SANCIONADO` no puede recibir prestamos | `SancionService` |
| 6 | `stockDisponible -= 1` al prestar | Dentro de la misma transaccion |
| 7 | `stockDisponible += 1` al devolver | Dentro de la misma transaccion |
| 8 | `0 <= stockDisponible <= stockTotal` | `StockValidator` en Java **y** `CHECK` en SQL |
| 9 | El libro debe existir | `ResourceNotFoundException` → 404 |
| 10 | El usuario debe existir | `ResourceNotFoundException` → 404 |
| 11 | El prestamo debe existir | `ResourceNotFoundException` → 404 |
| 12 | Sin devoluciones dobles | `ConflictException` → 409 |
| 13 | Validaciones de formato | Jakarta Validation → 400 `VALIDATION_ERROR` |

> **Politica de sanciones:** la especificacion no define una forma de levantar la
> sancion, por lo que **no se invento ninguna reactivacion automatica**. Toda la
> politica vive centralizada en `SancionService` para poder cambiarla sin tocar el
> resto del sistema.

---

## 6. Comportamiento bajo carga (mas de 100 peticiones concurrentes)

La prueba `ConcurrenciaApiTest` levanta el servidor real (Tomcat) y verifica:

1. **100 peticiones `GET` simultaneas → 100 respuestas `200`.**
2. **20 prestamos simultaneos sobre el mismo libro** (stock 20) → los 20 se conceden
   y el stock final es exactamente `0`, sin actualizaciones perdidas ni ejemplar
   prestado dos veces. El intento numero 21 responde `400`.

Medidas que lo hacen posible:

| Area | Medida |
|---|---|
| Hilos | `spring.threads.virtual.enabled: true` (hilos virtuales Java 21) |
| Tomcat | cola de `accept-count: 100`, `max-connections: 10000` |
| Conexiones | HikariCP con pool de 30 y `connection-timeout: 10000` — bajo saturacion la peticion **espera** en lugar de fallar. Si se supera la carga sostenida, conviene subir el pool junto con `max_connections` de MySQL || Concurrencia | `SELECT ... FOR UPDATE` (`PESSIMISTIC_WRITE`) sobre el libro al prestar y al devolver: serializa el acceso al stock |
| Defensa | `@Version` en `Libro` (bloqueo optimista) como segunda barrera |
| N+1 | `JOIN FETCH` de `usuario` y `libro` en el historial y en atrasados: una consulta por pagina, no una por fila |
| Sesiones | `open-in-view: false`: la sesion se cierra al terminar el servicio |
| Errores | Conflictos de bloqueo se traducen en `409 CONCURRENT_MODIFICATION`, jamas en `500` |

---

## 7. Estructura del proyecto

```
src/main/java/com/Evalucion/Kenny_EVAFINAL/
├── KennyEvafinalApplication.java
├── config/          JwtProperties, SecurityConfig (autorizacion por rol, CORS)
├── controller/      AuthController, LibroController, PrestamoController
├── dto/             common/, auth/, usuario/, libro/, prestamo/
├── entity/          Usuario, Libro, Prestamo
├── enums/           Rol, UsuarioEstado, PrestamoEstado
├── exception/       Excepciones de dominio + GlobalExceptionHandler + ApiError
├── mapper/          UsuarioMapper, LibroMapper, PrestamoMapper
├── repository/      UsuarioRepository, LibroRepository, PrestamoRepository, LibroSpecifications
├── security/        JwtService, JwtAuthenticationFilter, CustomUserDetailsService,
│                    RestAuthenticationEntryPoint, RestAccessDeniedHandler
├── service/         interfaces + service/impl (Usuario, Sancion, Libro, Prestamo, Auth)
└── util/            Constantes, StockValidator, PageUtils

src/main/resources/
├── application.yml              Configuracion (datos, Hikari, Tomcat, JWT)
└── db/schema.sql, db/data.sql   Esquema e informacion inicial

src/test/
├── resources/application.yml    H2 en modo PostgreSQL
└── java/.../api/, security/, util/   70 pruebas
```

---

## 8. Decisiones de diseno

- **Controladores delgados**: validan, delegan en el servicio y traducen a HTTP.
  Ninguna regla de negocio vive en un controller.
- **DTOs de entrada y salida**: las entidades JPA jamas se serializan; no hay
  riesgo de ciclos ni de exponer campos internos.
- **Eliminacion logica de libros**: el historial de prestamos debe conservarse,
  por lo que nunca se borra una fila de `libros`.
- **Transacciones acotadas**: cada operacion que modifica stock o prestamos es
  `@Transactional`; la sancion de la REGLA 4 usa `REQUIRES_NEW` para sobrevivir
  al rollback del prestamo que la origino.
- **Paginacion blindada**: `PageUtils` rechaza tamano de pagina fuera de rango y
  limita el tamano maximo a 100 registros.
- **ISBN unico**: comprobado en Java (409) y garantizado por restriccion en SQL.
- **BCrypt** para todas las contrasenas; jamas se devuelve una contrasena en la API.
- **MySQL 8.0** como motor de persistencia: `InnoDB` con `utf8mb4`, claves foraneas
  con `ON DELETE RESTRICT` y restricciones `CHECK` que refuerzan las reglas de
  negocio en la propia base de datos.

---

## 9. Colecta de Postman

Importa [`postman/Biblioteca.postman_collection.json`](postman/Biblioteca.postman_collection.json).
Cada solicitud de login guarda el token en la variable `token` de la coleccion, de
modo que basta con iniciar sesion una vez y ejecutar el resto. Las solicitudes
incluyen tests que verifican los codigos HTTP y las reglas de negocio asociadas.
