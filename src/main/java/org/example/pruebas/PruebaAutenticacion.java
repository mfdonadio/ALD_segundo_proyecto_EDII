package org.example.pruebas;

import org.example.modelo.Usuario;
import org.example.persistencia.RepositorioUsuarios;
import org.example.servicio.ServicioAutenticacion;

import java.io.IOException;
import java.nio.file.Path;

public class PruebaAutenticacion {

    public static void main(String[] args) {

        try {
            // Carga los usuarios y sus perfiles.
            RepositorioUsuarios repositorio = RepositorioUsuarios.cargar(
                    Path.of("datos", "usuarios.txt"),
                    Path.of("datos", "perfiles.txt"));

            ServicioAutenticacion servicio =
                    new ServicioAutenticacion(repositorio);

            // Comprueba el acceso con la contraseña de prueba de Marco.
            Usuario usuario = servicio.autenticar(
                    "marco@ejemplo.com", "Marco-Prueba-2026!");

            comprobar(
                    usuario != null
                            && usuario.getCorreoElectronico()
                            .equalsIgnoreCase("marco@ejemplo.com"),
                    "Acceso con contraseña correcta");

            // Comprueba que una contraseña incorrecta no permita entrar.
            comprobar(
                    servicio.autenticar(
                            "marco@ejemplo.com", "Incorrecta") == null,
                    "Contraseña incorrecta rechazada");

            // Comprueba que una cuenta inexistente no permita entrar.
            comprobar(
                    servicio.autenticar(
                            "noexiste@ejemplo.com", "Prueba") == null,
                    "Usuario inexistente rechazado");

            System.out.println(
                    "Todas las pruebas de autenticación local pasaron.");

        } catch (IOException | IllegalArgumentException e) {
            // Informa errores de archivos o credenciales mal formadas.
            System.err.println(
                    "No se pudo completar la prueba: " + e.getMessage());
        }
    }

    private static void comprobar(boolean condicion, String nombre) {

        if (!condicion) {
            throw new AssertionError("Falló: " + nombre);
        }

        System.out.println("OK: " + nombre);
    }
}