package org.example.pruebas;

import org.example.seguridad.RellenoPkcs5;

public class PruebaRellenoPkcs5 {

    public static void main(String[] args) {

        // Prueba mensajes vacíos y tamaños cercanos al límite de bloque.
        int[] longitudes = {0, 1, 7, 8, 9, 15, 16};

        for (int longitud : longitudes) {
            comprobarRecuperacion(longitud);
        }

        // Comprueba un ejemplo exacto, independiente del descifrado.
        byte[] ejemplo = RellenoPkcs5.agregar(
                new byte[]{10, 20, 30, 40, 50});

        comprobar(
                ejemplo.length == 8
                        && ejemplo[5] == 3
                        && ejemplo[6] == 3
                        && ejemplo[7] == 3,
                "Cinco bytes reciben el relleno 03 03 03");

        // El relleno nunca puede indicar cero bytes.
        comprobarRechazo(
                new byte[]{1, 2, 3, 4, 5, 6, 7, 0},
                "Relleno de valor cero rechazado");

        // El relleno no puede superar el tamaño del bloque.
        comprobarRechazo(
                new byte[]{1, 2, 3, 4, 5, 6, 7, 9},
                "Relleno mayor de ocho rechazado");

        // El último byte indica tres, pero uno de los anteriores no coincide.
        comprobarRechazo(
                new byte[]{10, 20, 30, 40, 50, 3, 2, 3},
                "Bytes de relleno inconsistentes rechazados");

        // Los datos con relleno deben formar bloques completos.
        comprobarRechazo(
                new byte[]{1, 2, 3},
                "Bloque incompleto rechazado");

        comprobarRechazo(
                new byte[0],
                "Entrada sin relleno rechazada");

        System.out.println("Todas las pruebas de relleno pasaron.");
    }

    private static void comprobarRecuperacion(int longitud) {

        byte[] original = new byte[longitud];

        // Incluye valores que Java representa como bytes negativos.
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (128 + i);
        }

        // Conserva también un cero final como parte de los datos originales.
        if (original.length > 0) {
            original[original.length - 1] = 0;
        }

        byte[] conRelleno = RellenoPkcs5.agregar(original);

        int cantidadEsperada = 8 - (longitud % 8);

        comprobar(
                conRelleno.length == longitud + cantidadEsperada,
                "Longitud con relleno para " + longitud + " bytes");

        // Comprueba que agregar relleno no cambió el contenido original.
        for (int i = 0; i < original.length; i++) {
            if (conRelleno[i] != original[i]) {
                throw new AssertionError(
                        "Se modificó un byte original en la posición " + i);
            }
        }

        byte[] recuperado = RellenoPkcs5.quitar(conRelleno);

        comprobar(
                recuperado.length == original.length,
                "Longitud recuperada para " + longitud + " bytes");

        // Compara todo el contenido, incluido el cero final.
        for (int i = 0; i < original.length; i++) {
            if (recuperado[i] != original[i]) {
                throw new AssertionError(
                        "Se recuperó un byte diferente en la posición " + i);
            }
        }

        System.out.println(
                "OK: recuperación exacta de " + longitud + " bytes");
    }

    private static void comprobarRechazo(byte[] datos, String nombre) {

        boolean rechazado = false;

        try {
            RellenoPkcs5.quitar(datos);
        } catch (IllegalArgumentException e) {
            // Esperamos que el método detecte el formato inválido.
            rechazado = true;
        }

        comprobar(rechazado, nombre);
    }

    private static void comprobar(boolean condicion, String nombre) {

        if (!condicion) {
            throw new AssertionError("Falló: " + nombre);
        }

        System.out.println("OK: " + nombre);
    }
}