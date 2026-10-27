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
import org.example.seguridad.Sha256;
import java.nio.file.Files;
import org.example.servicio.ServicioLibros;
import java.nio.file.StandardOpenOption;
import org.example.compresion.Huffman;

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

    private static void mostrarCatalogo(CanalCifrado canal) throws IOException {
        enviarTexto(canal, "CATALOGO");

        if (!"CATALOGO_OK".equals(recibirTexto(canal))) {
            throw new IOException(
                    "El servidor no pudo entregar el catálogo.");
        }

        int cantidad;

        try {
            cantidad = Integer.parseInt(recibirTexto(canal));
        } catch (NumberFormatException e) {
            throw new IOException(
                    "La cantidad de libros recibida no es válida.", e);
        }

        if (cantidad < 0) {
            throw new IOException(
                    "La cantidad de libros no puede ser negativa.");
        }

        System.out.println();
        System.out.println("=== Catálogo de libros ===");

        if (cantidad == 0) {
            System.out.println("El catálogo está vacío.");
            return;
        }

        for (int i = 0; i < cantidad; i++) {
            String isbn = recibirTexto(canal);
            String titulo = recibirTexto(canal);

            System.out.println(
                    (i + 1) + ". " + titulo + " | ISBN: " + isbn);
        }

        System.out.println("Total: " + cantidad + " libros.");
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
            System.out.println("2. Ver catálogo");
            System.out.println("3. Leer libro");
            System.out.println("4. Descargar libro cifrado (Premium)");
            System.out.println("5. Cerrar sesión");
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
                    mostrarCatalogo(canal);
                }

                case "3" -> leerLibro(canal, consola, teclado);

                case "4" -> descargarLibro(canal, consola, teclado);

                case "5" -> {
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
                        "Opción inválida. Escribe 1, 2, 3, 4 o 0.");
            }
        }
    }

    //Leer el libro desde terminal.
    private static void leerLibro(
            CanalCifrado canal,
            Console consola,
            BufferedReader teclado) throws IOException {

        // Pedimos el ISBN antes de iniciar la petición al servidor.
        String isbn = leerLinea(
                consola, teclado, "ISBN del libro (Enter para cancelar): ");

        if (isbn == null || isbn.isBlank()) {
            return;
        }

        isbn = isbn.trim();

        if (!isbn.matches("[0-9Xx-]+")) {
            System.out.println(
                    "El ISBN solo puede contener números, guiones o X.");
            return;
        }

        enviarTexto(canal, "LEER");
        enviarTexto(canal, isbn);

        String estado = recibirTexto(canal);

        if ("LECTURA_ERROR".equals(estado)) {
            System.out.println(recibirTexto(canal));
            return;
        }

        if (!"LECTURA_OK".equals(estado)) {
            throw new IOException("Respuesta de lectura no reconocida.");
        }

        int longitud;

        try {
            longitud = Integer.parseInt(recibirTexto(canal));
        } catch (NumberFormatException e) {
            throw new IOException("Longitud del libro inválida.", e);
        }

        // Coincide con el límite actual de los originales importados.
        if (longitud < 0 || longitud > 1024 * 1024) {
            throw new IOException("El tamaño del libro no está permitido.");
        }

        byte[] contenido = new byte[longitud];
        int recibidos = 0;

        while (recibidos < longitud) {
            byte[] parte = canal.recibir();

            // Validamos cada parte antes de copiarla.
            if (parte.length == 0
                    || parte.length > 32 * 1024
                    || parte.length > longitud - recibidos) {

                throw new IOException("Parte del libro inválida.");
            }

            System.arraycopy(
                    parte, 0, contenido, recibidos, parte.length);

            recibidos += parte.length;
        }

        // Convertimos después de reunir todos los bytes para no cortar acentos.
        String texto = new String(contenido, StandardCharsets.UTF_8);

        System.out.println();
        System.out.println("=== LECTURA DEL LIBRO DESDE TERMINAL ===\n");
        System.out.print(texto);
        System.out.println();
        System.out.println("\n=== FIN DEL LIBRO ===");
    }

    //Descargar el libro seleccionado, para usuarios premium.
    private static void descargarLibro(
            CanalCifrado canal,
            Console consola,
            BufferedReader teclado) throws IOException {

        String isbn = leerLinea(
                consola, teclado, "ISBN para descargar (Enter para cancelar): ");

        if (isbn == null || isbn.isBlank()) {
            return;
        }

        isbn = isbn.trim();

        //Usamos una regex para validar el formato del ISBN de nuestros libros.
        if (!isbn.matches("[0-9Xx-]+")) {
            System.out.println(
                    "El ISBN solo puede contener números, guiones o X.");
            return;
        }

        Path carpeta = Path.of("descargas");
        Files.createDirectories(carpeta);

        Path destino = carpeta.resolve(isbn + ".txt");

        // No reemplazamos una descarga existente.
        if (Files.exists(destino)) {
            System.out.println(
                    "El archivo ya existe en: " + destino.toAbsolutePath());
            return;
        }

        enviarTexto(canal, "DESCARGAR");
        enviarTexto(canal, isbn);

        String estado = recibirTexto(canal);

        if ("DESCARGA_DENEGADA".equals(estado)
                || "DESCARGA_ERROR".equals(estado)) {

            System.out.println(recibirTexto(canal));
            return;
        }

        if (!"DESCARGA_OK".equals(estado)) {
            throw new IOException("Respuesta de descarga no reconocida.");
        }

        int longitud;

        try {
            longitud = Integer.parseInt(recibirTexto(canal));
        } catch (NumberFormatException e) {
            throw new IOException("Longitud de descarga inválida.", e);
        }

        // El hexadecimal ocupa aproximadamente el doble del original.
        // Este límite cubre el formato actual y evita reservas descontroladas.
        if (longitud <= 0 || longitud > 3 * 1024 * 1024) {
            throw new IOException("El tamaño de descarga no está permitido.");
        }

        String hashEsperado = recibirTexto(canal);

        //Otra regex, para validar el formato del hash.
        if (!hashEsperado.matches("[0-9a-f]{64}")) {
            throw new IOException("El hash recibido tiene un formato inválido.");
        }

        // Recibimos las claves de libros mediante el canal cifrado.
        byte[] claveLibro = canal.recibir();
        byte[] claveHmacLibro = canal.recibir();

        if (claveLibro.length != 24 || claveHmacLibro.length != 32) {
            throw new IOException(
                    "Las claves recibidas tienen un tamaño inválido.");
        }

        //Creamos el archivo vacío.
        byte[] archivo = new byte[longitud];
        int recibidos = 0;

        while (recibidos < longitud) {
            byte[] parte = canal.recibir();

            if (parte.length == 0
                    || parte.length > 32 * 1024
                    || parte.length > longitud - recibidos) {

                throw new IOException("Parte de descarga inválida.");
            }

            System.arraycopy(
                    parte, 0, archivo, recibidos, parte.length);

            recibidos += parte.length;
        }

        String hashRecibido = Sha256.aHexadecimal(
                Sha256.calcular(archivo));

        if (!hashEsperado.equals(hashRecibido)) {
            throw new IOException(
                    "La descarga no coincide con el archivo enviado.");
        }

        // Guardamos primero en un archivo temporal.
        Path temporal = Files.createTempFile(carpeta, "libro-", ".part");

        try {
            Files.write(temporal, archivo);

            // Comprobamos también los bytes que quedaron escritos en disco.
            byte[] guardado = Files.readAllBytes(temporal);

            if (guardado.length != archivo.length) {
                throw new IOException("El archivo guardado está incompleto.");
            }

            for (int i = 0; i < archivo.length; i++) {
                if (guardado[i] != archivo[i]) {
                    throw new IOException(
                            "El archivo guardado difiere en el byte " + i + ".");
                }
            }

            // Sin REPLACE_EXISTING: no sobrescribimos otro archivo.
            Files.move(temporal, destino);

        } finally {
            // Retiramos el temporal si la escritura o el movimiento fallaron.
            Files.deleteIfExists(temporal);
        }

        System.out.println("Descarga verificada correctamente.");
        System.out.println("Bytes recibidos: " + archivo.length);
        System.out.println("Archivo: " + destino.toAbsolutePath());


        // Desciframos sin modificar el archivo cifrado que acabamos de guardar.
        byte[] original = ServicioLibros.descifrarArchivo(
                archivo, claveLibro, claveHmacLibro);

        Path destinoOriginal = carpeta.resolve(isbn + ".descifrado.txt");

        if (Files.exists(destinoOriginal)) {
            System.out.println(
                    "El libro se descifró, pero ya existe: "
                            + destinoOriginal.toAbsolutePath());
            return;
        }

            // Guardamos los bytes directamente para conservar el contenido exacto.
        Files.write(
                destinoOriginal,
                original,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);

        System.out.println("Libro descifrado correctamente.");
        System.out.println("Bytes recuperados: " + original.length);
        System.out.println(
                "Contenido guardado en: " + destinoOriginal.toAbsolutePath());


    }
}
