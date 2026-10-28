package org.example.servicio;

/*
 * Valida los campos del inicio de sesión.
 * Devuelve un mensaje cuando encuentra un error y null si el campo es válido.
 * No comprueba si la cuenta existe ni si la contraseña es correcta.
 */
public final class ValidacionAcceso {

    private ValidacionAcceso() {
        // Evitamos crear objetos de esta clase.
    }

    //Validamos el correo: su longitud, que contenga @, no permitimos espacios ni caracteres de control.
    public static String validarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return "Ingresa tu correo electrónico.";
        }

        // Ignoramos los espacios exteriores del correo.
        String limpio = correo.trim();

        if (limpio.length() > 254) {
            return "El correo no puede superar los 254 caracteres.";
        }

        // Rechazamos espacios y caracteres de control dentro del correo.
        for (int i = 0; i < limpio.length(); i++) {
            char caracter = limpio.charAt(i);

            if (Character.isWhitespace(caracter)
                    || Character.isSpaceChar(caracter)
                    || Character.isISOControl(caracter)) {

                return "El correo no debe contener espacios "
                        + "ni caracteres de control.";
            }
        }

        int arroba = limpio.indexOf('@');

        // Debe existir una sola arroba y contenido a ambos lados.
        if (arroba <= 0
                || arroba != limpio.lastIndexOf('@')
                || arroba == limpio.length() - 1) {

            return "Revisa el correo: debe incluir un usuario "
                    + "y un dominio separados por @.";
        }

        return null;
    }

    //Validamos la contraseña: longitud, no permitimos que no exista contraseña.
    public static String validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.isEmpty()) {
            return "Ingresa tu contraseña.";
        }

        if (contrasena.length() > 256) {
            return "La contraseña no puede superar los 256 caracteres.";
        }

        // Conservamos los espacios porque pueden formar parte de la contraseña.
        return null;
    }
        // Longitud mínima exigida al crear o cambiar una contraseña.
    private static final int MINIMO_CONTRASENA_NUEVA = 8;

    // Las cuentas nuevas piden una contraseña con un mínimo de seguridad.
    // El inicio de sesión no usa esta regla para no bloquear cuentas viejas.
    public static String validarContrasenaNueva(String contrasena) {
        String error = validarContrasena(contrasena);

        if (error != null) {
            return error;
        }

        if (contrasena.length() < MINIMO_CONTRASENA_NUEVA) {
            return "La contraseña nueva debe tener al menos "
                    + MINIMO_CONTRASENA_NUEVA + " caracteres.";
        }

        return null;
    }

    // Valida un nombre o apellido que se guardará en perfiles.txt.
    // Rechazamos '|' porque es el separador de campos del archivo.
    public static String validarNombre(String texto, String etiqueta) {
        if (texto == null || texto.isBlank()) {
            return "Ingresa tu " + etiqueta + ".";
        }

        String limpio = texto.trim();

        if (limpio.length() > 60) {
            return "El " + etiqueta + " no puede superar los 60 caracteres.";
        }

        for (int i = 0; i < limpio.length(); i++) {
            char caracter = limpio.charAt(i);

            if (caracter == '|' || Character.isISOControl(caracter)) {
                return "El " + etiqueta
                        + " no puede contener '|' ni caracteres de control.";
            }
        }

        return null;
    }
}
