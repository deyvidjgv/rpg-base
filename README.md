# Simulador de Batalla RPG - Proyecto base

Los dos talleres estan desarrollados. Se conserva el enunciado de la base y su orden;
el checklist esta marcado segun las pruebas realizadas.
Las explicaciones, investigaciones y comandos de prueba estan en [explicacion_taller.md](explicacion_taller.md).

Para compilar y ejecutar en PowerShell (JDK 21 o superior):

```powershell
$fuentes = Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object FullName
javac --release 21 -encoding UTF-8 -d out $fuentes
java -cp out com.rpg.Main
```

### Taller JDBC: Arena Segura

El informe esta en [INFORME.md](INFORME.md). Para jugar con login, bestiario en la base y ranking:

1. Crear la base (pide la clave de MySQL):
   `& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "source schema.sql"`
2. Copiar `config.example.txt` como `config.txt` y poner la clave. `config.txt` no se sube al repositorio.
3. Descargar el driver y ejecutar:

```powershell
mvn -q dependency:copy-dependencies -DoutputDirectory=target/lib
$fuentes = Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object FullName
javac --release 21 -encoding UTF-8 -d out $fuentes
java -cp "out;target/lib/*" com.rpg.Main
java -cp "out;target/lib/*" com.rpg.dao.demo.LoginInseguroDemo
```

Usuarios de prueba: `admin` / `admin123` (admin) y `heroe` / `heroe123` (jugador).
Sin `config.txt` o sin MySQL el juego arranca igual, sin login, ranking ni registro de victorias.

---

## Enunciado original

Este es el punto de partida del taller **Concurrencia e Hilos Aplicados al Simulador de Batalla RPG**,
continuacion del proyecto integrador "Simulador de Batalla RPG / Arena de Heroes" de la **Unidad 4.1**.
Es el simulador que ya construiste en los talleres 1 a 8, reunido en un solo proyecto y funcionando
de forma **100 % secuencial**: todo ocurre en un unico hilo, una cosa despues de otra.

## Objetivo

1. Leer el codigo y comprobar que reconoces cada concepto de los talleres anteriores.
2. Ejecutar el juego y ver donde la ejecucion secuencial se queda corta.
3. Identificar los puntos de extension (buscalos con los comentarios `TALLER CONCURRENCIA` y `TALLER ARCHIVOS`).

## Como compilar y ejecutar

Con Maven (JDK 21):

```
mvn -q compile
java -cp target/classes com.rpg.Main
```

Sin Maven, solo con `javac` (desde la carpeta del proyecto):

```
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
java -cp out com.rpg.Main
```

En IntelliJ IDEA basta con abrir la carpeta como proyecto Maven y ejecutar `Main`.
El juego lee de `System.in`, asi que tambien puedes probarlo sin teclado:

```
printf '1\n1\nThor\n1\n1\n1\n1\n0\n' | java -cp out com.rpg.Main
```

Hoy el juego no escribe ningun archivo: el progreso se guarda **en memoria** (`MotorJuego.getUltimoGuardado()`)
y es el *estado de la partida*, que se actualiza cuando termina una pelea. El taller de **Manejo de Archivos**
lleva ese estado a disco (buscalo con el comentario `TALLER ARCHIVOS`).

## Estructura de paquetes

```
com.rpg
  Main                      menu principal por consola
  core/                     flujo del juego
    MotorJuego              crear heroe, bucle de combate, resultado, guardarProgreso()
    Arena                   reglas: ataque, especial, efectos por turno, duelo automatico
    TorneoManager           4 gladiadores, 2 semifinales y final
    Bestiario               Map de monstruos + contador de derrotas
    Consola                 lectura con validacion y reintento
    RegistroCombate         imprime y acumula el registro de combate
    FormatoCombate          barra de vida [#####-----] y resumen
    Accion                  enum con las acciones del jugador
  entities/                 modelo del dominio
    Personaje (abstracta), ICombatiente, Guerrero, Mago, Enemigo
    EstadoEfecto, Arma, Gremio, Escuadron, PerfilJugador
    enums/                  TipoPersonaje, EstadoPersonaje, RaridadItem, TipoEfecto
  items/                    Inventario<T>, Pocion
  exceptions/               JuegoException, SinManaException, CapacidadExcedidaException,
                            VidaInsuficienteException
  concurrency/              VACIO a proposito (solo package-info.java)
  persistencia/             VACIO a proposito (solo package-info.java), para el taller de archivos
```

