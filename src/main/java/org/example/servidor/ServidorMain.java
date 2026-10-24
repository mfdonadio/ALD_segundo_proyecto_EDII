package org.example.servidor;

import org.example.configuracion.ConfiguracionServidor;

import java.io.IOException;
import java.net.ServerSocket; //Nos ayuda a abrir el puerto donde recibimos las conexiones
import java.net.Socket;
import java.nio.file.Path;

public class ServidorMain {

    public static void main(String[] args) {

        //inicializamos la configuracion del servidor
        final ConfiguracionServidor configuracion;

        //Carga y valida los datos antes de abrir el puerto
        try{
            configuracion = ConfiguracionServidor.cargar(
                    Path.of("config", "servidor.txt"));

        } catch (IOException e) {
            System.err.println(
                    "No se puedo cargar la configuracion: " + e.getMessage());
            return;
        }

        //Abrimos el puerto indicado en el archivo
        try(ServerSocket servidor = new ServerSocket(configuracion.getPuerto())){
            System.out.println("Servidor escuchando en el puerto: " + servidor.getLocalPort());

            //Esperamos la conexion mientras el servidor este activo
            while(true){
                System.out.println("Esperando conexiones...");

                //Esperamos hasta que se conecte un cliente
                Socket cliente = servidor.accept();

                //Entregamos la conexion y el tiempo maximo de lectura
                ManejadorCliente manejador = new ManejadorCliente(cliente, configuracion.getTiempoEsperaLecturaMs());

                //Atendemos al cliente, pero tambien seguimos acpetando conexiones.
                Thread hilo = new Thread(manejador);
                hilo.start();
            }
        } catch (IOException e) {
            System.err.println(
                    "Error del servidor: " + e.getMessage());
        }

    }
}
