package org.example.servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.example.modelo.Libro;
import org.example.modelo.Usuario;
import org.example.persistencia.RepositorioLibros;
import org.example.seguridad.CanalCifrado;
import org.example.seguridad.ClavesSesion;
import org.example.seguridad.IntercambioClaves;
import org.example.seguridad.Sha256;
import org.example.servicio.ServicioAutenticacion;
import org.example.servicio.ServicioLibros;
import org.example.servicio.ValidacionAcceso;



/*Runnable nos va a permitir representar a los clientes como tareas que
puede ejecutarse de manera concurrente en un hilo separado.*/

public class ManejadorCliente implements Runnable {

    private final Socket cliente;
    private final int tiempoEsperaLecturaMs;
    // Consulta las cuentas cargadas al iniciar el servidor.
    private final ServicioAutenticacion autenticacion;
    // Cada manejador conserva únicamente el usuario de su propia conexión.
    private Usuario usuarioAutenticado;
    // Catálogo cargado al iniciar el servidor.
    private final RepositorioLibros catalogo;
    // Recupera el contenido de los libros almacenados.
    private final ServicioLibros servicioLibros;

    public ManejadorCliente(Socket cliente, int tiempoEsperaLecturaMs,
                            ServicioAutenticacion autenticacion,
                            RepositorioLibros catalogo,
                            ServicioLibros servicioLibros){

        if (autenticacion == null) {
            throw new IllegalArgumentException(
                    "El servicio de autenticación es obligatorio.");
        }

        if (catalogo == null) {
            throw new IllegalArgumentException(
                    "El catálogo es obligatorio.");
        }

        if (servicioLibros == null) {
            throw new IllegalArgumentException(
                    "El servicio de libros es obligatorio.");
        }

        this.cliente = cliente;
        this.tiempoEsperaLecturaMs = tiempoEsperaLecturaMs;
        this.autenticacion = autenticacion;
        this.catalogo = catalogo;
        this.servicioLibros = servicioLibros;
    }

    // Atiende la conexión de un cliente en su propio hilo.
    @Override
    public void run() {
        try (cliente) {
            cliente.setSoTimeout(tiempoEsperaLecturaMs);

            System.out.println(
                    "Atendiendo a: " + cliente.getRemoteSocketAddress());

            // Recibe los datos del cliente.
            DataInputStream entrada =
                    new DataInputStream(cliente.getInputStream());

            // Envía los datos al cliente.
            DataOutputStream salida =
                    new DataOutputStream(cliente.getOutputStream());

            // Intercambia las claves públicas y calcula el secreto de esta conexión.
            BigInteger secretoCompartido =
                    IntercambioClaves.intercambiar(entrada, salida);

            System.out.println("Intercambio Diffie-Hellman completado.");

            // Confirma el secreto usando el papel de servidor.
            IntercambioClaves.confirmar(
                    entrada, salida, secretoCompartido, false);

            System.out.println("Secreto compartido confirmado.");

            // Deriva las claves de envío y recepción del servidor.
            ClavesSesion claves =
                    ClavesSesion.crear(secretoCompartido, false);

            // Este canal pertenece únicamente a esta conexión.
            CanalCifrado canal = new CanalCifrado(entrada, salida, claves);

            // Recibe el comando después de verificarlo y descifrarlo.
            String comando = recibirTexto(canal);

            if ("PING".equals(comando)) {
                // Permite seguir comprobando la conexión.
                enviarTexto(canal, "PONG");

            } else if ("LOGIN".equals(comando)) {
                // Recibe las credenciales y comprueba el acceso.
                atenderLogin(canal);

                //Solo iniciamos la sesion se las credenciales fueron correctas.
                if(usuarioAutenticado != null){
                    atenderSesion(canal);
                }

            } 
            else if ("REGISTRO".equals(comando)) {
                // Crear cuenta no requiere sesión: la conexión termina después.
                atenderRegistro(canal);

            }
            else {
                // Rechaza los comandos que todavía no reconoce.
                enviarTexto(canal, "ERROR_COMANDO");
            }
            

        } catch (IOException e) {
            // El socket se cierra automáticamente al salir del try.
            System.err.println(
                    "Error con el cliente "
                            + cliente.getRemoteSocketAddress()
                            + ": " + e.getMessage());
        }
    }

