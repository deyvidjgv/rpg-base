package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;
import java.util.ArrayList;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private final Arena arena;
    private final RegistroCombate registro;
    private List<Personaje> participantes = List.of();

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();
        synchronized (this) {
            participantes = new ArrayList<>(gladiadores);
        }

        Personaje[] ganadores = new Personaje[2];
        Thread semifinalA = new Thread(() -> ganadores[0] = simularSemifinal(
                gladiadores.get(0), gladiadores.get(1)), "semifinal-A");
        Thread semifinalB = new Thread(() -> ganadores[1] = simularSemifinal(
                gladiadores.get(2), gladiadores.get(3)), "semifinal-B");

        mostrarEstados("Antes de start", semifinalA, semifinalB);
        semifinalA.start();
        semifinalB.start();
        mostrarEstados("Durante la ejecucion", semifinalA, semifinalB);
        try {
            Thread.sleep(100);
            mostrarEstados("Durante las rondas", semifinalA, semifinalB);
            semifinalA.join();
            semifinalB.join();
        } catch (InterruptedException e) {
            semifinalA.interrupt();
            semifinalB.interrupt();
            // Se espera la limpieza de ambos antes de volver al menu.
            try {
                semifinalA.join();
                semifinalB.join();
            } catch (InterruptedException segundaInterrupcion) {
                Thread.currentThread().interrupt();
                System.out.println("Se interrumpio la espera del torneo.");
            }
            Thread.currentThread().interrupt();
            return null;
        }
        mostrarEstados("Despues de join", semifinalA, semifinalB);
        // join tambien hace visibles las escrituras en el arreglo de ganadores.
        if (ganadores[0] == null || ganadores[1] == null) {
            registro.agregar("Torneo cancelado.");
            return null;
        }
        return simularFinal(ganadores[0], ganadores[1]);
    }

    private void mostrarEstados(String momento, Thread a, Thread b) {
        registro.agregar("%s: A=%s, B=%s", momento, a.getState(), b.getState());
    }

    public synchronized List<Personaje> getParticipantes() {
        return new ArrayList<>(participantes);
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        if (ganador != null) {
            registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        }
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
