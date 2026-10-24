package org.example.pruebas;

import org.example.seguridad.Sha256;

import java.nio.charset.StandardCharsets;

public class PruebaSha256 {

    public static void main(String[] args) {

        // Comprueba el caso de un mensaje sin contenido.
        verificar(
                "Mensaje vacío",
                "",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        // Comprueba un mensaje corto.
        verificar(
                "Texto abc",
                "abc",
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");

        // Sus 56 bytes necesitan dos bloques al agregar el relleno.
        verificar(
                "Mensaje de 56 bytes",
                "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
                "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1");

        // Comprueba el procesamiento de muchos bloques.
        verificar(
                "Un millón de letras a",
                "a".repeat(1_000_000),
                "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0");

        System.out.println("Todas las pruebas de SHA-256 pasaron.");
    }

    private static void verificar(
            String nombre,
            String texto,
            String esperado) {

        // Usa UTF-8 de forma explícita en cualquier computadora.
        byte[] entrada = texto.getBytes(StandardCharsets.UTF_8);

        // Calcula con nuestra implementación y convierte el resultado.
        String obtenido = Sha256.aHexadecimal(
                Sha256.calcular(entrada));

        // Detiene la prueba si el resultado es diferente.
        if (!obtenido.equals(esperado)) {
            throw new AssertionError(
                    nombre
                            + "\nEsperado: " + esperado
                            + "\nObtenido: " + obtenido);
        }

        System.out.println("OK: " + nombre);
    }
}