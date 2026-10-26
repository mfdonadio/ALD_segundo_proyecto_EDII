package org.example.pruebas;

import org.example.modelo.Libro;
import org.example.persistencia.RepositorioLibros;
import org.example.seguridad.ClavesLibros;
import org.example.seguridad.MensajeCifrado;
import org.example.seguridad.Sha256;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/*
 * Importa un archivo original y prepara su versión cifrada.
 * Comprueba la recuperación exacta antes de guardar el resultado.
 * No modifica el original ni sobrescribe libros existentes.
 */
public class PrepararLibro {

    private static final int MAXIMO_ORIGINAL = 1024 * 1024;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println(
                    "Uso: PrepararLibro <ISBN> <ruta del archivo original>");
            return;
        }

        try {
            preparar(args[0].trim(), Path.of(args[1]));

        } catch (IOException | IllegalArgumentException e) {
            System.err.println(
                    "No se pudo preparar el libro: " + e.getMessage());
        }
    }

    private static void preparar(String isbn, Path rutaOriginal)
            throws IOException {

        // Permitimos únicamente caracteres seguros para el nombre del archivo.
        if (!isbn.matches("[0-9Xx-]+")) {
            throw new IllegalArgumentException(
                    "El ISBN solo puede contener números, guiones o X.");
        }

        RepositorioLibros catalogo = RepositorioLibros.cargar(
                Path.of("datos", "catalogo.txt"));

        Libro libro = catalogo.buscarPorIsbn(isbn);

        if (libro == null) {
            throw new IllegalArgumentException(
                    "El ISBN no está registrado en el catálogo.");
        }

        // Cargamos las claves existentes; aquí no generamos otras.
        ClavesLibros claves = ClavesLibros.cargar(
                Path.of("datos", "claves", "libros.key"));

        byte[] original;

        // Leemos un byte adicional para detectar archivos demasiado grandes.
        try (var entrada = Files.newInputStream(rutaOriginal)) {
            original = entrada.readNBytes(MAXIMO_ORIGINAL + 1);
        }

        if (original.length > MAXIMO_ORIGINAL) {
            throw new IOException(
                    "Esta versión admite originales de hasta 1 MiB.");
        }

        // MensajeCifrado incluye un IV nuevo y un HMAC.
        byte[] paquete = MensajeCifrado.cifrar(
                original, claves.getCifrado(), claves.getHmac());

        byte[] recuperado = MensajeCifrado.descifrar(
                paquete, claves.getCifrado(), claves.getHmac());

        comprobarRecuperacion(original, recuperado);

        Path carpetaDestino = Path.of("datos", "libros");
        Files.createDirectories(carpetaDestino);

        Path destino = carpetaDestino.resolve(isbn + ".txt");

        // El hexadecimal conserva todos los bytes del paquete cifrado.
        String archivo =
                "BIBLIOTECA-3DES-V1\n"
                        + Sha256.aHexadecimal(paquete)
                        + "\n";

        // Evitamos reemplazar un libro ya preparado.
        Files.writeString(
                destino,
                archivo,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);

        System.out.println("OK: recuperación exacta del original.");
        System.out.println("Libro preparado: " + libro.getTitulo());
        System.out.println("Bytes originales: " + original.length);
        System.out.println("Archivo guardado en: " + destino.toAbsolutePath());
    }

    private static void comprobarRecuperacion(
            byte[] original, byte[] recuperado) throws IOException {

        if (original.length != recuperado.length) {
            throw new IOException(
                    "La longitud recuperada no coincide con el original.");
        }

        // Comparamos los bytes directamente, incluidos saltos de línea y ceros.
        for (int i = 0; i < original.length; i++) {
            if (original[i] != recuperado[i]) {
                throw new IOException(
                        "El contenido recuperado difiere en el byte " + i + ".");
            }
        }
    }
}
