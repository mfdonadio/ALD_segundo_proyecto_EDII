package org.example.configuracion;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfiguracionServidor {

    //Designamos el puerto en donde el servidor puede aceptar conexiones
    private final int puerto;

    //Definimos el tiempo maximo de espera al leer datos del cliente
    private final int tiempoEsperaLecturaMs;

    //Constructor
    public ConfiguracionServidor(int puerto, int tiempoEsperaLecturaMs){
        this.puerto = puerto;
        this.tiempoEsperaLecturaMs = tiempoEsperaLecturaMs;
    }

    //Getters
    public int getPuerto() {
        return puerto;
    }

    public int getTiempoEsperaLecturaMs() {
        return tiempoEsperaLecturaMs;
    }

    public static ConfiguracionServidor cargar(Path ruta) throws IOException {

        // null permite detectar si falta alguna propiedad.
        Integer puerto = null;
        Integer tiempoLectura = null;

        // Lee el archivo en UTF-8 y lo cierra automáticamente.
        try (BufferedReader lector =
                     Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            // Procesa cada línea antes de leer la siguiente.
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                // Ignora líneas vacías y conserva su número real.
                if (linea.isBlank()) {
                    continue;
                }

                // Busca la separación entre el nombre y el valor.
                int separador = linea.indexOf('=');

                if (separador <= 0) {
                    throw new IOException(
                            ruta + ": línea " + numeroLinea
                                    + ": se esperaba nombre=valor.");
                }

                // Elimina los espacios externos de ambas partes.
                String nombre = linea.substring(0, separador).trim();
                String valor = linea.substring(separador + 1).trim();

                if (nombre.isEmpty() || valor.isEmpty()) {
                    throw new IOException(
                            ruta + ": línea " + numeroLinea
                                    + ": el nombre y el valor son obligatorios.");
                }

                // Reconoce y valida cada propiedad.
                switch (nombre) {
                    case "puerto" -> {
                        // Evita que una propiedad sobrescriba a otra repetida.
                        if (puerto != null) {
                            throw new IOException(
                                    ruta + ": línea " + numeroLinea
                                            + ": puerto repetido.");
                        }

                        puerto = leerEntero(
                                valor, nombre, 65535, ruta, numeroLinea);
                    }

                    case "tiempoEsperaLecturaMs" -> {
                        if (tiempoLectura != null) {
                            throw new IOException(
                                    ruta + ": línea " + numeroLinea
                                            + ": tiempoEsperaLecturaMs repetido.");
                        }

                        tiempoLectura = leerEntero(
                                valor, nombre, Integer.MAX_VALUE,
                                ruta, numeroLinea);
                    }

                    // Rechaza propiedades desconocidas o mal escritas.
                    default -> throw new IOException(
                            ruta + ": línea " + numeroLinea
                                    + ": propiedad desconocida: " + nombre);
                }
            }
        }

        // Comprueba que estén todas las propiedades obligatorias.
        if (puerto == null) {
            throw new IOException(ruta + ": falta la propiedad puerto.");
        }

        if (tiempoLectura == null) {
            throw new IOException(
                    ruta + ": falta la propiedad tiempoEsperaLecturaMs.");
        }

        // Devuelve un objeto con los valores ya validados.
        return new ConfiguracionServidor(puerto, tiempoLectura);
    }

    private static int leerEntero(
            String valor,
            String nombre,
            int maximo,
            Path ruta,
            int numeroLinea) throws IOException {

        final int numero;

        // Convierte el texto a un entero.
        try {
            numero = Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IOException(
                    ruta + ": línea " + numeroLinea
                            + ": " + nombre + " debe ser un entero válido.",
                    e);
        }

        // Rechaza cero, negativos y valores superiores al máximo.
        if (numero < 1 || numero > maximo) {
            throw new IOException(
                    ruta + ": línea " + numeroLinea
                            + ": " + nombre
                            + " debe estar entre 1 y " + maximo + ".");
        }

        return numero;
    }
}