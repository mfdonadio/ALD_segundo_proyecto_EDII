package org.example.pruebas;

import org.example.seguridad.Credenciales;

public class PruebaCredenciales {

    public static void main(String[] args) {

        // Contraseña ficticia usada solamente en estas pruebas.
        String contrasena = "Prueba-EDII-2026!";

        // Mide el tiempo de creación de una credencial.
        long inicio = System.nanoTime();
        String primera = Credenciales.crear(contrasena);
        long duracionMs = (System.nanoTime() - inicio) / 1_000_000;

        System.out.println("Tiempo de creación: " + duracionMs + " ms");

        // La contraseña original debe ser aceptada.
        comprobar(
                Credenciales.verificar(contrasena, primera),
                "Contraseña correcta");

        // Una contraseña diferente debe ser rechazada.
        comprobar(
                !Credenciales.verificar("Otra-contrasena", primera),
                "Contraseña incorrecta");

        // Crea otra credencial para la misma contraseña.
        String segunda = Credenciales.crear(contrasena);

        // Comprueba que las sales generadas sean distintas.
        String[] partesPrimera = primera.split("\\$", -1);
        String[] partesSegunda = segunda.split("\\$", -1);

        comprobar(
                !partesPrimera[2].equals(partesSegunda[2]),
                "Sales distintas");

        // La segunda credencial también debe aceptar la contraseña.
        comprobar(
                Credenciales.verificar(contrasena, segunda),
                "Segunda credencial válida");

        // Comprueba que un formato incorrecto produzca un error.
        boolean formatoRechazado = false;

        try {
            Credenciales.verificar(contrasena, "HASH_PENDIENTE_MARCO");
        } catch (IllegalArgumentException e) {
            formatoRechazado = true;
        }

        comprobar(formatoRechazado, "Formato inválido rechazado");

        // Rechaza un costo inferior al permitido.
        comprobarRechazo(
                contrasena,
                primera.replace("$600000$", "$1$"),
                "Iteraciones insuficientes rechazadas");

        // Rechaza un costo que excede nuestro límite.
        comprobarRechazo(
                contrasena,
                primera.replace("$600000$", "$1000001$"),
                "Iteraciones excesivas rechazadas");

        // Conserva el tamaño de la sal, pero introduce un carácter inválido.
        String salInvalida = "z" + partesPrimera[2].substring(1);

        String credencialInvalida =
                partesPrimera[0] + "$"
                        + partesPrimera[1] + "$"
                        + salInvalida + "$"
                        + partesPrimera[3];

        comprobarRechazo(
                contrasena,
                credencialInvalida,
                "Hexadecimal inválido rechazado");

        System.out.println("Todas las pruebas de credenciales pasaron.");
    }

    private static void comprobar(boolean condicion, String nombre) {

        // Detiene la prueba si el comportamiento no es el esperado.
        if (!condicion) {
            throw new AssertionError("Falló: " + nombre);
        }

        System.out.println("OK: " + nombre);
    }

    private static void comprobarRechazo(
            String contrasena,
            String credencial,
            String nombre) {

        boolean rechazada = false;

        try {
            Credenciales.verificar(contrasena, credencial);
        } catch (IllegalArgumentException e) {
            // La excepción confirma que se rechazaron los datos inválidos.
            rechazada = true;
        }

        comprobar(rechazada, nombre);
    }
}