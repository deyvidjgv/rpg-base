# Explicacion del taller

## Antes de empezar

La base se compilo con `javac --release 21` usando JDK 25. Se completo un combate
de Thor contra Goblin y un torneo antes de cambiar el codigo.

Los cinco puntos `TALLER CONCURRENCIA` estaban en `MotorJuego.pedirAccion`,
`MotorJuego.guardarProgreso`, `TorneoManager.jugarTorneo`, `Personaje.recibirDanio`
y `Personaje.procesarEfectos`. Los de archivos estaban en el menu, el motor,
el registro y las clases que forman la partida.

Los conceptos de la tabla del README se conservan: condiciones y `switch`, ciclos,
trabajo con `String`, constructores, encapsulamiento, herencia, interfaces,
genericos, relaciones, enums, colecciones y excepciones propias.

## Concurrencia

### 1. Temporizador de turno

`TemporizadorTurnoTask` implementa `Runnable`. Cuenta hacia atras, espera un segundo
en cada iteracion y llama a `TurnoCallback.onTiempoAgotado()` cuando llega a cero.
`GestorTurno` crea el hilo con `start()` y lo cancela con `interrupt()`.
El turno tiene 15 segundos. Si se acaba el tiempo, el jugador pierde la accion
y el enemigo juega sin tener que pulsar Enter.

Interrumpir un hilo no cancela la espera de `Scanner.nextLine()`. Por eso `Consola`
tiene un solo lector de teclado daemon y una cola. El teclado entrega lineas y el
callback entrega el aviso de tiempo agotado. Al terminar la lectura se interrumpe
el temporizador, se espera su salida y se retira cualquier aviso pendiente para
que no afecte al turno siguiente. No se usa una bandera para cancelar el hilo.

### 2. Efectos continuos

Cada DoT o HoT tiene un `EfectoContinuoTask` y un `Thread` propio. El intervalo
es de 1000 ms. La duracion que antes se contaba por turnos ahora se cuenta por ticks.
`procesarEfectos()` inicia los hilos que falten, pero ya no aplica dano por turno.

El antidoto quita los efectos de dano e interrumpe sus hilos. `quitarEfecto()` permite
cancelar un efecto concreto y `detenerEfectos()` cancela todos al terminar el combate.
El `catch` limpia el efecto, repone la interrupcion y sale con `break`. El `finally`
tambien hace la limpieza si el efecto termina normalmente.

Se comprueba la identidad del efecto al limpiarlo. Si se reemplaza un veneno por
otro del mismo nombre, el hilo anterior no puede borrar el nuevo. Los `join()`
de limpieza se hacen fuera del monitor del personaje, porque el `finally` del
efecto necesita entrar a ese mismo monitor.

La prueba aplico dano de 4 y curacion de 6 sobre 80 de vida: quedo en 82. Despues
del antidoto solo continuo la curacion y quedo en 88. Al cancelar todos los efectos
la vida dejo de cambiar. Tambien se comprobo el fin natural y el reemplazo por nombre.

### 3. Torneo en paralelo

Las dos semifinales tienen hilos distintos. Primero se arrancan ambas, despues se
hace `join()` de A y de B, y solamente entonces se consulta el arreglo de ganadores
y empieza la final. Si se interrumpe la espera se cancelan ambas semifinales.

En la ejecucion se observaron estos estados:

```text
Antes de start: A=NEW, B=NEW
Durante la ejecucion: A=RUNNABLE, B=RUNNABLE
Durante las rondas: A=TIMED_WAITING, B=TIMED_WAITING
Despues de join: A=TERMINATED, B=TERMINATED
```

`NEW` es un hilo creado sin arrancar. `RUNNABLE` puede estar ejecutando o esperando
CPU. `TIMED_WAITING` aparece durante el `sleep()` entre rondas. `TERMINATED` significa
que termino. El estado es una observacion de ese momento, asi que puede variar
segun cuando ejecute cada hilo. El registro compartido usa `synchronized` para que
las dos semifinales no danen el `StringBuilder`.

### 4. Auto-guardado daemon

`AutoGuardadoDaemon` tiene `while (true)` y espera 5000 ms entre guardados. Primero
se integro la copia en memoria, que sigue disponible con `getUltimoGuardado()`.
Contiene las estadisticas y el historial. Despues se agrego la escritura a disco
del taller de archivos. Se configura `setDaemon(true)` antes de `start()`.

Al salir se interrumpe y se espera su limpieza. No se espera a que transcurran los
cinco segundos. El teclado tambien es daemon, para que su espera no impida salir.

### 5. Integracion

El combate usa el temporizador y los efectos mientras el daemon guarda. El torneo
usa las semifinales en paralelo y espera ambas antes de la final. Las clases nuevas
de hilos estan en `com.rpg.concurrency` y `TurnoCallback` esta en `com.rpg.core`.

