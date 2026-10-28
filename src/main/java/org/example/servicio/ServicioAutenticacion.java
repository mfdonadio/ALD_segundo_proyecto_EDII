package org.example.servicio;

import org.example.modelo.Usuario;
import org.example.persistencia.RepositorioUsuarios;
import org.example.seguridad.Credenciales;
import java.io.IOException;


//Autenticacion de usuarios, en general jeje.
// Sirve tambien para el inicio de sesion, para validar credenciales y admitir el acceso.

public class ServicioAutenticacion {

    // Consulta las cuentas cargadas desde los archivos.
    private final RepositorioUsuarios repositorio;

    public ServicioAutenticacion(RepositorioUsuarios repositorio) {

        if (repositorio == null) {
            throw new IllegalArgumentException(
                    "El repositorio es obligatorio.");
        }

        this.repositorio = repositorio;
    }

    public Usuario autenticar(String correo, String contrasena) {

        // Validamos los campos antes de consultar las cuentas.
        if (ValidacionAcceso.validarCorreo(correo) != null
                || ValidacionAcceso.validarContrasena(contrasena) != null) {

            return null;
        }

        // Quitamos espacios exteriores del correo, nunca de la contraseña.
        Usuario usuario = repositorio.buscarPorCorreo(correo.trim());

        if (usuario == null) {
            return null;
        }

        // Comprobamos la contraseña contra la credencial almacenada.
        boolean coincide = Credenciales.verificar(
                contrasena, usuario.getHashContrasena());

        // Devolvemos la cuenta únicamente cuando la contraseña coincide.
        return coincide ? usuario : null;
    }
        // Crea una cuenta nueva (no premium). Devuelve un mensaje de error,
    // o null si la cuenta se creó y se guardó correctamente.
    public String registrar(
            String nombre,
            String apellido,
            String correo,
            String contrasena) throws IOException {

        String error = ValidacionAcceso.validarCorreo(correo);
        if (error == null) {
            error = ValidacionAcceso.validarNombre(nombre, "nombre");
        }
        if (error == null) {
            error = ValidacionAcceso.validarNombre(apellido, "apellido");
        }
        if (error == null) {
            error = ValidacionAcceso.validarContrasenaNueva(contrasena);
        }
        if (error != null) {
            return error;
        }

        // Evitamos calcular el hash si el correo ya existe.
        if (repositorio.buscarPorCorreo(correo.trim()) != null) {
            return "Ya existe una cuenta con ese correo.";
        }

        // PBKDF2 es lento a propósito, por eso se calcula fuera del
        // bloqueo del repositorio.
        String hash = Credenciales.crear(contrasena);

        try {
            repositorio.crear(
                    nombre.trim(), apellido.trim(), correo.trim(), hash);
        } catch (IllegalArgumentException e) {
            // Otro cliente creó el mismo correo mientras calculábamos.
            return "Ya existe una cuenta con ese correo.";
        }

        return null;
    }

    // Cambia nombre y apellido. Devuelve el usuario actualizado,
    // o lanza IllegalArgumentException con el mensaje para el cliente.
    public Usuario actualizarPerfil(
            Usuario actual,
            String nombre,
            String apellido) throws IOException {

        String error = ValidacionAcceso.validarNombre(nombre, "nombre");
        if (error == null) {
            error = ValidacionAcceso.validarNombre(apellido, "apellido");
        }
        if (error != null) {
            throw new IllegalArgumentException(error);
        }

        return repositorio.actualizarPerfil(
                actual.getCorreoElectronico(),
                nombre.trim(),
                apellido.trim());
    }

    // Cambia la contraseña tras comprobar la actual.
    // Devuelve null si la contraseña actual es incorrecta.
    public Usuario cambiarContrasena(
            Usuario actual,
            String contrasenaActual,
            String contrasenaNueva) throws IOException {

        String error = ValidacionAcceso.validarContrasena(contrasenaActual);
        if (error == null) {
            error = ValidacionAcceso.validarContrasenaNueva(contrasenaNueva);
        }
        if (error != null) {
            throw new IllegalArgumentException(error);
        }

        // Exigimos la contraseña actual aunque la sesión ya esté abierta.
        if (!Credenciales.verificar(
                contrasenaActual, actual.getHashContrasena())) {
            return null;
        }

        String nuevoHash = Credenciales.crear(contrasenaNueva);

        return repositorio.actualizarContrasena(
                actual.getCorreoElectronico(), nuevoHash);
    }
}
