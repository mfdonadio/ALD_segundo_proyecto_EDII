package org.example.cliente;

import org.example.configuracion.ConfiguracionCliente;
import org.example.seguridad.CanalCifrado;
import org.example.seguridad.ClavesSesion;
import org.example.seguridad.IntercambioClaves;
import org.example.servicio.ValidacionAcceso;

import java.io.BufferedReader;
import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ClienteMain {

    public static void main(String[] args) {

        final ConfiguracionCliente configuracion;

        try {
            // Cargamos la configuración antes de pedir las credenciales.
            configuracion = ConfiguracionCliente.cargar(
                    Path.of("config", "cliente.txt"));

        } catch (IOException e) {
            System.err.println(
                    "No se pudo cargar la configuración: " + e.getMessage());
            return;
        }

        Console consola = System.console();

        // IntelliJ normalmente no proporciona un objeto Console.
        BufferedReader teclado = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));

        if (consola == null) {
            System.out.println(
                    "En esta consola la contraseña será visible al escribir.");
        }

        String correo = "";

        try {
            while (true) {
                // Permitimos conservar el correo del intento anterior.
                String indicacion = correo.isEmpty()
                        ? "Correo: "
                        : "Correo [" + correo + "] (Enter para conservar): ";

                String escrito = leerLinea(consola, teclado, indicacion);

                // Enter conserva el último correo válido.
                String candidato = escrito.isEmpty() ? correo : escrito.trim();

                String errorCorreo = ValidacionAcceso.validarCorreo(candidato);

                if (errorCorreo != null) {
                    System.out.println(errorCorreo);
                    continue;
                }

                // Actualizamos el correo guardado solo cuando el formato es válido.
                correo = candidato;

                String contrasena;

                // Si la contraseña tiene un error de formato, repetimos solo ese campo.
                while (true) {
                    contrasena = leerContrasena(consola, teclado);

                    if (contrasena == null) {
                        return;
                    }

                    String error = ValidacionAcceso.validarContrasena(contrasena);

                    if (error == null) {
                        break;
                    }

                    System.out.println(error);
                }

                // Abrimos una conexión nueva por cada intento de autenticación.
                boolean finalizar = intentarAcceso(
                        configuracion, correo, contrasena, consola, teclado);

                if (finalizar) {
                    return;
                }

                String repetir = leerLinea(
                        consola, teclado, "¿Quieres volver al inicio de sesión? (s/n): ");

                if (repetir == null || !"s".equalsIgnoreCase(repetir.trim())) {
                    return;
                }
            }

        } catch (IOException e) {
            System.err.println(
                    "No se pudo leer la entrada de consola: " + e.getMessage());
        }
    }

    private static boolean intentarAcceso(
            ConfiguracionCliente configuracion,
            String correo,
            String contrasena,
            Console consola,
            BufferedReader teclado) {

        // El socket se cierra al terminar este intento.
        try (Socket conexion = new Socket()) {

            conexion.connect(
                    new InetSocketAddress(
                            configuracion.getHost(),
                            configuracion.getPuerto()),
                    configuracion.getTiempoEsperaConexionMs());

            conexion.setSoTimeout(
                    configuracion.getTiempoEsperaLecturaMs());

            DataOutputStream salida =
                    new DataOutputStream(conexion.getOutputStream());

            DataInputStream entrada =
                    new DataInputStream(conexion.getInputStream());

            // Establecemos y confirmamos el secreto de esta conexión.
            BigInteger secretoCompartido =
                    IntercambioClaves.intercambiar(entrada, salida);

            IntercambioClaves.confirmar(
                    entrada, salida, secretoCompartido, true);

            ClavesSesion claves =
                    ClavesSesion.crear(secretoCompartido, true);

            CanalCifrado canal = new CanalCifrado(entrada, salida, claves);

            // Enviamos el comando y los campos en el orden esperado.
            enviarTexto(canal, "LOGIN");
            enviarTexto(canal, correo);
            enviarTexto(canal, contrasena);

            String estado = recibirTexto(canal);

            if ("LOGIN_OK".equals(estado)) {
                // Recibimos únicamente los datos del perfil, nunca el hash.
                String nombre = recibirTexto(canal);
                String apellido = recibirTexto(canal);
                String tipoCuenta = recibirTexto(canal);

                if (!"PREMIUM".equals(tipoCuenta)
                        && !"NORMAL".equals(tipoCuenta)) {
                    throw new IOException("Tipo de cuenta no reconocido.");
                }

                System.out.println();
                System.out.println(
                        "¡Bienvenido, " + nombre + " " + apellido + "!");
                System.out.println("Cuenta: " + tipoCuenta);

                return atenderMenu(canal, consola, teclado);
            }

            if ("ERROR_CAMPO".equals(estado)
                    || "ERROR_CREDENCIALES".equals(estado)
                    || "ERROR_SERVIDOR".equals(estado)) {

                System.out.println(recibirTexto(canal));
                return false;
            }

            throw new IOException("Respuesta del servidor no reconocida.");

        } catch (IOException e) {
            System.err.println(
                    "Error de comunicación con el servidor: " + e.getMessage());
            return false;
        }
    }

    private static String leerLinea(
            Console consola,
            BufferedReader teclado,
            String indicacion) throws IOException {

        if (consola != null) {
            return consola.readLine("%s", indicacion);
        }

        System.out.print(indicacion);
        System.out.flush();
        return teclado.readLine();
    }

    private static String leerContrasena(
            Console consola,
            BufferedReader teclado) throws IOException {

        if (consola == null) {
            return leerLinea(null, teclado, "Contraseña: ");
        }

        // Console permite escribir sin mostrar la contraseña.
        char[] caracteres = consola.readPassword("Contraseña: ");

        if (caracteres == null) {
            return null;
        }

        String contrasena = new String(caracteres);

        // Limpiamos el arreglo temporal; el String sigue existiendo en memoria.
        for (int i = 0; i < caracteres.length; i++) {
            caracteres[i] = '\0';
        }

        return contrasena;
    }

    private static void enviarTexto(
            CanalCifrado canal, String texto) throws IOException {

        canal.enviar(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String recibirTexto(CanalCifrado canal) throws IOException {
        return new String(canal.recibir(), StandardCharsets.UTF_8);
    }

    //Menu con las opciones del cliente :)
    private static boolean atenderMenu(
            CanalCifrado canal,
            Console consola,
            BufferedReader teclado) throws IOException {

        while (true) {
            System.out.println();
            System.out.println("=== Biblioteca ===");
            System.out.println("1. Ver mi perfil");
            System.out.println("2. Cerrar sesión");
            System.out.println("0. Salir");

            String opcion = leerLinea(consola, teclado, "Elige una opción: ");

            // Si se cierra la entrada de consola, intentamos salir normalmente.
            if (opcion == null) {
                opcion = "0";
            }

            switch (opcion.trim()) {
                case "1" -> {
                    enviarTexto(canal, "PERFIL");

                    String estado = recibirTexto(canal);

                    if (!"PERFIL_OK".equals(estado)) {
                        throw new IOException(
                                "El servidor no pudo entregar el perfil.");
                    }

                    // El servidor devuelve el perfil de esta sesión.
                    String nombre = recibirTexto(canal);
                    String apellido = recibirTexto(canal);
                    String correo = recibirTexto(canal);
                    String cuenta = recibirTexto(canal);

                    if (!"PREMIUM".equals(cuenta) && !"NORMAL".equals(cuenta)) {
                        throw new IOException("Tipo de cuenta no reconocido.");
                    }

                    System.out.println();
                    System.out.println("Nombre: " + nombre + " " + apellido);
                    System.out.println("Correo: " + correo);
                    System.out.println("Cuenta: " + cuenta);
                }

                case "2" -> {
                    enviarTexto(canal, "LOGOUT");

                    if (!"LOGOUT_OK".equals(recibirTexto(canal))) {
                        throw new IOException(
                                "No se confirmó el cierre de sesión.");
                    }

                    System.out.println("Sesión cerrada.");

                    // Regresamos al flujo que permite iniciar sesión otra vez.
                    return false;
                }

                case "0" -> {
                    enviarTexto(canal, "SALIR");

                    if (!"SALIR_OK".equals(recibirTexto(canal))) {
                        throw new IOException(
                                "No se confirmó la salida.");
                    }

                    System.out.println("¡Hasta luego!");

                    // Indicamos al main que debe finalizar el programa.
                    return true;
                }

                default -> System.out.println(
                        "Opción inválida. Escribe 1, 2 o 0.");
            }
        }
    }
}
