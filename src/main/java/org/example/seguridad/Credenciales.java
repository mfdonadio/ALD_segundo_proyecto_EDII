package org.example.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public final class Credenciales {

    // Fuente de aleatoriedad para generar sales.
    private static final SecureRandom ALEATORIO = new SecureRandom();

    // Cada credencial tendrá una sal de 16 bytes.
    private static final int LONGITUD_SAL = 16;

    // Identifica el formato de nuestras credenciales.
    private static final String ALGORITMO = "pbkdf2-sha256";

    // Cantidad de iteraciones para crear nuevas credenciales.
    private static final int ITERACIONES = 600_000;

    // Límite propio para rechazar archivos con un costo excesivo.
    private static final int MAX_ITERACIONES = 1_000_000;

    // Nuestro PBKDF2 genera 32 bytes.
    private static final int LONGITUD_RESULTADO = 32;

    private Credenciales() {
    }

    private static byte[] generarSal() {

        byte[] sal = new byte[LONGITUD_SAL];

        // Llena el arreglo con bytes aleatorios.
        ALEATORIO.nextBytes(sal);

        return sal;
    }

    // Convierte un valor hexadecimal a un byte.
    private static byte[] desdeHexadecimal(String texto) {

        // Cada byte debe estar representado por dos caracteres.
        if (texto == null || texto.length() % 2 != 0) {
            throw new IllegalArgumentException(
                    "El hexadecimal debe tener una longitud par.");
        }

        byte[] resultado = new byte[texto.length() / 2];

        for (int i = 0; i < resultado.length; i++) {
            int posicion = i * 2;

            // Convierte cada carácter a un valor entre 0 y 15.
            int alto = valorHexadecimal(texto.charAt(posicion));
            int bajo = valorHexadecimal(texto.charAt(posicion + 1));

            // Une los dos grupos de cuatro bits.
            resultado[i] = (byte) ((alto << 4) | bajo);
        }

        return resultado;
    }

    // Convierte un carácter hexadecimal a un valor entre 0 y 15.
    private static int valorHexadecimal(char caracter) {

        if (caracter >= '0' && caracter <= '9') {
            return caracter - '0';
        }

        if (caracter >= 'a' && caracter <= 'f') {
            return caracter - 'a' + 10;
        }

        if (caracter >= 'A' && caracter <= 'F') {
            return caracter - 'A' + 10;
        }

        // Rechaza cualquier carácter que no sea hexadecimal.
        throw new IllegalArgumentException(
                "La credencial contiene un carácter hexadecimal inválido.");
    }

    public static String crear(String contrasena) {

        // Convierte la contraseña sin quitar espacios ni cambiar su contenido.
        byte[] contrasenaBytes = prepararContrasena(contrasena);

        // Cada nueva credencial recibe una sal aleatoria.
        byte[] sal = generarSal();

        byte[] resultado = Pbkdf2Sha256.derivar(
                contrasenaBytes, sal, ITERACIONES);

        // Guarda toda la información necesaria para verificar después.
        return ALGORITMO
                + "$" + ITERACIONES
                + "$" + Sha256.aHexadecimal(sal)
                + "$" + Sha256.aHexadecimal(resultado);
    }

    // Comprueba que la credencial almacenada tenga un formato válido.
    public static void validarFormato(String credencial) {

        // Limita el tamaño antes de procesar el texto almacenado.
        if (credencial == null || credencial.length() > 128) {
            throw new IllegalArgumentException(
                    "La credencial almacenada tiene un tamaño inválido.");
        }

        // Conserva campos vacíos para poder detectarlos.
        String[] partes = credencial.split("\\$", -1);

        if (partes.length != 4 || !partes[0].equals(ALGORITMO)) {
            throw new IllegalArgumentException(
                    "Formato o algoritmo de credencial inválido.");
        }

        // Exige una cantidad de iteraciones escrita con dígitos.
        if (partes[1].isEmpty()) {
            throw new IllegalArgumentException(
                    "Falta la cantidad de iteraciones.");
        }

        for (int i = 0; i < partes[1].length(); i++) {
            char caracter = partes[1].charAt(i);

            if (caracter < '0' || caracter > '9') {
                throw new IllegalArgumentException(
                        "Las iteraciones deben contener solamente dígitos.");
            }
        }

        final int iteraciones;

        try {
            iteraciones = Integer.parseInt(partes[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cantidad de iteraciones inválida.", e);
        }

        // Valida el costo antes de ejecutar PBKDF2.
        if (iteraciones < ITERACIONES
                || iteraciones > MAX_ITERACIONES) {

            throw new IllegalArgumentException(
                    "Iteraciones fuera del rango permitido.");
        }

        // Cada byte necesita dos caracteres hexadecimales.
        if (partes[2].length() != LONGITUD_SAL * 2
                || partes[3].length() != LONGITUD_RESULTADO * 2) {

            throw new IllegalArgumentException(
                    "Longitud de sal o resultado inválida.");
        }

        // Comprueba que la sal y el resultado sean hexadecimal válido.
        desdeHexadecimal(partes[2]);
        desdeHexadecimal(partes[3]);
    }

    // Verifica que la credencial sea válida.
    public static boolean verificar(
            String contrasena,
            String credencial) {

        byte[] contrasenaBytes = prepararContrasena(contrasena);

        // Reutiliza el mismo análisis de formato usado al cargar.
        validarFormato(credencial);

        String[] partes = credencial.split("\\$", -1);
        int iteraciones = Integer.parseInt(partes[1]);

        // Recupera los bytes almacenados.
        byte[] sal = desdeHexadecimal(partes[2]);
        byte[] esperado = desdeHexadecimal(partes[3]);

        // Repite el cálculo con la contraseña recibida.
        byte[] obtenido = Pbkdf2Sha256.derivar(
                contrasenaBytes, sal, iteraciones);

        // Revisa todos los bytes sin salir al encontrar una diferencia.
        int diferencias = 0;

        for (int i = 0; i < LONGITUD_RESULTADO; i++) {
            diferencias |= obtenido[i] ^ esperado[i];
        }

        return diferencias == 0;
    }

    // Prepara la contraseña para su uso en el cálculo de la credencial.
    private static byte[] prepararContrasena(String contrasena) {

        // Este límite propio evita entradas desproporcionadas.
        if (contrasena == null
                || contrasena.isEmpty()
                || contrasena.length() > 256) {

            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 1 y 256 caracteres Java.");
        }

        // Los espacios y las mayúsculas forman parte de la contraseña.
        return contrasena.getBytes(StandardCharsets.UTF_8);
    }
}