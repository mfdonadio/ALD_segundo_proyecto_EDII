package org.example.pruebas;

import java.io.IOException;
import java.nio.file.Path;

import org.example.modelo.Usuario;
import org.example.persistencia.RepositorioUsuarios;

public class PruebaUsuarios {

    public static void main(String[] args) {

        try {
            // Carga las credenciales y los perfiles desde ambos archivos.
            RepositorioUsuarios repositorio = RepositorioUsuarios.cargar(
                    Path.of("datos", "usuarios.txt"),
                    Path.of("datos", "perfiles.txt"));

            System.out.println(
                    "Usuarios cargados: " + repositorio.getCantidad());

            // Comprueba que la búsqueda ignore mayúsculas y minúsculas.
            Usuario usuario = repositorio.buscarPorCorreo(
                    "MARCO@ejemplo.com");

            if (usuario == null) {
                // Sin el usuario de prueba no hay nada que verificar: la prueba falla.
                throw new AssertionError(
                        "No se encontró al usuario de prueba.");
            }

            // Muestra el perfil sin exponer el hash.
            System.out.println("¡Bienvenido, " + usuario.getNombre() + "!");
            System.out.println("Apellido: " + usuario.getApellido());
            System.out.println(
                    "Cuenta: " + (usuario.isPremium() ? "Premium" : "Normal"));

        } catch (IOException e) {
            // Informa los errores encontrados al cargar los archivos.
            System.err.println(
                    "No se pudieron cargar los usuarios: " + e.getMessage());
            // Una prueba que no pudo verificar nada debe fallar con código ≠ 0.
            System.exit(1);
        }
    }
}