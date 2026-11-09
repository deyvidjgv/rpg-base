package com.rpg.core;

import java.util.Scanner;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Entrada por teclado con validacion y reintento. */
public class Consola {

    private static final int LARGO_MAXIMO_NOMBRE = 15;
    private static final String PATRON_NOMBRE = "[\\p{L}0-9 ]+";

    private static final String FIN_ENTRADA = "\u0000fin";
    private static final String TIEMPO_AGOTADO = "\u0000tiempo";
    private final BlockingQueue<String> lineas = new LinkedBlockingQueue<>();

    public Consola(Scanner scanner) {
        // Solo este hilo lee el Scanner. Interrumpir un hilo no desbloquea nextLine().
        Thread teclado = new Thread(() -> {
            try {
                while (scanner.hasNextLine()) {
                    lineas.offer(scanner.nextLine());
                }
            } finally {
                lineas.offer(FIN_ENTRADA);
            }
        }, "teclado");
        teclado.setDaemon(true);
        teclado.start();
    }

    public String leerLinea(String mensaje) {
        System.out.print(mensaje);
        try {
            String linea = lineas.take();
            if (linea.equals(FIN_ENTRADA)) {
                lineas.offer(FIN_ENTRADA);
                throw new NoSuchElementException("Entrada finalizada");
            }
            if (linea.equals(TIEMPO_AGOTADO)) {
                return null;
            }
            return linea.trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Lectura interrumpida");
        }
    }

    public void avisarTiempoAgotado() {
        lineas.offer(TIEMPO_AGOTADO);
    }

    public void limpiarTiempoAgotado() {
        lineas.remove(TIEMPO_AGOTADO);
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerLinea(mensaje);
            if (texto == null) {
                return -1;
            }
            if (texto.isEmpty()) {
                System.out.println("No escribiste nada.");
                continue;
            }
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" no es un numero.");
            }
        }
    }

    public String leerNombre(String mensaje) {
        while (true) {
            String nombre = leerLinea(mensaje);
            if (nombre.isEmpty()) {
                System.out.println("El nombre no puede estar vacio.");
            } else if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
                System.out.println("Maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.");
            } else if (!nombre.matches(PATRON_NOMBRE)) {
                System.out.println("Usa solo letras, numeros y espacios.");
            } else {
                return nombre;
            }
        }
    }
}
