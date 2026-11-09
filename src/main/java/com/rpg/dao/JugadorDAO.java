package com.rpg.dao;

import com.rpg.exceptions.DatosException;
import com.rpg.model.Jugador;
import com.rpg.model.Rol;
import com.rpg.seguridad.HashClave;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

/** Acceso a la tabla jugadores. Todas las consultas usan PreparedStatement con parametros "?". */
public class JugadorDAO {

    /** @return el jugador creado, con el id que genero la base de datos */
    public Jugador registrar(String usuario, String clave, Rol rol) throws DatosException {
        String sql = "INSERT INTO jugadores (usuario, clave, rol) VALUES (?, ?, ?)";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            consulta.setString(1, usuario);
            consulta.setString(2, HashClave.sha256(clave));
            consulta.setString(3, rol.getValor());
            consulta.executeUpdate();
            try (ResultSet generadas = consulta.getGeneratedKeys()) {
                if (!generadas.next()) {
                    throw new DatosException("La base de datos no devolvio el id del jugador.");
                }
                return new Jugador(generadas.getInt(1), usuario, rol);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatosException("El usuario '" + usuario + "' ya existe.", e);
        } catch (SQLException e) {
            throw new DatosException("No se pudo registrar el jugador: " + e.getMessage(), e);
        }
    }

    /**
     * La consulta y los datos viajan por separado: el usuario y el hash se envian como
     * valores de los "?", nunca como parte del texto SQL. Por eso una entrada como
     * admin' OR 1=1 -- se busca como un nombre de usuario literal y no encuentra a nadie.
     *
     * @return el jugador si el usuario y la clave coinciden; vacio si no
     */
    public Optional<Jugador> autenticar(String usuario, String clave) throws DatosException {
        String sql = "SELECT id, usuario, rol FROM jugadores WHERE usuario = ? AND clave = ?";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, usuario);
            consulta.setString(2, HashClave.sha256(clave));
            try (ResultSet filas = consulta.executeQuery()) {
                if (filas.next()) {
                    return Optional.of(new Jugador(filas.getInt("id"), filas.getString("usuario"),
                            Rol.desdeValor(filas.getString("rol"))));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo validar el usuario: " + e.getMessage(), e);
        }
    }

    /** @return true si existia un jugador con ese id */
    public boolean cambiarRol(int id, Rol rol) throws DatosException {
        String sql = "UPDATE jugadores SET rol = ? WHERE id = ?";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, rol.getValor());
            consulta.setInt(2, id);
            return consulta.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatosException("No se pudo cambiar el rol: " + e.getMessage(), e);
        }
    }

    /** Sus victorias se borran con el (ON DELETE CASCADE). @return true si existia */
    public boolean eliminar(int id) throws DatosException {
        String sql = "DELETE FROM jugadores WHERE id = ?";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);
            return consulta.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatosException("No se pudo eliminar el jugador: " + e.getMessage(), e);
        }
    }
}
