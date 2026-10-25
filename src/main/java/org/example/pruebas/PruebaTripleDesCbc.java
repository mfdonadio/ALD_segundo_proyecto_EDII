package org.example.pruebas;

import org.example.seguridad.TripleDesCbc;

public class PruebaTripleDesCbc {

    public static void main(String[] args) {
        // Usamos una clave fija únicamente para estas pruebas.
        byte[] clave = {
                0x01, 0x23, 0x45, 0x67,
                (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF,
                0x23, 0x45, 0x67, (byte) 0x89,
                (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, 0x01,
                0x45, 0x67, (byte) 0x89, (byte) 0xAB,
                (byte) 0xCD, (byte) 0xEF, 0x01, 0x23
        };

        // Probamos los límites de bloque y un mensaje más grande.
        int[] longitudes = {0, 1, 7, 8, 9, 15, 16, 17, 1024};

        for (int longitud : longitudes) {
            comprobarRecuperacion(longitud, clave);
        }

        comprobarCambioDeIv(clave);

        comprobarVectorNist();

        System.out.println("Todas las pruebas de recuperación CBC pasaron.");
    }

    private static void comprobarRecuperacion(int longitud, byte[] clave) {
        byte[] original = new byte[longitud];
        byte[] copiaOriginal = new byte[longitud];

        // Incluimos distintos valores de byte, incluidos valores negativos.
        for (int i = 0; i < longitud; i++) {
            original[i] = (byte) (i * 37 + 128);
        }

        // Un cero final debe conservarse al retirar el relleno.
        if (longitud > 0) {
            original[longitud - 1] = 0;
        }

        System.arraycopy(original, 0, copiaOriginal, 0, longitud);

        byte[] iv = TripleDesCbc.generarIv();

        byte[] cifrado = TripleDesCbc.cifrar(original, clave, iv);
        byte[] recuperado = TripleDesCbc.descifrar(cifrado, clave, iv);

        // PKCS#5 siempre agrega entre uno y ocho bytes.
        int longitudEsperada = longitud + 8 - (longitud % 8);

        comprobar(
                cifrado.length == longitudEsperada,
                "Longitud cifrada incorrecta para " + longitud + " bytes.");

        comprobar(
                iguales(original, copiaOriginal),
                "El cifrado modificó los datos originales.");

        comprobar(
                iguales(recuperado, copiaOriginal),
                "No se recuperaron exactamente " + longitud + " bytes.");

        System.out.println(
                "OK: recuperación exacta de " + longitud + " bytes.");
    }

    private static void comprobarCambioDeIv(byte[] clave) {
        byte[] original = {10, 20, 30, 40, 50, 60, 70, 80};

        // Estos IV son fijos para que la prueba sea reproducible.
        byte[] iv1 = {0, 1, 2, 3, 4, 5, 6, 7};
        byte[] iv2 = {1, 1, 2, 3, 4, 5, 6, 7};

        byte[] cifrado1 = TripleDesCbc.cifrar(original, clave, iv1);
        byte[] cifrado2 = TripleDesCbc.cifrar(original, clave, iv2);

        comprobar(
                !iguales(cifrado1, cifrado2),
                "Cambiar el IV debería cambiar el cifrado.");

        // Cada resultado debe recuperarse utilizando su propio IV.
        comprobar(
                iguales(original,
                        TripleDesCbc.descifrar(cifrado1, clave, iv1)),
                "Falló la recuperación con el primer IV.");

        comprobar(
                iguales(original,
                        TripleDesCbc.descifrar(cifrado2, clave, iv2)),
                "Falló la recuperación con el segundo IV.");

        System.out.println("OK: IV distintos producen cifrados distintos.");
    }

    private static boolean iguales(byte[] primero, byte[] segundo) {
        if (primero.length != segundo.length) {
            return false;
        }

        // Comparamos todos los bytes, sin convertirlos a texto.
        for (int i = 0; i < primero.length; i++) {
            if (primero[i] != segundo[i]) {
                return false;
            }
        }

        return true;
    }

    private static void comprobar(boolean condicion, String mensaje) {
        // Detenemos la prueba cuando encontramos un resultado incorrecto.
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    private static void comprobarVectorNist() {
        // Clave, IV y mensaje del ejemplo oficial de NIST.
        byte[] clave = desdeHexadecimal(
                "0123456789ABCDEF"
                        + "23456789ABCDEF01"
                        + "456789ABCDEF0123");

        byte[] iv = desdeHexadecimal("F69F2445DF4F9B17");

        byte[] original = desdeHexadecimal(
                "6BC1BEE22E409F96"
                        + "E93D7E117393172A"
                        + "AE2D8A571E03AC9C"
                        + "9EB76FAC45AF8E51");

        byte[] esperado = desdeHexadecimal(
                "2079C3D53AA763E1"
                        + "93B79E2569AB5262"
                        + "516570481F25B50F"
                        + "73C0BDA85C8E0DA7");

        byte[] cifrado = TripleDesCbc.cifrar(original, clave, iv);

        // Nuestra implementación agrega un bloque completo de relleno.
        comprobar(
                cifrado.length == esperado.length + 8,
                "Falta el bloque adicional de relleno.");

        // Los primeros cuatro bloques deben coincidir con NIST.
        for (int i = 0; i < esperado.length; i++) {
            comprobar(
                    cifrado[i] == esperado[i],
                    "El vector NIST no coincide en el byte " + i + ".");
        }

        System.out.println("OK: cifrado CBC coincide con el vector NIST.");
    }

    private static byte[] desdeHexadecimal(String texto) {
        // Cada pareja de caracteres representa un byte.
        if (texto == null || texto.length() % 2 != 0) {
            throw new IllegalArgumentException("Hexadecimal inválido.");
        }

        byte[] resultado = new byte[texto.length() / 2];

        for (int i = 0; i < resultado.length; i++) {
            int alto = Character.digit(texto.charAt(i * 2), 16);
            int bajo = Character.digit(texto.charAt(i * 2 + 1), 16);

            if (alto < 0 || bajo < 0) {
                throw new IllegalArgumentException("Hexadecimal inválido.");
            }

            resultado[i] = (byte) ((alto << 4) | bajo);
        }

        return resultado;
    }
}