package com.rpg.core;

import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.GestorTurno;
import com.rpg.model.Monstruo;
import com.rpg.persistencia.GestorArchivos;
import com.rpg.persistencia.Partida;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Consumer;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final int SEGUNDOS_TURNO = 15;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private Bestiario bestiario = new Bestiario();
    private PerfilJugador perfil = new PerfilJugador();
    private Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private String ultimoGuardado = "";
    private final Thread autoGuardado;
    private Personaje jugadorActual;
    private Enemigo enemigoActual;
    private int rondaActual = 1;
    private boolean combateEnCurso;
    // 0: inicio de ronda, 1: accion del jugador, 2: accion del enemigo.
    private int faseTurno;
    private final GestorArchivos archivos = new GestorArchivos();
    private boolean guardadoActivo;
    // Se ejecuta con el nombre del rival cada vez que el jugador gana un combate.
    private volatile Consumer<String> alVencerMonstruo;

    public MotorJuego(Consola consola) {
        this.consola = consola;
        autoGuardado = new Thread(new AutoGuardadoDaemon(this, 5000), "auto-guardado");
        // Un daemon no mantiene viva la JVM; un hilo de usuario si lo hace.
        autoGuardado.setDaemon(true);
        autoGuardado.start();
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        registro.limpiar();
        Personaje jugador = crearJugador();
        Enemigo enemigo = elegirEnemigo();
        synchronized (this) {
            jugadorActual = jugador;
            enemigoActual = enemigo;
            rondaActual = 1;
            faseTurno = 0;
            combateEnCurso = true;
            guardadoActivo = true;
        }
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        terminarPartida(jugador, enemigo);
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        if (campeon == null) {
            return;
        }
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        synchronized (this) {
            jugadorActual = campeon;
            enemigoActual = null;
            combateEnCurso = false;
            guardadoActivo = true;
            perfil.desbloquearLogro("Campeon del Torneo");
            guardarProgreso();
        }
        registro.guardarEnArchivo(archivos);
    }

    /** Los rivales del combate rapido pasan a ser los monstruos de la tabla monstruos. */
    public synchronized void sincronizarMonstruos(List<Monstruo> monstruos) {
        bestiario.sincronizar(monstruos);
    }

    public void setAlVencerMonstruo(Consumer<String> accion) {
        alVencerMonstruo = accion;
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        synchronized (this) {
            entregarKitInicial(jugador);
            jugador.unirseAGremio(gremio);
        }
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        jugador.setRegistroCombate(registro);
        enemigo.setRegistroCombate(registro);
        jugador.procesarEfectos();
        enemigo.procesarEfectos();
        try {
            while (jugador.estaVivo() && enemigo.estaVivo()) {
                synchronized (this) {
                    if (faseTurno == 0) {
                        registro.agregar("%n-- Ronda %d --", rondaActual);
                        faseTurno = arena.iniciarTurno(jugador) ? 1 : 2;
                    }
                }
                if (faseTurno == 1 && turnoJugador(jugador, enemigo)) {
                    return true;
                }
                synchronized (this) {
                    arena.turnoAutomatico(enemigo, jugador);
                    rondaActual++;
                    faseTurno = 0;
                }
            }
            return false;
        } catch (NoSuchElementException e) {
            synchronized (this) {
                // Si termina la entrada a mitad de pelea se guarda antes de cancelar los efectos.
                guardarProgreso();
                guardadoActivo = false;
            }
            throw e;
        } finally {
            jugador.detenerEfectos();
            enemigo.detenerEfectos();
        }
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (accion == Accion.HUIR) {
                return true;
            }
            if (accion == Accion.TIEMPO_AGOTADO) {
                registro.agregar("Tiempo agotado. Pierdes el turno.");
                synchronized (this) {
                    faseTurno = 2;
                }
                return false;
            }
            if (!jugador.estaVivo() || !enemigo.estaVivo()) {
                return false;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    synchronized (this) {
                        arena.atacar(jugador, enemigo);
                        faseTurno = 2;
                    }
                    yield true;
                }
                case ESPECIAL -> {
                    synchronized (this) {
                        boolean realizado = arena.intentarEspecial(jugador, enemigo);
                        if (realizado) {
                            faseTurno = 2;
                        }
                        yield realizado;
                    }
                }
                case POCION -> usarPocion(jugador);
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && jugador.estaVivo() && enemigo.estaVivo());
        return false;
    }

    /**
     * La cola de Consola permite que el callback libere la lectura aunque no se escriba nada.
     */
    private Accion pedirAccion() {
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");
        System.out.println("Tienes " + SEGUNDOS_TURNO + " segundos.");
        GestorTurno gestor = new GestorTurno();
        gestor.iniciar(SEGUNDOS_TURNO, consola::avisarTiempoAgotado);
        int opcion;
        try {
            opcion = consola.leerEntero("Accion: ", 0, 4);
        } finally {
            gestor.cancelar();
            consola.limpiarTiempoAgotado();
        }
        return switch (opcion) {
            case -1 -> Accion.TIEMPO_AGOTADO;
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        synchronized (this) {
            registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
            faseTurno = 2;
        }
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d ticks restantes)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void terminarPartida(Personaje jugador, Enemigo enemigo) {
        boolean huyo = combatir(jugador, enemigo);
        boolean victoria;
        synchronized (this) {
            cerrarCombate(jugador, enemigo, huyo);
            victoria = !huyo && jugador.estaVivo();
            combateEnCurso = false;
            guardarProgreso();
        }
        registro.guardarEnArchivo(archivos);
        // Fuera del synchronized: registrar en la base no debe frenar al auto-guardado.
        Consumer<String> accion = alVencerMonstruo;
        if (victoria && accion != null) {
            accion.accept(enemigo.getNombre());
        }
    }

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    public synchronized void guardarProgreso() {
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
        for (Personaje personaje : gremio.getMiembros()) {
            contenido.append(FormatoCombate.resumen(personaje)).append(System.lineSeparator());
        }
        if (jugadorActual != null && jugadorActual.getGremio() == null) {
            contenido.append(FormatoCombate.resumen(jugadorActual)).append(System.lineSeparator());
        }
        if (enemigoActual != null) {
            contenido.append(FormatoCombate.resumen(enemigoActual)).append(System.lineSeparator());
        }
        for (Personaje personaje : torneo.getParticipantes()) {
            contenido.append(FormatoCombate.resumen(personaje)).append(System.lineSeparator());
        }
        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();
        if (guardadoActivo) {
            Partida partida = new Partida(jugadorActual, enemigoActual, perfil, gremio, bestiario,
                    registro.getTexto(), rondaActual, combateEnCurso, faseTurno);
            archivos.guardarPartida(partida);
        }
    }

    public synchronized String getUltimoGuardado() {
        return ultimoGuardado;
    }

    public void cargarProgreso() {
        synchronized (this) {
            Partida partida = archivos.cargarPartida();
            if (partida == null) {
                return;
            }
            jugadorActual = partida.getJugador();
            enemigoActual = partida.getEnemigo();
            perfil = partida.getPerfil();
            gremio = partida.getGremio();
            bestiario = partida.getBestiario();
            registro.recuperar(partida.getHistorial());
            rondaActual = partida.getRonda();
            combateEnCurso = partida.isCombateEnCurso();
            faseTurno = partida.getFaseTurno();
            guardadoActivo = true;
        }
        System.out.println("Partida cargada.");
        mostrarBestiario();
        if (jugadorActual == null || !jugadorActual.estaVivo()) {
            System.out.println("El heroe no puede continuar. Puedes iniciar otro combate desde el menu.");
            return;
        }
        System.out.println(FormatoCombate.resumen(jugadorActual));
        if (!combateEnCurso) {
            Enemigo rival = elegirEnemigo();
            synchronized (this) {
                enemigoActual = rival;
                rondaActual = 1;
                faseTurno = 0;
                combateEnCurso = true;
                registro.limpiar();
            }
            registro.agregar("%n=== %s vs %s ===", jugadorActual.getNombre(), rival.getNombre());
        }
        terminarPartida(jugadorActual, enemigoActual);
    }

    public void mostrarHistorial() {
        archivos.leerHistorial();
    }

    public void mostrarArchivos() {
        archivos.mostrarInformacion();
    }

    public void borrarPartida() {
        String respuesta = consola.leerLinea("Seguro que quieres borrar la partida guardada? (s/n): ");
        if (respuesta.equalsIgnoreCase("s")) {
            synchronized (this) {
                if (archivos.borrarPartida()) {
                    // El daemon no vuelve a crear lo que el jugador acaba de borrar.
                    guardadoActivo = false;
                }
            }
        } else {
            System.out.println("Borrado cancelado.");
        }
    }

    public void borrarHistorial() {
        String respuesta = consola.leerLinea("Seguro que quieres borrar el historial? (s/n): ");
        if (respuesta.equalsIgnoreCase("s")) {
            archivos.borrarLog();
        } else {
            System.out.println("Borrado cancelado.");
        }
    }

    public void cerrar() {
        autoGuardado.interrupt();
        try {
            autoGuardado.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        guardarProgreso();
        for (Personaje personaje : gremio.getMiembros()) {
            personaje.detenerEfectos();
        }
        if (jugadorActual != null) {
            jugadorActual.detenerEfectos();
        }
        if (enemigoActual != null) {
            enemigoActual.detenerEfectos();
        }
    }
}
