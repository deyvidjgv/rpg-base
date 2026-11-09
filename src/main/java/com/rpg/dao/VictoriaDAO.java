package com.rpg.dao;

import com.rpg.exceptions.DatosException;
import com.rpg.model.RankingJugador;
import com.rpg.model.Victoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla victorias y consultas que la cruzan con jugadores y monstruos. */
public class VictoriaDAO {

    /** La fecha la pone MySQL con NOW() en el momento de insertar. */
    public void registrar(int jugadorId, int monstruoId) throws DatosException {
        String sql = "INSERT INTO victorias (jugador_id, monstruo_id, fecha) VALUES (?, ?, NOW())";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, jugadorId);
            consulta.setInt(2, monstruoId);
            consulta.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo registrar la victoria: " + e.getMessage(), e);
        }
    }

    /**
     * JOIN une cada victoria con su jugador y GROUP BY cuenta las victorias de cada usuario.
     * Solo aparecen jugadores con al menos una victoria.
     *
     * @return usuario y total de victorias, de mayor a menor
     */
    public List<RankingJugador> rankingPorJugador() throws DatosException {
        String sql = "SELECT j.usuario, COUNT(v.id) AS total "
                + "FROM jugadores j "
                + "JOIN victorias v ON v.jugador_id = j.id "
                + "GROUP BY j.id, j.usuario "
                + "ORDER BY total DESC, j.usuario";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet filas = consulta.executeQuery()) {
            List<RankingJugador> ranking = new ArrayList<>();
            while (filas.next()) {
                ranking.add(new RankingJugador(filas.getString("usuario"), filas.getInt("total")));
            }
            return ranking;
        } catch (SQLException e) {
            throw new DatosException("No se pudo calcular el ranking: " + e.getMessage(), e);
        }
    }

    /** @return monstruo y fecha de cada victoria del usuario, de la mas reciente a la mas antigua */
    public List<Victoria> victoriasDeJugador(String usuario) throws DatosException {
        String sql = "SELECT m.nombre, v.fecha "
                + "FROM victorias v "
                + "JOIN jugadores j ON j.id = v.jugador_id "
                + "JOIN monstruos m ON m.id = v.monstruo_id "
                + "WHERE j.usuario = ? "
                + "ORDER BY v.fecha DESC, v.id DESC";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, usuario);
            try (ResultSet filas = consulta.executeQuery()) {
                List<Victoria> victorias = new ArrayList<>();
                while (filas.next()) {
                    victorias.add(new Victoria(filas.getString("nombre"),
                            filas.getObject("fecha", LocalDateTime.class)));
                }
                return victorias;
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer las victorias: " + e.getMessage(), e);
        }
    }

    /** Lo usa MonstruoController para no intentar borrar un monstruo con victorias. */
    public int contarPorMonstruo(int monstruoId) throws DatosException {
        String sql = "SELECT COUNT(*) FROM victorias WHERE monstruo_id = ?";
        try (Connection conexion = ConexionDB.obtenerConexion();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, monstruoId);
            try (ResultSet filas = consulta.executeQuery()) {
                filas.next();
                return filas.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo contar las victorias: " + e.getMessage(), e);
        }
    }
}
