package org.example.servidor;

import org.example.configuracion.ConfiguracionServidor;

import java.io.IOException;
import java.net.ServerSocket; //Nos ayuda a abrir el puerto donde recibimos las conexiones
import java.net.Socket;
import java.nio.file.Path;
import org.example.persistencia.RepositorioUsuarios;
import org.example.servicio.ServicioAutenticacion;

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

        // Cargamos las cuentas una sola vez, antes de aceptar conexiones.
        final ServicioAutenticacion autenticacion;

        try {
            RepositorioUsuarios repositorio = RepositorioUsuarios.cargar(
                    Path.of("datos", "usuarios.txt"),
                    Path.of("datos", "perfiles.txt"));

            autenticacion = new ServicioAutenticacion(repositorio);

        } catch (IOException e) {
            // Si los archivos fallan, no iniciamos un servidor sin cuentas.
            System.err.println(
                    "No se pudieron cargar los usuarios: " + e.getMessage());
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

                // Entregamos la conexión, el tiempo de espera y el servicio de autenticación.
                ManejadorCliente manejador = new ManejadorCliente(
                        cliente,
                        configuracion.getTiempoEsperaLecturaMs(),
                        autenticacion);

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