## Lo que ya debes tener (checklist)

| Taller | Concepto | Donde se ve |
|--------|----------|-------------|
| T1 | `switch` moderno (`->`), ternario, condiciones `&&` / `\|\|` | `MotorJuego.pedirAccion`, `Arena.atacar` (ternario del critico), `Arena.turnoAutomatico` |
| T1 | `equals` vs `==` en String; division decimal y casteos | `Bestiario.crearEnemigo` (`equalsIgnoreCase`), `Personaje.defender` y `calcularDanio` |
| T2 | Bucles `while` / `do-while` / `for`, `break` / `continue` | `MotorJuego.combatir`, `turnoJugador` (do-while), `Consola.leerEntero` (reintento con `continue`), `Personaje.procesarEfectos` |
| T3 | `String`: `trim`, `equalsIgnoreCase`, `format`, `StringBuilder` | `Consola.leerNombre`, `FormatoCombate.barraVida`, `RegistroCombate`, `MotorJuego.guardarProgreso` |
| T4 | Clases, objetos, constructores sobrecargados y `this(...)` | `Guerrero`, `Mago`, `Enemigo`, `Pocion`, `Personaje` |
| T5 | Encapsulamiento: atributos privados, getters/setters validados | `Personaje.setVida` (nunca < 0 ni > vida maxima), `setNombre`, `Arma` |
| T6 | Clase abstracta, interfaz, polimorfismo, genericos | `Personaje`, `ICombatiente`, `realizarAtaqueEspecial` en `Guerrero` / `Mago` / `Enemigo`, `Inventario<T>` |
| T7 | Composicion, agregacion, asociacion | `Personaje` crea su `Inventario` (composicion); `Gremio` y `Escuadron` (agregacion); `Personaje.equiparArma` y `unirseAGremio` (asociacion) |
| T8 | Enums, `Set`, `Map`, excepciones propias | `enums/`, `Personaje.habilidades` (`HashSet`), `PerfilJugador.logros` (`TreeSet`), `Bestiario` (`Map`), `exceptions/` capturadas en `Arena.intentarEspecial`, `MotorJuego.repartirBotin` y `entregarKitInicial` |

## Que NO tiene aun (lo haras en el taller de concurrencia)

Estos cuatro modulos van en el paquete `com.rpg.concurrency` (y en los puntos marcados del motor):

1. **Temporizador de turno**: hoy el jugador puede tardar lo que quiera en elegir su accion
   (`MotorJuego.pedirAccion` bloquea esperando el teclado).
2. **Efectos DoT / HoT con interrupcion**: hoy `Personaje.procesarEfectos` aplica los efectos turno a turno;
   no hay tareas independientes ni forma de cancelarlas a mitad de camino.
3. **Semifinales en paralelo con `join`**: `TorneoManager.jugarTorneo` simula una semifinal despues de otra.
4. **Auto-guardado daemon**: `MotorJuego.guardarProgreso` solo se invoca manualmente al terminar una pelea
   (y guarda en memoria; llevarlo a disco es parte del taller de archivos).

## Que NO tiene aun (taller de archivos)

Estos modulos van en el paquete `com.rpg.persistencia` (y en los puntos marcados con `TALLER ARCHIVOS`):

1. **Log de combate en texto**: `RegistroCombate` solo acumula y muestra el texto; no se guarda ni se lee
   desde un archivo con `BufferedWriter` / `BufferedReader`.
