package com.rpg.core;

import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.model.Monstruo;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catalogo de monstruos (Map nombre -> plantilla) y contador de derrotas por monstruo. */
public class Bestiario implements Serializable {

    private static final long serialVersionUID = 1L;
    // La tabla monstruos no tiene defensa: los monstruos nuevos usan este valor.
    private static final int DEFENSA_MONSTRUO_TABLA = 3;

    private final Map<String, Enemigo> catalogo = new LinkedHashMap<>();
    private final Map<String, Integer> derrotados = new LinkedHashMap<>();

    public Bestiario() {
        for (Enemigo enemigo : plantillasBase()) {
            registrar(enemigo);
        }
    }

    /** Los monstruos propios del juego, con su defensa y su habilidad especial. */
    private static List<Enemigo> plantillasBase() {
        return List.of(
                new Enemigo("Goblin", 50, 8, 2),
                new Enemigo("Arana Gigante", 60, 10, 3,
                        new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 4, 3), false),
                new Enemigo("Ogro", 100, 14, 6, null, true),
                new Enemigo("Dragon Joven", 160, 20, 10,
                        new EstadoEfecto("Quemadura", TipoEfecto.DANIO_CONTINUO, 6, 3), false));
    }

    private void registrar(Enemigo enemigo) {
        catalogo.put(enemigo.getNombre(), enemigo);
    }

    /**
     * Usa como rivales los monstruos de la tabla monstruos. La vida y el ataque vienen de la
     * tabla; si el nombre coincide con un monstruo propio del juego, conserva su defensa y su
     * habilidad. Con la tabla vacia se mantiene el catalogo actual, para que siempre haya rivales.
     */
    public void sincronizar(List<Monstruo> monstruos) {
        if (monstruos.isEmpty()) {
            return;
        }
        catalogo.clear();
        for (Monstruo monstruo : monstruos) {
            Enemigo plantilla = buscarPlantilla(monstruo.getNombre());
            registrar(plantilla != null
                    ? plantilla.conEstadisticas(monstruo.getNombre(), monstruo.getVida(), monstruo.getAtaque())
                    : new Enemigo(monstruo.getNombre(), monstruo.getVida(), monstruo.getAtaque(),
                            DEFENSA_MONSTRUO_TABLA));
        }
    }

    private static Enemigo buscarPlantilla(String nombre) {
        for (Enemigo plantilla : plantillasBase()) {
            if (plantilla.getNombre().equalsIgnoreCase(nombre)) {
                return plantilla;
            }
        }
        return null;
    }

    public List<String> nombres() {
        return new ArrayList<>(catalogo.keySet());
    }

    /** Busca ignorando mayusculas (equalsIgnoreCase, nunca ==). @return copia nueva o null si no existe */
    public Enemigo crearEnemigo(String nombre) {
        for (Map.Entry<String, Enemigo> entrada : catalogo.entrySet()) {
            if (entrada.getKey().equalsIgnoreCase(nombre)) {
                return entrada.getValue().copia();
            }
        }
        return null;
    }

    public void registrarEnemigoDerrotado(String nombre) {
        derrotados.put(nombre, derrotados.getOrDefault(nombre, 0) + 1);
    }

    public int vecesDerrotado(String nombre) {
        return derrotados.getOrDefault(nombre, 0);
    }

    public String describir() {
        StringBuilder texto = new StringBuilder();
        int numero = 1;
        for (Map.Entry<String, Enemigo> entrada : catalogo.entrySet()) {
            Enemigo enemigo = entrada.getValue();
            texto.append(String.format("%d. %-14s HP %3d  ATK %2d  DEF %2d  Habilidad: %-9s Derrotado: %d%n",
                    numero++, entrada.getKey(), enemigo.getVidaMaxima(), enemigo.getAtaque(),
                    enemigo.getDefensa(), enemigo.getHabilidad(), vecesDerrotado(entrada.getKey())));
        }
        return texto.toString();
    }

    public String resumenDerrotados() {
        if (derrotados.isEmpty()) {
            return "ninguno";
        }
        return derrotados.toString();
    }
}
