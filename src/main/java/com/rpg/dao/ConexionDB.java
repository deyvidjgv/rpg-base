package com.rpg.dao;

import com.rpg.exceptions.DatosException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones a rpg_arena_db con los datos de config.txt.
 *
 * Que es JDBC: es la API estandar de Java (paquete java.sql) para hablar con una base
 * de datos relacional. El codigo usa siempre las mismas interfaces (Connection,
 * PreparedStatement, ResultSet) y cada motor (MySQL, PostgreSQL...) aporta un driver
 * que las implementa. Asi el programa no depende de como se comunica cada motor.
 *
 * Que hace DriverManager: recibe la URL (jdbc:mysql://...), busca entre los drivers
 * cargados uno que acepte ese tipo de URL y le pide que abra la conexion con el usuario
 * y la clave. Los drivers modernos se registran solos al estar en el classpath, por eso
 * no hace falta Class.forName.
 *
 * Que pasa si falta el driver: se quito la dependencia mysql-connector-j del classpath y
 * al conectar se obtuvo exactamente:
 *   java.sql.SQLException: No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db
 * El codigo compila igual (java.sql viene con el JDK), pero en tiempo de ejecucion
 * DriverManager no encuentra ningun driver que entienda la URL jdbc:mysql.
 */
public final class ConexionDB {

    private static final String ARCHIVO_CONFIG = "config.txt";

    private static Properties configuracion;

    private ConexionDB() {
    }

    /**
     * Cada llamada abre una conexion nueva. Quien la pide la cierra con try-with-resources,
     * asi ninguna conexion queda abierta aunque ocurra un error.
     */
    public static Connection obtenerConexion() throws SQLException {
        Properties config = leerConfiguracion();
        return DriverManager.getConnection(
                config.getProperty("url"),
                config.getProperty("usuario"),
                config.getProperty("clave"));
    }

    /** Abre y cierra una conexion de prueba, para avisar al inicio si la base no responde. */
    public static void verificarConexion() throws DatosException {
        try (Connection conexion = obtenerConexion()) {
            if (!conexion.isValid(2)) {
                throw new DatosException("La base de datos no responde.");
            }
        } catch (SQLException e) {
            throw new DatosException(e.getMessage(), e);
        }
    }

    private static synchronized Properties leerConfiguracion() throws SQLException {
        if (configuracion != null) {
            return configuracion;
        }
        Properties config = new Properties();
        try (FileReader lector = new FileReader(ARCHIVO_CONFIG)) {
            config.load(lector);
        } catch (FileNotFoundException e) {
            throw new SQLException("No existe " + ARCHIVO_CONFIG
                    + ". Copia config.example.txt como config.txt y pon tus datos.", e);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer " + ARCHIVO_CONFIG + ": " + e.getMessage(), e);
        }
        for (String campo : new String[] {"url", "usuario", "clave"}) {
            if (config.getProperty(campo) == null) {
                throw new SQLException("Falta la linea '" + campo + "=' en " + ARCHIVO_CONFIG);
            }
        }
        configuracion = config;
        return configuracion;
    }
}
