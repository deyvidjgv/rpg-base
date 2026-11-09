package com.rpg.exceptions;

/**
 * Error al leer o escribir en la base de datos. Los DAO envuelven aqui el error de JDBC
 * para que el Controller y la View no dependan de las clases de la base de datos.
 */
public class DatosException extends JuegoException {

    private static final long serialVersionUID = 1L;

    public DatosException(String mensaje) {
        super(mensaje);
    }

    public DatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
