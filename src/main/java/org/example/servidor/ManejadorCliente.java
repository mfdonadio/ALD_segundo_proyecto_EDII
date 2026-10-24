package org.example.servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


/*Runnable nos va a permitir representar a los clientes como tareas que
puede ejecutarse de manera concurrente en un hilo separado.*/

public class ManejadorCliente implements Runnable {

    private final Socket cliente;

    public ManejadorCliente(Socket cliente){
        this.cliente = cliente;
    }

    // Tarea ejecutable
    @Override
    public void run() {
        try(cliente){
            cliente.setSoTimeout(10000);

            System.out.println("Atendiendo a: "
            + cliente.getRemoteSocketAddress());

            //Enviamos la(s) entrada(s) Y recibimos la(s) salida(s)
            DataInputStream entrada = new DataInputStream(cliente.getInputStream());
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

            //Obligamos toda la salida y limpiamos el buffer
            salida.flush();
        } catch (IOException e) {
            System.out.println("Error con el cliente "
                    + cliente.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }
}
