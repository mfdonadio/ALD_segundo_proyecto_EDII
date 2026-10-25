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

        // Rechaza datos incompletos antes de buscar la cuenta.
        if (correo == null || correo.isBlank()
                || contrasena == null || contrasena.isEmpty()
                || contrasena.length() > 256) {

            return null;
        }

        Usuario usuario = repositorio.buscarPorCorreo(correo);

        // No existe una cuenta con ese correo.
        if (usuario == null) {
            return null;
        }

        // Comprueba la contraseña contra la credencial almacenada.
        boolean coincide = Credenciales.verificar(
                contrasena, usuario.getHashContrasena());

        // Solo devuelve la cuenta cuando la contraseña es correcta.
        return coincide ? usuario : null;
    }
}