    private void atenderLogin(CanalCifrado canal) throws IOException {

        // Recibimos ambos campos antes de responder.
        String correo = recibirTexto(canal);
        String contrasena = recibirTexto(canal);

        // Comprobamos también en el servidor el formato del correo.
        String errorCorreo = ValidacionAcceso.validarCorreo(correo);

        if (errorCorreo != null) {
            enviarTexto(canal, "ERROR_CAMPO");
            enviarTexto(canal, errorCorreo);
            return;
        }

        // Conservamos la contraseña exactamente como fue recibida.
        String errorContrasena =
                ValidacionAcceso.validarContrasena(contrasena);

        if (errorContrasena != null) {
            enviarTexto(canal, "ERROR_CAMPO");
            enviarTexto(canal, errorContrasena);
            return;
        }

        try {
            // Consultamos las cuentas cargadas al iniciar el servidor.
            usuarioAutenticado = autenticacion.autenticar(
                    correo.trim(), contrasena);

        } catch (IllegalArgumentException e) {
            // Una credencial almacenada inválida es un problema del servidor.
            // No mostramos ni registramos contraseñas o hashes.
            System.err.println(
                    "No se pudo verificar una credencial almacenada.");

            enviarTexto(canal, "ERROR_SERVIDOR");
            enviarTexto(canal,
                    "No se pudo completar el inicio de sesión. Intenta más tarde.");
            return;
        }

        if (usuarioAutenticado == null) {
            // No revelamos si el correo está registrado.
            enviarTexto(canal, "ERROR_CREDENCIALES");
            enviarTexto(canal, "Correo o contraseña incorrectos.");
            return;
        }

        // Confirmamos el acceso y enviamos únicamente los datos del perfil.
        enviarTexto(canal, "LOGIN_OK");
        enviarTexto(canal, usuarioAutenticado.getNombre());
        enviarTexto(canal, usuarioAutenticado.getApellido());
        enviarTexto(canal,
                usuarioAutenticado.isPremium() ? "PREMIUM" : "NORMAL");

        System.out.println(
                "Inicio de sesión correcto desde: "
                        + cliente.getRemoteSocketAddress());
    }
        // Crea una cuenta nueva. Orden de los campos: nombre, apellido,
    // correo y contraseña. La cuenta nueva siempre es NORMAL (no premium).
    private void atenderRegistro(CanalCifrado canal) throws IOException {

        String nombre = recibirTexto(canal);
        String apellido = recibirTexto(canal);
        String correo = recibirTexto(canal);
        String contrasena = recibirTexto(canal);

        String error;

        try {
            error = autenticacion.registrar(
                    nombre, apellido, correo, contrasena);

        } catch (IOException e) {
            // No mostramos rutas ni detalles internos al cliente.
            System.err.println("No se pudo guardar la cuenta nueva: "
                    + e.getMessage());

            enviarTexto(canal, "ERROR_SERVIDOR");
            enviarTexto(canal,
                    "No se pudo crear la cuenta. Intenta más tarde.");
            return;
        }

        if (error != null) {
            enviarTexto(canal, "ERROR_CAMPO");
            enviarTexto(canal, error);
            return;
        }

        enviarTexto(canal, "REGISTRO_OK");

        System.out.println(
                "Cuenta nueva creada desde: "
                        + cliente.getRemoteSocketAddress());
    }

