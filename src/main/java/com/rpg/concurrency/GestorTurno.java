package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class GestorTurno {
    private Thread temporizador;

    public void iniciar(int segundos, TurnoCallback callback) {
        temporizador = new Thread(new TemporizadorTurnoTask(segundos, callback), "temporizador-turno");
        temporizador.start();
    }

    public void cancelar() {
        if (temporizador != null) {
            temporizador.interrupt();
            try {
                temporizador.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
