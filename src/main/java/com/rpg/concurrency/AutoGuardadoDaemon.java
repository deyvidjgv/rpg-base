package com.rpg.concurrency;

import com.rpg.core.MotorJuego;

public class AutoGuardadoDaemon implements Runnable {
    private final MotorJuego motor;
    private final long intervalo;

    public AutoGuardadoDaemon(MotorJuego motor, long intervalo) {
        this.motor = motor;
        this.intervalo = intervalo;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervalo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            motor.guardarProgreso();
        }
    }
}
