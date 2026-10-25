package org.example.pruebas;

import org.example.seguridad.HkdfSha256;
import org.example.seguridad.Sha256;

/*
 * Comprueba HKDF-SHA-256 con resultados publicados en RFC 5869.
 * Incluye sal explícita, sal vacía y sal no proporcionada.
 */
public class PruebaHkdfSha256 {

    public static void main(String[] args) {
        // Los dos casos utilizan 22 bytes con el valor 0x0B.
        byte[] secreto = new byte[22];

        for (int i = 0; i < secreto.length; i++) {
            secreto[i] = 0x0B;
        }

        // Caso 1: sal de 13 bytes y contexto de 10 bytes.
        byte[] sal = new byte[13];
        byte[] contexto = new byte[10];

        for (int i = 0; i < sal.length; i++) {
            sal[i] = (byte) i;
        }

        for (int i = 0; i < contexto.length; i++) {
            contexto[i] = (byte) (0xF0 + i);
        }

        byte[] resultado1 = HkdfSha256.derivar(
                secreto, sal, contexto, 42);

        String esperado1 =
                "3cb25f25faacd57a90434f64d0362f2a"
                        + "2d2d0a90cf1a5a4c5db02d56ecc4c5bf"
                        + "34007208d5b887185865";

        comprobar(
                esperado1.equals(Sha256.aHexadecimal(resultado1)),
                "HKDF-SHA-256, caso 1 de RFC 5869");

        // Caso 3: sal y contexto vacíos.
        byte[] resultado3 = HkdfSha256.derivar(
                secreto, new byte[0], new byte[0], 42);

        String esperado3 =
                "8da4e775a563c18f715f802a063c5a31"
                        + "b8a11f5c5ee1879ec3454e5f3c738d2d"
                        + "9d201395faa4b61a96c8";

        comprobar(
                esperado3.equals(Sha256.aHexadecimal(resultado3)),
                "HKDF-SHA-256, caso 3 de RFC 5869");

        // Sin sal, la implementación utiliza 32 bytes de ceros.
        byte[] sinSal = HkdfSha256.derivar(
                secreto, null, new byte[0], 42);

        comprobar(
                esperado3.equals(Sha256.aHexadecimal(sinSal)),
                "Sal no proporcionada produce el resultado esperado");

        // Nuestra API admite longitudes entre 1 y 8160 bytes.
        comprobarLongitudRechazada(secreto, 0);
        comprobarLongitudRechazada(secreto, 8161);

        System.out.println("Todas las pruebas de HKDF pasaron.");
    }

    private static void comprobarLongitudRechazada(
            byte[] secreto, int longitud) {

        boolean rechazado = false;

        try {
            HkdfSha256.derivar(
                    secreto, null, new byte[0], longitud);
        } catch (IllegalArgumentException e) {
            // Esperamos que una longitud fuera del límite sea rechazada.
            rechazado = true;
        }

        comprobar(
                rechazado,
                "Longitud inválida rechazada: " + longitud);
    }

    private static void comprobar(boolean condicion, String descripcion) {
        if (!condicion) {
            throw new AssertionError("Falló: " + descripcion);
        }

        System.out.println("OK: " + descripcion);
    }
}