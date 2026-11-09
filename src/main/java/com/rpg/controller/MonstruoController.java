package com.rpg.controller;

import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.exceptions.DatosException;
import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;

import java.util.List;
import java.util.Optional;

/**
 * Reglas del bestiario: solo un admin crea, edita o elimina; los datos deben ser validos;
 * el nombre no se repite; y un monstruo con victorias no se elimina.
 */
public class MonstruoController {

    private static final int LARGO_MAXIMO_NOMBRE = 40;

    private final MonstruoDAO monstruoDAO;
    private final VictoriaDAO victoriaDAO;
    private final LoginController sesion;

    public MonstruoController(MonstruoDAO monstruoDAO, VictoriaDAO victoriaDAO, LoginController sesion) {
        this.monstruoDAO = monstruoDAO;
        this.victoriaDAO = victoriaDAO;
        this.sesion = sesion;
    }

    // ---- consultas: cualquier jugador con sesion

    public List<Monstruo> listar() throws DatosException {
        return monstruoDAO.listar();
    }

    public Optional<Monstruo> buscarPorNombre(String nombre) throws DatosException {
        return monstruoDAO.buscarPorNombre(nombre);
    }

    public Optional<Monstruo> buscarPorId(int id) throws DatosException {
        return monstruoDAO.buscarPorId(id);
    }

    public List<Monstruo> listarPorVidaMinima(int vidaMinima) throws DatosException {
        return monstruoDAO.listarPorVidaMinima(vidaMinima);
    }

    // ---- cambios: solo admin

    /** @return null si el jugador actual es admin; si no, el motivo del rechazo */
    public String comprobarPermiso() {
        Jugador jugador = sesion.getJugadorActual();
        if (jugador == null || !jugador.esAdmin()) {
            return "Solo un administrador puede crear, editar o eliminar monstruos.";
        }
        return null;
    }

    public String crear(String nombre, int vida, int ataque) {
        String rechazo = validar(nombre, vida, ataque);
        if (rechazo != null) {
            return rechazo;
        }
        try {
            if (monstruoDAO.buscarPorNombre(nombre.trim()).isPresent()) {
                return "Ya existe un monstruo llamado '" + nombre.trim() + "'.";
            }
            Monstruo creado = monstruoDAO.crear(new Monstruo(nombre.trim(), vida, ataque));
            return "Monstruo creado: " + creado;
        } catch (DatosException e) {
            return "No se pudo crear: " + e.getMessage();
        }
    }

    public String editar(int id, String nombre, int vida, int ataque) {
        String rechazo = validar(nombre, vida, ataque);
        if (rechazo != null) {
            return rechazo;
        }
        try {
            if (monstruoDAO.buscarPorId(id).isEmpty()) {
                return "No existe un monstruo con id " + id + ".";
            }
            Optional<Monstruo> mismoNombre = monstruoDAO.buscarPorNombre(nombre.trim());
            if (mismoNombre.isPresent() && mismoNombre.get().getId() != id) {
                return "Ya existe otro monstruo llamado '" + nombre.trim() + "'.";
            }
            Monstruo editado = new Monstruo(id, nombre.trim(), vida, ataque);
            return monstruoDAO.actualizar(editado)
                    ? "Monstruo actualizado: " + editado
                    : "No existe un monstruo con id " + id + ".";
        } catch (DatosException e) {
            return "No se pudo editar: " + e.getMessage();
        }
    }

    /** Consulta al DAO las victorias antes de borrar, para no llegar al error de la FOREIGN KEY. */
    public String eliminar(int id) {
        String rechazo = comprobarPermiso();
        if (rechazo != null) {
            return rechazo;
        }
        try {
            Optional<Monstruo> monstruo = monstruoDAO.buscarPorId(id);
            if (monstruo.isEmpty()) {
                return "No existe un monstruo con id " + id + ".";
            }
            int victorias = victoriaDAO.contarPorMonstruo(id);
            if (victorias > 0) {
                return "No se puede eliminar '" + monstruo.get().getNombre() + "': tiene "
                        + victorias + " victorias registradas.";
            }
            return monstruoDAO.eliminar(id)
                    ? "Monstruo eliminado: " + monstruo.get().getNombre()
                    : "No existe un monstruo con id " + id + ".";
        } catch (DatosException e) {
            return "No se pudo eliminar: " + e.getMessage();
        }
    }

    private String validar(String nombre, int vida, int ataque) {
        String rechazo = comprobarPermiso();
        if (rechazo != null) {
            return rechazo;
        }
        if (nombre == null || nombre.isBlank()) {
            return "El nombre no puede estar vacio.";
        }
        if (nombre.trim().length() > LARGO_MAXIMO_NOMBRE) {
            return "El nombre admite como maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.";
        }
        if (vida <= 0) {
            return "La vida debe ser mayor que 0.";
        }
        if (ataque < 0) {
            return "El ataque no puede ser negativo.";
        }
        return null;
    }
}
