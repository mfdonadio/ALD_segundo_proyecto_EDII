package org.example.seguridad;

/*
 * El relleno permite procesar mensajes cuya longitud no es múltiplo de 8.
 * 3DES trabaja con bloques completos de 8 bytes.
 *
 * PKCS#5 añade entre 1 y 8 bytes antes de cifrar. Cada byte añadido
 * contiene la cantidad de relleno, lo que permite validarlo y retirarlo
 * después de descifrar para recuperar exactamente los datos originales.
 *
 * Si los datos ya ocupan bloques completos, añade un bloque de relleno.
 * El relleno no detecta por sí solo todas las alteraciones del mensaje.
 */

public final class RellenoPkcs5 {

    private static final int TAMANO_BLOQUE = 8;

    private RellenoPkcs5() {
    }

    public static byte[] agregar(byte[] datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos no pueden ser null.");
        }

        // Calcula cuántos bytes faltan para completar el siguiente bloque.
        int cantidad = TAMANO_BLOQUE - (datos.length % TAMANO_BLOQUE);

        // Evita desbordar el tamaño del arreglo.
        if (datos.length > Integer.MAX_VALUE - cantidad) {
            throw new IllegalArgumentException(
                    "Los datos son demasiado grandes.");
        }

        byte[] resultado = new byte[datos.length + cantidad];

        // Conserva todos los bytes originales.
        for (int i = 0; i < datos.length; i++) {
            resultado[i] = datos[i];
        }

        // Cada byte añadido indica la cantidad de relleno.
        for (int i = datos.length; i < resultado.length; i++) {
            resultado[i] = (byte) cantidad;
        }

        return resultado;
    }

    public static byte[] quitar(byte[] datos) {

        // Un mensaje con relleno debe contener bloques completos.
        if (datos == null
                || datos.length == 0
                || datos.length % TAMANO_BLOQUE != 0) {

            throw new IllegalArgumentException(
                    "Datos con relleno inválido.");
        }

        // El último byte indica cuántos bytes debemos quitar.
        int cantidad = datos[datos.length - 1] & 0xff;

        if (cantidad < 1 || cantidad > TAMANO_BLOQUE) {
            throw new IllegalArgumentException(
                    "Datos con relleno inválido.");
        }

        int inicioRelleno = datos.length - cantidad;
        int diferencias = 0;

        // Comprueba todos los bytes del relleno indicado.
        for (int i = inicioRelleno; i < datos.length; i++) {
            diferencias |= (datos[i] & 0xff) ^ cantidad;
        }

        if (diferencias != 0) {
            throw new IllegalArgumentException(
                    "Datos con relleno inválido.");
        }

        // Recupera únicamente los bytes originales.
        byte[] resultado = new byte[inicioRelleno];

        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = datos[i];
        }

        return resultado;
    }
}