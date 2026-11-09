package com.rpg.dao.demo;

import com.rpg.dao.ConexionDB;
import com.rpg.dao.JugadorDAO;
import com.rpg.exceptions.DatosException;
import com.rpg.model.Jugador;
import com.rpg.seguridad.HashClave;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * SOLO CON FINES DIDACTICOS. No se usa en el juego.
 *
 * Muestra por que no se debe armar SQL concatenando texto del usuario con Statement,
 * y compara el mismo login con JugadorDAO.autenticar, que usa PreparedStatement.
 * Es la unica clase del proyecto donde aparece Statement; tambien contiene la
 * comparativa de 1000 inserciones del informe por ese mismo motivo.
 *
 * Ejecutar con la base creada (schema.sql) y config.txt listo.
 */
public final class LoginInseguroDemo {

    private static final String ENTRADA_MALICIOSA = "admin' OR 1=1 -- ";
    private static final int INSERCIONES = 1000;
    private static final int CALENTAMIENTO = 100;

    private LoginInseguroDemo() {
    }

    /** Arma la consulta pegando el texto del usuario dentro del SQL. */
    static String construirConsulta(String usuario, String clave) {
        return "SELECT usuario FROM jugadores WHERE usuario = '" + usuario
                + "' AND clave = '" + HashClave.sha256(clave) + "'";
    }

    /** VULNERABLE: el texto del usuario puede cambiar la logica del WHERE. */
    static Optional<String> loginInseguro(String usuario, String clave) throws SQLException {
        String sql = construirConsulta(usuario, clave);
        try (Connection conexion = ConexionDB.obtenerConexion();
             Statement sentencia = conexion.createStatement();
             ResultSet filas = sentencia.executeQuery(sql)) {
            return filas.next() ? Optional.of(filas.getString("usuario")) : Optional.empty();
        }
    }

    public static void main(String[] args) {
        try {
            demostrarInyeccion();
            compararRendimiento();
        } catch (SQLException | DatosException e) {
            System.out.println("No se pudo completar la demostracion: " + e.getMessage());
        }
    }

    private static void demostrarInyeccion() throws SQLException, DatosException {
        String claveCualquiera = "no-la-se";
        System.out.println("Entrada usada como usuario: \"" + ENTRADA_MALICIOSA + "\"");
        System.out.println("Clave usada: \"" + claveCualquiera + "\"");

        System.out.println("\n=== 1. Login inseguro (concatenacion + Statement) ===");
        System.out.println("SQL enviado: " + construirConsulta(ENTRADA_MALICIOSA, claveCualquiera));
        Optional<String> inseguro = loginInseguro(ENTRADA_MALICIOSA, claveCualquiera);
        System.out.println(inseguro.isPresent()
                ? "Resultado: ENTRA como '" + inseguro.get() + "' sin conocer la clave"
                : "Resultado: rechazado");

        System.out.println("\n=== 2. Login seguro (JugadorDAO.autenticar con PreparedStatement) ===");
        JugadorDAO dao = new JugadorDAO();
        Optional<Jugador> seguro = dao.autenticar(ENTRADA_MALICIOSA, claveCualquiera);
        System.out.println(seguro.isPresent()
                ? "Resultado: ENTRA como '" + seguro.get().getUsuario() + "'"
                : "Resultado: RECHAZADO (la entrada se busco como un usuario literal)");

        Optional<Jugador> legitimo = dao.autenticar("admin", "admin123");
        System.out.println("Control con admin/admin123: "
                + (legitimo.isPresent() ? "entra correctamente" : "rechazado"));
    }

    /**
     * Inserta 1000 filas con cada tipo sobre la misma conexion. Cada lote va en una sola
     * transaccion: con autocommit, cada INSERT espera la escritura en disco y esa espera tapa
     * la diferencia entre los dos. Antes hay un calentamiento para que la primera medicion
     * no cargue con el arranque de la JVM. Al final se borran las filas de prueba.
     */
    private static void compararRendimiento() throws SQLException {
        System.out.println("\n=== 3. Comparativa: " + INSERCIONES + " inserciones con cada uno ===");
        try (Connection conexion = ConexionDB.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                borrarPruebas(conexion);
                insertarConStatement(conexion, "calentar_s_", CALENTAMIENTO);
                insertarConPrepared(conexion, "calentar_p_", CALENTAMIENTO);
                borrarPruebas(conexion);
                conexion.commit();

                long inicio = System.nanoTime();
                insertarConStatement(conexion, "bench_s_", INSERCIONES);
                conexion.commit();
                long msStatement = (System.nanoTime() - inicio) / 1_000_000;

                inicio = System.nanoTime();
                insertarConPrepared(conexion, "bench_p_", INSERCIONES);
                conexion.commit();
                long msPrepared = (System.nanoTime() - inicio) / 1_000_000;

                borrarPruebas(conexion);
                conexion.commit();
                System.out.printf("Statement:         %d ms%n", msStatement);
                System.out.printf("PreparedStatement: %d ms%n", msPrepared);
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    /** Arma un SQL nuevo en cada vuelta: la base lo recibe como texto distinto cada vez. */
    private static void insertarConStatement(Connection conexion, String prefijo, int cantidad) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            for (int i = 1; i <= cantidad; i++) {
                sentencia.executeUpdate("INSERT INTO monstruos (nombre, vida, ataque) VALUES ('"
                        + prefijo + i + "', 10, 1)");
            }
        }
    }

    /** Prepara la consulta una sola vez y en cada vuelta solo cambian los valores. */
    private static void insertarConPrepared(Connection conexion, String prefijo, int cantidad) throws SQLException {
        String sql = "INSERT INTO monstruos (nombre, vida, ataque) VALUES (?, ?, ?)";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            for (int i = 1; i <= cantidad; i++) {
                consulta.setString(1, prefijo + i);
                consulta.setInt(2, 10);
                consulta.setInt(3, 1);
                consulta.executeUpdate();
            }
        }
    }

    private static void borrarPruebas(Connection conexion) throws SQLException {
        String sql = "DELETE FROM monstruos WHERE nombre LIKE 'bench\\_%' OR nombre LIKE 'calentar\\_%'";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.executeUpdate();
        }
    }
}
