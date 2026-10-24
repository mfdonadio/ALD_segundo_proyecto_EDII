package org.example.servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


/*Runnable nos va a permitir representar a los clientes como tareas que
puede ejecutarse de manera concurrente en un hilo separado.*/

public class ManejadorCliente implements Runnable {

    private final Socket cliente;
    private final int tiempoEsperaLecturaMs;

    public ManejadorCliente(Socket cliente, int tiempoEsperaLecturaMs){
        this.cliente = cliente;
        this.tiempoEsperaLecturaMs = tiempoEsperaLecturaMs;
    }

    // Tarea ejecutable
    @Override
    public void run() {
        try(cliente){
            cliente.setSoTimeout(tiempoEsperaLecturaMs);

            System.out.println("Atendiendo a: "
            + cliente.getRemoteSocketAddress());

            //Recibe los datos del cliente
            DataInputStream entrada = new DataInputStream(cliente.getInputStream());

            //Envia los datos al cliente
            DataOutputStream salida = new DataOutputStream(cliente.getOutputStream());

            //Preparamos el mensaje recibido
            String mensaje = entrada.readUTF();
            System.out.println("Mensaje recibido: " + mensaje);

            //Si recibimos, por ambitos de pruebas, la palabra "PING", devolemos "PONG"
            if(mensaje.equals("PING")){
                salida.writeUTF("PONG");
            } else {
                salida.writeUTF("ERROR: mensaje desconocido.");
            }

            //Solicitamos enviar cualquier dato pendiente en la salida
            salida.flush();
        } catch (IOException e) {
            System.out.println("Error con el cliente "
                    + cliente.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }
}
