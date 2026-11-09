package com.rpg.dao;

import com.rpg.exceptions.DatosException;
import com.rpg.model.Monstruo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD de la tabla monstruos. Devuelve objetos Monstruo, nunca un ResultSet abierto. */
public class MonstruoDAO {

    private static final String COLUMNAS = "SELECT id, nombre, vida, ataque FROM monstruos";

    /** @return el monstruo creado, con su id */
    public Monstruo crear(Monstruo monstruo) throws DatosException {
        String sql = "INSERT INTO monstruos (nombre, vida, ataque) VALUES (?, ?, ?)";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            consulta.setString(1, monstruo.getNombre());
            consulta.setInt(2, monstruo.getVida());
            consulta.setInt(3, monstruo.getAtaque());
            consulta.executeUpdate();
            try (ResultSet generadas = consulta.getGeneratedKeys()) {
                if (!generadas.next()) {
                    throw new DatosException("La base de datos no devolvio el id del monstruo.");
                }
                return new Monstruo(generadas.getInt(1), monstruo.getNombre(),
                        monstruo.getVida(), monstruo.getAtaque());
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatosException("Ya existe un monstruo llamado '" + monstruo.getNombre() + "'.", e);
        } catch (SQLException e) {
            throw new DatosException("No se pudo crear el monstruo: " + e.getMessage(), e);
        }
    }

    public Optional<Monstruo> buscarPorId(int id) throws DatosException {
        String sql = COLUMNAS + " WHERE id = ?";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);
            try (ResultSet filas = consulta.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo buscar el monstruo: " + e.getMessage(), e);
        }
    }

    /** El nombre es unico. La comparacion no distingue mayusculas (collation _ci de la tabla). */
    public Optional<Monstruo> buscarPorNombre(String nombre) throws DatosException {
        String sql = COLUMNAS + " WHERE nombre = ?";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, nombre);
            try (ResultSet filas = consulta.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo buscar el monstruo: " + e.getMessage(), e);
        }
    }

    public List<Monstruo> listar() throws DatosException {
        String sql = COLUMNAS + " ORDER BY id";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet filas = consulta.executeQuery()) {
            return mapearLista(filas);
        } catch (SQLException e) {
            throw new DatosException("No se pudo listar los monstruos: " + e.getMessage(), e);
        }
    }

    /** @return los monstruos con al menos esa vida, de menor a mayor vida */
    public List<Monstruo> listarPorVidaMinima(int vidaMinima) throws DatosException {
        String sql = COLUMNAS + " WHERE vida >= ? ORDER BY vida, nombre";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, vidaMinima);
            try (ResultSet filas = consulta.executeQuery()) {
                return mapearLista(filas);
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo filtrar los monstruos: " + e.getMessage(), e);
        }
    }

    /** @return true si existia un monstruo con ese id */
    public boolean actualizar(Monstruo monstruo) throws DatosException {
        String sql = "UPDATE monstruos SET nombre = ?, vida = ?, ataque = ? WHERE id = ?";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, monstruo.getNombre());
            consulta.setInt(2, monstruo.getVida());
            consulta.setInt(3, monstruo.getAtaque());
            consulta.setInt(4, monstruo.getId());
            return consulta.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatosException("Ya existe otro monstruo llamado '" + monstruo.getNombre() + "'.", e);
        } catch (SQLException e) {
            throw new DatosException("No se pudo actualizar el monstruo: " + e.getMessage(), e);
        }
    }

    /**
     * MonstruoController comprueba antes que no tenga victorias. Si aun asi la FOREIGN KEY
     * lo impide (por ejemplo, otra victoria registrada justo despues), se informa sin caer.
     *
     * @return true si existia un monstruo con ese id
     */
    public boolean eliminar(int id) throws DatosException {
        String sql = "DELETE FROM monstruos WHERE id = ?";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);
            return consulta.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatosException("El monstruo tiene victorias registradas y no se puede eliminar.", e);
        } catch (SQLException e) {
            throw new DatosException("No se pudo eliminar el monstruo: " + e.getMessage(), e);
        }
    }

    private static List<Monstruo> mapearLista(ResultSet filas) throws SQLException {
        List<Monstruo> monstruos = new ArrayList<>();
        while (filas.next()) {
            monstruos.add(mapear(filas));
        }
        return monstruos;
    }

    private static Monstruo mapear(ResultSet fila) throws SQLException {
        return new Monstruo(fila.getInt("id"), fila.getString("nombre"),
                fila.getInt("vida"), fila.getInt("ataque"));
    }
}
