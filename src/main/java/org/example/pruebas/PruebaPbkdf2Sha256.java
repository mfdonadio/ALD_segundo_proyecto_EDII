package org.example.pruebas;

import org.example.seguridad.Pbkdf2Sha256;
import org.example.seguridad.Sha256;

import java.nio.charset.StandardCharsets;

public class PruebaPbkdf2Sha256 {

    public static void main(String[] args) {

        // Comprueba el primer HMAC, sin rondas adicionales.
        verificar(
                "Una iteración",
                "passwd",
                "salt",
                1,
                "55ac046e56e3089fec1691c22544b605"
                        + "f94185216dde0465e68b9d57c20dacbc");

        // Comprueba las rondas adicionales y su combinación con XOR.
        verificar(
                "80000 iteraciones",
                "Password",
                "NaCl",
                80000,
                "4ddcd8f60b98be21830cee5ef22701f9"
                        + "641a4418d04c0414aeff08876b34ab56");

        System.out.println("Todas las pruebas de PBKDF2 pasaron.");
    }

    private static void verificar(
            String nombre,
            String contrasena,
            String sal,
            int iteraciones,
            String esperado) {

        // Convierte los textos a bytes usando una codificación explícita.
        byte[] contrasenaBytes =
                contrasena.getBytes(StandardCharsets.UTF_8);

        byte[] salBytes =
                sal.getBytes(StandardCharsets.UTF_8);

        // Calcula el resultado usando nuestra implementación.
        byte[] resultado = Pbkdf2Sha256.derivar(
                contrasenaBytes, salBytes, iteraciones);

        // Convierte los bytes para compararlos con el valor esperado.
        String obtenido = Sha256.aHexadecimal(resultado);

        if (!obtenido.equals(esperado)) {
            throw new AssertionError(
                    nombre
                            + "\nEsperado: " + esperado
                            + "\nObtenido: " + obtenido);
        }

        System.out.println("OK: " + nombre);
    }
}
