package com.rpg.controller;

import com.rpg.dao.ConexionDB;
import com.rpg.dao.JugadorDAO;
import com.rpg.exceptions.DatosException;
import com.rpg.model.Jugador;
import com.rpg.model.Rol;

import java.util.Optional;

/**
 * Reglas del login: maximo 3 intentos fallidos (al tercero la sesion queda bloqueada)
 * y validacion de los datos de registro. Guarda el jugador que inicio sesion.
 */
public class LoginController {

    public static final int MAXIMO_INTENTOS = 3;
    private static final int LARGO_MINIMO_USUARIO = 3;
    private static final int LARGO_MAXIMO_USUARIO = 30;
    private static final int LARGO_MINIMO_CLAVE = 6;
    private static final String PATRON_USUARIO = "[A-Za-z0-9_]+";

    private final JugadorDAO jugadorDAO;
    private int intentosFallidos;
    private boolean bloqueado;
    private Jugador jugadorActual;

    public LoginController(JugadorDAO jugadorDAO) {
        this.jugadorDAO = jugadorDAO;
    }

    /** @return null si la base responde; si no, el motivo para mostrarlo */
    public String verificarConexion() {
        try {
            ConexionDB.verificarConexion();
            return null;
        } catch (DatosException e) {
            return e.getMessage();
        }
    }

    /** @return el mensaje para el jugador. Consultar haySesion() y estaBloqueado() despues. */
    public String iniciarSesion(String usuario, String clave) {
        if (bloqueado) {
            return "Sesion bloqueada: se superaron los " + MAXIMO_INTENTOS + " intentos.";
        }
        if (usuario != null && !usuario.isBlank() && clave != null && !clave.isEmpty()) {
            try {
                Optional<Jugador> jugador = jugadorDAO.autenticar(usuario, clave);
                if (jugador.isPresent()) {
                    jugadorActual = jugador.get();
                    intentosFallidos = 0;
                    return "Bienvenido, " + jugadorActual + ".";
                }
            } catch (DatosException e) {
                // Un fallo de la base no es culpa del jugador: no cuenta como intento.
                return "No se pudo validar el login: " + e.getMessage();
            }
        }
        intentosFallidos++;
        if (intentosFallidos >= MAXIMO_INTENTOS) {
            bloqueado = true;
            return "Usuario o clave incorrectos. Sesion bloqueada tras " + MAXIMO_INTENTOS + " intentos.";
        }
        int restantes = getIntentosRestantes();
        return "Usuario o clave incorrectos. " + (restantes == 1
                ? "Te queda 1 intento." : "Te quedan " + restantes + " intentos.");
    }

    /** Los jugadores nuevos siempre tienen rol jugador. @return el mensaje para el jugador */
    public String registrar(String usuario, String clave, String confirmacion) {
        if (usuario == null || usuario.length() < LARGO_MINIMO_USUARIO
                || usuario.length() > LARGO_MAXIMO_USUARIO) {
            return "El usuario debe tener entre " + LARGO_MINIMO_USUARIO + " y "
                    + LARGO_MAXIMO_USUARIO + " caracteres.";
        }
        if (!usuario.matches(PATRON_USUARIO)) {
            return "El usuario solo puede tener letras sin tildes, numeros y guion bajo.";
        }
        if (clave == null || clave.length() < LARGO_MINIMO_CLAVE) {
            return "La clave debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.";
        }
        if (!clave.equals(confirmacion)) {
            return "Las claves no coinciden.";
        }
        try {
            Jugador nuevo = jugadorDAO.registrar(usuario, clave, Rol.JUGADOR);
            return "Jugador " + nuevo + " registrado. Ya puedes iniciar sesion.";
        } catch (DatosException e) {
            return "No se pudo registrar: " + e.getMessage();
        }
    }

    public boolean haySesion() {
        return jugadorActual != null;
    }

    public boolean estaBloqueado() {
        return bloqueado;
    }

    public int getIntentosRestantes() {
        return MAXIMO_INTENTOS - intentosFallidos;
    }

    public Jugador getJugadorActual() {
        return jugadorActual;
    }
}
