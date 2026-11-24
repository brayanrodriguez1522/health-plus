-- =====================================================================
--  HEALTH+ · Datos de prueba
--  Los 20 ejercicios y las 15 rutinas los inserta solo la clase config/CargaInicial.java
--  la primera vez que arranca la app. Aquí va un usuario de demostración
--  Correo: demo@healthplus.com   Contraseña: Health2026
-- =====================================================================
INSERT INTO usuarios (nombre, correo, contrasena_hash, fecha_registro, proveedor)
VALUES ('Usuario Demo', 'demo@healthplus.com',
        '$2a$10$2iU91byhybH1CzBS9vbNVefKxvCCMMv8R4gDg0Xe7Lcqme5fD4yAG', CURRENT_DATE, 'Correo');

INSERT INTO perfil_fisico (id_usuario, peso_kg, estatura_cm, edad, sexo)
SELECT id_usuario, 70, 172, 20, 'Masculino' FROM usuarios WHERE correo = 'demo@healthplus.com';

INSERT INTO metas (id_usuario, tipo, valor_objetivo, fecha_inicio, fecha_fin, estado)
SELECT id_usuario, 'Días de ejercicio por semana', 4, CURRENT_DATE, DATEADD('DAY', 30, CURRENT_DATE), 'Activa'
FROM usuarios WHERE correo = 'demo@healthplus.com';
