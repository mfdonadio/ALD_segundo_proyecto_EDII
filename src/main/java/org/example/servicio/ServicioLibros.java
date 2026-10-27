package org.example.servicio;

import org.example.modelo.Libro;
import org.example.persistencia.RepositorioLibros;
import org.example.seguridad.ClavesLibros;
import org.example.seguridad.MensajeCifrado;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Localiza los libros del catálogo y recupera su contenido original.
 * Valida el formato del archivo y verifica el HMAC antes de descifrar.
 * No modifica los archivos cifrados almacenados.
 */
public final class ServicioLibros {

    private static final String CABECERA = "BIBLIOTECA-3DES-V1";

    // Coincide con el máximo admitido por MensajeCifrado.
    private static final int MAXIMO_PAQUETE = 1024 * 1024 + 48;

    // Cada byte ocupa dos caracteres hexadecimales.
    // Incluimos la cabecera y hasta dos saltos de línea CRLF.
    private static final int MAXIMO_ARCHIVO =
            CABECERA.length() + 4 + MAXIMO_PAQUETE * 2;

    private final RepositorioLibros catalogo;
    private final ClavesLibros claves;
    private final Path carpeta;

    public ServicioLibros(
            RepositorioLibros catalogo,
            ClavesLibros claves,
            Path carpeta) {

        if (catalogo == null || claves == null || carpeta == null) {
            throw new IllegalArgumentException(
                    "El catálogo, las claves y la carpeta son obligatorios.");
        }

        this.catalogo = catalogo;
        this.claves = claves;
        this.carpeta = carpeta.toAbsolutePath().normalize();
    }

    //Accedemos al contenido del libro.
    public byte[] leerContenido(String isbn) throws IOException {
        Path ruta = localizar(isbn);

        byte[] archivo;

        // Leemos un byte adicional para detectar archivos demasiado grandes.
        try (var entrada = Files.newInputStream(ruta)) {
            archivo = entrada.readNBytes(MAXIMO_ARCHIVO + 1);
        }

        if (archivo.length > MAXIMO_ARCHIVO) {
            throw new IOException(
                    "El archivo del libro supera el tamaño permitido.");
        }

        String texto = new String(archivo, StandardCharsets.UTF_8);

        // Adaptamos los saltos del contenedor, no los del contenido original.
        String normalizado = texto.replace("\r\n", "\n");
        String[] lineas = normalizado.split("\n", -1);

        // Admitimos la cabecera y el hexadecimal, con o sin salto final.
        boolean estructuraValida =
                lineas.length == 2
                        || (lineas.length == 3 && lineas[2].isEmpty());

        if (!estructuraValida || !CABECERA.equals(lineas[0])) {
            throw new IOException(
                    "El archivo no tiene un formato de libro compatible.");
        }

        byte[] paquete = desdeHexadecimal(lineas[1]);

        try {
            // MensajeCifrado verifica el HMAC antes de descifrar.
            return MensajeCifrado.descifrar(
                    paquete,
                    claves.getCifrado(),
                    claves.getHmac());

        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "El libro está dañado o sus claves no corresponden.", e);
        }
    }

    //Buscamos el archivo en nuestro directorio.
    private Path localizar(String isbn) throws IOException {
        if (isbn == null || !isbn.trim().matches("[0-9Xx-]+")) {
            throw new IOException("El ISBN tiene un formato inválido.");
        }

        Libro libro = catalogo.buscarPorIsbn(isbn.trim());

        if (libro == null) {
            throw new IOException("El libro no está en el catálogo.");
        }

        Path ruta = carpeta.resolve(libro.getIsbn() + ".txt").normalize();

        // Comprobamos que la ruta construida permanezca dentro de la carpeta.
        if (!ruta.startsWith(carpeta)) {
            throw new IOException("La ruta del libro no está permitida.");
        }

        if (!Files.isRegularFile(ruta)) {
            throw new IOException(
                    "El libro está en el catálogo, "
                            + "pero su archivo no está disponible.");
        }

        return ruta;
    }

    //Convertimos el contenido hexadecimal en bytes.
    private static byte[] desdeHexadecimal(String texto) throws IOException {
        if (texto.isEmpty()
                || texto.length() % 2 != 0
                || texto.length() / 2 > MAXIMO_PAQUETE) {

            throw new IOException(
                    "El contenido hexadecimal del libro es inválido.");
        }

        byte[] resultado = new byte[texto.length() / 2];

        // Cada pareja de caracteres representa un byte del paquete.
        for (int i = 0; i < resultado.length; i++) {
            int alto = valorHexadecimal(texto.charAt(i * 2));
            int bajo = valorHexadecimal(texto.charAt(i * 2 + 1));

            if (alto < 0 || bajo < 0) {
                throw new IOException(
                        "El libro contiene caracteres hexadecimales inválidos.");
            }

            resultado[i] = (byte) ((alto << 4) | bajo);
        }

        return resultado;
    }

    //transformamos los caracteres a su valor hexadecimal.
    private static int valorHexadecimal(char caracter) {
        if (caracter >= '0' && caracter <= '9') {
            return caracter - '0';
        }

        if (caracter >= 'a' && caracter <= 'f') {
            return caracter - 'a' + 10;
        }

        if (caracter >= 'A' && caracter <= 'F') {
            return caracter - 'A' + 10;
        }

        return -1;
    }

    //Accedemos al contenido del libro cuando esta cifrado.
    public byte[] leerArchivoCifrado(String isbn) throws IOException {
        Path ruta = localizar(isbn);

        byte[] archivo;

        // Leemos los bytes originales del archivo sin modificar su formato.
        try (var entrada = Files.newInputStream(ruta)) {
            archivo = entrada.readNBytes(MAXIMO_ARCHIVO + 1);
        }

        if (archivo.length > MAXIMO_ARCHIVO) {
            throw new IOException(
                    "El archivo del libro supera el tamaño permitido.");
        }

        return archivo;
    }
}