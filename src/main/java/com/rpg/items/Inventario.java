package com.rpg.items;

import com.rpg.exceptions.CapacidadExcedidaException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Contenedor generico con capacidad limitada: Inventario&lt;Pocion&gt; solo acepta pociones. */
public class Inventario<T> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<T> elementos = new ArrayList<>();
    private final int capacidad;

    public Inventario(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser positiva");
        }
        this.capacidad = capacidad;
    }

    public synchronized void agregar(T elemento) throws CapacidadExcedidaException {
        if (elementos.size() >= capacidad) {
            throw new CapacidadExcedidaException(capacidad);
        }
        elementos.add(elemento);
    }

    public synchronized T obtener(int indice) {
        return elementos.get(indice);
    }

    public synchronized T quitar(int indice) {
        return elementos.remove(indice);
    }

    public synchronized int tamanio() {
        return elementos.size();
    }

    public synchronized boolean estaVacio() {
        return elementos.isEmpty();
    }

    @Override
    public synchronized Iterator<T> iterator() {
        return new ArrayList<>(elementos).iterator();
    }
}
