package org.example.configuracion;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfiguracionCliente {

    //Variable para guardar el host
    private final String host;
    //Variable para guardar el puerto
    private final int puerto;
    //Para tener el tiempo de espera de conexion del cliente en ms
    private final int tiempoEsperaConexionMs;
    //Para tener el tiempo de espera de lecutra del servidor
    private final int tiempoEsperaLecturaMs;

    //Constructor
    private ConfiguracionCliente(String host, int puerto, int tiempoEsperaConexionMS,
                                int tiempoEsperaLecturaMS) {
        this.host = host;
        this.puerto = puerto;
        this.tiempoEsperaConexionMs = tiempoEsperaConexionMS;
        this.tiempoEsperaLecturaMs = tiempoEsperaLecturaMS;
    }

    //Getters
    public String getHost() {
        return host;
    }

    public int getPuerto() {
        return puerto;
    }

    public int getTiempoEsperaLecturaMs() {
        return tiempoEsperaLecturaMs;
    }

    public int getTiempoEsperaConexionMs() {
        return tiempoEsperaConexionMs;
    }

    //Carga de la configuracion del cliente
    public static ConfiguracionCliente cargar(Path ruta) throws IOException {

        String host = null;
        Integer puerto = null;
        Integer tiempoConexion = null;
        Integer tiempoLectura = null;

        // Abre el archivo en UTF-8 y lo cierra automáticamente al terminar.
        try (BufferedReader lector =
                     Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            // Lee y procesa cada línea hasta llegar al final del archivo.
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                // Ignora las líneas vacías, conservando su número real.
                if (linea.isBlank()) {
                    continue;
                }

                // Busca el signo que separa el nombre y el valor.
                int separador = linea.indexOf('=');

                if (separador <= 0) {
                    throw new IOException(
                            ruta + ": línea " + numeroLinea
                                    + ": se esperaba un formato de 'nombre=valor'.");
                }

                // Obtiene el nombre de la propiedad y elimina espacios externos.
                String nombre = linea.substring(0, separador).trim();

                // Obtiene el valor de la propiedad y elimina espacios externos.
                String valor = linea.substring(separador + 1).trim();

                // Ambas partes deben tener contenido.
                if (nombre.isEmpty() || valor.isEmpty()) {
                    throw new IOException(
                            ruta + ": línea " + numeroLinea
                                    + ": el nombre y el valor son obligatorios.");
                }

                // Valida y guarda cada propiedad reconocida.
                switch (nombre) {

                    case "host" -> {
                        // Evita que el host aparezca más de una vez.
                        if (host != null) {
                            throw new IOException(
                                    ruta + ": línea " + numeroLinea
                                            + ": host repetido.");
                        }

                        host = valor;
                    }

                    case "puerto" -> {
                        // Evita que el puerto aparezca más de una vez.
                        if (puerto != null) {
                            throw new IOException(
                                    ruta + ": línea " + numeroLinea
                                            + ": puerto repetido.");
                        }

                        // El puerto debe estar entre 1 y 65535.
                        puerto = leerEntero(
                                valor, nombre, 65535, ruta, numeroLinea);
                    }

                    case "tiempoEsperaConexionMs" -> {
                        // Evita repetir el tiempo máximo de conexión.
                        if (tiempoConexion != null) {
                            throw new IOException(
                                    ruta + ": línea " + numeroLinea
                                            + ": tiempoEsperaConexionMs repetido.");
                        }

                        // Valida que el tiempo sea un entero positivo.
                        tiempoConexion = leerEntero(
                                valor, nombre, Integer.MAX_VALUE,
                                ruta, numeroLinea);
                    }

                    case "tiempoEsperaLecturaMs" -> {
                        // Evita repetir el tiempo máximo de lectura.
                        if (tiempoLectura != null) {
                            throw new IOException(
                                    ruta + ": línea " + numeroLinea
                                            + ": tiempoEsperaLecturaMs repetido.");
                        }

                        // Valida que el tiempo sea un entero positivo.
                        tiempoLectura = leerEntero(
                                valor, nombre, Integer.MAX_VALUE,
                                ruta, numeroLinea);
                    }

                    // Rechaza nombres de propiedades que no reconocemos.
                    default -> throw new IOException(
                            ruta + ": línea " + numeroLinea
                                    + ": propiedad desconocida: " + nombre);
                }
            }
        }

        // Comprueba que el archivo incluya todas las propiedades obligatorias.
        if (host == null) {
            throw new IOException(ruta + ": falta la propiedad host.");
        }

        if (puerto == null) {
            throw new IOException(ruta + ": falta la propiedad puerto.");
        }

        if (tiempoConexion == null) {
            throw new IOException(
                    ruta + ": falta la propiedad tiempoEsperaConexionMs.");
        }

        if (tiempoLectura == null) {
            throw new IOException(
                    ruta + ": falta la propiedad tiempoEsperaLecturaMs.");
        }

        // Crea la configuración únicamente después de validar todos los datos.
        return new ConfiguracionCliente(
                host, puerto, tiempoConexion, tiempoLectura);
    }

    //Funcion para leer enteros
    private static int leerEntero(String valor, String nombre, int maximo,
                                  Path ruta, int numeroLinea) throws IOException {

        final int numero;

        try{
            //Intentamos parsear el valor a un entero
            numero = Integer.parseInt(valor);
        } catch (NumberFormatException e){
            //De lo contrario, tiramos el error
            throw new IOException(
                    ruta + ": línea " + numeroLinea
                    + ": " + nombre + " debe ser un entero válido.", e);
        }

        //Validamos el tamaño dle valor del numero
        if (numero < 1 || numero > maximo) {
            throw new IOException(
                    ruta + ": línea " + numeroLinea
                    + ": " + nombre + " debe ser un entero válido entre 1 y " + maximo + ".");
        }

        return numero;
    }

}
