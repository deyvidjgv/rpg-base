package com.rpg;

import com.rpg.controller.LoginController;
import com.rpg.controller.MonstruoController;
import com.rpg.controller.RankingController;
import com.rpg.core.Bestiario;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.dao.JugadorDAO;
import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.entities.Enemigo;
import com.rpg.exceptions.DatosException;
import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;
import com.rpg.model.Rol;
import com.rpg.seguridad.HashClave;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pruebas de las reglas de negocio de los Controller sin MySQL: los DAO se reemplazan por
 * versiones en memoria. Comprueban que las reglas viven en el Controller y no en el DAO.
 * Ejecutar desde una carpeta temporal: el juego crea partidas/ en la carpeta actual.
 */
public class PruebasArena {

    public static void main(String[] args) throws Exception {
        probarHash();
        probarLoginTresIntentos();
        probarRegistro();
        probarPermisosMonstruos();
        probarEliminarConVictorias();
        probarRegistroDeVictoria();
        probarSincronizarBestiario();
        probarIntegracionConCombate();
        System.out.println("TODAS LAS PRUEBAS DE ARENA SEGURA PASARON");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    // ---- DAO en memoria

    static class JugadorDAOFalso extends JugadorDAO {
        final Map<String, String> claves = new LinkedHashMap<>();
        final Map<String, Jugador> jugadores = new LinkedHashMap<>();
        boolean fallarConexion;
        int consultas;

        void agregar(int id, String usuario, String clave, Rol rol) {
            claves.put(usuario, HashClave.sha256(clave));
            jugadores.put(usuario, new Jugador(id, usuario, rol));
        }

        @Override
        public Optional<Jugador> autenticar(String usuario, String clave) throws DatosException {
            consultas++;
            if (fallarConexion) {
                throw new DatosException("Communications link failure");
            }
            boolean ok = HashClave.sha256(clave).equals(claves.get(usuario));
            return ok ? Optional.of(jugadores.get(usuario)) : Optional.empty();
        }

        @Override
        public Jugador registrar(String usuario, String clave, Rol rol) throws DatosException {
            if (claves.containsKey(usuario)) {
                throw new DatosException("El usuario '" + usuario + "' ya existe.");
            }
            agregar(jugadores.size() + 1, usuario, clave, rol);
            return jugadores.get(usuario);
        }
    }

    static class MonstruoDAOFalso extends MonstruoDAO {
        final Map<Integer, Monstruo> filas = new LinkedHashMap<>();
        int eliminaciones;

        @Override
        public Monstruo crear(Monstruo m) {
            Monstruo creado = new Monstruo(filas.size() + 1, m.getNombre(), m.getVida(), m.getAtaque());
            filas.put(creado.getId(), creado);
            return creado;
        }

        @Override
        public Optional<Monstruo> buscarPorId(int id) {
            return Optional.ofNullable(filas.get(id));
        }

        @Override
        public Optional<Monstruo> buscarPorNombre(String nombre) {
            return filas.values().stream().filter(m -> m.getNombre().equalsIgnoreCase(nombre)).findFirst();
        }

        @Override
        public List<Monstruo> listar() {
            return new ArrayList<>(filas.values());
        }

        @Override
        public boolean actualizar(Monstruo m) {
            return filas.replace(m.getId(), m) != null;
        }

        @Override
        public boolean eliminar(int id) {
            eliminaciones++;
            return filas.remove(id) != null;
        }
    }

    static class VictoriaDAOFalso extends VictoriaDAO {
        final List<int[]> victorias = new ArrayList<>();

        @Override
        public void registrar(int jugadorId, int monstruoId) {
            victorias.add(new int[] {jugadorId, monstruoId});
        }

        @Override
        public int contarPorMonstruo(int monstruoId) {
            return (int) victorias.stream().filter(v -> v[1] == monstruoId).count();
        }
    }

    private static LoginController sesionCon(Rol rol) {
        JugadorDAOFalso dao = new JugadorDAOFalso();
        dao.agregar(1, "usuario", "clave123", rol);
        LoginController login = new LoginController(dao);
        login.iniciarSesion("usuario", "clave123");
        return login;
    }

    // ---- pruebas

    private static void probarHash() {
        String hash = HashClave.sha256("admin123");
        comprobar(hash.length() == 64 && hash.equals(hash.toLowerCase()), "Hash: 64 caracteres en minusculas");
        comprobar(hash.equals(HashClave.sha256("admin123")), "Hash: mismo texto, mismo hash");
        comprobar(!hash.equals(HashClave.sha256("admin124")), "Hash: otro texto, otro hash");
        System.out.println("Hash SHA-256 de admin123: " + hash);
    }

    private static void probarLoginTresIntentos() {
        JugadorDAOFalso dao = new JugadorDAOFalso();
        dao.agregar(1, "heroe", "heroe123", Rol.JUGADOR);
        LoginController login = new LoginController(dao);

        System.out.println(login.iniciarSesion("heroe", "mal1"));
        comprobar(login.getIntentosRestantes() == 2 && !login.estaBloqueado(), "Login: primer fallo");
        System.out.println(login.iniciarSesion("admin' OR 1=1 -- ", "x"));
        comprobar(login.getIntentosRestantes() == 1, "Login: segundo fallo");
        System.out.println(login.iniciarSesion("heroe", "mal3"));
        comprobar(login.estaBloqueado() && !login.haySesion(), "Login: el tercer fallo bloquea");

        int consultasAntes = dao.consultas;
        System.out.println(login.iniciarSesion("heroe", "heroe123"));
        comprobar(!login.haySesion(), "Login: bloqueado, ni la clave correcta entra");
        comprobar(dao.consultas == consultasAntes, "Login: bloqueado, ya no consulta al DAO");

        LoginController otro = new LoginController(dao);
        otro.iniciarSesion("heroe", "mal");
        otro.iniciarSesion("heroe", "heroe123");
        comprobar(otro.haySesion() && otro.getIntentosRestantes() == 3, "Login: el acierto reinicia los intentos");

        dao.fallarConexion = true;
        LoginController sinBase = new LoginController(dao);
        for (int i = 0; i < 5; i++) {
            sinBase.iniciarSesion("heroe", "heroe123");
        }
        comprobar(!sinBase.estaBloqueado(), "Login: un fallo de la base no cuenta como intento");
        System.out.println("Login: 3 intentos y bloqueo correctos.");
    }

    private static void probarRegistro() {
        JugadorDAOFalso dao = new JugadorDAOFalso();
        LoginController login = new LoginController(dao);
        comprobar(login.registrar("ab", "clave123", "clave123").contains("entre"), "Registro: usuario corto");
        comprobar(login.registrar("ana'--", "clave123", "clave123").contains("solo puede"), "Registro: caracteres");
        comprobar(login.registrar("ana", "123", "123").contains("al menos"), "Registro: clave corta");
        comprobar(login.registrar("ana", "clave123", "otra123").contains("no coinciden"), "Registro: confirmacion");
        comprobar(login.registrar("ana", "clave123", "clave123").contains("registrado"), "Registro: correcto");
        comprobar(dao.jugadores.get("ana").getRol() == Rol.JUGADOR, "Registro: rol jugador por defecto");
        comprobar(login.registrar("ana", "clave123", "clave123").contains("ya existe"), "Registro: duplicado");
        System.out.println("Registro: validaciones correctas.");
    }

    private static void probarPermisosMonstruos() throws DatosException {
        MonstruoDAOFalso monstruos = new MonstruoDAOFalso();
        monstruos.crear(new Monstruo("Goblin", 50, 8));

        MonstruoController jugador = new MonstruoController(monstruos, new VictoriaDAOFalso(), sesionCon(Rol.JUGADOR));
        comprobar(jugador.comprobarPermiso() != null, "Monstruos: un jugador no tiene permiso");
        comprobar(jugador.crear("Kraken", 90, 12).startsWith("Solo un administrador"), "Monstruos: jugador no crea");
        comprobar(jugador.editar(1, "Goblin", 1, 1).startsWith("Solo un administrador"), "Monstruos: jugador no edita");
        comprobar(jugador.eliminar(1).startsWith("Solo un administrador"), "Monstruos: jugador no elimina");
        comprobar(monstruos.filas.size() == 1 && monstruos.filas.get(1).getVida() == 50, "Monstruos: nada cambio");
        comprobar(jugador.listar().size() == 1, "Monstruos: un jugador si puede consultar");

        MonstruoController sinSesion = new MonstruoController(monstruos, new VictoriaDAOFalso(),
                new LoginController(new JugadorDAOFalso()));
        comprobar(sinSesion.crear("Kraken", 90, 12).startsWith("Solo un administrador"), "Monstruos: sin sesion");

        MonstruoController admin = new MonstruoController(monstruos, new VictoriaDAOFalso(), sesionCon(Rol.ADMIN));
        comprobar(admin.crear("Kraken", 90, 12).startsWith("Monstruo creado"), "Monstruos: admin crea");
        comprobar(admin.crear("kraken", 10, 1).contains("Ya existe"), "Monstruos: nombre repetido");
        comprobar(admin.crear("Hidra", 0, 5).contains("vida"), "Monstruos: vida 0 rechazada");
        comprobar(admin.crear("Hidra", 10, -1).contains("ataque"), "Monstruos: ataque negativo rechazado");
        comprobar(admin.crear("  ", 10, 1).contains("vacio"), "Monstruos: nombre vacio");
        comprobar(admin.editar(2, "Kraken Anciano", 120, 15).startsWith("Monstruo actualizado"), "Monstruos: admin edita");
        comprobar(admin.editar(2, "Goblin", 120, 15).contains("Ya existe otro"), "Monstruos: editar a nombre ajeno");
        comprobar(admin.editar(99, "Nadie", 1, 1).contains("No existe"), "Monstruos: editar id inexistente");
        System.out.println("Monstruos: solo el admin crea, edita y elimina.");
    }

    private static void probarEliminarConVictorias() throws DatosException {
        MonstruoDAOFalso monstruos = new MonstruoDAOFalso();
        VictoriaDAOFalso victorias = new VictoriaDAOFalso();
        Monstruo goblin = monstruos.crear(new Monstruo("Goblin", 50, 8));
        Monstruo ogro = monstruos.crear(new Monstruo("Ogro", 100, 14));
        victorias.registrar(1, goblin.getId());
        victorias.registrar(1, goblin.getId());

        MonstruoController admin = new MonstruoController(monstruos, victorias, sesionCon(Rol.ADMIN));
        String mensaje = admin.eliminar(goblin.getId());
        System.out.println(mensaje);
        comprobar(mensaje.contains("tiene 2 victorias"), "Eliminar: mensaje claro con victorias");
        comprobar(monstruos.eliminaciones == 0, "Eliminar: ni siquiera intenta el DELETE");
        comprobar(monstruos.filas.containsKey(goblin.getId()), "Eliminar: el monstruo sigue");
        comprobar(admin.eliminar(ogro.getId()).startsWith("Monstruo eliminado"), "Eliminar: sin victorias si");
        comprobar(admin.eliminar(77).contains("No existe"), "Eliminar: id inexistente");
        System.out.println("Eliminar: el Controller consulta las victorias antes de borrar.");
    }

    private static void probarRegistroDeVictoria() throws DatosException {
        MonstruoDAOFalso monstruos = new MonstruoDAOFalso();
        VictoriaDAOFalso victorias = new VictoriaDAOFalso();
        monstruos.crear(new Monstruo("Goblin", 50, 8));

        RankingController sinSesion = new RankingController(victorias, monstruos,
                new LoginController(new JugadorDAOFalso()));
        comprobar(sinSesion.registrarVictoria("Goblin").startsWith("Sin sesion"), "Victoria: sin sesion no registra");

        RankingController ranking = new RankingController(victorias, monstruos, sesionCon(Rol.JUGADOR));
        comprobar(ranking.registrarVictoria("Minotauro").contains("no esta en la tabla"), "Victoria: rival fuera de la tabla");
        comprobar(victorias.victorias.isEmpty(), "Victoria: nada registrado aun");
        System.out.println(ranking.registrarVictoria("Goblin"));
        comprobar(victorias.victorias.size() == 1 && victorias.victorias.get(0)[0] == 1
                && victorias.victorias.get(0)[1] == 1, "Victoria: registrada con jugador y monstruo");
        System.out.println("Victorias: se registran solo contra monstruos de la tabla.");
    }

    private static void probarSincronizarBestiario() {
        Bestiario bestiario = new Bestiario();
        bestiario.sincronizar(List.of());
        comprobar(bestiario.nombres().size() == 4, "Bestiario: tabla vacia conserva los rivales");

        bestiario.sincronizar(List.of(new Monstruo(2, "Arana Gigante", 75, 11), new Monstruo(5, "Kraken", 90, 12)));
        comprobar(bestiario.nombres().equals(List.of("Arana Gigante", "Kraken")), "Bestiario: rivales de la tabla");
        Enemigo arana = bestiario.crearEnemigo("Arana Gigante");
        comprobar(arana.getVidaMaxima() == 75 && arana.getAtaque() == 11, "Bestiario: vida y ataque de la tabla");
        comprobar(arana.getHabilidad().equals("Veneno") && arana.getDefensa() == 3, "Bestiario: conserva habilidad");
        Enemigo kraken = bestiario.crearEnemigo("Kraken");
        comprobar(kraken.getVidaMaxima() == 90 && kraken.getHabilidad().equals("Ninguna"), "Bestiario: monstruo nuevo");
        System.out.println("Bestiario: usa los monstruos de la tabla.");
    }

    /** Juega un combate rapido real contra un Goblin y comprueba que el juego avisa de la victoria. */
    private static void probarIntegracionConCombate() {
        StringBuilder entrada = new StringBuilder("1\nThor\nGoblin\n");
        for (int i = 0; i < 60; i++) {
            entrada.append("1\n");
        }
        Consola consola = new Consola(new Scanner(new ByteArrayInputStream(
                entrada.toString().getBytes(StandardCharsets.UTF_8))));
        MotorJuego motor = new MotorJuego(consola);
        AtomicReference<String> vencido = new AtomicReference<>();
        try {
            motor.sincronizarMonstruos(List.of(new Monstruo(1, "Goblin", 30, 5)));
            motor.setAlVencerMonstruo(vencido::set);
            motor.combateRapido();
        } finally {
            motor.cerrar();
        }
        comprobar("Goblin".equals(vencido.get()), "Integracion: el juego avisa la victoria contra el Goblin");
        System.out.println("Integracion: al ganar el combate se avisa con el nombre del monstruo.");
    }
}
