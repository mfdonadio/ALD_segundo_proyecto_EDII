package org.example.cliente;

import org.example.configuracion.ConfiguracionCliente;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.io.IOException;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.example.seguridad.IntercambioClaves;
import java.math.BigInteger;

public class ClienteMain {

    public static void main(String[] args) {

        // Guarda los valores de conexión leídos del archivo.
        final ConfiguracionCliente configuracion;

        try {
            // La ruta parte de la carpeta principal del proyecto.
            configuracion = ConfiguracionCliente.cargar(
                    Path.of("config", "cliente.txt"));

        } catch (IOException e) {
            System.err.println(
                    "No se pudo cargar la configuración: " + e.getMessage());

            // Si la configuración falla, no intentamos conectarnos.
            return;
        }

        // El socket se cierra automáticamente al salir de este bloque.
        try (Socket conexion = new Socket()) {

            // Conecta usando el host, puerto y tiempo máximo configurados.
            conexion.connect(
                    new InetSocketAddress(
                            configuracion.getHost(),
                            configuracion.getPuerto()),
                    configuracion.getTiempoEsperaConexionMs());

            // Limita cuánto esperamos al leer datos del servidor.
            conexion.setSoTimeout(
                    configuracion.getTiempoEsperaLecturaMs());

            System.out.println(
                    "Conectado al servidor "
                            + conexion.getInetAddress().getHostAddress()
                            + ":" + conexion.getPort());

            // Permite enviar datos al servidor.
            DataOutputStream salida =
                    new DataOutputStream(conexion.getOutputStream());

            // Permite recibir datos del servidor.
            DataInputStream entrada =
                    new DataInputStream(conexion.getInputStream());

            // Intercambia las claves públicas y calcula el secreto de esta conexión.
            BigInteger secretoCompartido =
                    IntercambioClaves.intercambiar(entrada, salida);

            // Confirma que terminó el intercambio sin mostrar información secreta.
            System.out.println("Intercambio Diffie-Hellman completado.");

            // Confirma el secreto usando el papel de cliente.
            IntercambioClaves.confirmar(
                    entrada, salida, secretoCompartido, true);

            System.out.println("Secreto compartido confirmado.");

            // Envía un mensaje de prueba para comprobar la comunicación.
            salida.writeUTF("PING");
            salida.flush();

            // Espera y muestra la respuesta del servidor.
            String respuesta = entrada.readUTF();
            System.out.println("Respuesta del servidor: " + respuesta);

        } catch (IOException e) {
            // Informa errores al conectar, enviar o recibir datos.
            System.err.println(
                    "Error de comunicación con el servidor: " + e.getMessage());
        }
    }
}
