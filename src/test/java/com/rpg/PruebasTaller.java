package com.rpg;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.GestorTurno;
import com.rpg.core.Arena;
import com.rpg.core.Bestiario;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.core.RegistroCombate;
import com.rpg.core.TorneoManager;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.GestorArchivos;
import com.rpg.persistencia.Partida;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

// Pruebas de los puntos que pide el README, sin agregar dependencias al proyecto.
public class PruebasTaller {
    public static void main(String[] args) throws Exception {
        probarTemporizador();
        probarEfectos();
        probarTorneo();
        probarAutoGuardado();
        probarCarreraCorregida();
        probarArchivos();
        System.out.println("TODAS LAS PRUEBAS DEL TALLER PASARON");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Prueba interrumpida", e);
        }
    }

    private static void probarTemporizador() throws Exception {
        CountDownLatch aviso = new CountDownLatch(1);
        GestorTurno gestor = new GestorTurno();
        gestor.iniciar(1, aviso::countDown);
        comprobar(aviso.await(2, TimeUnit.SECONDS), "No llego el callback");
        gestor.cancelar();

        CountDownLatch cancelado = new CountDownLatch(1);
        gestor.iniciar(1, cancelado::countDown);
        gestor.cancelar();
        comprobar(!cancelado.await(1200, TimeUnit.MILLISECONDS), "El temporizador cancelado aviso");

        try (PipedInputStream entrada = new PipedInputStream();
             PipedOutputStream escritura = new PipedOutputStream(entrada)) {
            Consola consola = new Consola(new Scanner(entrada));
            gestor.iniciar(1, consola::avisarTiempoAgotado);
            try {
                comprobar(consola.leerEntero("Sin escribir: ", 0, 4) == -1,
                        "La lectura no se libero al terminar el tiempo");
            } finally {
                gestor.cancelar();
                consola.limpiarTiempoAgotado();
            }
            escritura.write("2\n".getBytes());
            escritura.flush();
            comprobar(consola.leerEntero("Siguiente entrada: ", 0, 4) == 2,
                    "El turno siguiente perdio la entrada");
        }
        System.out.println("Temporizador: callback, interrupt y lectura sin teclado correctos.");
    }

    private static void probarEfectos() {
        Guerrero personaje = new Guerrero("Efectos");
        personaje.recibirDanio(40);
        try {
            personaje.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 4, 5));
            personaje.aplicarEfecto(new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 5));
            esperar(1150);
            comprobar(personaje.getVida() == 82, "DoT y HoT no aplicaron un tick cada uno");
            personaje.usarPocion(new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
            comprobar(personaje.getEstado() == EstadoPersonaje.VIVO, "El antidoto no limpio el estado");
            esperar(1050);
            comprobar(personaje.getVida() == 88, "El veneno siguio despues del antidoto");
            personaje.detenerEfectos();
            int vida = personaje.getVida();
            esperar(1050);
            comprobar(personaje.getVida() == vida, "Un efecto siguio despues de terminar el combate");
            comprobar(personaje.getEfectos().isEmpty(), "Quedaron efectos activos");

            personaje.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 4, 2));
            personaje.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 7, 1));
            esperar(1150);
            comprobar(personaje.getVida() == vida - 7, "El hilo viejo borro el efecto reemplazado");
            comprobar(personaje.getEfectos().isEmpty(), "El efecto terminado no se limpio");
            personaje.recibirDanio(1000);
            comprobar(personaje.curar(1000) == 0 && !personaje.estaVivo(), "La curacion revivio al muerto");
        } finally {
            personaje.detenerEfectos();
        }
        System.out.println("Efectos: DoT, HoT, antidoto, reemplazo y fin de combate correctos.");
    }

    private static void probarTorneo() {
        RegistroCombate registro = new RegistroCombate();
        TorneoManager torneo = new TorneoManager(new Arena(registro, new Random(10)), registro);
        comprobar(torneo.jugarTorneo() != null, "El torneo no devolvio campeon");
        String texto = registro.getTexto();
        comprobar(texto.contains("A=NEW, B=NEW"), "No se imprimio NEW");
        comprobar(texto.contains("A=TERMINATED, B=TERMINATED"), "No se espero a las dos semifinales");
        int finalTorneo = texto.indexOf("=== FINAL:");
        comprobar(texto.lastIndexOf("Clasifica a la final:") < finalTorneo, "La final empezo antes del join");
        System.out.println("Torneo: ambas semifinales terminaron antes de la final.");
    }

    private static void probarAutoGuardado() throws Exception {
        MotorJuego motor = new MotorJuego(new Consola(new Scanner("")));
        Thread hilo = new Thread(new AutoGuardadoDaemon(motor, 50), "prueba-auto-guardado");
        hilo.setDaemon(true);
        hilo.start();
        try {
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (motor.getUltimoGuardado().isEmpty() && System.nanoTime() < limite) {
                esperar(20);
            }
            comprobar(motor.getUltimoGuardado().contains("Victorias:"), "El daemon no guardo en memoria");
            comprobar(motor.getUltimoGuardado().contains("Ultimo combate"), "No se guardo el historial");
        } finally {
            hilo.interrupt();
            hilo.join();
            motor.cerrar();
        }
        System.out.println("Auto-guardado: actualizo estadisticas e historial en memoria.");
    }

    private static void probarCarreraCorregida() throws Exception {
        Guerrero personaje = new Guerrero("Carrera");
        personaje.setVidaMaxima(1000000);
        personaje.setVida(1000000);
        CountDownLatch inicio = new CountDownLatch(1);
        Runnable dano = () -> {
            try {
                inicio.await();
                for (int i = 0; i < 100000; i++) {
                    personaje.recibirDanio(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Thread a = new Thread(dano);
        Thread b = new Thread(dano);
        a.start();
        b.start();
        inicio.countDown();
        a.join();
        b.join();
        comprobar(personaje.getVida() == 800000, "Se perdieron actualizaciones de vida");
        System.out.println("Carrera corregida: esperado 800000, obtenido " + personaje.getVida());
    }

    private static void probarArchivos() throws Exception {
        Files.createDirectories(Path.of("target"));
        File carpeta = Files.createTempDirectory(Path.of("target"), "prueba-archivos-").toFile();
        GestorArchivos archivos = new GestorArchivos(carpeta);
        archivos.leerHistorial();
        comprobar(archivos.cargarPartida() == null, "Cargo un archivo inexistente");
        archivos.mostrarInformacion();
        File log = new File(carpeta, "combates.txt");
        comprobar(log.exists() && log.getAbsoluteFile().isAbsolute(), "Fallo File/createNewFile");
        comprobar(archivos.escribirLog("primero"), "Fallo el primer log");
        comprobar(archivos.escribirLog("segundo"), "Fallo el append");
        comprobar(Files.readAllLines(log.toPath()).size() == 2, "Append borro el primer combate");
        archivos.leerHistorial();
        try (FileWriter escritor = new FileWriter(log)) {
            escritor.write("reemplazado");
        }
        comprobar(Files.readString(log.toPath()).equals("reemplazado"), "No se comprobo sobrescritura");
        comprobar(archivos.borrarLog() && !log.exists(), "Fallo File.delete");

        Mago jugador = new Mago("Merlin", 40);
        Gremio gremio = new Gremio("Prueba");
        jugador.unirseAGremio(gremio);
        jugador.recibirDanio(15);
        jugador.getInventario().agregar(new Pocion("Elixir", 0, RaridadItem.RARO,
                new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false));
        PerfilJugador perfil = new PerfilJugador();
        perfil.registrarVictoria();
        perfil.desbloquearLogro("Primera Victoria");
        Bestiario bestiario = new Bestiario();
        bestiario.registrarEnemigoDerrotado("Goblin");
        Enemigo enemigo = bestiario.crearEnemigo("Arana Gigante");
        Partida datos = new Partida(jugador, enemigo, perfil, gremio, bestiario, "historial", 3, true, 1);
        jugador.setRegistroCombate(new RegistroCombate());
        jugador.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 4, 3));
        try {
            comprobar(archivos.guardarPartida(datos), "No se guardo el grafo de objetos");
        } finally {
            jugador.detenerEfectos();
        }
        Partida cargada = archivos.cargarPartida();
        comprobar(cargada != null, "No se cargo la partida");
        Mago recuperado = (Mago) cargada.getJugador();
        comprobar(recuperado.getVida() == 65 && recuperado.getMana() == 40, "Cambio la vida o el mana");
        comprobar(recuperado.getArma() != null && recuperado.getInventario().tamanio() == 1,
                "Falto serializar el arma o el inventario");
        comprobar(recuperado.getGremio() == cargada.getGremio(), "Se rompio el ciclo del gremio");
        comprobar(cargada.getGremio().getMiembros().get(0) == recuperado, "Se duplico el personaje del gremio");
        comprobar(cargada.getPerfil().getVictorias() == 1 && cargada.getBestiario().vecesDerrotado("Goblin") == 1,
                "No se recuperaron las estadisticas");
        comprobar(cargada.getRonda() == 3 && cargada.getFaseTurno() == 1, "No se guardo el punto del combate");
        Field hilos = Personaje.class.getDeclaredField("hilosEfectos");
        hilos.setAccessible(true);
        Field registro = Personaje.class.getDeclaredField("registro");
        registro.setAccessible(true);
        comprobar(hilos.get(recuperado) == null && registro.get(recuperado) == null, "transient no volvio a null");
        recuperado.procesarEfectos();
        try {
            esperar(1150);
            comprobar(recuperado.getVida() == 61, "El efecto cargado no reanudo con un hilo nuevo");
        } finally {
            recuperado.detenerEfectos();
        }
        probarObjetoNoSerializable();

        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread escritor = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                if (!archivos.guardarPartida(datos)) {
                    error.set(new AssertionError("Fallo guardado concurrente"));
                }
            }
        });
        Thread lector = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                if (archivos.cargarPartida() == null) {
                    error.set(new AssertionError("Se leyo un archivo incompleto"));
                }
            }
        });
        escritor.start();
        lector.start();
        escritor.join();
        lector.join();
        comprobar(error.get() == null, "Lectura y escritura no compartieron el candado");
        File archivoPartida = new File(carpeta, "partida.dat");
        try (FileWriter flujo = new FileWriter(archivoPartida)) {
            flujo.write("archivo corrupto");
        }
        comprobar(archivos.cargarPartida() == null, "Se cargo un archivo corrupto");
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(archivoPartida))) {
            salida.writeObject("esto no es una partida");
        }
        comprobar(archivos.cargarPartida() == null, "No se controlo el cast de una partida incorrecta");
        comprobar(archivos.borrarPartida() && !archivos.borrarPartida(), "Fallo Files.deleteIfExists");

        File carpetaInvalida = new File(carpeta, "no-es-carpeta");
        comprobar(carpetaInvalida.createNewFile(), "No se preparo el error de escritura");
        GestorArchivos sinPermisoDeEscritura = new GestorArchivos(carpetaInvalida);
        comprobar(!sinPermisoDeEscritura.guardarPartida(datos), "No se manejo el error al escribir");
        System.out.println("Archivos: append, lectura, borrados, serializacion, transient y errores correctos.");
    }

    private static void probarObjetoNoSerializable() throws Exception {
        Inventario<Object> inventarioMalo = new Inventario<>(1);
        inventarioMalo.agregar(new Object());
        try (ObjectOutputStream salida = new ObjectOutputStream(new ByteArrayOutputStream())) {
            salida.writeObject(inventarioMalo);
            throw new AssertionError("No se provoco NotSerializableException");
        } catch (NotSerializableException e) {
            System.out.println("Error anidado reproducido: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        Inventario<Pocion> inventarioBueno = new Inventario<>(1);
        inventarioBueno.agregar(new Pocion("Vida", 30, RaridadItem.COMUN));
        try (ObjectOutputStream salida = new ObjectOutputStream(new ByteArrayOutputStream())) {
            salida.writeObject(inventarioBueno);
        }
        System.out.println("Corregido: Inventario y su Pocion implementan Serializable.");
    }
}
