package com.rpg.core;

import com.rpg.persistencia.GestorArchivos;

/** Imprime cada evento del combate y lo guarda para incluirlo en el guardado de la partida. */
public class RegistroCombate {

    private final StringBuilder texto = new StringBuilder();

    public synchronized void agregar(String linea) {
        System.out.println(linea);
        texto.append(linea).append(System.lineSeparator());
    }

    public void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        texto.setLength(0);
    }

    public synchronized String getTexto() {
        return texto.toString();
    }

    public synchronized void recuperar(String historial) {
        texto.setLength(0);
        texto.append(historial);
    }

    public void guardarEnArchivo(GestorArchivos archivos) {
        archivos.escribirLog(getTexto());
    }
}
