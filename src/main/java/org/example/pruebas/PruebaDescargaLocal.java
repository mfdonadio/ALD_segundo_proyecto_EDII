package org.example.pruebas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Comprueba que la descarga conserva el archivo cifrado
 * y que su descifrado recupera exactamente el original.
 */
public class PruebaDescargaLocal {

    public static void main(String[] args) throws IOException {
        // Cambia este ISBN si descargaste el otro libro.
        String isbn = "9788424136464";

        comprobarIguales(
                Path.of("datos", "libros", isbn + ".txt"),
                Path.of("descargas", isbn + ".txt"),
                "Descarga cifrada idéntica al archivo del servidor");

        comprobarIguales(
                Path.of("datos", "originales", isbn + ".txt"),
                Path.of("descargas", isbn + ".descifrado.txt"),
                "Descifrado idéntico al original");
    }

    private static void comprobarIguales(
            Path esperado,
            Path recibido,
            String descripcion) throws IOException {

        // Devuelve -1 cuando los archivos tienen exactamente los mismos bytes.
        long diferencia = Files.mismatch(esperado, recibido);

        if (diferencia != -1) {
            throw new AssertionError(
                    descripcion + ": diferencia en el byte " + diferencia);
        }

        System.out.println("OK: " + descripcion);
    }
}