Se completo un combate y un torneo desde el menu. En otra ejecucion se dejo el
teclado quieto: el turno se perdio a los 15,1 segundos y el daemon ya habia creado
la partida durante la espera. Despues de cerrar se cargo el mismo heroe y se
continuo el combate. Tambien se comprobo cancelar el borrado y luego confirmarlo;
al salir, el archivo borrado no se volvio a crear.

### 6. Condicion de carrera

Antes de cambiar `Personaje` se probaron dos hilos, cada uno haciendo 100000 llamadas
a `recibirDanio(1)`, sobre un personaje con 1000000 de vida. Ambos empezaron con un
`CountDownLatch`. El resultado observado en la base fue:

```text
Esperado: 800000. Vida sin proteccion: 850197
```

Los dos hilos podian leer la misma vida y escribir sus resultados, perdiendo una
de las restas. El valor incorrecto puede cambiar entre ejecuciones.

Se uso `synchronized` en `recibirDanio` y `curar` para proteger toda la operacion.
El acceso a vida, estado y efectos usa el mismo monitor. `getEfectos()` devuelve
copias y no una vista del mapa que otro hilo pueda estar cambiando. La misma prueba
con la correccion dio:

```text
Carrera corregida: esperado 800000, obtenido 800000
```

`AtomicInteger.addAndGet(-danio)` solo protege esa suma. No limita el resultado a
`0..vidaMaxima`, no cambia `estado` a `MUERTO` y no coordina la curacion con el estado.
Esas operaciones dependientes deben quedar juntas. Por eso se mantiene
`synchronized` como mecanismo principal.

Cada turno y cada efecto nuevo crean otro `Thread`. No se llama a `run()`
directamente, no se reinicia un hilo terminado y no se usa `Thread.stop()`.

## Manejo de archivos

### 1. File

`GestorArchivos` usa `partidas/` y la crea con `mkdirs()` si no existe. La opcion 6
muestra una ruta relativa y otro `File` construido con `getAbsolutePath()`.
La relativa se interpreta desde la carpeta donde se ejecuto el programa; la
absoluta incluye toda la ubicacion en el disco.

Se usan `getName()`, `exists()` y `length()`. `createNewFile()` crea el historial
vacio si hace falta y su `IOException` se captura. No se crea un `.dat` vacio,
porque todavia no seria una partida serializada.

### 2. Escritura

Al terminar un combate o torneo, `RegistroCombate.guardarEnArchivo()` envia su
texto al gestor. `BufferedWriter` y `FileWriter(file, true)` escriben cada linea
en `partidas/combates.txt` con `newLine()` y `flush()`.

La prueba escribio `primero` y luego `segundo`: ambos quedaron en el archivo.
Despues se abrio el mismo archivo con `new FileWriter(file)` y se escribio
`reemplazado`: los textos anteriores desaparecieron. `true` agrega al final;
el constructor sin ese argumento sobrescribe.

### 3. Lectura

La opcion 5 lee el historial con `BufferedReader` y `FileReader`, usando
`while ((linea = lector.readLine()) != null)`. Si no existe, se captura
`FileNotFoundException` antes de `IOException` y se muestra un mensaje sin tildes.

### 4. Borrado

La opcion 8 borra el historial con `File.delete()` y revisa el booleano. La opcion 7
borra la partida con `Files.deleteIfExists()` y pide confirmacion antes.
`File.delete()` devuelve `false` si no pudo borrar; `deleteIfExists()` devuelve
`false` si no existia y lanza `IOException` si hay un problema de borrado.
El daemon no vuelve a crear una partida que se acaba de borrar desde el menu.

### 5. Serializacion

`Partida` agrupa al jugador, enemigo, perfil, gremio, bestiario, historial, ronda
y fase del turno. Se guarda con `ObjectOutputStream.writeObject()` y se carga con
`ObjectInputStream.readObject()` y el cast a `Partida`.

`Personaje`, sus tres subclases, `Inventario`, `Pocion`, `Arma`, `Gremio`,
`EstadoEfecto`, `PerfilJugador`, `Bestiario` y `Partida` tienen `serialVersionUID`.
Ese numero identifica la version de la clase para comprobar compatibilidad al cargar.
Los enums y las colecciones utilizadas ya permiten serializacion.

Los campos `hilosEfectos` y `registro` de `Personaje` son `transient`: no se guardan
hilos ni la salida de consola. La prueba comprobo que al cargar ambos eran `null`.
Los datos de los efectos si se guardan, y al continuar se crean hilos nuevos.

Para provocar el error anidado se puso un `new Object()` dentro de un
`Inventario<Object>` y se intento escribir el inventario:

```text
Error anidado reproducido: NotSerializableException: java.lang.Object
```

No basta con que `Inventario` sea serializable: tambien debe serlo lo que contiene.
Se repitio con `Inventario<Pocion>` y la escritura termino correctamente, porque
`Pocion` y su `EstadoEfecto` tambien implementan `Serializable`.

