-- =====================================================================
-- Sistema de Gestion de Biblioteca Universitaria
-- Esquema de base de datos (PostgreSQL)
--
-- Fuente unica de verdad del esquema: Spring Boot la ejecuta al arrancar
-- (spring.sql.init) y tambien puede ejecutarse manualmente con:
--   psql -U <usuario> -d <base> -f src/main/resources/db/schema.sql
--
-- Todos los objetos usan IF NOT EXISTS: el script es idempotente.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tabla: usuarios
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGSERIAL      PRIMARY KEY,
    nombre          VARCHAR(100)   NOT NULL,
    email           VARCHAR(150)   NOT NULL,
    password        VARCHAR(100)   NOT NULL,
    estado          VARCHAR(20)    NOT NULL,
    rol             VARCHAR(20)    NOT NULL,
    fecha_creacion  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_estado CHECK (estado IN ('ACTIVO', 'SANCIONADO')),
    CONSTRAINT ck_usuarios_rol    CHECK (rol    IN ('ADMIN', 'BIBLIOTECARIO', 'LECTOR')),
    CONSTRAINT ck_usuarios_email_formato CHECK (position('@' IN email) > 1)
);

-- ---------------------------------------------------------------------
-- Tabla: libros
-- La eliminacion es LOGICA (eliminado = TRUE): el historial de prestamos
-- debe conservarse siempre, por lo que nunca se borra una fila.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS libros (
    id                BIGSERIAL      PRIMARY KEY,
    isbn              VARCHAR(20)    NOT NULL,
    titulo            VARCHAR(200)   NOT NULL,
    autor             VARCHAR(120)   NOT NULL,
    categoria         VARCHAR(80)    NOT NULL,
    stock_total       INTEGER        NOT NULL DEFAULT 0,
    stock_disponible  INTEGER        NOT NULL DEFAULT 0,
    eliminado         BOOLEAN        NOT NULL DEFAULT FALSE,
    version           BIGINT         NOT NULL DEFAULT 0,
    fecha_creacion    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_libros_isbn UNIQUE (isbn),
    -- REGLA 8: invariantes de stock a nivel de base de datos.
    CONSTRAINT ck_libros_stock_total_positivo  CHECK (stock_total >= 0),
    CONSTRAINT ck_libros_stock_disp_positivo   CHECK (stock_disponible >= 0),
    CONSTRAINT ck_libros_stock_disp_le_total   CHECK (stock_disponible <= stock_total)
);

-- ---------------------------------------------------------------------
-- Tabla: prestamos
-- ON DELETE RESTRICT: no se puede eliminar un usuario o un libro que tenga
-- historial de prestamos; la baja es logica.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prestamos (
    id                       BIGSERIAL   PRIMARY KEY,
    usuario_id               BIGINT      NOT NULL,
    libro_id                 BIGINT      NOT NULL,
    fecha_prestamo           TIMESTAMP   NOT NULL,
    fecha_devolucion_esperada TIMESTAMP  NOT NULL,
    fecha_devolucion_real    TIMESTAMP   NULL,
    estado                   VARCHAR(20) NOT NULL,
    fecha_creacion           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prestamos_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_prestamos_libro FOREIGN KEY (libro_id)
        REFERENCES libros (id) ON DELETE RESTRICT,
    CONSTRAINT ck_prestamos_estado CHECK (estado IN ('ACTIVO', 'DEVUELTO', 'ATRASADO')),
    CONSTRAINT ck_prestamos_plazo  CHECK (fecha_devolucion_esperada >= fecha_prestamo),
    CONSTRAINT ck_prestamos_devuelta CHECK (
        (estado = 'DEVUELTO' AND fecha_devolucion_real IS NOT NULL)
        OR (estado <> 'DEVUELTO' AND fecha_devolucion_real IS NULL)
    )
);

-- ---------------------------------------------------------------------
-- Indices: aceleran las consultas de catalogo, paginacion y deteccion de
-- atrasos, que son las que mas se repiten bajo carga concurrente.
-- ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_libros_titulo     ON libros (titulo);
CREATE INDEX IF NOT EXISTS idx_libros_categoria  ON libros (categoria);
CREATE INDEX IF NOT EXISTS idx_libros_catalogo   ON libros (eliminado, titulo);
CREATE INDEX IF NOT EXISTS idx_prestamos_usuario ON prestamos (usuario_id, fecha_prestamo DESC);
CREATE INDEX IF NOT EXISTS idx_prestamos_estado  ON prestamos (estado, fecha_devolucion_esperada);
CREATE INDEX IF NOT EXISTS idx_prestamos_libro   ON prestamos (libro_id);
