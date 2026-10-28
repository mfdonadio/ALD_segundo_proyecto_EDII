package org.example.pruebas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.example.persistencia.RepositorioLibros;
import org.example.seguridad.ClavesLibros;
import org.example.servicio.ServicioLibros;

/*
 * Comprueba que el archivo cifrado del servidor se descifra
 * y recupera exactamente el original versionado.
 * No depende de descargas previas: usa solo los datos del repositorio.
 */
public class PruebaDescargaLocal {

    public static void main(String[] args) throws IOException {
        String isbn = "9788424136464";

        // Reproducimos el descifrado con los datos versionados, sin descargas.
        RepositorioLibros catalogo = RepositorioLibros.cargar(
                Path.of("datos", "catalogo.txt"));

        ClavesLibros claves = ClavesLibros.cargar(
                Path.of("datos", "claves", "libros.key"));

        ServicioLibros servicio = new ServicioLibros(
                catalogo, claves, Path.of("datos", "libros"));

        byte[] original = Files.readAllBytes(
                Path.of("datos", "originales", isbn + ".txt"));

        byte[] descifrado = servicio.leerContenido(isbn);

        comprobarIguales(
                original, descifrado,
                "Descifrado idéntico al original versionado");
    }

    private static void comprobarIguales(
            byte[] esperado,
            byte[] recibido,
            String descripcion) {

        if (esperado.length != recibido.length) {
            throw new AssertionError(
                    descripcion + ": longitudes diferentes.");
        }

        for (int i = 0; i < esperado.length; i++) {
            if (esperado[i] != recibido[i]) {
                throw new AssertionError(
                        descripcion + ": diferencia en el byte " + i + ".");
            }
        }

        System.out.println("OK: " + descripcion);
    }
}
