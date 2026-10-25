package org.example.seguridad;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/*
 * Deriva las claves de una conexión desde el secreto de Diffie-Hellman.
 * Separa las claves de cifrado y HMAC para cada dirección.
 * Asigna las claves de envío y recepción según el participante.
 * Devuelve copias para evitar modificaciones de las claves almacenadas.
 */
public final class ClavesSesion {

    private static final int TAMANO_SECRETO = 256;

    private final byte[] cifradoEnvio;
    private final byte[] hmacEnvio;
    private final byte[] cifradoRecepcion;
    private final byte[] hmacRecepcion;

    private ClavesSesion(
            byte[] cifradoEnvio,
            byte[] hmacEnvio,
            byte[] cifradoRecepcion,
            byte[] hmacRecepcion) {

        // Guardamos copias independientes de los arreglos recibidos.
        this.cifradoEnvio = cifradoEnvio.clone();
        this.hmacEnvio = hmacEnvio.clone();
        this.cifradoRecepcion = cifradoRecepcion.clone();
        this.hmacRecepcion = hmacRecepcion.clone();
    }

    public static ClavesSesion crear(
            BigInteger secretoCompartido, boolean esCliente) {

        byte[] secreto = codificarSecreto(secretoCompartido);

        // Las etiquetas identifican la dirección, el propósito y la versión.
        byte[] cifradoClienteServidor = derivar(
                secreto, "biblioteca-v1-cliente-servidor-3des", 24);

        byte[] hmacClienteServidor = derivar(
                secreto, "biblioteca-v1-cliente-servidor-hmac", 32);

        byte[] cifradoServidorCliente = derivar(
                secreto, "biblioteca-v1-servidor-cliente-3des", 24);

        byte[] hmacServidorCliente = derivar(
                secreto, "biblioteca-v1-servidor-cliente-hmac", 32);

        if (esCliente) {
            // El cliente envía hacia el servidor.
            return new ClavesSesion(
                    cifradoClienteServidor,
                    hmacClienteServidor,
                    cifradoServidorCliente,
                    hmacServidorCliente);
        }

        // El servidor utiliza las mismas claves en el sentido contrario.
        return new ClavesSesion(
                cifradoServidorCliente,
                hmacServidorCliente,
                cifradoClienteServidor,
                hmacClienteServidor);
    }

    // Deriva una clave
    private static byte[] derivar(
            byte[] secreto, String etiqueta, int longitud) {

        byte[] contexto = etiqueta.getBytes(StandardCharsets.UTF_8);

        // Usamos la opción sin sal de HKDF.
        // Cada conexión debe aportar un secreto DH nuevo.
        return HkdfSha256.derivar(
                secreto, null, contexto, longitud);
    }

    //Codifica el secreto compartido
    private static byte[] codificarSecreto(BigInteger secreto) {
        // Esta validación no sustituye la validación de la clave pública DH.
        if (secreto == null
                || secreto.compareTo(BigInteger.TWO) < 0
                || secreto.bitLength() > TAMANO_SECRETO * 8) {

            throw new IllegalArgumentException(
                    "El secreto compartido tiene un valor inválido.");
        }

        byte[] original = secreto.toByteArray();
        byte[] resultado = new byte[TAMANO_SECRETO];

        // BigInteger puede agregar un cero inicial para indicar signo positivo.
        int inicio = original.length == TAMANO_SECRETO + 1 ? 1 : 0;
        int cantidad = original.length - inicio;

        // Representamos siempre el secreto como 256 bytes sin signo.
        System.arraycopy(
                original, inicio,
                resultado, TAMANO_SECRETO - cantidad,
                cantidad);

        return resultado;
    }

    //Getters
    public byte[] getCifradoEnvio() {
        return cifradoEnvio.clone();
    }

    public byte[] getHmacEnvio() {
        return hmacEnvio.clone();
    }

    public byte[] getCifradoRecepcion() {
        return cifradoRecepcion.clone();
    }

    public byte[] getHmacRecepcion() {
        return hmacRecepcion.clone();
    }
}
