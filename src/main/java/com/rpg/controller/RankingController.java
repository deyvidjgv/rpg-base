package com.rpg.controller;

import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.exceptions.DatosException;
import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;
import com.rpg.model.RankingJugador;
import com.rpg.model.Victoria;

import java.util.List;
import java.util.Optional;

/** Ranking, victorias del jugador actual y registro automatico de victorias del juego. */
public class RankingController {

    private final VictoriaDAO victoriaDAO;
    private final MonstruoDAO monstruoDAO;
    private final LoginController sesion;

    public RankingController(VictoriaDAO victoriaDAO, MonstruoDAO monstruoDAO, LoginController sesion) {
        this.victoriaDAO = victoriaDAO;
        this.monstruoDAO = monstruoDAO;
        this.sesion = sesion;
    }

    public List<RankingJugador> ranking() throws DatosException {
        return victoriaDAO.rankingPorJugador();
    }

    public List<Victoria> misVictorias() throws DatosException {
        Jugador jugador = sesion.getJugadorActual();
        if (jugador == null) {
            throw new DatosException("No hay una sesion iniciada.");
        }
        return victoriaDAO.victoriasDeJugador(jugador.getUsuario());
    }

    /**
     * Lo llama el juego al ganar un combate. Solo cuenta si el rival existe en la tabla monstruos.
     *
     * @return el mensaje para el jugador
     */
    public String registrarVictoria(String nombreMonstruo) {
        Jugador jugador = sesion.getJugadorActual();
        if (jugador == null) {
            return "Sin sesion iniciada: la victoria no se registra en el ranking.";
        }
        try {
            Optional<Monstruo> monstruo = monstruoDAO.buscarPorNombre(nombreMonstruo);
            if (monstruo.isEmpty()) {
                return nombreMonstruo + " no esta en la tabla monstruos: la victoria no se registra.";
            }
            victoriaDAO.registrar(jugador.getId(), monstruo.get().getId());
            return "Victoria registrada en el ranking: " + jugador.getUsuario()
                    + " vencio a " + monstruo.get().getNombre() + ".";
        } catch (DatosException e) {
            return "No se pudo registrar la victoria: " + e.getMessage();
        }
    }
}