2. **Inspeccion y borrado de archivos**: no hay uso de `File` (existe, tamano, ruta) ni borrado con
   `Files.deleteIfExists`.
3. **Guardar y cargar partida**: `MotorJuego.guardarProgreso` deja un `String` en memoria; falta guardar el
   estado con `Serializable` + `ObjectOutputStream` / `ObjectInputStream`, y una forma de cargarlo desde `Main`.
4. **Control de la serializacion**: `transient` para lo que no debe guardarse y `serialVersionUID` en cada clase.

### Advertencia: condicion de carrera en la vida

`Personaje.recibirDanio` y `Personaje.curar` modifican la vida con operaciones de leer, calcular y escribir
que **no estan protegidas**. Hoy funcionan porque solo hay un hilo. Cuando varios hilos toquen la vida del
mismo personaje al mismo tiempo (por ejemplo un veneno y un ataque), se podran perder actualizaciones.
Parte del taller consiste en reproducir el problema y resolverlo.

El problema no es solo la vida: tambien comparten estado mutable el campo `estado` (`setVida` lo pasa a
`MUERTO`, y `curar` lo consulta con `estaVivo()`) y el mapa `efectos`, que `aplicarEfecto` y `quitarEfecto`
modifican mientras `procesarEfectos` lo recorre (puede lanzar `ConcurrentModificationException`). Cuando una
operacion toca varias variables dependientes, la unidad 4.2 recomienda `synchronized`.

## Checklist del taller: lo que debes realizar

Marca cada punto cuando lo tengas funcionando. El orden sigue las secciones de la guia del taller.
Las clases nuevas van en `com.rpg.concurrency` (y `TurnoCallback` en `com.rpg.core`).
Mas abajo hay un checklist aparte para el taller de archivos.

### Antes de empezar

- [x] El proyecto compila y ejecutas `Main` sin errores.
- [x] Jugaste un combate rapido y un torneo completo, y localizaste los 5 comentarios `TALLER CONCURRENCIA`.
- [x] Repasaste la tabla "Lo que ya debes tener": si algun concepto no lo reconoces, repasalo antes de seguir.

### Seccion 1: Temporizador de turno (`Thread`, `Runnable`, `sleep`)

- [x] Crear la interfaz `TurnoCallback` con el metodo `void onTiempoAgotado()`.
- [x] Crear `TemporizadorTurnoTask` que implemente `Runnable` y reciba por constructor la duracion en segundos y el `TurnoCallback`.
- [x] En `run()`, hacer una cuenta regresiva con un `for` o `while` decreciente, un segundo por iteracion.
- [x] Usar `Thread.sleep(1000)` dentro de un `try-catch` que capture `InterruptedException`.
- [x] Al llegar a 0, llamar a `callback.onTiempoAgotado()`.
- [x] Crear `GestorTurno`, que cree el `Thread` con el `Runnable` (o con una lambda) y llame a `.start()`.
- [x] Integrarlo en `MotorJuego.pedirAccion`: si el tiempo se agota antes de que el jugador elija, el turno se pierde.
- [x] Si el jugador responde a tiempo, cancelar el temporizador con `.interrupt()` (sin banderas booleanas manuales).

### Seccion 2: Efectos continuos DoT / HoT (`interrupt`, `InterruptedException`)

- [x] Crear `EfectoContinuoTask` (`Runnable`) con nombre del efecto e intervalo en milisegundos.
- [x] En `run()`, usar `while (!Thread.currentThread().isInterrupted())` y aplicar el dano o la curacion sobre el `Personaje`.
- [x] En el `catch (InterruptedException e)`: limpiar el estado del personaje, llamar a `Thread.currentThread().interrupt()` y salir del bucle con `break`.
- [x] Cada efecto corre en su propio hilo; no se usa `Thread.stop()` ni banderas booleanas.
- [x] Reemplazar la logica por turnos de `Personaje.procesarEfectos` por hilos que se cancelan con `.interrupt()`.
- [x] Al usar un antidoto, al recibir una purificacion o al terminar el combate, el hilo del efecto se interrumpe y termina limpio.

