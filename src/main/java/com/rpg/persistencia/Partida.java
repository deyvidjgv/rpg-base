package com.rpg.persistencia;

import com.rpg.core.Bestiario;
import com.rpg.entities.Enemigo;
import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import java.io.Serializable;

public class Partida implements Serializable {
    // Identifica la version de esta clase al guardar y al cargar objetos.
    private static final long serialVersionUID = 1L;

    private final Personaje jugador;
    private final Enemigo enemigo;
    private final PerfilJugador perfil;
    private final Gremio gremio;
    private final Bestiario bestiario;
    private final String historial;
    private final int ronda;
    private final boolean combateEnCurso;
    private final int faseTurno;

    public Partida(Personaje jugador, Enemigo enemigo, PerfilJugador perfil, Gremio gremio,
                   Bestiario bestiario, String historial, int ronda, boolean combateEnCurso, int faseTurno) {
        this.jugador = jugador;
        this.enemigo = enemigo;
        this.perfil = perfil;
        this.gremio = gremio;
        this.bestiario = bestiario;
        this.historial = historial;
        this.ronda = ronda;
        this.combateEnCurso = combateEnCurso;
        this.faseTurno = faseTurno;
    }

    public Personaje getJugador() {
        return jugador;
    }

    public Enemigo getEnemigo() {
        return enemigo;
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Bestiario getBestiario() {
        return bestiario;
    }

    public String getHistorial() {
        return historial;
    }

    public int getRonda() {
        return ronda;
    }

    public boolean isCombateEnCurso() {
        return combateEnCurso;
    }

    public int getFaseTurno() {
        return faseTurno;
    }
}
