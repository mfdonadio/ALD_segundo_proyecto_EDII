package org.example.modelo;

/*
 * Representa un libro del catálogo.
 * Conserva su ISBN y título sin almacenar el contenido del archivo.
 */
public final class Libro {

    private final String isbn;
    private final String titulo;

    public Libro(String isbn, String titulo) {
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException(
                    "El ISBN del libro es obligatorio.");
        }

        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException(
                    "El título del libro es obligatorio.");
        }

        // Conservamos el ISBN como texto para no perder ceros iniciales.
        this.isbn = isbn.trim();
        this.titulo = titulo.trim();
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitulo() {
        return titulo;
    }
}