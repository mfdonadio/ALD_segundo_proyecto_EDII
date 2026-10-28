package org.example.persistencia;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;

import org.example.modelo.Usuario;
import org.example.seguridad.Credenciales;

// Lee las credenciales y los perfiles, y los relaciona por correo.
public class RepositorioUsuarios {

    // Arreglo donde guardamos los usuarios cargados.
    private Usuario[] usuarios;

    // Cantidad de posiciones ocupadas.
    private int cantidad;
        // Archivos de donde se cargaron los datos y donde se vuelven a guardar.
    private Path rutaUsuarios;
    private Path rutaPerfiles;

    public RepositorioUsuarios() {
        usuarios = new Usuario[10];
        cantidad = 0;
    }

    public synchronized Usuario buscarPorCorreo(String correoElectronico) {

        // Rechaza una búsqueda sin correo.
        if (correoElectronico == null || correoElectronico.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo de búsqueda es obligatorio.");
        }

        String correoBuscado = correoElectronico.trim();

        // Revisa únicamente las posiciones ocupadas.
        for (int i = 0; i < cantidad; i++) {
            if (usuarios[i].getCorreoElectronico()
                    .equalsIgnoreCase(correoBuscado)) {

                return usuarios[i];
            }
        }

        // null indica que no encontramos al usuario.
        return null;
    }

    private void agregar(Usuario usuario) {

        // No permite guardar objetos vacíos.
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario es obligatorio.");
        }

        // Evita registrar dos usuarios con el mismo correo.
        if (buscarPorCorreo(usuario.getCorreoElectronico()) != null) {
            throw new IllegalArgumentException(
                    "Correo repetido: " + usuario.getCorreoElectronico());
        }

        // Amplía el arreglo cuando ya no quedan posiciones libres.
        if (cantidad == usuarios.length) {
            Usuario[] ampliado = new Usuario[usuarios.length * 2];

            // Copia las referencias al arreglo nuevo.
            for (int i = 0; i < cantidad; i++) {
                ampliado[i] = usuarios[i];
            }

            usuarios = ampliado;
        }

