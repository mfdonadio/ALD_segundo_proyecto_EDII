package org.example.pruebas;

import org.example.seguridad.HmacSha256;
import org.example.seguridad.Sha256;

import java.nio.charset.StandardCharsets;

public class PruebaHmacSha256 {

    public static void main(String[] args) {

        // Clave de prueba: veinte bytes con el valor 0x0b.
        byte[] clave = new byte[20];

        for (int i = 0; i < clave.length; i++) {
            clave[i] = (byte) 0x0b;
        }

        byte[] mensaje =
                "Hi There".getBytes(StandardCharsets.UTF_8);

        // Resultado publicado en el caso 1 de RFC 4231.
        String esperado =
                "b0344c61d8db38535ca8afceaf0bf12b"
                        + "881dc200c9833da726e9376c2e32cff7";

        String obtenido = Sha256.aHexadecimal(
                HmacSha256.calcular(clave, mensaje));

        // Detiene la prueba si el resultado no coincide.
        if (!obtenido.equals(esperado)) {
            throw new AssertionError(
                    "HMAC incorrecto"
                            + "\nEsperado: " + esperado
                            + "\nObtenido: " + obtenido);
        }

        System.out.println("OK: HMAC-SHA-256, caso 1 de RFC 4231.");

        // Crea una clave mayor que el bloque de 64 bytes.
        byte[] claveLarga = new byte[131];

        for (int i = 0; i < claveLarga.length; i++) {
            claveLarga[i] = (byte) 0xaa;
        }

        // Conserva exactamente el texto del caso de prueba.
        byte[] mensajeLargo =
                "Test Using Larger Than Block-Size Key - Hash Key First"
                        .getBytes(StandardCharsets.UTF_8);

        // Resultado publicado para el caso 6.
        String esperadoLargo =
                "60e431591ee0b67f0d8a26aacbf5b77f"
                        + "8e0bc6213728c5140546040f0ee37f54";

        String obtenidoLargo = Sha256.aHexadecimal(
                HmacSha256.calcular(claveLarga, mensajeLargo));

        // Comprueba la ruta de procesamiento de claves largas.
        if (!obtenidoLargo.equals(esperadoLargo)) {
            throw new AssertionError(
                    "HMAC incorrecto con clave larga"
                            + "\nEsperado: " + esperadoLargo
                            + "\nObtenido: " + obtenidoLargo);
        }

        System.out.println("OK: HMAC-SHA-256, caso 6 de RFC 4231.");
    }
}