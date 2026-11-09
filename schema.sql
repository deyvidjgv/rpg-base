-- Arena Segura: base de datos del login, el bestiario y el ranking.
-- Ejecutar completo en MySQL 8: crea la base desde cero (borra las tablas si ya existian).

CREATE DATABASE IF NOT EXISTS rpg_arena_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rpg_arena_db;

-- Primero la tabla hija, porque sus claves foraneas apuntan a las otras dos.
DROP TABLE IF EXISTS victorias;
DROP TABLE IF EXISTS monstruos;
DROP TABLE IF EXISTS jugadores;

CREATE TABLE jugadores (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(30) NOT NULL,
    clave   CHAR(64)    NOT NULL COMMENT 'Hash SHA-256 en hexadecimal, nunca la clave en texto plano',
    rol     ENUM('jugador', 'admin') NOT NULL DEFAULT 'jugador',
    CONSTRAINT uq_jugadores_usuario UNIQUE (usuario)
);

CREATE TABLE monstruos (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL,
    vida   INT NOT NULL,
    ataque INT NOT NULL,
    CONSTRAINT uq_monstruos_nombre UNIQUE (nombre),
    CONSTRAINT ck_monstruos_vida CHECK (vida > 0),
    CONSTRAINT ck_monstruos_ataque CHECK (ataque >= 0)
);

CREATE TABLE victorias (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    jugador_id  INT NOT NULL,
    monstruo_id INT NOT NULL,
    fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Si se elimina un jugador, sus victorias se van con el.
    CONSTRAINT fk_victorias_jugador FOREIGN KEY (jugador_id)
        REFERENCES jugadores (id) ON DELETE CASCADE,
    -- Un monstruo con victorias no se puede borrar: la base lo impide (RESTRICT)
    -- y MonstruoController lo comprueba antes para mostrar un mensaje claro.
    CONSTRAINT fk_victorias_monstruo FOREIGN KEY (monstruo_id)
        REFERENCES monstruos (id) ON DELETE RESTRICT
);

-- Usuarios de prueba. SHA2(texto, 256) da el mismo hash hexadecimal que HashClave en Java.
-- admin / admin123 (rol admin) y heroe / heroe123 (rol jugador).
INSERT INTO jugadores (usuario, clave, rol) VALUES
    ('admin', SHA2('admin123', 256), 'admin'),
    ('heroe', SHA2('heroe123', 256), 'jugador');

-- Los mismos monstruos del bestiario del juego.
INSERT INTO monstruos (nombre, vida, ataque) VALUES
    ('Goblin', 50, 8),
    ('Arana Gigante', 60, 10),
    ('Ogro', 100, 14),
    ('Dragon Joven', 160, 20);
