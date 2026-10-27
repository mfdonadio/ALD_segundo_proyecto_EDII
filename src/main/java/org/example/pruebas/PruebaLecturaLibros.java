package org.example.pruebas;

import org.example.persistencia.RepositorioLibros;
import org.example.seguridad.ClavesLibros;
import org.example.servicio.ServicioLibros;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Comprueba que los libros cifrados guardados en disco
 * recuperan exactamente los bytes de los archivos originales.
 */
public class PruebaLecturaLibros {

    public static void main(String[] args) throws IOException {
        // Cargamos el catálogo y las claves que ya existen.
        RepositorioLibros catalogo = RepositorioLibros.cargar(
                Path.of("datos", "catalogo.txt"));

        ClavesLibros claves = ClavesLibros.cargar(
                Path.of("datos", "claves", "libros.key"));

        ServicioLibros servicio = new ServicioLibros(
                catalogo, claves, Path.of("datos", "libros"));

        comprobarLibro(servicio, "9788420412146");
        comprobarLibro(servicio, "9788424136464");

        System.out.println("Todas las pruebas de lectura pasaron.");
    }

    private static void comprobarLibro(
            ServicioLibros servicio, String isbn) throws IOException {

        // Leemos el original únicamente para realizar la comparación.
        byte[] original = Files.readAllBytes(
                Path.of("datos", "originales", isbn + ".txt"));

        // El servicio lee, verifica y descifra el archivo almacenado.
        byte[] recuperado = servicio.leerContenido(isbn);

        if (original.length != recuperado.length) {
            throw new AssertionError(
                    "La longitud no coincide para el libro " + isbn);
        }

        // Incluimos en la comparación acentos, espacios y saltos de línea.
        for (int i = 0; i < original.length; i++) {
            if (original[i] != recuperado[i]) {
                throw new AssertionError(
                        "Diferencia en el byte " + i
                                + " del libro " + isbn);
            }
        }

        System.out.println(
                "OK: archivo guardado recuperado exactamente: " + isbn);
    }
}