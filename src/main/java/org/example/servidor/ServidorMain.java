package org.example.servidor;

import java.io.IOException;
import java.net.ServerSocket; //Nos ayuda a abrir el puerto donde recibimos las conexiones
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class ServidorMain {

    public static void main(String[] args){
        //definimos el puerto de prueba
        int puerto = 5000;

        //probamos la conexion del puerto
        try(ServerSocket servidor = new ServerSocket(puerto)){
            System.out.println("Servidor escuchando en el puerto: " + puerto);

            //Mantenemos al servidor escuchando peticiones del cliente
            while(true){
                System.out.println("Esperando conexiones...");

                //Creamos el punto de conexion del cliente
                Socket cliente = servidor.accept();

                //Utilizamos el manejador para atender al cliente
                ManejadorCliente manejador = new ManejadorCliente(cliente);
                //Creammos un hilo de ejecucion para atender al cliente
                Thread hilo = new Thread(manejador);

                //Iniciamos el hilo de ejecucion
                hilo.start();
            }
        } catch (IOException e) {
            System.out.println("Error del servidor: " + e.getMessage());
        }
    }
}
