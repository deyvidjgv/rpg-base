package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class GestorArchivos {
    private final File carpeta;
    private final File partida;
    private final File log;

    public GestorArchivos() {
        this(new File("partidas"));
    }

    public GestorArchivos(File carpeta) {
        this.carpeta = carpeta;
        partida = new File(carpeta, "partida.dat");
        log = new File(carpeta, "combates.txt");
    }

    private void crearCarpeta() throws IOException {
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getPath());
        }
    }

    public synchronized void mostrarInformacion() {
        try {
            crearCarpeta();
            if (log.createNewFile()) {
                System.out.println("Se creo el archivo de historial.");
            }
            File relativo = new File(carpeta, "partida.dat");
            File absoluto = new File(relativo.getAbsolutePath());
            System.out.println("Ruta relativa: " + relativo.getPath());
            System.out.println("Ruta absoluta: " + absoluto.getPath());
            mostrarDatos(partida);
            mostrarDatos(log);
        } catch (IOException e) {
            System.out.println("No se pudo revisar el archivo: " + e.getMessage());
        }
    }

    private void mostrarDatos(File archivo) {
        System.out.printf("Nombre: %s | Existe: %b | Tamano: %d bytes%n",
                archivo.getName(), archivo.exists(), archivo.length());
    }

    public synchronized boolean escribirLog(String texto) {
        try {
            crearCarpeta();
            // true conserva los combates anteriores; sin true se reemplaza el contenido.
            try (FileWriter archivo = new FileWriter(log, true);
                 BufferedWriter escritor = new BufferedWriter(archivo)) {
                for (String linea : texto.lines().toList()) {
                    escritor.write(linea);
                    escritor.newLine();
                }
                escritor.flush();
            }
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("No se pudo abrir el historial para escribir: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("No se pudo escribir el historial: " + e.getMessage());
        }
        return false;
    }

    public synchronized void leerHistorial() {
        try (FileReader archivo = new FileReader(log);
             BufferedReader lector = new BufferedReader(archivo)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                System.out.println(linea);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Todavia no existe un historial de combates.");
        } catch (IOException e) {
            System.out.println("No se pudo leer el historial: " + e.getMessage());
        }
    }

    public synchronized boolean borrarLog() {
        if (!log.exists()) {
            System.out.println("No existe un historial para borrar.");
            return false;
        }
        boolean borrado = log.delete();
        if (borrado) {
            System.out.println("Historial borrado con File.delete().");
        } else {
            System.out.println("No se pudo borrar el historial.");
        }
        return borrado;
    }

    public synchronized boolean borrarPartida() {
        try {
            boolean borrado = Files.deleteIfExists(partida.toPath());
            if (borrado) {
                System.out.println("Partida guardada borrada.");
            } else {
                System.out.println("No existe una partida guardada.");
            }
            return borrado;
        } catch (IOException e) {
            System.out.println("No se pudo borrar la partida: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean guardarPartida(Partida datos) {
        File temporal = new File(carpeta, "partida.tmp");
        try {
            crearCarpeta();
            try (FileOutputStream archivo = new FileOutputStream(temporal);
                 ObjectOutputStream salida = new ObjectOutputStream(archivo)) {
                salida.writeObject(datos);
            }
            // Solo se reemplaza el .dat cuando el flujo ya termino de escribir y cerrar.
            try {
                Files.move(temporal.toPath(), partida.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal.toPath(), partida.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (NotSerializableException e) {
            System.out.println("Falta implementar Serializable en: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("No se pudo abrir la partida para guardar: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("No se pudo guardar la partida: " + e.getMessage());
        }
        return false;
    }

    public synchronized Partida cargarPartida() {
        try (FileInputStream archivo = new FileInputStream(partida);
             ObjectInputStream entrada = new ObjectInputStream(archivo)) {
            return (Partida) entrada.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("Todavia no existe una partida guardada.");
        } catch (ClassNotFoundException e) {
            System.out.println("Falta una clase necesaria para cargar la partida: " + e.getMessage());
        } catch (IOException | ClassCastException e) {
            System.out.println("No se pudo cargar la partida. El archivo esta danado o no es compatible.");
        }
        return null;
    }
}
