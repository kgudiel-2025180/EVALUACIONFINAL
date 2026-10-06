-- =====================================================================
-- Sistema de Gestion de Biblioteca Universitaria
-- Carga inicial de datos (seed) para MySQL
--
-- Script idempotente: cada INSERT esta protegido con WHERE NOT EXISTS,
-- por lo que puede ejecutarse cuantas veces se quiera sin duplicar filas.
--
-- Cuentas de prueba (password cifrada con BCrypt):
--   admin@kinal.edu.gt          / Admin123!   -> ADMIN         (ACTIVO)
--   bibliotecario@kinal.edu.gt  / Biblio123!  -> BIBLIOTECARIO (ACTIVO)
--   lector@kinal.edu.gt         / Lector123!   -> LECTOR        (ACTIVO)
--   lector-atrasos@kinal.edu.gt / Lector123!   -> LECTOR        (ACTIVO)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
INSERT INTO usuarios (nombre, email, password, estado, rol, fecha_creacion)
SELECT 'Administrador del Sistema', 'admin@kinal.edu.gt',
       '$2a$10$Z5YVTBQeg1sdlJ96Thjd8e15Owr131ylaErOg.RKF4bLQT0laify6',
       'ACTIVO', 'ADMIN', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'admin@kinal.edu.gt');

INSERT INTO usuarios (nombre, email, password, estado, rol, fecha_creacion)
SELECT 'Bibliotecario Principal', 'bibliotecario@kinal.edu.gt',
       '$2a$10$ZJwLM58xOPDtlbplPbXqNOSh1e62z8Hu.r0yyEAK4BEBdmk3ZTkgK',
       'ACTIVO', 'BIBLIOTECARIO', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'bibliotecario@kinal.edu.gt');

INSERT INTO usuarios (nombre, email, password, estado, rol, fecha_creacion)
SELECT 'Estudiante Lector', 'lector@kinal.edu.gt',
       '$2a$10$sjePLmqAQiNpetTkeA2z1u.YuAcG4S./KtjX0PkGmPUQsDnVRNU4q',
       'ACTIVO', 'LECTOR', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'lector@kinal.edu.gt');

INSERT INTO usuarios (nombre, email, password, estado, rol, fecha_creacion)
SELECT 'Estudiante con Atrasos', 'lector-atrasos@kinal.edu.gt',
       '$2a$10$sjePLmqAQiNpetTkeA2z1u.YuAcG4S./KtjX0PkGmPUQsDnVRNU4q',
       'ACTIVO', 'LECTOR', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'lector-atrasos@kinal.edu.gt');

-- ---------------------------------------------------------------------
-- Libros del catalogo
-- El ISBN es unico por restriccion de base de datos.
-- ---------------------------------------------------------------------
INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-376-0494-7', 'El principito', 'Antoine de Saint-Exupery', 'Ficcion',
       5, 5, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-376-0494-7');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-206-3304-6', 'Cien anos de soledad', 'Gabriel Garcia Marquez', 'Realismo magico',
       4, 3, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-206-3304-6');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-9759-253-6', 'El infinito en un junco', 'Irene Vallejo', 'Ensayo',
       3, 3, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-9759-253-6');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-667-2346-5', 'Breve historia del tiempo', 'Stephen Hawking', 'Ciencia',
       3, 3, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-667-2346-5');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-397-3099-5', 'La sombra del viento', 'Carlos Ruiz Zafon', 'Misterio',
       2, 2, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-397-3099-5');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-663-3756-4', 'Fundacion', 'Isaac Asimov', 'Ciencia ficcion',
       4, 4, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-663-3756-4');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-675-2647-9', 'La casa de los espiritus', 'Isabel Allende', 'Novela',
       3, 3, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-675-2647-9');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-9838-994-4', 'Pensar rapido, pensar despacio', 'Daniel Kahneman', 'Psicologia',
       2, 2, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-9838-994-4');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-450-0567-9', 'El senor de los anillos', 'J.R.R. Tolkien', 'Fantasia',
       5, 5, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-450-0567-9');

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, eliminado, version, fecha_creacion)
SELECT '978-84-155-1203-4', 'Codigo limpio', 'Robert C. Martin', 'Programacion',
       3, 3, 0, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '978-84-155-1203-4');

-- ---------------------------------------------------------------------
-- Prestamos de ejemplo
--   1) Un prestamo YA DEVUELTO por el lector normal (historial).
--   2) Un prestamo ATRASADO por 'lector-atrasos' para probar:
--        - GET /api/v1/prestamos/atrasados
--        - REGLA 4 (sancion automatica al intentar un nuevo prestamo)
--        - REGLA 5 (sancionado no puede prestar)
-- Las fechas son fijas y anteriores al arranque, por lo que el atraso
-- se mantiene estable en cualquier fecha de ejecucion.
-- ---------------------------------------------------------------------
INSERT INTO prestamos (usuario_id, libro_id, fecha_prestamo, fecha_devolucion_esperada,
                       fecha_devolucion_real, estado, fecha_creacion)
SELECT u.id, l.id, '2026-08-10 09:00:00', '2026-08-24 09:00:00',
       '2026-08-22 11:30:00', 'DEVUELTO', CURRENT_TIMESTAMP
FROM usuarios u, libros l
WHERE u.email = 'lector@kinal.edu.gt'
  AND l.isbn = '978-84-376-0494-7'
  AND NOT EXISTS (
      SELECT 1 FROM prestamos p
      WHERE p.usuario_id = u.id AND p.libro_id = l.id AND p.estado = 'DEVUELTO'
  );

INSERT INTO prestamos (usuario_id, libro_id, fecha_prestamo, fecha_devolucion_esperada,
                       fecha_devolucion_real, estado, fecha_creacion)
SELECT u.id, l.id, '2026-09-01 09:00:00', '2026-09-15 09:00:00',
       NULL, 'ATRASADO', CURRENT_TIMESTAMP
FROM usuarios u, libros l
WHERE u.email = 'lector-atrasos@kinal.edu.gt'
  AND l.isbn = '978-84-206-3304-6'
  AND NOT EXISTS (
      SELECT 1 FROM prestamos p
      WHERE p.usuario_id = u.id AND p.libro_id = l.id AND p.estado = 'ATRASADO'
  );
