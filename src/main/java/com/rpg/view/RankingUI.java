package com.rpg.view;

import com.rpg.controller.RankingController;
import com.rpg.core.Consola;
import com.rpg.exceptions.DatosException;
import com.rpg.model.RankingJugador;
import com.rpg.model.Victoria;

import java.util.List;

/** Vista del ranking y de las victorias del jugador que inicio sesion. */
public class RankingUI {

    private final Consola consola;
    private final RankingController controller;

    public RankingUI(Consola consola, RankingController controller) {
        this.consola = consola;
        this.controller = controller;
    }

    public void mostrar() {
        int opcion;
        do {
            System.out.println("\n=== RANKING ===");
            System.out.println("1. Ranking de jugadores  2. Mis victorias  0. Volver");
            opcion = consola.leerEntero("Opcion: ", 0, 2);
            try {
                switch (opcion) {
                    case 1 -> mostrarRanking(controller.ranking());
                    case 2 -> mostrarVictorias(controller.misVictorias());
                    default -> {
                    }
                }
            } catch (DatosException e) {
                System.out.println("Error de base de datos: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    /** El juego la usa para avisar cuando registra una victoria. */
    public void mostrarMensaje(String mensaje) {
        System.out.println(mensaje);
    }

    private void mostrarRanking(List<RankingJugador> ranking) {
        if (ranking.isEmpty()) {
            System.out.println("Todavia no hay victorias registradas.");
        }
        int puesto = 1;
        for (RankingJugador fila : ranking) {
            System.out.printf("%d. %s%n", puesto++, fila);
        }
    }

    private void mostrarVictorias(List<Victoria> victorias) {
        if (victorias.isEmpty()) {
            System.out.println("Todavia no tienes victorias registradas.");
        }
        for (Victoria victoria : victorias) {
            System.out.println(victoria);
        }
    }
}
