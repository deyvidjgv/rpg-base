package com.rpg.model;

/** Rol de un jugador. El valor coincide con el ENUM de la columna jugadores.rol. */
public enum Rol {
    JUGADOR("jugador"),
    ADMIN("admin");

    private final String valor;

    Rol(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static Rol desdeValor(String valor) {
        for (Rol rol : values()) {
            if (rol.valor.equalsIgnoreCase(valor)) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol desconocido: " + valor);
    }
}