Despues de cargar se comprobo por identidad (`==`) que el jugador apunta al mismo
gremio guardado y que el miembro de la lista es ese mismo jugador. No se rompio el
ciclo ni se crearon dos copias distintas del heroe.

La opcion 4 continua el combate pendiente. Se guarda tambien si corresponde jugar
al jugador o al enemigo, para no repetir una accion ni regenerar mana otra vez al
cargar. Si el combate anterior ya termino, se elige otro rival usando el heroe
guardado. Si murio, se conserva el perfil y se vuelve al menu.

### 6. Recursos y excepciones

Los flujos usan `try-with-resources`. Se manejan los archivos inexistentes,
`IOException`, `ClassNotFoundException`, `NotSerializableException` y un tipo de
objeto incorrecto al cargar. Ningun `catch` queda vacio ni solo imprime la traza.

La prueba del archivo corrupto encontro un detalle: si falla el constructor de
`ObjectInputStream`, un `FileInputStream` creado dentro de ese constructor puede
quedar abierto. Por eso ambos se declaran como recursos separados en el `try`.
Asi tambien se cierra el archivo cuando no se pudo construir el flujo de objetos.
Se comprobo que despues se puede borrar el archivo en Windows.

### 7. Integracion con concurrencia

El motor y el daemon comparten una sola instancia de `GestorArchivos`. Sus metodos
de lectura, escritura y borrado son `synchronized`, usando el mismo monitor.
Tambien se protege la preparacion de los datos de la partida en el motor.

Primero se escribe y cierra `partida.tmp`; despues se reemplaza `partida.dat` con
un movimiento atomico cuando el sistema lo permite. El archivo de partida anterior
no se abre para truncarlo mientras se esta serializando. Al salir se espera la
escritura que ya este en curso, y se guarda el estado final antes de cerrar.
La prueba hizo diez guardados y diez lecturas en hilos distintos sobre el mismo
gestor: todas las lecturas recuperaron una partida completa.

## Investigacion obligatoria

### Hilos daemon

Un hilo de usuario mantiene viva la JVM. Un daemon es de apoyo: la JVM puede
terminar cuando ya no quedan hilos de usuario aunque el daemon siga esperando.
`PruebaDaemon` dejo el daemon esperando en un latch y termino `main`. El proceso
cerro y la salida fue:

```text
Fin de main. El daemon sigue vivo: true
```

Con el argumento `tarde`, el hilo arranca y queda esperando antes de intentar
cambiarlo a daemon. Se obtuvo:

```text
setDaemon despues de start: IllegalThreadStateException
```

La regla y la diferencia entre hilos se consultaron en la
[documentacion de Thread de Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html).

### ReentrantLock

`PruebaReentrantLock.PersonajeConLock` reescribe `recibirDanio()` usando `lock()` y
`unlock()` en un `finally`. Se conserva la validacion del dano y la llamada a
`setVida()`. Dos hilos restando 100000 veces dieron 800000.

Preferiria `ReentrantLock` si necesito `tryLock()` con tiempo limite o `lockInterruptibly()`.
Para usarlo en el juego, curacion, estado y efectos deben compartir el mismo lock.
El `unlock()` va en `finally` para liberarlo tambien cuando ocurre una excepcion.
Se consulto la
[documentacion de ReentrantLock](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html).

## Investigacion obligatoria de archivos

### FileWriter y Files.writeString

`FileWriter` con `BufferedWriter` permite escribir por partes y requiere cerrar el flujo; lo usaria para agregar lineas a un log.
`Files.writeString()` escribe el texto completo y gestiona la apertura y el cierre en una llamada.
Usaria `writeString()` para un texto pequeno que ya esta preparado en memoria.
Se consultaron [FileWriter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/FileWriter.html)
y [Files.writeString](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html#writeString(java.nio.file.Path,java.lang.CharSequence,java.nio.file.OpenOption...)).

### Seguridad de Serializable

No se deben deserializar datos de origen desconocido. Un archivo manipulado puede
ejecutar codigo de las clases reconstruidas o consumir demasiada memoria y CPU.
La carga del juego es para sus propias partidas. Como alternativas existen JSON
con validacion o un formato de texto propio. La advertencia esta en la
[documentacion de Serializable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Serializable.html).

## Ejecutar las pruebas

Desde la carpeta del proyecto, en PowerShell:

```powershell
$fuentes = Get-ChildItem src/main/java,src/test/java -Recurse -Filter *.java | ForEach-Object FullName
javac --release 21 -encoding UTF-8 -d target/pruebas $fuentes
java -cp target/pruebas com.rpg.PruebasTaller
java -cp target/pruebas com.rpg.PruebaDaemon
java -cp target/pruebas com.rpg.PruebaDaemon tarde
java -cp target/pruebas com.rpg.PruebaReentrantLock
```

`PruebasTaller` comprueba el temporizador, los efectos, el torneo, el auto-guardado,
la carrera corregida, los archivos y la serializacion. Los archivos de prueba se
crean dentro de `target/`.
