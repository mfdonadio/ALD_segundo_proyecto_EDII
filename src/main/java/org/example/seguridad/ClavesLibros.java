package org.example.seguridad;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;

/*
 * Guarda las claves utilizadas para proteger los archivos de libros.
 * Conserva 24 bytes para 3DES y 32 bytes para HMAC.
 * Solo genera claves cuando el archivo todavía no existe.
 * Nunca reemplaza automáticamente un archivo existente.
 */
public final class ClavesLibros {

    private static final int TAMANO_CIFRADO = 24;
    private static final int TAMANO_HMAC = 32;
    private static final int TAMANO_TOTAL =
            TAMANO_CIFRADO + TAMANO_HMAC;

    private final byte[] cifrado;
    private final byte[] hmac;

    private ClavesLibros(byte[] contenido) {
        cifrado = new byte[TAMANO_CIFRADO];
        hmac = new byte[TAMANO_HMAC];

        System.arraycopy(
                contenido, 0, cifrado, 0, TAMANO_CIFRADO);

        System.arraycopy(
                contenido, TAMANO_CIFRADO, hmac, 0, TAMANO_HMAC);
    }

    public static ClavesLibros cargar(Path ruta) throws IOException {
        validarRuta(ruta);

        // Leemos como máximo un byte adicional para detectar tamaños inválidos.
        byte[] contenido;

        try (var entrada = Files.newInputStream(ruta)) {
            contenido = entrada.readNBytes(TAMANO_TOTAL + 1);
        }

        if (contenido.length != TAMANO_TOTAL) {
            throw new IOException(
                    ruta + ": el archivo de claves debe contener "
                            + TAMANO_TOTAL + " bytes.");
        }

        return new ClavesLibros(contenido);
    }

    public static ClavesLibros cargarOCrear(Path ruta) throws IOException {
        validarRuta(ruta);

        Path carpeta = ruta.toAbsolutePath().getParent();
        Files.createDirectories(carpeta);

        byte[] contenido = new byte[TAMANO_TOTAL];
        new SecureRandom().nextBytes(contenido);

        try {
            // CREATE_NEW impide sobrescribir las claves existentes.
            Files.write(
                    ruta,
                    contenido,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);

        } catch (FileAlreadyExistsException e) {
            // Si ya existe, utilizamos ese archivo sin reemplazarlo.
            return cargar(ruta);
        }

        // Comprobamos también el archivo que acabamos de escribir.
        return cargar(ruta);
    }

    public byte[] getCifrado() {
        // Entregamos una copia para proteger el arreglo interno.
        return cifrado.clone();
    }

    public byte[] getHmac() {
        return hmac.clone();
    }

    private static void validarRuta(Path ruta) {
        if (ruta == null) {
            throw new IllegalArgumentException(
                    "La ruta de las claves es obligatoria.");
        }
    }
}
