package org.example.pruebas;

import org.example.seguridad.ClavesLibros;

import java.io.IOException;
import java.nio.file.Path;

/*
 * Prepara las claves de libros y comprueba que se conservan.
 * No muestra sus valores en la consola.
 */
public class PrepararClavesLibros {

    public static void main(String[] args) throws IOException {
        Path ruta = Path.of("datos", "claves", "libros.key");

        ClavesLibros primera = ClavesLibros.cargarOCrear(ruta);
        ClavesLibros segunda = ClavesLibros.cargarOCrear(ruta);

        comprobarIguales(
                primera.getCifrado(),
                segunda.getCifrado(),
                "La clave de cifrado se conserva");

        comprobarIguales(
                primera.getHmac(),
                segunda.getHmac(),
                "La clave HMAC se conserva");

        System.out.println(
                "Claves de libros disponibles en: " + ruta.toAbsolutePath());
    }

    private static void comprobarIguales(
            byte[] primera, byte[] segunda, String descripcion) {

        if (primera.length != segunda.length) {
            throw new AssertionError("Falló: " + descripcion);
        }

        for (int i = 0; i < primera.length; i++) {
            if (primera[i] != segunda[i]) {
                throw new AssertionError("Falló: " + descripcion);
            }
        }

        System.out.println("OK: " + descripcion);
    }
}