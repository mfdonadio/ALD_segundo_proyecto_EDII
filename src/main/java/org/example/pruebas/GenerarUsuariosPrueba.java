package org.example.pruebas;

import org.example.seguridad.Credenciales;

public class GenerarUsuariosPrueba {

    public static void main(String[] args) {

        // Contraseñas ficticias, exclusivamente para las cuentas de prueba.
        String credencialMarco =
                Credenciales.crear("Marco-Prueba-2026!");

        String credencialEdwin =
                Credenciales.crear("Edwin-Prueba-2026!");

        // Muestra las filas listas para copiar a usuarios.txt.
        System.out.println(
                "marco@ejemplo.com | " + credencialMarco + " | true");

        System.out.println(
                "edwin@ejemplo.com | " + credencialEdwin+ " | false");
    }
}