### Seccion 3: Torneo en paralelo (`join`)

- [x] En `TorneoManager.jugarTorneo`, crear dos `Thread`, uno por semifinal, cada uno con su `Runnable`.
- [x] Llamar a `.start()` en ambos para que corran al mismo tiempo.
- [x] En el hilo principal, llamar a `.join()` sobre la semifinal A y luego sobre la B.
- [x] Proteger los `join()` con `try-catch` de `InterruptedException`.
- [x] Consultar los ganadores **solo despues** de que ambos `join()` hayan retornado, y entonces lanzar la final.
- [x] Imprimir `Thread.getState()` de cada semifinal antes de `start()`, durante la ejecucion y despues del `join()`, y relacionar lo que ves con el ciclo de vida del hilo (`NEW`, `RUNNABLE`, `TIMED_WAITING`, `TERMINATED`).

### Seccion 4: Auto-guardado en segundo plano (hilo daemon)

- [x] Crear `AutoGuardadoDaemon` con un bucle `while (true)` y `Thread.sleep(intervalo)` entre guardados.
- [x] Hacer que el guardado registre periodicamente las estadisticas de los personajes y el historial de la partida **en memoria** (por ejemplo, un `String` o una lista con la ultima copia). No uses archivos todavia.
- [x] Llamar a `setDaemon(true)` **antes** de `.start()` (si no, `IllegalThreadStateException`).
- [x] Comprobar que la aplicacion cierra de inmediato al salir, sin esperar al hilo de guardado.

### Seccion 5: Reto integrador

- [x] Los cuatro modulos funcionan juntos: temporizador de turno, efectos DoT/HoT, torneo con `join` y auto-guardado daemon.
- [x] Cumples la estructura esperada: `com.rpg.core` (`MotorJuego`, `TorneoManager`, `TurnoCallback`), `com.rpg.entities` (`Personaje`, `EstadoEfecto`), `com.rpg.concurrency` (`TemporizadorTurnoTask`, `EfectoContinuoTask`, `AutoGuardadoDaemon`) y `Main`.
- [x] El combate y el torneo se juegan completos, sin bloqueos ni excepciones sin controlar.

### Seccion 6: Depuracion y condiciones de carrera

- [x] Reproduciste la condicion de carrera: dos efectos DoT (o un efecto y un ataque) sobre el mismo `Personaje` pierden actualizaciones de vida.
- [x] La corrigiste en `Personaje.recibirDanio` y `curar` con `synchronized` (mecanismo principal, visto en la unidad 4.2), y puedes explicar por que lo elegiste.
- [x] Protegiste tambien el acceso a `estado` y `efectos` (ver la advertencia de arriba).
- [x] Puedes explicar por que `AtomicInteger.addAndGet(-danio)` no basta aqui: `setVida` acota la vida a `0..vidaMaxima` y cambia `estado`. (Reto extra: resolverlo con `updateAndGet` o un bucle con `compareAndSet`.)
- [x] Ningun `catch (InterruptedException)` esta vacio ni solo imprime la traza.
- [x] Nunca llamas a `.start()` dos veces sobre el mismo `Thread`: para cada turno o combate se crea un `Thread` nuevo.
- [x] Nunca llamas a `run()` directamente: usas siempre `start()`, que es lo que crea el hilo nuevo.
- [x] Todos los `Thread.sleep()` estan dentro de `try-catch` que gestiona `InterruptedException`.
- [x] Todo hilo de auto-guardado o monitoreo usa `setDaemon(true)` antes de `.start()` (ver "Investigacion obligatoria").
- [x] El `join()` del torneo espera a ambas semifinales antes de leer la memoria compartida de los ganadores.

## Checklist del taller de archivos: lo que debes realizar

