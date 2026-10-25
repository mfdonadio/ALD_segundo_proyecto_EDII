package org.example.pruebas;

import org.example.seguridad.MensajeCifrado;

public class PruebaMensajeCifrado {

    public static void main(String[] args) {
        // Estas claves son fijas y se utilizan únicamente en las pruebas.
        byte[] claveCifrado = new byte[24];
        byte[] claveHmac = new byte[32];

        for (int i = 0; i < claveCifrado.length; i++) {
            claveCifrado[i] = (byte) (i * 7 + 3);
        }

        for (int i = 0; i < claveHmac.length; i++) {
            claveHmac[i] = (byte) (i * 11 + 5);
        }

        // Incluimos valores negativos y un cero final.
        byte[] original = {10, 20, -1, -128, 50, 0};

        byte[] paquete = MensajeCifrado.cifrar(
                original, claveCifrado, claveHmac);

        byte[] recuperado = MensajeCifrado.descifrar(
                paquete, claveCifrado, claveHmac);

        comprobar(
                iguales(original, recuperado),
                "Recuperación exacta del mensaje");

        byte[] paqueteVacio = MensajeCifrado.cifrar(
                new byte[0], claveCifrado, claveHmac);

        comprobar(
                MensajeCifrado.descifrar(
                        paqueteVacio, claveCifrado, claveHmac).length == 0,
                "Recuperación del mensaje vacío");

        // Cambiamos un bit del IV.
        byte[] ivModificado = paquete.clone();
        ivModificado[0] ^= 1;

        comprobarRechazo(
                ivModificado, claveCifrado, claveHmac,
                "IV modificado rechazado");

        // El contenido cifrado comienza después de los ocho bytes del IV.
        byte[] cifradoModificado = paquete.clone();
        cifradoModificado[8] ^= 1;

        comprobarRechazo(
                cifradoModificado, claveCifrado, claveHmac,
                "Contenido cifrado modificado rechazado");

        // El último byte pertenece al HMAC.
        byte[] hmacModificado = paquete.clone();
        hmacModificado[hmacModificado.length - 1] ^= 1;

        comprobarRechazo(
                hmacModificado, claveCifrado, claveHmac,
                "HMAC modificado rechazado");

        // Una clave HMAC diferente no debe aceptar el mensaje.
        byte[] otraClaveHmac = claveHmac.clone();
        otraClaveHmac[0] ^= 1;

        comprobarRechazo(
                paquete, claveCifrado, otraClaveHmac,
                "Clave HMAC incorrecta rechazada");

        // Quitamos un byte para simular una transmisión incompleta.
        byte[] truncado = new byte[paquete.length - 1];
        System.arraycopy(paquete, 0, truncado, 0, truncado.length);

        comprobarRechazo(
                truncado, claveCifrado, claveHmac,
                "Paquete incompleto rechazado");

        comprobarRechazo(
                new byte[0], claveCifrado, claveHmac,
                "Paquete vacío rechazado");

        // Comprobamos el límite antes de intentar procesar el contenido.
        comprobarRechazo(
                new byte[1024 * 1024 + 49], claveCifrado, claveHmac,
                "Paquete demasiado grande rechazado");

        System.out.println(
                "Todas las pruebas de mensajes cifrados pasaron.");
    }

    private static void comprobarRechazo(
            byte[] paquete,
            byte[] claveCifrado,
            byte[] claveHmac,
            String descripcion) {

        boolean rechazado = false;

        try {
            MensajeCifrado.descifrar(
                    paquete, claveCifrado, claveHmac);
        } catch (IllegalArgumentException e) {
            // Esperamos que los mensajes inválidos sean rechazados.
            rechazado = true;
        }

        comprobar(rechazado, descripcion);
    }

    private static boolean iguales(byte[] primero, byte[] segundo) {
        if (primero.length != segundo.length) {
            return false;
        }

        // Comparamos los bytes directamente para conservar cualquier contenido.
        for (int i = 0; i < primero.length; i++) {
            if (primero[i] != segundo[i]) {
                return false;
            }
        }

        return true;
    }

    private static void comprobar(boolean condicion, String descripcion) {
        if (!condicion) {
            throw new AssertionError("Falló: " + descripcion);
        }

        System.out.println("OK: " + descripcion);
    }
}
