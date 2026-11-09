package com.rpg.concurrency;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Personaje;

public class EfectoContinuoTask implements Runnable {
    private final Personaje personaje;
    private final EstadoEfecto efecto;
    private final String nombre;
    private final long intervalo;

    public EfectoContinuoTask(Personaje personaje, EstadoEfecto efecto, long intervalo) {
        this.personaje = personaje;
        this.efecto = efecto;
        this.nombre = efecto.getNombre();
        this.intervalo = intervalo;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(intervalo);
                } catch (InterruptedException e) {
                    personaje.finalizarEfecto(efecto);
                    Thread.currentThread().interrupt();
                    break;
                }
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                String mensaje = personaje.aplicarTickContinuo(efecto);
                if (mensaje == null) {
                    break;
                }
                personaje.registrarEfecto(mensaje);
                if (efecto.haTerminado()) {
                    personaje.registrarEfecto(personaje.getNombre() + ": termina " + nombre);
                    break;
                }
            }
        } finally {
            personaje.finalizarEfecto(efecto);
        }
    }
}
