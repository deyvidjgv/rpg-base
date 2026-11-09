package com.rpg.model;

/** Una fila del ranking: usuario y total de victorias. */
public class RankingJugador {

    private final String usuario;
    private final int totalVictorias;

    public RankingJugador(String usuario, int totalVictorias) {
        this.usuario = usuario;
        this.totalVictorias = totalVictorias;
    }

    public String getUsuario() {
        return usuario;
    }

    public int getTotalVictorias() {
        return totalVictorias;
    }

    @Override
    public String toString() {
        return String.format("%-15s %d victorias", usuario, totalVictorias);
    }
}
