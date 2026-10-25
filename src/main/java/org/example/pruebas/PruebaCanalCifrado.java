package org.example.pruebas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.example.seguridad.CanalCifrado;
import org.example.seguridad.ClavesSesion;
import org.example.seguridad.DiffieHellman;

/*
 * Comprueba el envío de mensajes consecutivos y una respuesta.
 * Verifica que un mensaje repetido sea rechazado.
 * Utiliza flujos en memoria para controlar los datos de la prueba.
 */
public class PruebaCanalCifrado {

    public static void main(String[] args) throws IOException {
        DiffieHellman clienteDh = new DiffieHellman();
        DiffieHellman servidorDh = new DiffieHellman();

        BigInteger secretoCliente = clienteDh.calcularSecreto(
                servidorDh.getClavePublica());

        BigInteger secretoServidor = servidorDh.calcularSecreto(
                clienteDh.getClavePublica());

        ClavesSesion clavesCliente =
                ClavesSesion.crear(secretoCliente, true);

        ClavesSesion clavesServidor =
                ClavesSesion.crear(secretoServidor, false);

        // Guardamos los bytes que enviaría el cliente por el socket.
        ByteArrayOutputStream enviadosCliente =
                new ByteArrayOutputStream();

        CanalCifrado cliente = new CanalCifrado(
                entradaVacia(),
                new DataOutputStream(enviadosCliente),
                clavesCliente);

        cliente.enviar("PING".getBytes(StandardCharsets.UTF_8));

        // Conservamos el primer paquete completo para repetirlo después.
        byte[] primerPaquete = enviadosCliente.toByteArray();

        cliente.enviar("SEGUNDO".getBytes(StandardCharsets.UTF_8));

        // Simulamos la repetición del primer paquete tras los dos mensajes.
        enviadosCliente.write(
                primerPaquete, 0, primerPaquete.length);

        ByteArrayOutputStream enviadosServidor =
                new ByteArrayOutputStream();

        CanalCifrado servidor = new CanalCifrado(
                new DataInputStream(
                        new ByteArrayInputStream(
                                enviadosCliente.toByteArray())),
                new DataOutputStream(enviadosServidor),
                clavesServidor);

        comprobar(
                "PING".equals(leerTexto(servidor)),
                "Primer mensaje recibido");

        comprobar(
                "SEGUNDO".equals(leerTexto(servidor)),
                "Segundo mensaje recibido en orden");

        // La dirección contraria tiene su propio contador, que empieza en cero.
        servidor.enviar("PONG".getBytes(StandardCharsets.UTF_8));

        CanalCifrado receptorCliente = new CanalCifrado(
                new DataInputStream(
                        new ByteArrayInputStream(
                                enviadosServidor.toByteArray())),
                new DataOutputStream(new ByteArrayOutputStream()),
                clavesCliente);

        comprobar(
                "PONG".equals(leerTexto(receptorCliente)),
                "Respuesta recibida con contador independiente");

        // El servidor espera el contador dos, pero recibe otra vez el cero.
        boolean repetidoRechazado = false;

        try {
            servidor.recibir();
        } catch (IOException e) {
            repetidoRechazado =
                    "Mensaje repetido o fuera de orden.".equals(e.getMessage());
        }

        comprobar(
                repetidoRechazado,
                "Mensaje repetido rechazado");

        // Después del fallo, tampoco debe permitirse enviar.
        boolean canalBloqueado = false;

        try {
            servidor.enviar("OTRO".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            canalBloqueado =
                    "El canal ya no puede utilizarse. Cierra la conexión."
                            .equals(e.getMessage());
        }

        comprobar(
                canalBloqueado,
                "Canal inutilizado después del fallo");

        System.out.println("Todas las pruebas del canal cifrado pasaron.");
    }

    private static DataInputStream entradaVacia() {
        // Este flujo se utiliza cuando la prueba solo necesita enviar.
        return new DataInputStream(
                new ByteArrayInputStream(new byte[0]));
    }

    private static String leerTexto(CanalCifrado canal) throws IOException {
        // Convertimos a texto únicamente después de recibir y verificar.
        return new String(canal.recibir(), StandardCharsets.UTF_8);
    }

    private static void comprobar(boolean condicion, String descripcion) {
        if (!condicion) {
            throw new AssertionError("Falló: " + descripcion);
        }

        System.out.println("OK: " + descripcion);
    }
}