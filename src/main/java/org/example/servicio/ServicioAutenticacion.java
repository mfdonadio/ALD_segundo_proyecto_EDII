package org.example.servicio;

import org.example.modelo.Usuario;
import org.example.persistencia.RepositorioUsuarios;
import org.example.seguridad.Credenciales;


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
}
