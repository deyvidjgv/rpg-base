package com.rpg;

import com.rpg.controller.LoginController;
import com.rpg.controller.MonstruoController;
import com.rpg.dao.JugadorDAO;
import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.exceptions.DatosException;
import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;
import com.rpg.model.RankingJugador;
import com.rpg.model.Rol;
import com.rpg.model.Victoria;

import java.util.List;
import java.util.Optional;

/**
 * Prueba los tres DAO contra MySQL real. Necesita la base creada con schema.sql y config.txt.
 * Ejecutar desde la carpeta del proyecto. Borra todo lo que crea, aunque falle a mitad.
 */
public class PruebasArenaBD {

    private static final String USUARIO = "prueba_bd";
    private static final String CLAVE = "clave_prueba";
    private static final String MONSTRUO = "Prueba Kraken";

    public static void main(String[] args) throws DatosException {
        JugadorDAO jugadores = new JugadorDAO();
        MonstruoDAO monstruos = new MonstruoDAO();
        VictoriaDAO victorias = new VictoriaDAO();
        limpiar(jugadores, monstruos);
        Jugador jugador = null;
        Monstruo kraken = null;
        try {
            // JugadorDAO: registrar, autenticar, cambiarRol
            jugador = jugadores.registrar(USUARIO, CLAVE, Rol.JUGADOR);
            comprobar(jugador.getId() > 0, "registrar devuelve el id generado");
            comprobar(jugadores.autenticar(USUARIO, CLAVE).isPresent(), "autenticar con la clave correcta");
            comprobar(jugadores.autenticar(USUARIO, "otra").isEmpty(), "autenticar con clave incorrecta");
            comprobar(jugadores.autenticar("admin' OR 1=1 -- ", "x").isEmpty(), "la inyeccion no entra");
            try {
                jugadores.registrar(USUARIO, CLAVE, Rol.JUGADOR);
                throw new AssertionError("Se permitio un usuario repetido");
            } catch (DatosException e) {
                System.out.println("Usuario repetido: " + e.getMessage());
            }
            comprobar(jugadores.cambiarRol(jugador.getId(), Rol.ADMIN), "cambiarRol");
            comprobar(jugadores.autenticar(USUARIO, CLAVE).get().esAdmin(), "el rol nuevo se guardo");
            System.out.println("JugadorDAO: registrar, autenticar y cambiarRol correctos.");

            // MonstruoDAO: CRUD, buscarPorNombre y listarPorVidaMinima
            kraken = monstruos.crear(new Monstruo(MONSTRUO, 9000, 12));
            comprobar(monstruos.buscarPorId(kraken.getId()).isPresent(), "buscarPorId");
            comprobar(monstruos.buscarPorNombre("prueba kraken").isPresent(), "buscarPorNombre sin mayusculas");
            int idKraken = kraken.getId();
            comprobar(monstruos.listar().stream().anyMatch(m -> m.getId() == idKraken), "listar");
            List<Monstruo> fuertes = monstruos.listarPorVidaMinima(9000);
            comprobar(fuertes.size() == 1 && fuertes.get(0).getNombre().equals(MONSTRUO), "listarPorVidaMinima");
            comprobar(monstruos.actualizar(new Monstruo(kraken.getId(), MONSTRUO, 9500, 15)), "actualizar");
            comprobar(monstruos.buscarPorId(kraken.getId()).get().getVida() == 9500, "el cambio se guardo");
            System.out.println("MonstruoDAO: CRUD, buscarPorNombre y listarPorVidaMinima correctos.");

            // VictoriaDAO: registrar, victoriasDeJugador, rankingPorJugador
            victorias.registrar(jugador.getId(), kraken.getId());
            victorias.registrar(jugador.getId(), kraken.getId());
            List<Victoria> suyas = victorias.victoriasDeJugador(USUARIO);
            comprobar(suyas.size() == 2 && suyas.get(0).getMonstruo().equals(MONSTRUO)
                    && suyas.get(0).getFecha() != null, "victoriasDeJugador con monstruo y fecha");
            Optional<RankingJugador> fila = victorias.rankingPorJugador().stream()
                    .filter(r -> r.getUsuario().equals(USUARIO)).findFirst();
            comprobar(fila.isPresent() && fila.get().getTotalVictorias() == 2, "rankingPorJugador cuenta 2");
            comprobar(victorias.contarPorMonstruo(kraken.getId()) == 2, "contarPorMonstruo");
            System.out.println("Victoria: " + suyas.get(0));
            System.out.println("Ranking: " + victorias.rankingPorJugador());
            System.out.println("VictoriaDAO: registrar, ranking con JOIN/GROUP BY y victoriasDeJugador correctos.");

            // Regla del Controller: no borrar un monstruo con victorias (sin llegar a la FOREIGN KEY)
            LoginController login = new LoginController(jugadores);
            login.iniciarSesion(USUARIO, CLAVE);
            MonstruoController controller = new MonstruoController(monstruos, victorias, login);
            String mensaje = controller.eliminar(kraken.getId());
            System.out.println("Eliminar con victorias: " + mensaje);
            comprobar(mensaje.contains("2 victorias"), "el Controller avisa de las victorias");
            comprobar(monstruos.buscarPorId(kraken.getId()).isPresent(), "el monstruo sigue en la tabla");

            // JugadorDAO.eliminar: las victorias se borran en cascada y el monstruo ya se puede borrar
            comprobar(jugadores.eliminar(jugador.getId()), "eliminar jugador");
            jugador = null;
            comprobar(victorias.contarPorMonstruo(kraken.getId()) == 0, "ON DELETE CASCADE de victorias");
            System.out.println("Eliminar sin victorias: " + controller.eliminar(kraken.getId()));
            comprobar(monstruos.buscarPorId(kraken.getId()).isEmpty(), "el monstruo se elimino");
            kraken = null;
            System.out.println("TODAS LAS PRUEBAS CONTRA MYSQL PASARON");
        } finally {
            limpiar(jugadores, monstruos);
        }
    }

    private static void limpiar(JugadorDAO jugadores, MonstruoDAO monstruos) throws DatosException {
        Optional<Jugador> jugador = jugadores.autenticar(USUARIO, CLAVE);
        if (jugador.isPresent()) {
            jugadores.eliminar(jugador.get().getId());
        }
        Optional<Monstruo> monstruo = monstruos.buscarPorNombre(MONSTRUO);
        if (monstruo.isPresent()) {
            monstruos.eliminar(monstruo.get().getId());
        }
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
