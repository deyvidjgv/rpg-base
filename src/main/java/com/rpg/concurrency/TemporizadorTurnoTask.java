package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class TemporizadorTurnoTask implements Runnable {
    private final int segundos;
    private final TurnoCallback callback;

    public TemporizadorTurnoTask(int segundos, TurnoCallback callback) {
        this.segundos = segundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        for (int restante = segundos; restante > 0; restante--) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (!Thread.currentThread().isInterrupted()) {
            callback.onTiempoAgotado();
        }
    }
}