Las clases nuevas van en `com.rpg.persistencia`. Los puntos de extension estan marcados con `TALLER ARCHIVOS`
en `MotorJuego`, `RegistroCombate`, `Main` y las clases del dominio.

### Antes de empezar

- [x] El proyecto compila y ejecutas `Main` sin errores.
- [x] Localizaste los comentarios `TALLER ARCHIVOS` y entiendes que el guardado actual es solo en memoria.

### Seccion 1: Clase `File`

- [x] Crear un `File` con una ruta relativa y otro con una ruta absoluta (`getAbsolutePath()`), y explicar la diferencia.
- [x] Usar `exists()`, `length()` y `getName()` para mostrar informacion de un archivo de partida o de log.
- [x] Usar `createNewFile()` y manejar su `IOException`.
- [x] Crear la carpeta `partidas/` si no existe (`mkdirs()`), sin fallar si ya existe.

### Seccion 2: Escritura (`BufferedWriter` + `FileWriter`)

- [x] Volcar el log de `RegistroCombate` a un `.txt` con `BufferedWriter` + `FileWriter`.
- [x] Escribir con `newLine()` en vez de concatenar saltos de linea a mano.
- [x] Usar `FileWriter(file, true)` para **agregar** (append) sin borrar los combates anteriores, y comprobar la diferencia con el modo por defecto, que sobrescribe.
- [x] Hacer `flush()` / cerrar el escritor para que el contenido llegue al disco.

### Seccion 3: Lectura (`BufferedReader` + `FileReader`)

- [x] Leer el log con `BufferedReader` + `FileReader` usando `while ((linea = lector.readLine()) != null)`.
- [x] Mostrar por consola el historial leido (por ejemplo, una opcion nueva en el menu de `Main`).
- [x] Capturar `FileNotFoundException` cuando el archivo aun no existe y mostrar un mensaje claro (sin tildes).

### Seccion 4: Borrado

- [x] Borrar un archivo con `File.delete()` y revisar el `boolean` que devuelve.
- [x] Borrar con `Files.deleteIfExists(Path)` y explicar la diferencia con `File.delete()`.
- [x] Ofrecer en el menu una opcion para borrar la partida guardada, confirmando antes con el jugador.

### Seccion 5: Serializacion (`Serializable`, `ObjectOutputStream`, `ObjectInputStream`)

- [x] Hacer `Serializable` las clases del dominio que forman la partida: `Personaje` (y `Guerrero`, `Mago`, `Enemigo`), `Inventario`, `Pocion`, `Arma`, `Gremio`, `EstadoEfecto`, `PerfilJugador`.
- [x] Declarar `serialVersionUID` en cada clase serializable y explicar para que sirve.
- [x] Marcar con `transient` al menos un campo que no deba guardarse y comprobar que vuelve con su valor por defecto al cargar.
- [x] Guardar la partida con `ObjectOutputStream.writeObject(...)` en un `.dat`.
- [x] Cargar la partida con `ObjectInputStream.readObject()`, con el cast al tipo correcto.
- [x] Provocar y resolver una `NotSerializableException` por un objeto anidado que no es `Serializable`.
- [x] Comprobar que despues de cargar, el `Gremio` y sus `Personaje` siguen relacionados (el ciclo `Personaje` - `Gremio` se serializa bien).
- [x] Agregar una opcion "Cargar partida" y comprobar que el juego continua con el estado guardado.

### Seccion 6: `try-with-resources` y excepciones

- [x] Todos los flujos (`BufferedWriter`, `BufferedReader`, `ObjectOutputStream`, `ObjectInputStream`) se abren con `try-with-resources` (`AutoCloseable`); ningun `close()` manual.
- [x] Manejas `IOException`, `FileNotFoundException` (la mas especifica va primero), `ClassNotFoundException` y `NotSerializableException` con mensajes utiles.
- [x] Ningun `catch` esta vacio ni solo imprime la traza.
- [x] El juego no se cae si el archivo no existe, esta corrupto o no se puede escribir.

