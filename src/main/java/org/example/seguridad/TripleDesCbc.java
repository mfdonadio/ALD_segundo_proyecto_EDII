package org.example.seguridad;

import java.security.SecureRandom;


/*
 * Cifra y descifra datos con 3DES en modo CBC y relleno PKCS#5.
 * Procesa bloques de 8 bytes utilizando una clave de 24 bytes y un IV de 8 bytes.
 * Al descifrar, retira el relleno para recuperar exactamente los datos originales.
 * El IV debe ser nuevo en cada cifrado y conservarse para el descifrado.
 * Esta clase no verifica la integridad de los datos; eso corresponde al HMAC.
 */

//CBC es la forma de aplicar 3DES a una secuencia de bloques de 8 bytes.

public final class TripleDesCbc {

    private static final int TAMANO_BLOQUE = 8;
    private static final int TAMANO_CLAVE = 24;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TripleDesCbc() {
        // Evitamos crear objetos de esta clase.
    }

    public static byte[] generarIv() {
        // Generamos un IV nuevo para cada cifrado.
        byte[] iv = new byte[TAMANO_BLOQUE];
        ALEATORIO.nextBytes(iv);
        return iv;
    }

    public static byte[] cifrar(byte[] datos, byte[] clave, byte[] iv) {
        validarClaveEIv(clave, iv);

        // Completamos el último bloque sin perder los bytes originales.
        byte[] datosConRelleno = RellenoPkcs5.agregar(datos);
        byte[] cifrado = new byte[datosConRelleno.length];

        // Dividimos los 24 bytes en las tres claves de DES.
        long clave1 = leerBloque(clave, 0);
        long clave2 = leerBloque(clave, 8);
        long clave3 = leerBloque(clave, 16);

        // El primer bloque utiliza el IV como bloque anterior.
        long anterior = leerBloque(iv, 0);

        for (int posicion = 0;
             posicion < datosConRelleno.length;
             posicion += TAMANO_BLOQUE) {

            long bloque = leerBloque(datosConRelleno, posicion);

            // CBC combina el bloque original con el cifrado anterior.
            long combinado = bloque ^ anterior;

            long bloqueCifrado = TripleDes.cifrarBloque(
                    combinado, clave1, clave2, clave3);

            escribirBloque(bloqueCifrado, cifrado, posicion);

            // Guardamos el bloque cifrado para la siguiente vuelta.
            anterior = bloqueCifrado;
        }

        return cifrado;
    }

    public static byte[] descifrar(byte[] cifrado, byte[] clave, byte[] iv) {
        validarClaveEIv(clave, iv);

        // El cifrado debe contener al menos un bloque completo.
        if (cifrado == null
                || cifrado.length == 0
                || cifrado.length % TAMANO_BLOQUE != 0) {

            throw new IllegalArgumentException(
                    "El cifrado debe contener bloques completos de 8 bytes.");
        }

        byte[] datosConRelleno = new byte[cifrado.length];

        long clave1 = leerBloque(clave, 0);
        long clave2 = leerBloque(clave, 8);
        long clave3 = leerBloque(clave, 16);

        long anterior = leerBloque(iv, 0);

        for (int posicion = 0;
             posicion < cifrado.length;
             posicion += TAMANO_BLOQUE) {

            long bloqueCifrado = leerBloque(cifrado, posicion);

            long bloqueDescifrado = TripleDes.descifrarBloque(
                    bloqueCifrado, clave1, clave2, clave3);

            // Deshacemos la combinación aplicada durante el cifrado.
            long original = bloqueDescifrado ^ anterior;

            escribirBloque(original, datosConRelleno, posicion);

            // Conservamos el bloque cifrado, no el bloque descifrado.
            anterior = bloqueCifrado;
        }

        // Validamos y retiramos únicamente los bytes de relleno.
        return RellenoPkcs5.quitar(datosConRelleno);
    }

    private static void validarClaveEIv(byte[] clave, byte[] iv) {
        // Cada una de las tres claves ocupa 8 bytes.
        if (clave == null || clave.length != TAMANO_CLAVE) {
            throw new IllegalArgumentException(
                    "La clave de 3DES debe contener 24 bytes.");
        }

        if (iv == null || iv.length != TAMANO_BLOQUE) {
            throw new IllegalArgumentException(
                    "El IV debe contener 8 bytes.");
        }
    }

    private static long leerBloque(byte[] datos, int inicio) {
        long bloque = 0;

        // Convertimos 8 bytes en un long, empezando por el más significativo.
        for (int i = 0; i < TAMANO_BLOQUE; i++) {
            bloque = (bloque << 8) | (datos[inicio + i] & 0xFFL);
        }

        return bloque;
    }

    private static void escribirBloque(long bloque, byte[] destino, int inicio) {
        // Recuperamos los 8 bytes en el mismo orden de lectura.
        for (int i = 0; i < TAMANO_BLOQUE; i++) {
            int desplazamiento = 56 - (i * 8);
            destino[inicio + i] = (byte) (bloque >>> desplazamiento);
        }
    }
}