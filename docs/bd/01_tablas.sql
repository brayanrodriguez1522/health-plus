-- =====================================================================
--  HEALTH+ · Script de tablas (H2 / compatible con MySQL con cambios mínimos)
--  No hace falta correrlo para usar la app. Hibernate crea estas tablas solo
--  (spring.jpa.hibernate.ddl-auto=update) a partir de las clases de la carpeta "modelo".
--  Este archivo es el "script de BD" que pide la entrega (Parte 2) y sirve para explicarla.
-- =====================================================================

CREATE TABLE usuarios (
    id_usuario       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    correo           VARCHAR(120) NOT NULL UNIQUE,
    contrasena_hash  VARCHAR(255) NOT NULL,          -- BCrypt, nunca la contraseña en texto plano
    fecha_registro   DATE         NOT NULL,
    proveedor        VARCHAR(20),                    -- 'Correo', 'Google' o 'Facebook'
    foto_url         VARCHAR(500)
);

-- Registros que esperan el código de 6 dígitos enviado al correo
CREATE TABLE verificacion_correo (
    id_verificacion  BIGINT AUTO_INCREMENT PRIMARY KEY,
    correo           VARCHAR(120) NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    contrasena_hash  VARCHAR(255) NOT NULL,
    codigo_hash      VARCHAR(255) NOT NULL,          -- el código también va cifrado
    enviado_en       TIMESTAMP    NOT NULL,
    expira_en        TIMESTAMP    NOT NULL,
    intentos         INT          NOT NULL
);

-- SCRUM-16: datos antropométricos (1 por usuario)
CREATE TABLE perfil_fisico (
    id_perfil    BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario   BIGINT      NOT NULL UNIQUE,
    peso_kg      DOUBLE      NOT NULL,
    estatura_cm  DOUBLE      NOT NULL,
    edad         INT         NOT NULL,
    sexo         VARCHAR(20) NOT NULL
);

-- SCRUM-17: metas de entrenamiento y salud
CREATE TABLE metas (
    id_meta         BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario      BIGINT      NOT NULL,
    tipo            VARCHAR(50) NOT NULL,
    valor_objetivo  DOUBLE      NOT NULL,
    fecha_inicio    DATE        NOT NULL,
    fecha_fin       DATE        NOT NULL,
    estado          VARCHAR(20) NOT NULL
);

-- Catálogo
CREATE TABLE ejercicios (
    id_ejercicio    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    descripcion     VARCHAR(500) NOT NULL,
    grupo_muscular  VARCHAR(50)  NOT NULL,
    duracion_min    INT          NOT NULL
);

CREATE TABLE rutinas (
    id_rutina    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    nivel        VARCHAR(30)  NOT NULL,             -- 'Bajo peso', 'Peso normal', 'Sobrepeso'
    descripcion  VARCHAR(500),
    dia_semana   INT                                -- 1 = lunes ... 7 = domingo
);

-- Qué ejercicios lleva cada rutina (tabla intermedia muchos-a-muchos)
CREATE TABLE rutina_ejercicio (
    id_rutina     BIGINT NOT NULL,
    id_ejercicio  BIGINT NOT NULL,
    series        INT,
    repeticiones  INT,
    PRIMARY KEY (id_rutina, id_ejercicio)
);

-- El plan concreto de cada usuario, día por día (lo que se marca como "hecho")
CREATE TABLE plan_ejercicio (
    id_plan       BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario    BIGINT  NOT NULL,
    id_rutina     BIGINT  NOT NULL,
    id_ejercicio  BIGINT  NOT NULL,
    fecha         DATE    NOT NULL,
    completado    BOOLEAN NOT NULL
);
