package org.example.pruebas;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.example.seguridad.ClavesSesion;
import org.example.seguridad.DiffieHellman;
import org.example.seguridad.MensajeCifrado;

/*
 * Comprueba que cliente y servidor derivan claves compatibles.
 * Verifica mensajes en ambas direcciones y la protección de las claves.
 */
public class PruebaClavesSesion {

    public static void main(String[] args) {
        // Cada participante genera su propio par de claves DH.
        DiffieHellman clienteDh = new DiffieHellman();
        DiffieHellman servidorDh = new DiffieHellman();

        // Cada uno calcula el secreto usando la clave pública del otro.
        BigInteger secretoCliente = clienteDh.calcularSecreto(
                servidorDh.getClavePublica());

        BigInteger secretoServidor = servidorDh.calcularSecreto(
                clienteDh.getClavePublica());

        ClavesSesion cliente = ClavesSesion.crear(secretoCliente, true);
        ClavesSesion servidor = ClavesSesion.crear(secretoServidor, false);

        comprobar(
                iguales(cliente.getCifradoEnvio(),
                        servidor.getCifradoRecepcion()),
                "Cifrado cliente hacia servidor");

        comprobar(
                iguales(cliente.getHmacEnvio(),
                        servidor.getHmacRecepcion()),
                "HMAC cliente hacia servidor");

        comprobar(
                iguales(servidor.getCifradoEnvio(),
                        cliente.getCifradoRecepcion()),
                "Cifrado servidor hacia cliente");

        comprobar(
                iguales(servidor.getHmacEnvio(),
                        cliente.getHmacRecepcion()),
                "HMAC servidor hacia cliente");

        // Las dos direcciones deben utilizar claves diferentes.
        comprobar(
                !iguales(cliente.getCifradoEnvio(),
                        cliente.getCifradoRecepcion()),
                "Claves de cifrado distintas por dirección");

        comprobar(
                !iguales(cliente.getHmacEnvio(),
                        cliente.getHmacRecepcion()),
                "Claves HMAC distintas por dirección");

        comprobarMensaje(
                "PING", cliente, servidor,
                "Mensaje del cliente recuperado por el servidor");

        comprobarMensaje(
                "PONG", servidor, cliente,
                "Mensaje del servidor recuperado por el cliente");

        // Modificar una copia no debe cambiar la clave guardada.
        byte[] copia = cliente.getCifradoEnvio();
        copia[0] ^= 1;

        comprobar(
                iguales(cliente.getCifradoEnvio(),
                        servidor.getCifradoRecepcion()),
                "Las claves internas están protegidas frente a cambios externos");

        System.out.println("Todas las pruebas de claves de sesión pasaron.");
    }

    private static void comprobarMensaje(
            String texto,
            ClavesSesion emisor,
            ClavesSesion receptor,
            String descripcion) {

    byte[] original = texto.getBytes(StandardCharsets.UTF_8);

        // El emisor utiliza exclusivamente sus claves de envío.
        byte[] paquete = MensajeCifrado.cifrar(
                original,
                emisor.getCifradoEnvio(),
                emisor.getHmacEnvio());

        // El receptor utiliza las claves correspondientes de recepción.
        byte[] recuperado = MensajeCifrado.descifrar(
                paquete,
                receptor.getCifradoRecepcion(),
                receptor.getHmacRecepcion());

        comprobar(iguales(original, recuperado), descripcion);
    }

    private static boolean iguales(byte[] primero, byte[] segundo) {
        if (primero.length != segundo.length) {
            return false;
        }

        // Comparamos el contenido completo de los arreglos.
        for (int i = 0; i < primero.length; i++) {
            if (primero[i] != segundo[i]) {
                return false;
            }
        }

        return true;
    }

    private static void comprobar(boolean condicion, String descripcion) {
        if (!condicion) {
            throw new AssertionError("Falló: " + descripcion);
        }

        System.out.println("OK: " + descripcion);
    }
}