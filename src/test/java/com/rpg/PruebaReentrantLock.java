package com.rpg;

import com.rpg.entities.Guerrero;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class PruebaReentrantLock {
    // Variante de recibirDanio para la investigacion. El juego conserva synchronized.
    private static class PersonajeConLock extends Guerrero {
        private static final long serialVersionUID = 1L;
        private final ReentrantLock candado = new ReentrantLock();

        PersonajeConLock(String nombre) {
            super(nombre);
        }

        @Override
        public void recibirDanio(int cantidad) {
            candado.lock();
            try {
                if (cantidad < 0) {
                    throw new IllegalArgumentException("El danio no puede ser negativo");
                }
                setVida(getVida() - cantidad);
            } finally {
                candado.unlock();
            }
        }
        // Preferiria este lock si necesito tryLock con tiempo o lockInterruptibly.
        // Para usar esta variante en el juego, curar, estado y efectos tambien deben
        // compartir el mismo lock. Esta prueba compara solamente dos hilos de dano.
    }

    public static void main(String[] args) throws InterruptedException {
        PersonajeConLock personaje = new PersonajeConLock("Lock");
        personaje.setVidaMaxima(1000000);
        personaje.setVida(1000000);
        CountDownLatch inicio = new CountDownLatch(1);
        Runnable dano = () -> {
            try {
                inicio.await();
                for (int i = 0; i < 100000; i++) {
                    personaje.recibirDanio(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Thread a = new Thread(dano);
        Thread b = new Thread(dano);
        a.start();
        b.start();
        inicio.countDown();
        a.join();
        b.join();
        if (personaje.getVida() != 800000) {
            throw new AssertionError("Se perdieron actualizaciones usando el lock");
        }
        System.out.println("ReentrantLock: esperado 800000, obtenido " + personaje.getVida());
    }
}
