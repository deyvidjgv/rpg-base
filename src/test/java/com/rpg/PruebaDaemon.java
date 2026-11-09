package com.rpg;

import java.util.concurrent.CountDownLatch;

public class PruebaDaemon {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch fin = new CountDownLatch(1);
        Thread hilo = new Thread(() -> {
            try {
                fin.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "prueba-daemon");

        if (args.length > 0 && args[0].equals("tarde")) {
            hilo.start();
            try {
                hilo.setDaemon(true);
                throw new AssertionError("Se permitio cambiar a daemon despues de start");
            } catch (IllegalThreadStateException e) {
                System.out.println("setDaemon despues de start: " + e.getClass().getSimpleName());
            } finally {
                fin.countDown();
                hilo.join();
            }
        } else {
            // El hilo de usuario mantiene la JVM viva; el daemon no impide que termine.
            hilo.setDaemon(true);
            hilo.start();
            System.out.println("Fin de main. El daemon sigue vivo: " + hilo.isAlive());
        }
    }
}
