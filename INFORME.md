# Informe - Arena Segura (JDBC + MVC + DAO)

## 1. Flujo del login: View -> Controller -> DAO -> BD

```text
LoginUI                LoginController              JugadorDAO                    MySQL (rpg_arena_db)
lee usuario/clave ---> iniciarSesion()     -------> autenticar(usuario, clave) -> SELECT id, usuario, rol
                       - si esta bloqueado, corta   PreparedStatement:              FROM jugadores
                       - cuenta los fallos          ?1 = usuario                    WHERE usuario = ? AND clave = ?
                       - al 3er fallo: bloqueo      ?2 = sha256(clave)
muestra el mensaje <-- mensaje + sesion <---------- Optional<Jugador>  <--------- ResultSet (se mapea y se cierra)
```

La View solo lee y muestra. Las reglas (3 intentos, solo admin modifica monstruos, no borrar un
monstruo con victorias) estan en los Controller. `java.sql` solo aparece en el paquete `dao`.

## 2. Inyeccion SQL: evidencia (`LoginInseguroDemo`)

Salida de `LoginInseguroDemo` contra MySQL 8.0 (clave usada: `no-la-se`):

```text
=== 1. Login inseguro (concatenacion + Statement) ===
SQL enviado: SELECT usuario FROM jugadores WHERE usuario = 'admin' OR 1=1 -- ' AND clave = '71bc7de0...'
Resultado: ENTRA como 'admin' sin conocer la clave
=== 2. Login seguro (JugadorDAO.autenticar con PreparedStatement) ===
Resultado: RECHAZADO (la entrada se busco como un usuario literal)
Control con admin/admin123: entra correctamente
```

Desde el juego (`LoginUI`) la misma entrada responde "Usuario o clave incorrectos" y cuenta como intento.

**Por que falla con PreparedStatement (el "Escudo").** Con concatenacion, la comilla de
`admin' OR 1=1 -- ` cierra el texto, `OR 1=1` vuelve verdadero el `WHERE` y `-- ` comenta la
comprobacion de la clave: la entrada cambia la estructura de la consulta. `PreparedStatement`
envia primero la consulta con `?` y despues los valores por separado. El valor nunca se
interpreta como SQL: se busca un usuario que se llame literalmente `admin' OR 1=1 -- `, y no existe.

## 3. Statement vs PreparedStatement (pruebas propias)

| Aspecto | Statement (`LoginInseguroDemo`) | PreparedStatement (los DAO) |
|---|---|---|
| Como arma la consulta | Concatena el texto del usuario | Plantilla con `?` y valores aparte |
| Entrada `admin' OR 1=1 -- ` | Entra como `admin` | Rechazada |
| 1000 inserciones, driver por defecto (media de 3) | 1222 ms (1142 / 1207 / 1316) | 1104 ms (1100 / 963 / 1250) |
| 1000 inserciones, `useServerPrepStmts=true` (media de 3) | 1045 ms (1303 / 1099 / 732) | 843 ms (1172 / 875 / 481) |
| Repeticion de la consulta | Un SQL distinto en cada vuelta | Se prepara una vez; solo cambian los valores |

PreparedStatement fue mas rapido en las 6 corridas: un 10 % con el driver por defecto (que arma la
consulta en el cliente) y un 19 % con `useServerPrepStmts=true` (MySQL la compila una sola vez).
Primero se midio con autocommit: cada INSERT esperaba la escritura en disco (unos 10 ms) y los
tiempos variaban de 6 a 15 s sin un ganador claro. Por eso cada lote de 1000 va en una transaccion.

## 4. executeQuery y executeUpdate

- `executeQuery`: para `SELECT`. Devuelve un `ResultSet` con las filas; cada DAO lo recorre, lo
  convierte en objetos (`Jugador`, `Monstruo`, `Victoria`, `RankingJugador`) y lo cierra.
- `executeUpdate`: para `INSERT`, `UPDATE` y `DELETE`. Devuelve un `int` con las filas afectadas;
  por ejemplo, `eliminar` y `cambiarRol` devuelven `true` solo si afectaron una fila.

## 5. Error encontrado y solucion

Al quitar la dependencia `mysql-connector-j` y conectar se obtuvo:
`java.sql.SQLException: No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db`.
El codigo compila igual porque `java.sql` viene con el JDK, pero en ejecucion `DriverManager` no
tiene ningun driver que acepte URLs `jdbc:mysql`. Se resolvio con la dependencia en el `pom.xml`
y ejecutando con el jar del driver en el classpath.

## 6. Reto extra: clave con hash SHA-256

`HashClave` usa `MessageDigest` (SHA-256) y la tabla guarda 64 caracteres hexadecimales; `schema.sql`
usa `SHA2(texto, 256)`, que da el mismo resultado. Si la tabla se filtra, no se ven las claves.
**PreparedStatement sigue siendo necesario**: el hash solo transforma la clave, pero el usuario
sigue siendo texto escrito por el jugador; concatenado, `admin' OR 1=1 -- ` en el usuario evita la
comprobacion de la clave aunque este hasheada. (SHA-256 sin sal es rapido de atacar por fuerza
bruta; en produccion convendria bcrypt o Argon2.)

**Autoevaluacion:** `Statement` sin `Prepared` solo aparece en `LoginInseguroDemo`; `java.sql`
no aparece fuera de `dao` (el demo esta aislado en `dao/demo`).
