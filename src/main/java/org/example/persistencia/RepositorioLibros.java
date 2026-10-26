package org.example.persistencia;

import org.example.modelo.Libro;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Carga y consulta el catálogo de libros.
 * Utiliza un arreglo propio y rechaza ISBN repetidos.
 * Informa la ruta y la línea cuando encuentra datos inválidos.
 */
public final class RepositorioLibros {

    private Libro[] libros = new Libro[10];
    private int cantidad = 0;

    private RepositorioLibros() {
        // El repositorio se construye mediante cargar().
    }

    public static RepositorioLibros cargar(Path ruta) throws IOException {
        if (ruta == null) {
            throw new IllegalArgumentException(
                    "La ruta del catálogo es obligatoria.");
        }

        RepositorioLibros repositorio = new RepositorioLibros();

        try (BufferedReader lector =
                     Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                // Admitimos una marca BOM al inicio de un archivo UTF-8.
                if (numeroLinea == 1 && linea.startsWith("\uFEFF")) {
                    linea = linea.substring(1);
                }

                if (linea.isBlank()) {
                    continue;
                }

                // Cada registro debe contener exactamente ISBN | Titulo.
                int separador = linea.indexOf('|');

                if (separador < 0
                        || separador != linea.lastIndexOf('|')) {

                    throw error(ruta, numeroLinea,
                            "se esperaba el formato ISBN | Titulo.");
                }

                String isbn = linea.substring(0, separador).trim();
                String titulo = linea.substring(separador + 1).trim();

                if (isbn.isEmpty() || titulo.isEmpty()) {
                    throw error(ruta, numeroLinea,
                            "el ISBN y el título son obligatorios.");
                }

                if (repositorio.buscarPorIsbn(isbn) != null) {
                    throw error(ruta, numeroLinea,
                            "ISBN repetido: " + isbn);
                }

                repositorio.agregar(new Libro(isbn, titulo));
            }
        }

        // Solo devolvemos el catálogo cuando terminó de cargar sin errores.
        return repositorio;
    }

    private void agregar(Libro libro) {
        if (cantidad == libros.length) {
            // Duplicamos la capacidad cuando el arreglo está lleno.
            Libro[] ampliado = new Libro[libros.length * 2];

            for (int i = 0; i < cantidad; i++) {
                ampliado[i] = libros[i];
            }

            libros = ampliado;
        }

        libros[cantidad] = libro;
        cantidad++;
    }

    public Libro buscarPorIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }

        String buscado = isbn.trim();

        for (int i = 0; i < cantidad; i++) {
            if (libros[i].getIsbn().equals(buscado)) {
                return libros[i];
            }
        }

        return null;
    }

    public int getCantidad() {
        return cantidad;
    }

    public Libro obtener(int indice) {
        if (indice < 0 || indice >= cantidad) {
            throw new IndexOutOfBoundsException(
                    "El índice del libro está fuera del catálogo.");
        }

        // Libro es inmutable: consultar el objeto no permite modificarlo.
        return libros[indice];
    }

    private static IOException error(
            Path ruta, int numeroLinea, String detalle) {

        return new IOException(
                ruta + ": línea " + numeroLinea + ": " + detalle);
    }
}