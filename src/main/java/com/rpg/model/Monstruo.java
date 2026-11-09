package com.rpg.model;

/** Fila de la tabla monstruos. Un id 0 significa que todavia no se ha guardado. */
public class Monstruo {

    private final int id;
    private final String nombre;
    private final int vida;
    private final int ataque;

    public Monstruo(String nombre, int vida, int ataque) {
        this(0, nombre, vida, ataque);
    }

    public Monstruo(int id, String nombre, int vida, int ataque) {
        this.id = id;
        this.nombre = nombre;
        this.vida = vida;
        this.ataque = ataque;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getVida() {
        return vida;
    }

    public int getAtaque() {
        return ataque;
    }

    @Override
    public String toString() {
        return String.format("#%d %-15s vida %3d  ataque %2d", id, nombre, vida, ataque);
    }
}