        // Guarda el usuario y actualiza la cantidad.
        usuarios[cantidad] = usuario;
        cantidad++;
    }

    public synchronized int getCantidad() {
        return cantidad;
    }
        // ===== Escritura de cuentas =====

    // Crea una cuenta nueva (siempre no premium) y la guarda en disco.
    public synchronized Usuario crear(
            String nombre,
            String apellido,
            String correo,
            String hashContrasena) throws IOException {

        Usuario nuevo = new Usuario(
                nombre, apellido, correo, hashContrasena, false);

        // agregar() rechaza correos repetidos.
        agregar(nuevo);

        try {
            guardar();
        } catch (IOException e) {
            // Si no se pudo escribir, deshacemos el cambio en memoria.
            cantidad--;
            usuarios[cantidad] = null;
            throw e;
        }

        return nuevo;
    }

    // Cambia nombre y apellido. El correo y el tipo de cuenta no cambian.
    public synchronized Usuario actualizarPerfil(
            String correo,
            String nombre,
            String apellido) throws IOException {

        int posicion = posicionDe(correo);
        Usuario anterior = usuarios[posicion];

        Usuario actualizado = new Usuario(
                nombre,
                apellido,
                anterior.getCorreoElectronico(),
                anterior.getHashContrasena(),
                anterior.isPremium());

        return reemplazar(posicion, anterior, actualizado);
    }

    // Cambia el hash de la contraseña. Recibe el hash ya calculado.
    public synchronized Usuario actualizarContrasena(
            String correo,
            String nuevoHash) throws IOException {

        int posicion = posicionDe(correo);
        Usuario anterior = usuarios[posicion];

        Usuario actualizado = new Usuario(
                anterior.getNombre(),
                anterior.getApellido(),
                anterior.getCorreoElectronico(),
                nuevoHash,
                anterior.isPremium());

        return reemplazar(posicion, anterior, actualizado);
    }

    private int posicionDe(String correo) {
        for (int i = 0; i < cantidad; i++) {
            if (usuarios[i].getCorreoElectronico()
                    .equalsIgnoreCase(correo.trim())) {
                return i;
            }
        }

        throw new IllegalArgumentException(
                "No existe una cuenta con ese correo.");
    }

    // Sustituye al usuario y guarda; si falla el guardado, restaura el anterior.
    private Usuario reemplazar(
            int posicion,
            Usuario anterior,
            Usuario actualizado) throws IOException {

        usuarios[posicion] = actualizado;

        try {
            guardar();
        } catch (IOException e) {
            usuarios[posicion] = anterior;
            throw e;
        }

        return actualizado;
    }

    // Reescribe usuarios.txt y perfiles.txt con el contenido actual.
    private void guardar() throws IOException {

        if (rutaUsuarios == null || rutaPerfiles == null) {
            throw new IOException(
                    "El repositorio no se cargó desde archivos.");
        }

        StringBuilder cuentas = new StringBuilder();
        StringBuilder perfiles = new StringBuilder();

        for (int i = 0; i < cantidad; i++) {
            Usuario u = usuarios[i];

            cuentas.append(u.getCorreoElectronico()).append(" | ")
                    .append(u.getHashContrasena()).append(" | ")
                    .append(u.isPremium()).append(System.lineSeparator());

            perfiles.append(u.getCorreoElectronico()).append(" | ")
                    .append(u.getNombre()).append(" | ")
                    .append(u.getApellido()).append(System.lineSeparator());
        }

        // Primero las cuentas: un usuario sin perfil solo genera una
        // advertencia al cargar, pero un perfil sin usuario es un error.
        escribirReemplazando(rutaUsuarios, cuentas.toString());
        escribirReemplazando(rutaPerfiles, perfiles.toString());
    }

    private static void escribirReemplazando(
            Path destino, String contenido) throws IOException {

        Path carpeta = destino.toAbsolutePath().getParent();
        Path temporal = Files.createTempFile(carpeta, "usuarios-", ".tmp");

        try {
            Files.writeString(temporal, contenido, StandardCharsets.UTF_8);

            try {
                Files.move(temporal, destino,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static String[] separarCampos(
            String linea,
            String archivo,
            int numeroLinea) throws IOException {

        // Separa por | y conserva los campos vacíos del final.
        String[] campos = linea.split("\\|", -1);

        // Cada registro debe contener exactamente tres campos.
        if (campos.length != 3) {
            throw new IOException(
                    archivo + ": línea " + numeroLinea
                            + ": se esperaban tres campos separados por '|'.");
        }

        // Elimina espacios externos y comprueba cada campo.
        for (int i = 0; i < campos.length; i++) {
            campos[i] = campos[i].trim();

            if (campos[i].isEmpty()) {
                throw new IOException(
                        archivo + ": línea " + numeroLinea
                                + ": el campo " + (i + 1) + " está vacío.");
            }
        }

        return campos;
    }

    // Guarda los datos personales antes de unirlos con las credenciales.
    private static class Perfil {

        private final String correo;
        private final String nombre;
        private final String apellido;

        private Perfil(String correo, String nombre, String apellido) {
            this.correo = correo;
            this.nombre = nombre;
            this.apellido = apellido;
        }
    }

    private static Perfil[] cargarPerfiles(Path ruta) throws IOException {

        // Guarda temporalmente los perfiles leídos.
        Perfil[] perfiles = new Perfil[10];
        int cantidadPerfiles = 0;

        // Lee en UTF-8 y cierra el archivo automáticamente.
        try (BufferedReader lector =
                     Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                // Omite líneas vacías sin alterar su numeración.
                if (linea.isBlank()) {
                    continue;
                }

                // Obtiene correo, nombre y apellido.
                String[] campos = separarCampos(
                        linea, ruta.toString(), numeroLinea);

                String correo = campos[0];

                // Aplica las mismas reglas de correo a ambos archivos.
                validarCorreo(correo, ruta, numeroLinea);

                // Impide asociar dos perfiles al mismo correo.
                for (int i = 0; i < cantidadPerfiles; i++) {
                    if (perfiles[i].correo.equalsIgnoreCase(correo)) {
                        throw new IOException(
                                ruta + ": línea " + numeroLinea
                                        + ": correo de perfil repetido: "
                                        + correo);
                    }
                }

                // Amplía el arreglo si se llenaron sus posiciones.
                if (cantidadPerfiles == perfiles.length) {
                    Perfil[] ampliado = new Perfil[perfiles.length * 2];

                    for (int i = 0; i < cantidadPerfiles; i++) {
                        ampliado[i] = perfiles[i];
                    }

                    perfiles = ampliado;
                }

                // Guarda los datos del perfil actual.
                perfiles[cantidadPerfiles] =
                        new Perfil(correo, campos[1], campos[2]);

                cantidadPerfiles++;
            }
        }

        // Devuelve un arreglo sin posiciones vacías al final.
        Perfil[] resultado = new Perfil[cantidadPerfiles];

        for (int i = 0; i < cantidadPerfiles; i++) {
            resultado[i] = perfiles[i];
        }

        return resultado;
    }

    public static RepositorioUsuarios cargar(
            Path rutaUsuarios,
            Path rutaPerfiles) throws IOException {

        // Carga los perfiles una sola vez.
        Perfil[] perfiles = cargarPerfiles(rutaPerfiles);

        // Construye el repositorio antes de entregarlo al programa.
        RepositorioUsuarios repositorio = new RepositorioUsuarios();
        repositorio.rutaUsuarios = rutaUsuarios;
        repositorio.rutaPerfiles = rutaPerfiles;

        try (BufferedReader lector =
                     Files.newBufferedReader(
                             rutaUsuarios, StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                // Omite líneas vacías.
                if (linea.isBlank()) {
                    continue;
                }

                // Obtiene correo, hash y estado premium.
                String[] campos = separarCampos(
                        linea, rutaUsuarios.toString(), numeroLinea);

                String correo = campos[0];
                String hash = campos[1];

                validarCorreo(correo, rutaUsuarios, numeroLinea);

                // Detecta un hash mal formado al cargar, no durante el login.
                try {
                    Credenciales.validarFormato(hash);
                } catch (IllegalArgumentException e) {
                    throw new IOException(
                            rutaUsuarios + ": línea " + numeroLinea
                                    + ": credencial inválida: "
                                    + e.getMessage());
                }

                // Evita registrar dos cuentas con el mismo correo.
                if (repositorio.buscarPorCorreo(correo) != null) {
                    throw new IOException(
                            rutaUsuarios + ": línea " + numeroLinea
                                    + ": correo repetido: " + correo);
                }

                // Solo acepta los valores exactos true y false.
                if (!campos[2].equals("true")
                        && !campos[2].equals("false")) {

                    throw new IOException(
                            rutaUsuarios + ": línea " + numeroLinea
                                    + ": premium debe ser true o false.");
                }

                boolean premium = campos[2].equals("true");

                // Busca el perfil correspondiente a esta cuenta.
                Perfil perfilEncontrado = null;

                for (int i = 0; i < perfiles.length; i++) {
                    if (perfiles[i].correo.equalsIgnoreCase(correo)) {
                        perfilEncontrado = perfiles[i];
                        break;
                    }
                }

                // Usa el correo como saludo si falta el perfil.
                String nombre = correo;
                String apellido = "";

                if (perfilEncontrado != null) {
                    nombre = perfilEncontrado.nombre;
                    apellido = perfilEncontrado.apellido;
                } else {
                    System.err.println(
                            "Advertencia: no hay perfil para " + correo
                                    + ". Se usará el correo para el saludo.");
                }

                // Une las credenciales y los datos personales.
                Usuario usuario = new Usuario(
                        nombre, apellido, correo, hash, premium);

                repositorio.agregar(usuario);
            }
        }

        // Rechaza perfiles que no pertenecen a ninguna cuenta.
        for (int i = 0; i < perfiles.length; i++) {
            if (repositorio.buscarPorCorreo(perfiles[i].correo) == null) {
                throw new IOException(
                        rutaPerfiles + ": perfil sin usuario asociado: "
                                + perfiles[i].correo);
            }
        }

        // Entrega el repositorio solo si toda la carga fue válida.
        return repositorio;
    }

    private static void validarCorreo(
            String correo,
            Path ruta,
            int numeroLinea) throws IOException {

        int arroba = correo.indexOf('@');

        // Exige una sola arroba, con contenido a ambos lados.
        if (arroba <= 0
                || arroba != correo.lastIndexOf('@')
                || arroba == correo.length() - 1) {

            throw new IOException(
                    ruta + ": línea " + numeroLinea
                            + ": formato de correo inválido: " + correo);
        }

        // Rechaza espacios y caracteres de control dentro del correo.
        for (int i = 0; i < correo.length(); i++) {
            char caracter = correo.charAt(i);

            if (Character.isWhitespace(caracter)
                    || Character.isSpaceChar(caracter)
                    || Character.isISOControl(caracter)) {

                throw new IOException(
                        ruta + ": línea " + numeroLinea
                                + ": el correo contiene espacios"
                                + " o caracteres de control.");
            }
        }
    }
}