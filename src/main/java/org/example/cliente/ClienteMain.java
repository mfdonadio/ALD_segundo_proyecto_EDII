package org.example.cliente;

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
import java.nio.file.Files;
import java.nio.file.Path;

import org.example.compresion.Huffman;
import org.example.configuracion.ConfiguracionCliente;
import org.example.seguridad.CanalCifrado;
import org.example.seguridad.ClavesSesion;
import org.example.seguridad.IntercambioClaves;
import org.example.seguridad.Sha256;
import org.example.servicio.ServicioLibros;
import org.example.servicio.ValidacionAcceso;



public class ClienteMain {

    public static void main(String[] args) {
        Console consola = System.console();

        // Compartimos el mismo lector durante toda la ejecución.
        BufferedReader teclado = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));

        try {
            while (true) {
                System.out.println();
                imprimirEncabezado("BIBLIOTECA");
                imprimirOpcion("1", "Iniciar sesión");
                imprimirOpcion("2", "Leer archivo Huffman local (sin conexión)");
                imprimirOpcion("0", "Salir");
                imprimirSeparador();

                String opcion = leerLinea(
                        consola, teclado, "Elige una opción: ");

                if (opcion == null) {
                    return;
                }

                switch (opcion.trim()) {
                    case "1" -> {
                        // Solo esta opción necesita conectarse al servidor.
                        boolean finalizar = iniciarSesion(consola, teclado);

                        if (finalizar) {
                            return;
                        }
                    }

                    case "2" -> {
                        // Lee el archivo local sin abrir ningún socket.
                        leerHuffmanLocal(consola, teclado);
                    }

                    case "0" -> {
                        System.out.println("¡Hasta luego!");
                        return;
                    }

                    default -> imprimirError(
                            "Opción inválida. Escribe 1, 2 o 0.");
                }
            }

        } catch (IOException e) {
            imprimirError(
                    "No se pudo leer la entrada de consola: " + e.getMessage());
        }
    }

    private static boolean iniciarSesion(
            Console consola,
            BufferedReader teclado) throws IOException {

        final ConfiguracionCliente configuracion;

        try {
            // La configuración de red solo se carga para iniciar sesión.
            configuracion = ConfiguracionCliente.cargar(
                    Path.of("config", "cliente.txt"));

        } catch (IOException e) {
            imprimirError(
                    "No se pudo cargar la configuración: " + e.getMessage());

            // Permitimos regresar al menú y utilizar la lectura local.
            return false;
        }

        System.out.println();
        imprimirEncabezado("INICIAR SESIÓN");

        if (consola == null) {
            imprimirAviso(
                    "En esta consola la contraseña será visible al escribir.");
        }

        String correo = "";

        while (true) {
            String indicacion = correo.isEmpty()
                    ? "Correo: "
                    : "Correo [" + correo + "] (Enter para conservar): ";

            String escrito = leerLinea(consola, teclado, indicacion);

            if (escrito == null) {
                return true;
            }

            // Conservamos únicamente el último correo con formato válido.
            String candidato = escrito.isEmpty()
                    ? correo
                    : escrito.trim();

            String errorCorreo = ValidacionAcceso.validarCorreo(candidato);

            if (errorCorreo != null) {
                imprimirError(errorCorreo);
                continue;
            }

            correo = candidato;

            String contrasena;

            while (true) {
                contrasena = leerContrasena(consola, teclado);

                if (contrasena == null) {
                    return true;
                }

                String error =
                        ValidacionAcceso.validarContrasena(contrasena);

                if (error == null) {
                    break;
                }

                imprimirError(error);
            }

            // Conservamos el flujo de conexión y menú que ya tenemos.
            boolean finalizar = intentarAcceso(
                    configuracion, correo, contrasena, consola, teclado);

            if (finalizar) {
                return true;
            }

            imprimirSeparador();

            String repetir = leerLinea(
                    consola,
                    teclado,
                    "¿Reintentar inicio de sesión? (s = sí, n = volver al menú): ");

            if (repetir == null) {
                return true;
            }

            if (!"s".equalsIgnoreCase(repetir.trim())) {
                // Volvemos al menú inicial, donde sigue disponible la lectura local.
                return false;
            }
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
                imprimirOk("Sesión iniciada. ¡Bienvenido, "
                        + nombre + " " + apellido + "!");

                // Pasamos los datos del perfil para el encabezado del menú.
                return atenderMenu(
                        canal, consola, teclado, nombre, apellido, tipoCuenta);
            }

            if ("ERROR_CAMPO".equals(estado)
                    || "ERROR_CREDENCIALES".equals(estado)
                    || "ERROR_SERVIDOR".equals(estado)) {

                imprimirError(recibirTexto(canal));
                return false;
            }

            throw new IOException("Respuesta del servidor no reconocida.");

        } catch (IOException e) {
            imprimirError(
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

    // Ancho fijo del diseño de los menús en columnas.
    private static final int ANCHO = 60;

    // Construye una línea con un carácter repetido para los marcos.
    private static String repetir(char caracter, int veces) {
        StringBuilder linea = new StringBuilder();

        for (int i = 0; i < veces; i++) {
            linea.append(caracter);
        }

        return linea.toString();
    }

    // Imprime un encabezado centrado entre dos líneas de '='.
    private static void imprimirEncabezado(String titulo) {
        String marco = repetir('=', ANCHO);

        // Centramos el título dentro del ancho del marco.
        int espacios = (ANCHO - titulo.length()) / 2;
        String sangria = espacios > 0 ? repetir(' ', espacios) : "";

        System.out.println(marco);
        System.out.println(sangria + titulo);
        System.out.println(marco);
    }

    // Imprime una línea separadora de '-'.
    private static void imprimirSeparador() {
        System.out.println(repetir('-', ANCHO));
    }

    // Imprime una opción del menú con sangría de dos espacios.
    private static void imprimirOpcion(String numero, String texto) {
        System.out.println("  " + numero + ". " + texto);
    }

    // Imprime un mensaje de éxito con el prefijo uniforme.
    private static void imprimirOk(String mensaje) {
        System.out.println("[OK] " + mensaje);
    }

    // Imprime un mensaje de error con el prefijo uniforme.
    private static void imprimirError(String mensaje) {
        System.out.println("[Error] " + mensaje);
    }

    // Imprime un aviso con el prefijo uniforme.
    private static void imprimirAviso(String mensaje) {
        System.out.println("[Aviso] " + mensaje);
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
        imprimirSeparador();
        System.out.println("  CATÁLOGO DE LIBROS");
        imprimirSeparador();

        if (cantidad == 0) {
            System.out.println("  El catálogo está vacío.");
            imprimirSeparador();
            return;
        }

        // Encabezado de la tabla con columnas alineadas.
        System.out.printf("  %-3s %-15s %s%n", "#", "ISBN", "Título");
        System.out.println("  " + repetir('-', 3) + " "
                + repetir('-', 15) + " " + repetir('-', 30));

        for (int i = 0; i < cantidad; i++) {
            String isbn = recibirTexto(canal);
            String titulo = recibirTexto(canal);

            System.out.printf("  %-3d %-15s %s%n", i + 1, isbn, titulo);
        }

        imprimirSeparador();

        // Concuerda el singular y el plural según la cantidad.
        String palabra = cantidad == 1 ? "libro" : "libros";
        System.out.println("  Total: " + cantidad + " " + palabra);
    }

    //Menu con las opciones del cliente :)
    private static boolean atenderMenu(
            CanalCifrado canal,
            Console consola,
            BufferedReader teclado,
            String nombre,
            String apellido,
            String tipoCuenta) throws IOException {

        while (true) {
            System.out.println();
            // Encabezado con el usuario y el tipo de cuenta de la sesión.
            imprimirEncabezado(
                    nombre + " " + apellido + "  |  Cuenta: " + tipoCuenta);
            imprimirOpcion("1", "Ver mi perfil");
            imprimirOpcion("2", "Ver catálogo");
            imprimirOpcion("3", "Leer libro");
            imprimirOpcion("4", "Descargar libro cifrado (solo Premium)");
            imprimirOpcion("5", "Cerrar sesión");
            imprimirOpcion("0", "Salir");
            imprimirSeparador();

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
                    String perfilNombre = recibirTexto(canal);
                    String perfilApellido = recibirTexto(canal);
                    String perfilCorreo = recibirTexto(canal);
                    String perfilCuenta = recibirTexto(canal);

                    if (!"PREMIUM".equals(perfilCuenta)
                            && !"NORMAL".equals(perfilCuenta)) {
                        throw new IOException("Tipo de cuenta no reconocido.");
                    }

                    System.out.println();
                    imprimirSeparador();
                    System.out.println("  MI PERFIL");
                    imprimirSeparador();
                    System.out.println("  Nombre : "
                            + perfilNombre + " " + perfilApellido);
                    System.out.println("  Correo : " + perfilCorreo);
                    System.out.println("  Cuenta : " + perfilCuenta);
                    imprimirSeparador();
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

                    imprimirOk("Sesión cerrada.");

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

                default -> imprimirError(
                        "Opción inválida. Escribe 1, 2, 3, 4, 5 o 0.");
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
            imprimirError(
                    "El ISBN solo puede contener números, guiones o X.");
            return;
        }

        enviarTexto(canal, "LEER");
        enviarTexto(canal, isbn);

        String estado = recibirTexto(canal);

        if ("LECTURA_ERROR".equals(estado)) {
            imprimirError(recibirTexto(canal));
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
        imprimirEncabezado("LECTURA DEL LIBRO");
        System.out.print(texto);
        System.out.println();
        imprimirEncabezado("FIN DEL LIBRO");
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
            imprimirError(
                    "El ISBN solo puede contener números, guiones o X.");
            return;
        }

        Path carpeta = Path.of("descargas");


        Path destino = carpeta.resolve(isbn + ".txt");


        enviarTexto(canal, "DESCARGAR");
        enviarTexto(canal, isbn);

        String estado = recibirTexto(canal);

        if ("DESCARGA_DENEGADA".equals(estado)
                || "DESCARGA_ERROR".equals(estado)) {

            imprimirAviso(recibirTexto(canal));
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

        // Reservamos espacio para recibir todos los bytes del archivo.
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

        // La descarga ya terminó. Un error local no debe cerrar la sesión.
        try {
            // Guardamos el cifrado o verificamos que el existente sea idéntico.
            guardarArchivoVerificado(destino, archivo);

            imprimirOk("Descarga cifrada verificada:");
            System.out.println("     " + destino.toAbsolutePath());

            // Desciframos sin modificar el archivo cifrado que acabamos de guardar.
            byte[] original = ServicioLibros.descifrarArchivo(
                    archivo, claveLibro, claveHmacLibro);

            // Comprimimos los bytes originales del libro.
            byte[] comprimido = Huffman.comprimir(original);

            // Comprobamos que la compresión permita recuperar el contenido exacto.
            byte[] recuperado = Huffman.descomprimir(comprimido);

            if (original.length != recuperado.length) {
                throw new IOException(
                        "Huffman no recuperó la longitud original.");
            }

            for (int i = 0; i < original.length; i++) {
                if (original[i] != recuperado[i]) {
                    throw new IOException(
                            "Huffman no recuperó correctamente el byte " + i + ".");
                }
            }

            Path destinoHuffman = carpeta.resolve(isbn + ".huff");

            // Guardamos los bytes comprimidos o verificamos el archivo existente.
            guardarArchivoVerificado(destinoHuffman, comprimido);

            imprimirOk("Libro comprimido y verificado correctamente.");
            System.out.println("     Original : " + original.length + " bytes");
            System.out.println("     Huffman  : " + comprimido.length + " bytes");
            System.out.println("     Guardado : " + destinoHuffman.toAbsolutePath());

        } catch (IOException e) {
            // Volvemos al menú sin confundir un fallo local con uno de conexión.
            imprimirError(
                    "No se pudo completar el procesamiento local del libro: "
                            + e.getMessage());
        }
    }
    //Leemos un archivo comprimido con Huffman desde un archivo comprimido, sin necesidad de abrir el server.
    /*
     * Abre un archivo Huffman local y muestra su contenido.
     * No necesita una conexión con el servidor.
     * Un archivo inválido muestra un error y permite volver al menú.
     */
    private static void leerHuffmanLocal(
            Console consola,
            BufferedReader teclado) throws IOException {

        String rutaEscrita = leerLinea(
                consola,
                teclado,
                "Ruta del archivo .huff (Enter para cancelar): ");

        if (rutaEscrita == null || rutaEscrita.isBlank()) {
            return;
        }

        try {
            // Quitamos espacios alrededor de la ruta.
            String rutaLimpia = rutaEscrita.trim();

            // Windows puede copiar la ruta entre comillas.
            if (rutaLimpia.length() >= 2
                    && rutaLimpia.startsWith("\"")
                    && rutaLimpia.endsWith("\"")) {
                rutaLimpia = rutaLimpia.substring(1, rutaLimpia.length() - 1);
            }

            Path ruta = Path.of(rutaLimpia);

            // Cubre los archivos generados con nuestro límite de 1 MiB original.
            int maximoArchivo = 2 * 1024 * 1024;

            byte[] comprimido;

            // Leemos un byte adicional para detectar archivos demasiado grandes.
            try (var entrada = Files.newInputStream(ruta)) {
                comprimido = entrada.readNBytes(maximoArchivo + 1);
            }

            if (comprimido.length > maximoArchivo) {
                imprimirError(
                        "El archivo supera el tamaño admitido.");
                return;
            }

            // Huffman valida el formato antes de devolver los bytes recuperados.
            byte[] original = Huffman.descomprimir(comprimido);

            // Nuestros libros de texto están preparados en UTF-8.
            String texto = new String(original, StandardCharsets.UTF_8);

            System.out.println();
            imprimirEncabezado("LECTURA DEL ARCHIVO HUFFMAN");
            System.out.print(texto);
            System.out.println();
            imprimirEncabezado("FIN DEL LIBRO");

        } catch (IOException | IllegalArgumentException e) {
            // El error se atiende aquí para conservar el funcionamiento del menú.
            imprimirError(
                    "No se pudo abrir el archivo Huffman: " + e.getMessage());
        }
    }

    /*
     * Conserva un archivo existente si coincide exactamente con los datos.
     * Si no existe, lo escribe primero en un temporal.
     * Nunca sobrescribe un archivo diferente.
     */
    private static void guardarArchivoVerificado(
            Path destino, byte[] datos) throws IOException {

        if (Files.exists(destino)) {
            comprobarArchivo(destino, datos);

            imprimirOk("Archivo existente verificado:");
            System.out.println("     " + destino.toAbsolutePath());
            return;
        }

        Path carpeta = destino.toAbsolutePath().getParent();
        Files.createDirectories(carpeta);


        Path temporal = Files.createTempFile(carpeta, "biblioteca-", ".part");

        try {
            Files.write(temporal, datos);
            comprobarArchivo(temporal, datos);

            // Si otro archivo aparece en el destino, no lo reemplazamos.
            Files.move(temporal, destino);

        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static void comprobarArchivo(
            Path ruta, byte[] esperado) throws IOException {

        byte[] guardado;

        // Leemos un byte adicional para detectar una longitud mayor.
        try (var entrada = Files.newInputStream(ruta)) {
            guardado = entrada.readNBytes(esperado.length + 1);
        }

        if (guardado.length != esperado.length) {
            throw new IOException(
                    "El archivo existente tiene contenido diferente: " + ruta);
        }

        for (int i = 0; i < esperado.length; i++) {
            if (guardado[i] != esperado[i]) {
                throw new IOException(
                        "El archivo difiere en el byte " + i + ": " + ruta);
            }
        }
    }
}
