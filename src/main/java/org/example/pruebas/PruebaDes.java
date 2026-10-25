package org.example.pruebas;

import org.example.seguridad.Des;

public class PruebaDes {

    public static void main(String[] args) {

        // Valores hexadecimales de un vector conocido de DES.
        long clave = 0x133457799BBCDFF1L;
        long original = 0x0123456789ABCDEFL;
        long esperado = 0x85E813540F0AB405L;

        // Comprueba el cifrado contra un resultado externo conocido.
        long cifrado = Des.cifrarBloque(original, clave);

        comprobar(
                cifrado == esperado,
                "Cifrado DES coincide con el valor esperado");

        // Descifra directamente el valor conocido.
        long recuperado = Des.descifrarBloque(esperado, clave);

        comprobar(
                recuperado == original,
                "Descifrado DES recupera el bloque original");

        // Muestra siempre los dieciséis dígitos del bloque.
        System.out.printf("Original:  %016X%n", original);
        System.out.printf("Cifrado:   %016X%n", cifrado);
        System.out.printf("Recuperado: %016X%n", recuperado);

        System.out.println("Todas las pruebas de este vector DES pasaron.");
    }

    private static void comprobar(boolean condicion, String nombre) {

        // Detiene la prueba si alguna comparación falla.
        if (!condicion) {
            throw new AssertionError("Falló: " + nombre);
        }

        System.out.println("OK: " + nombre);
    }
}
