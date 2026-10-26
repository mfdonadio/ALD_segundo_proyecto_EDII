package org.example.pruebas;

import org.example.modelo.Libro;
import org.example.persistencia.RepositorioLibros;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Comprueba la carga, búsqueda y crecimiento del catálogo.
 * Utiliza archivos temporales para probar entradas inválidas.
 */
public class PruebaLibros {

    public static void main(String[] args) throws IOException {
        RepositorioLibros catalogo = RepositorioLibros.cargar(
                Path.of("datos", "catalogo.txt"));

        System.out.println("Libros cargados: " + catalogo.getCantidad());

        for (int i = 0; i < catalogo.getCantidad(); i++) {
            Libro libro = catalogo.obtener(i);

            System.out.println(
                    libro.getIsbn() + " | " + libro.getTitulo());
        }

        // Las pruebas siguientes no modifican el catálogo del proyecto.
        Path temporal = Files.createTempFile("prueba-catalogo-", ".txt");

        try {
            // Doce registros obligan a ampliar el arreglo inicial de diez.
            StringBuilder contenido = new StringBuilder();

            for (int i = 0; i < 12; i++) {
                contenido.append("000")
                        .append(i)
                        .append(" | Libro de prueba ")
                        .append(i)
                        .append('\n');
            }

            Files.writeString(
                    temporal, contenido, StandardCharsets.UTF_8);

            RepositorioLibros prueba = RepositorioLibros.cargar(temporal);

            comprobar(
                    prueba.getCantidad() == 12,
                    "Crecimiento del arreglo");

            Libro encontrado = prueba.buscarPorIsbn("00011");

            comprobar(
                    encontrado != null
                            && "Libro de prueba 11".equals(encontrado.getTitulo()),
                    "Búsqueda y conservación de ceros iniciales");

            comprobar(
                    prueba.buscarPorIsbn("NO-EXISTE") == null,
                    "ISBN inexistente devuelve null");

            comprobarRechazo(
                    temporal,
                    "001 | Primero\n001 | Segundo\n",
                    "ISBN repetido rechazado");

            comprobarRechazo(
                    temporal,
                    "001 | \n",
                    "Título vacío rechazado");

            comprobarRechazo(
                    temporal,
                    " | Libro\n",
                    "ISBN vacío rechazado");

            comprobarRechazo(
                    temporal,
                    "001 Libro sin separador\n",
                    "Separador ausente rechazado");

            comprobarRechazo(
                    temporal,
                    "001 | Título | Campo adicional\n",
                    "Campos adicionales rechazados");

        } finally {
            // Eliminamos únicamente el archivo temporal de esta prueba.
            Files.deleteIfExists(temporal);
        }

        System.out.println("Todas las pruebas del catálogo pasaron.");
    }

    private static void comprobarRechazo(
            Path ruta, String contenido, String descripcion) throws IOException {

        Files.writeString(ruta, contenido, StandardCharsets.UTF_8);

        boolean rechazado = false;

        try {
            RepositorioLibros.cargar(ruta);
        } catch (IOException e) {
            // Confirmamos que el error identifica la línea del archivo.
            rechazado = e.getMessage().contains(": línea ");
        }

        comprobar(rechazado, descripcion);
    }

    private static void comprobar(boolean condicion, String descripcion) {
        if (!condicion) {
            throw new AssertionError("Falló: " + descripcion);
        }

        System.out.println("OK: " + descripcion);
    }
}