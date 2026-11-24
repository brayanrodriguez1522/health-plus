-- =====================================================================
--  HEALTH+ · Cómo LIMPIAR la base de datos (pegar en http://localhost:8080/h2-console)
--  Las tablas no tienen llaves foráneas, así que borra primero lo "hijo" y al final el usuario.
-- =====================================================================

-- 1) Ver todo lo que hay
SELECT id_usuario, nombre, correo, proveedor, fecha_registro FROM usuarios;
SELECT * FROM verificacion_correo;

-- 2) Borrar UN usuario y todo lo suyo (cambia el 5 por el id de arriba)
DELETE FROM plan_ejercicio WHERE id_usuario = 5;
DELETE FROM metas          WHERE id_usuario = 5;
DELETE FROM perfil_fisico  WHERE id_usuario = 5;
DELETE FROM usuarios       WHERE id_usuario = 5;

-- 3) Borrar registros que nunca escribieron el código
DELETE FROM verificacion_correo;

-- 4) Borrar TODOS los usuarios (deja intactos los ejercicios y rutinas)
DELETE FROM plan_ejercicio;
DELETE FROM metas;
DELETE FROM perfil_fisico;
DELETE FROM verificacion_correo;
DELETE FROM usuarios;
ALTER TABLE usuarios ALTER COLUMN id_usuario RESTART WITH 1;   -- que los ids vuelvan a empezar en 1

-- 5) Opción "borrón y cuenta nueva": apaga la app y borra la carpeta data/ del proyecto.
--    Al volver a darle Run, se crea todo de nuevo (vacío + ejercicios y rutinas de ejemplo).