### Seccion 7: Integracion con concurrencia (si ya hiciste el taller anterior)

- [x] `AutoGuardadoDaemon` escribe a disco (ya no solo en memoria) cada cierto intervalo.
- [x] Protegiste la escritura y la lectura del mismo archivo con `synchronized` (sobre un mismo objeto candado o metodos de una clase compartida), para que el daemon y el hilo principal no lo accedan a la vez.
- [x] Comprobaste que, al cerrar el juego, el hilo daemon no deja un archivo a medio escribir.

## Investigacion obligatoria

Dos temas de la guia no se explican en las presentaciones de las unidades 4.1 y 4.2. Debes investigarlos
por tu cuenta y dejar evidencia en el proyecto:

- [x] **Hilos daemon.** Demuestra con una prueba que la JVM cierra aunque el hilo daemon siga vivo, y que
  `setDaemon(true)` despues de `start()` lanza `IllegalThreadStateException`. Anota en un comentario la
  diferencia entre hilo de usuario y hilo daemon.
- [x] **`ReentrantLock`.** Reescribe `Personaje.recibirDanio` con `lock()` / `unlock()` dentro de `try-finally`
  y explica en 2 o 3 lineas cuando lo preferirias a `synchronized`. Si el `unlock()` no esta en el `finally`,
  el lock puede quedar tomado para siempre.

## Investigacion obligatoria (taller de archivos)

- [x] **`FileWriter` vs `Files.writeString`.** Explica en 2 o 3 lineas la diferencia (flujo con buffer que
  abres y cierras tu, frente a una sola llamada de NIO que escribe todo el texto) y cuando usarias cada uno.
- [x] **Seguridad de `Serializable`.** Explica por que no se debe deserializar (`readObject`) datos que no
  son de confianza: un archivo manipulado puede ejecutar codigo o consumir recursos al reconstruir objetos.
  Anota que alternativas existen (por ejemplo JSON o un formato de texto propio).

## Adaptaciones respecto a la guia

Este proyecto sigue la guia del taller, con estas diferencias a proposito:

- **Auto-guardado en memoria.** La guia habla de guardar "en el almacenamiento". En el taller de concurrencia
  el guardado es en memoria (un `String` o una lista con la ultima copia): ese es el estado de la partida.
  El taller de archivos lo lleva a disco (ver la seccion 7 del checklist de archivos).
- **Reemplazo de `Personaje.procesarEfectos`.** La guia pide que cada efecto DoT/HoT corra en su propio hilo;
  este taller ademas te pide sustituir la logica por turnos actual por esos hilos.
- **Mecanismo de sincronizacion.** La guia sugiere `synchronized`, `AtomicInteger` o `ReentrantLock`. Aqui el
  mecanismo principal es `synchronized` (unidad 4.2); `ReentrantLock` y los hilos daemon pasan a la seccion
  "Investigacion obligatoria" porque las presentaciones no los cubren.
- **Nombre del temporizador.** La guia usa `TemporizadorTurno` en la tabla y los pasos de la seccion 1, pero
  `TemporizadorTurnoTask` en la estructura de la seccion 5. Usa `TemporizadorTurnoTask`.
- **Nombre del paquete.** El esqueleto de la guia declara `package com.rpg.concurrencia;`. En este proyecto el
  paquete es `com.rpg.concurrency`: ajusta la linea `package` si copias el esqueleto.
- **Detalles de `EfectoContinuoTask` que la guia sugiere.** Usa un `finally` para liberar recursos y consulta
  `Thread.currentThread().isInterrupted()` donde no haya un `sleep()` que lance `InterruptedException`.

## Notas

- Los mensajes de consola no llevan tildes a proposito, para que se vean bien en cualquier terminal.
- Para repetir el juego con resultados distintos no hay semilla fija: el azar (criticos, habilidades de los
  enemigos) cambia en cada ejecucion.
