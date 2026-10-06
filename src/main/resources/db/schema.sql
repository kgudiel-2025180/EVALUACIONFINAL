-- =====================================================================
-- Sistema de Gestion de Biblioteca Universitaria
-- Esquema de base de datos (MySQL 8.0+)
--
-- Fuente unica de verdad del esquema: Spring Boot lo ejecuta al arrancar
-- (spring.sql.init) y tambien puede ejecutarse manualmente con:
--   mysql -u <usuario> -p <base> < src/main/resources/db/schema.sql
--
-- Todos los objetos usan IF NOT EXISTS: el script es idempotente.
-- Los indices se definen DENTRO de CREATE TABLE porque MySQL no admite
-- CREATE INDEX IF NOT EXISTS.
--
-- Nota: las restricciones CHECK se aplican de verdad desde MySQL 8.0.16.
-- En versiones anteriores se parsearian pero se ignorarian; la capa Java
-- (StockValidator) sigue aplicando las mismas reglas en cualquier caso.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tabla: usuarios
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(100)   NOT NULL,
    email           VARCHAR(150)   NOT NULL,
    password        VARCHAR(100)   NOT NULL,
    estado          VARCHAR(20)    NOT NULL,
    rol             VARCHAR(20)    NOT NULL,
    fecha_creacion  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuarios_email (email),
    CONSTRAINT ck_usuarios_estado CHECK (estado IN ('ACTIVO', 'SANCIONADO')),
    CONSTRAINT ck_usuarios_rol    CHECK (rol    IN ('ADMIN', 'BIBLIOTECARIO', 'LECTOR')),
    CONSTRAINT ck_usuarios_email_formato CHECK (POSITION('@' IN email) > 1),
    KEY idx_usuarios_rol (rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Tabla: libros
-- La eliminacion es LOGICA (eliminado = TRUE): el historial de prestamos
-- debe conservarse siempre, por lo que nunca se borra una fila.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS libros (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    isbn              VARCHAR(20)    NOT NULL,
    titulo            VARCHAR(200)   NOT NULL,
    autor             VARCHAR(120)   NOT NULL,
    categoria         VARCHAR(80)    NOT NULL,
    stock_total       INT            NOT NULL DEFAULT 0,
    stock_disponible  INT            NOT NULL DEFAULT 0,
    eliminado         TINYINT(1)     NOT NULL DEFAULT 0,
    version           BIGINT         NOT NULL DEFAULT 0,
    fecha_creacion    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_libros_isbn (isbn),
    -- REGLA 8: invariantes de stock a nivel de base de datos.
    CONSTRAINT ck_libros_stock_total_positivo  CHECK (stock_total >= 0),
    CONSTRAINT ck_libros_stock_disp_positivo   CHECK (stock_disponible >= 0),
    CONSTRAINT ck_libros_stock_disp_le_total   CHECK (stock_disponible <= stock_total),
    KEY idx_libros_titulo    (titulo),
    KEY idx_libros_categoria (categoria),
    KEY idx_libros_catalogo  (eliminado, titulo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Tabla: prestamos
-- ON DELETE RESTRICT: no se puede eliminar un usuario o un libro que tenga
-- historial de prestamos; la baja es logica.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prestamos (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id                BIGINT       NOT NULL,
    libro_id                  BIGINT       NOT NULL,
    fecha_prestamo            DATETIME     NOT NULL,
    fecha_devolucion_esperada DATETIME     NOT NULL,
    fecha_devolucion_real     DATETIME     NULL,
    estado                    VARCHAR(20)  NOT NULL,
    fecha_creacion            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_prestamos_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_prestamos_libro FOREIGN KEY (libro_id)
        REFERENCES libros (id) ON DELETE RESTRICT,
    CONSTRAINT ck_prestamos_estado CHECK (estado IN ('ACTIVO', 'DEVUELTO', 'ATRASADO')),
    CONSTRAINT ck_prestamos_plazo  CHECK (fecha_devolucion_esperada >= fecha_prestamo),
    CONSTRAINT ck_prestamos_devuelta CHECK (
        (estado = 'DEVUELTO' AND fecha_devolucion_real IS NOT NULL)
        OR (estado <> 'DEVUELTO' AND fecha_devolucion_real IS NULL)
    ),
    KEY idx_prestamos_usuario (usuario_id, fecha_prestamo),
    KEY idx_prestamos_estado  (estado, fecha_devolucion_esperada),
    KEY idx_prestamos_libro   (libro_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
