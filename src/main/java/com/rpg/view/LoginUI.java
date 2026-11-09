package com.rpg.view;

import com.rpg.controller.LoginController;
import com.rpg.core.Consola;

/** Vista del login: pide los datos y muestra lo que responde LoginController. */
public class LoginUI {

    private final Consola consola;
    private final LoginController controller;

    public LoginUI(Consola consola, LoginController controller) {
        this.consola = consola;
        this.controller = controller;
    }

    /**
     * @return true si se puede seguir al juego (sesion iniciada, o sin base de datos);
     *         false si la sesion se bloqueo o el jugador eligio salir
     */
    public boolean mostrar() {
        System.out.println("\n=== ARENA SEGURA ===");
        String errorConexion = controller.verificarConexion();
        if (errorConexion != null) {
            System.out.println("Sin conexion con la base de datos: " + errorConexion);
            System.out.println("Se juega sin login: no habra bestiario en la base, ranking ni registro de victorias.");
            return true;
        }
        while (!controller.haySesion() && !controller.estaBloqueado()) {
            System.out.println("1. Iniciar sesion  2. Registrarse  0. Salir");
            int opcion = consola.leerEntero("Opcion: ", 0, 2);
            switch (opcion) {
                case 1 -> iniciarSesion();
                case 2 -> registrarse();
                default -> {
                    return false;
                }
            }
        }
        return controller.haySesion();
    }

    private void iniciarSesion() {
        String usuario = consola.leerLinea("Usuario: ");
        String clave = consola.leerLinea("Clave: ");
        System.out.println(controller.iniciarSesion(usuario, clave));
    }

    private void registrarse() {
        String usuario = consola.leerLinea("Nuevo usuario: ");
        String clave = consola.leerLinea("Clave: ");
        String confirmacion = consola.leerLinea("Repite la clave: ");
        System.out.println(controller.registrar(usuario, clave, confirmacion));
    }
}