    // Cambia nombre y apellido del usuario de esta sesión.
    private void atenderActualizarPerfil(CanalCifrado canal)
            throws IOException {

        if (usuarioAutenticado == null) {
            throw new IOException("No existe una sesión autenticada.");
        }

        String nombre = recibirTexto(canal);
        String apellido = recibirTexto(canal);

        try {
            usuarioAutenticado = autenticacion.actualizarPerfil(
                    usuarioAutenticado, nombre, apellido);

        } catch (IllegalArgumentException e) {
            enviarTexto(canal, "ERROR_CAMPO");
            enviarTexto(canal, e.getMessage());
            return;

        } catch (IOException e) {
            System.err.println("No se pudo guardar el perfil: "
                    + e.getMessage());

            enviarTexto(canal, "ERROR_SERVIDOR");
            enviarTexto(canal,
                    "No se pudieron guardar los cambios. Intenta más tarde.");
            return;
        }

        enviarTexto(canal, "ACTUALIZAR_OK");
        enviarTexto(canal, usuarioAutenticado.getNombre());
        enviarTexto(canal, usuarioAutenticado.getApellido());
    }

    // Cambia la contraseña del usuario de esta sesión.
    private void atenderCambioContrasena(CanalCifrado canal)
            throws IOException {

        if (usuarioAutenticado == null) {
            throw new IOException("No existe una sesión autenticada.");
        }

        String actual = recibirTexto(canal);
        String nueva = recibirTexto(canal);

        try {
            Usuario actualizado = autenticacion.cambiarContrasena(
                    usuarioAutenticado, actual, nueva);

            if (actualizado == null) {
                enviarTexto(canal, "ERROR_CREDENCIALES");
                enviarTexto(canal, "La contraseña actual es incorrecta.");
                return;
            }

            usuarioAutenticado = actualizado;

        } catch (IllegalArgumentException e) {
            enviarTexto(canal, "ERROR_CAMPO");
            enviarTexto(canal, e.getMessage());
            return;

        } catch (IOException e) {
            System.err.println("No se pudo guardar la contraseña: "
                    + e.getMessage());

            enviarTexto(canal, "ERROR_SERVIDOR");
            enviarTexto(canal,
                    "No se pudieron guardar los cambios. Intenta más tarde.");
            return;
        }

        enviarTexto(canal, "CONTRASENA_OK");
    }

    private static String recibirTexto(CanalCifrado canal) throws IOException {
        // Convertimos a texto después de verificar y descifrar el mensaje.
        return new String(canal.recibir(), StandardCharsets.UTF_8);
    }

    private static void enviarTexto(
            CanalCifrado canal, String texto) throws IOException {

        // Cada texto se envía como un mensaje protegido independiente.
        canal.enviar(texto.getBytes(StandardCharsets.UTF_8));
    }

    //Enviamos el catalogo cuando el usuario lo solicite
    private void enviarCatalogo(CanalCifrado canal) throws IOException {
        // Esta operación requiere una sesión autenticada.
        if (usuarioAutenticado == null) {
            throw new IOException("No existe una sesión autenticada.");
        }

        enviarTexto(canal, "CATALOGO_OK");

        // Indicamos cuántos registros debe recibir el cliente.
        enviarTexto(canal, Integer.toString(catalogo.getCantidad()));

        for (int i = 0; i < catalogo.getCantidad(); i++) {
            Libro libro = catalogo.obtener(i);

            // Enviamos cada campo por separado, sin separadores ambiguos.
            enviarTexto(canal, libro.getIsbn());
            enviarTexto(canal, libro.getTitulo());
        }
    }

    //Experiencia del usuario dentro de la sesion, por eso la "atendemos"
    private void atenderSesion(CanalCifrado canal) throws IOException {
        // Damos tiempo para elegir una opción del menú.
        // Las operaciones de lectura siguen teniendo un límite.
        cliente.setSoTimeout(300_000);

        try {
            while (usuarioAutenticado != null) {
                String comando = recibirTexto(canal);

                switch (comando) {
                    case "PERFIL" -> {
                        // Consultamos el usuario autenticado en el servidor.
                        enviarTexto(canal, "PERFIL_OK");
                        enviarTexto(canal, usuarioAutenticado.getNombre());
                        enviarTexto(canal, usuarioAutenticado.getApellido());
                        enviarTexto(canal, usuarioAutenticado.getCorreoElectronico());
                        enviarTexto(canal,
                                usuarioAutenticado.isPremium()
                                        ? "PREMIUM" : "NORMAL");
                    }

                    case "CATALOGO" -> enviarCatalogo(canal);

                    case "LEER" -> enviarLectura(canal);

                    case "DESCARGAR" -> enviarDescarga(canal);
                    
                    case "ACTUALIZAR_PERFIL" -> atenderActualizarPerfil(canal);

                    case "CAMBIAR_CONTRASENA" -> atenderCambioContrasena(canal);

                    case "LOGOUT" -> {
                        // Eliminamos la sesión antes de confirmar su cierre.
                        usuarioAutenticado = null;
                        enviarTexto(canal, "LOGOUT_OK");
                        return;
                    }

                    case "SALIR" -> {
                        usuarioAutenticado = null;
                        enviarTexto(canal, "SALIR_OK");
                        return;
                    }

                    default -> enviarTexto(canal, "ERROR_COMANDO");
                }
            }
        } finally {
            // También limpiamos la sesión si hay una desconexión o un error.
            usuarioAutenticado = null;
        }
    }

