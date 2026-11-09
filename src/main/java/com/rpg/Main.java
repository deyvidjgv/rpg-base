package com.rpg;

import com.rpg.controller.LoginController;
import com.rpg.controller.MonstruoController;
import com.rpg.controller.RankingController;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.dao.JugadorDAO;
import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.exceptions.DatosException;
import com.rpg.view.LoginUI;
import com.rpg.view.MonstruoUI;
import com.rpg.view.RankingUI;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Consola consola = new Consola(new Scanner(System.in));
        MotorJuego motor = new MotorJuego(consola);

        // Cada View recibe su Controller; cada Controller recibe sus DAO.
        MonstruoDAO monstruoDAO = new MonstruoDAO();
        VictoriaDAO victoriaDAO = new VictoriaDAO();
        LoginController login = new LoginController(new JugadorDAO());
        MonstruoController monstruos = new MonstruoController(monstruoDAO, victoriaDAO, login);
        RankingController ranking = new RankingController(victoriaDAO, monstruoDAO, login);
        LoginUI loginUI = new LoginUI(consola, login);
        MonstruoUI monstruoUI = new MonstruoUI(consola, monstruos);
        RankingUI rankingUI = new RankingUI(consola, ranking);

        try {
            if (!loginUI.mostrar()) {
                System.out.println(login.estaBloqueado()
                        ? "Sesion bloqueada. Hasta luego." : "Hasta luego.");
                return;
            }
            if (login.haySesion()) {
                sincronizarRivales(motor, monstruos);
                motor.setAlVencerMonstruo(
                        nombre -> rankingUI.mostrarMensaje(ranking.registrarVictoria(nombre)));
            }
            ejecutarMenu(consola, motor, login, monstruos, monstruoUI, rankingUI);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        } finally {
            motor.cerrar();
        }
    }

    private static void ejecutarMenu(Consola consola, MotorJuego motor, LoginController login,
                                     MonstruoController monstruos, MonstruoUI monstruoUI, RankingUI rankingUI) {
        int opcion;
        do {
            mostrarMenu(login);
            opcion = consola.leerEntero("Opcion: ", 0, 10);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.cargarProgreso();
                case 5 -> motor.mostrarHistorial();
                case 6 -> motor.mostrarArchivos();
                case 7 -> motor.borrarPartida();
                case 8 -> motor.borrarHistorial();
                case 9 -> {
                    if (requiereSesion(login)) {
                        monstruoUI.mostrar();
                        sincronizarRivales(motor, monstruos);
                    }
                }
                case 10 -> {
                    if (requiereSesion(login)) {
                        rankingUI.mostrar();
                    }
                }
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != 0);
    }

    /** Los rivales del combate rapido se leen de la tabla monstruos. */
    private static void sincronizarRivales(MotorJuego motor, MonstruoController monstruos) {
        try {
            motor.sincronizarMonstruos(monstruos.listar());
        } catch (DatosException e) {
            System.out.println("No se pudieron leer los monstruos de la base: " + e.getMessage());
        }
    }

    private static boolean requiereSesion(LoginController login) {
        if (!login.haySesion()) {
            System.out.println("Esta opcion necesita la base de datos y una sesion iniciada.");
            return false;
        }
        return true;
    }

    private static void mostrarMenu(LoginController login) {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println(login.haySesion()
                ? "Sesion: " + login.getJugadorActual() : "Sin sesion (sin base de datos)");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Cargar partida");
        System.out.println("5. Ver historial de combates");
        System.out.println("6. Ver archivos de la partida");
        System.out.println("7. Borrar partida guardada");
        System.out.println("8. Borrar historial");
        System.out.println("9. Monstruos (base de datos)");
        System.out.println("10. Ranking y mis victorias");
        System.out.println("0. Salir");
    }
}
