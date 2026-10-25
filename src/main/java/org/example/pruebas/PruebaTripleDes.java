package org.example.pruebas;

import org.example.seguridad.TripleDes;

public class PruebaTripleDes {

    public static void main(String[] args) {

        // Claves públicas de prueba; no se usarán en nuestra aplicación.
        long clave1 = 0x0123456789ABCDEFL;
        long clave2 = 0x23456789ABCDEF01L;
        long clave3 = 0x456789ABCDEF0123L;

        // Bloques originales publicados por NIST.
        long[] originales = {
                0x6BC1BEE22E409F96L,
                0xE93D7E117393172AL,
                0xAE2D8A571E03AC9CL,
                0x9EB76FAC45AF8E51L
        };

        // Resultados esperados para esas claves.
        long[] esperados = {
                0x714772F339841D34L,
                0x267FCC4BD2949CC3L,
                0xEE11C22A576A3038L,
                0x76183F99C0B6DE87L
        };

        for (int i = 0; i < originales.length; i++) {

            // Compara el cifrado con el valor externo conocido.
            long cifrado = TripleDes.cifrarBloque(
                    originales[i], clave1, clave2, clave3);

            if (cifrado != esperados[i]) {
                throw new AssertionError(
                        "Cifrado incorrecto en el bloque " + (i + 1));
            }

            // Descifra directamente el bloque publicado por NIST.
            long recuperado = TripleDes.descifrarBloque(
                    esperados[i], clave1, clave2, clave3);

            if (recuperado != originales[i]) {
                throw new AssertionError(
                        "Descifrado incorrecto en el bloque " + (i + 1));
            }

            System.out.println(
                    "OK: cifrado y descifrado 3DES, bloque " + (i + 1));
        }

        System.out.println("Todas las pruebas de estos vectores 3DES pasaron.");
    }
}