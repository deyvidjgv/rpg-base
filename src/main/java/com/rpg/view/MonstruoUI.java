package com.rpg.view;

import com.rpg.controller.MonstruoController;
import com.rpg.core.Consola;
import com.rpg.exceptions.DatosException;
import com.rpg.model.Monstruo;

import java.util.List;
import java.util.Optional;

/** Vista del bestiario guardado en la base de datos. Los permisos los decide MonstruoController. */
public class MonstruoUI {

    // Solo convierte el texto en numero; si el valor es valido lo decide el Controller.
    private static final int NUMERO_MINIMO = -1_000_000;
    private static final int NUMERO_MAXIMO = 1_000_000;

    private final Consola consola;
    private final MonstruoController controller;

    public MonstruoUI(Consola consola, MonstruoController controller) {
        this.consola = consola;
        this.controller = controller;
    }

    public void mostrar() {
        int opcion;
        do {
            System.out.println("\n=== MONSTRUOS ===");
            System.out.println("1. Listar todos  2. Buscar por nombre  3. Listar por vida minima");
            System.out.println("4. Crear  5. Editar  6. Eliminar  (solo admin)  0. Volver");
            opcion = consola.leerEntero("Opcion: ", 0, 6);
            try {
                switch (opcion) {
                    case 1 -> mostrarLista(controller.listar());
                    case 2 -> buscarPorNombre();
                    case 3 -> mostrarLista(controller.listarPorVidaMinima(leerNumero("Vida minima: ")));
                    case 4 -> crear();
                    case 5 -> editar();
                    case 6 -> eliminar();
                    default -> {
                    }
                }
            } catch (DatosException e) {
                System.out.println("Error de base de datos: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    private void buscarPorNombre() throws DatosException {
        String nombre = consola.leerLinea("Nombre: ");
        Optional<Monstruo> monstruo = controller.buscarPorNombre(nombre);
        System.out.println(monstruo.map(Monstruo::toString).orElse("No existe un monstruo llamado '" + nombre + "'."));
    }

    private void crear() {
        if (mostrarRechazo()) {
            return;
        }
        String nombre = consola.leerLinea("Nombre: ");
        int vida = leerNumero("Vida: ");
        int ataque = leerNumero("Ataque: ");
        System.out.println(controller.crear(nombre, vida, ataque));
    }

    private void editar() throws DatosException {
        if (mostrarRechazo()) {
            return;
        }
        int id = leerNumero("Id del monstruo: ");
        Optional<Monstruo> actual = controller.buscarPorId(id);
        if (actual.isEmpty()) {
            System.out.println("No existe un monstruo con id " + id + ".");
            return;
        }
        System.out.println("Actual: " + actual.get());
        String nombre = consola.leerLinea("Nuevo nombre: ");
        int vida = leerNumero("Nueva vida: ");
        int ataque = leerNumero("Nuevo ataque: ");
        System.out.println(controller.editar(id, nombre, vida, ataque));
    }

    private void eliminar() {
        if (mostrarRechazo()) {
            return;
        }
        int id = leerNumero("Id del monstruo: ");
        String respuesta = consola.leerLinea("Seguro que quieres eliminarlo? (s/n): ");
        if (respuesta.equalsIgnoreCase("s")) {
            System.out.println(controller.eliminar(id));
        } else {
            System.out.println("Eliminacion cancelada.");
        }
    }

    /** Pregunta al Controller antes de pedir datos, para no hacerlos escribir en vano. */
    private boolean mostrarRechazo() {
        String rechazo = controller.comprobarPermiso();
        if (rechazo != null) {
            System.out.println(rechazo);
            return true;
        }
        return false;
    }

    private int leerNumero(String mensaje) {
        return consola.leerEntero(mensaje, NUMERO_MINIMO, NUMERO_MAXIMO);
    }

    private void mostrarLista(List<Monstruo> monstruos) {
        if (monstruos.isEmpty()) {
            System.out.println("No hay monstruos que mostrar.");
        }
        for (Monstruo monstruo : monstruos) {
            System.out.println(monstruo);
        }
    }
}