    //Cuando el usuario solicita leer un libro.
    private void enviarLectura(CanalCifrado canal) throws IOException {
        if (usuarioAutenticado == null) {
            throw new IOException("No existe una sesión autenticada.");
        }

        // Recibimos el ISBN después del comando LEER.
        String isbn = recibirTexto(canal);
        byte[] contenido;

        try {
            // Completamos la lectura y verificación antes de anunciar éxito.
            contenido = servicioLibros.leerContenido(isbn);

        } catch (IOException e) {
            System.err.println("No se pudo leer un libro: " + e.getMessage());

            enviarTexto(canal, "LECTURA_ERROR");
            enviarTexto(canal,
                    "No se pudo abrir el libro. Revisa el ISBN y su disponibilidad.");
            return;
        }

        enviarTexto(canal, "LECTURA_OK");
        enviarTexto(canal, Integer.toString(contenido.length));

        // Cada parte viaja como un mensaje cifrado y autenticado.
        int posicion = 0;

        while (posicion < contenido.length) {
            int cantidad = Math.min(32 * 1024, contenido.length - posicion);
            byte[] parte = new byte[cantidad];

            System.arraycopy(contenido, posicion, parte, 0, cantidad);
            canal.enviar(parte);

            posicion += cantidad;
        }
    }

    //Los usuarios premium pueden descargar libros cifrados.
    private void enviarDescarga(CanalCifrado canal) throws IOException {
        if (usuarioAutenticado == null) {
            throw new IOException("No existe una sesión autenticada.");
        }

        // Consumimos el ISBN para mantener el orden del protocolo.
        String isbn = recibirTexto(canal);

        // El permiso se comprueba en el servidor, no solo en el menú.
        if (!usuarioAutenticado.isPremium()) {
            enviarTexto(canal, "DESCARGA_DENEGADA");
            enviarTexto(canal,
                    "La descarga está disponible únicamente para cuentas premium.");
            return;
        }

        byte[] archivo;

        try {
            archivo = servicioLibros.leerArchivoCifrado(isbn);

        } catch (IOException e) {
            System.err.println("Error al preparar la descarga: " + e.getMessage());

            enviarTexto(canal, "DESCARGA_ERROR");
            enviarTexto(canal,
                    "No se pudo descargar el libro. Revisa el ISBN y su disponibilidad.");
            return;
        }

        // Calculamos el hash sobre los bytes exactos que vamos a enviar.
        String hash = Sha256.aHexadecimal(Sha256.calcular(archivo));

        enviarTexto(canal, "DESCARGA_OK");
        enviarTexto(canal, Integer.toString(archivo.length));
        enviarTexto(canal, hash);

        // Esta sección solo se alcanza después de autorizar la descarga premium.
        // Las claves viajan protegidas por las claves de sesión del canal.
        canal.enviar(servicioLibros.obtenerClaveCifrado());
        canal.enviar(servicioLibros.obtenerClaveHmac());

        int posicion = 0;

        while (posicion < archivo.length) {
            int cantidad = Math.min(32 * 1024, archivo.length - posicion);
            byte[] parte = new byte[cantidad];

            System.arraycopy(archivo, posicion, parte, 0, cantidad);
            canal.enviar(parte);

            posicion += cantidad;
        }
    }

}
