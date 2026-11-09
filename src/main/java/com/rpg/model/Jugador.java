package com.rpg.model;

/** Fila de la tabla jugadores. La clave no se guarda en el objeto: solo se usa para autenticar. */
public class Jugador {

    private final int id;
    private final String usuario;
    private final Rol rol;

    public Jugador(int id, String usuario, Rol rol) {
        this.id = id;
        this.usuario = usuario;
        this.rol = rol;
    }

    public int getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    @Override
    public String toString() {
        return usuario + " (" + rol.getValor() + ")";
    }
}
