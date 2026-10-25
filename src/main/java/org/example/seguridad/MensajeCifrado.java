package org.example.seguridad;

/*
 * Combina 3DES-CBC con HMAC-SHA-256 para cifrar y detectar modificaciones.
 * Conserva el IV (initialization vector) junto al contenido cifrado.
 * Verifica el HMAC antes de intentar descifrar.
 * Utiliza claves distintas para el cifrado y la autenticación.
 */
public final class MensajeCifrado {

    private static final int TAMANO_IV = 8;
    private static final int TAMANO_HMAC = 32;

    // Limitamos cada mensaje a 1 MiB de contenido original.
    private static final int MAXIMO_DATOS = 1024 * 1024;

    // Incluye IV, contenido, hasta ocho bytes de relleno y HMAC.
    private static final int MAXIMO_PAQUETE =
            TAMANO_IV + MAXIMO_DATOS + 8 + TAMANO_HMAC;

    private MensajeCifrado() {
        // Evitamos crear objetos de esta clase.
    }

    public static byte[] cifrar(
            byte[] datos, byte[] claveCifrado, byte[] claveHmac) {

        validarClaves(claveCifrado, claveHmac);

        if (datos == null || datos.length > MAXIMO_DATOS) {
            throw new IllegalArgumentException(
                    "Los datos deben contener como máximo 1 MiB.");
        }

        byte[] iv = TripleDesCbc.generarIv();
        byte[] cifrado = TripleDesCbc.cifrar(datos, claveCifrado, iv);

        // El HMAC protege tanto el IV como el contenido cifrado.
        byte[] contenido = new byte[TAMANO_IV + cifrado.length];

        System.arraycopy(iv, 0, contenido, 0, TAMANO_IV);
        System.arraycopy(
                cifrado, 0, contenido, TAMANO_IV, cifrado.length);

        byte[] etiqueta = HmacSha256.calcular(claveHmac, contenido);

        // Agregamos el HMAC al final del paquete.
        byte[] paquete = new byte[contenido.length + TAMANO_HMAC];

        System.arraycopy(contenido, 0, paquete, 0, contenido.length);
        System.arraycopy(
                etiqueta, 0, paquete, contenido.length, TAMANO_HMAC);

        return paquete;
    }

    public static byte[] descifrar(
            byte[] paquete, byte[] claveCifrado, byte[] claveHmac) {

        validarClaves(claveCifrado, claveHmac);

        // Hasta un mensaje vacío necesita un bloque cifrado.
        int minimoPaquete = TAMANO_IV + 8 + TAMANO_HMAC;

        if (paquete == null
                || paquete.length < minimoPaquete
                || paquete.length > MAXIMO_PAQUETE) {

            throw new IllegalArgumentException("Mensaje inválido.");
        }

        int longitudCifrado =
                paquete.length - TAMANO_IV - TAMANO_HMAC;

        if (longitudCifrado % 8 != 0) {
            throw new IllegalArgumentException("Mensaje inválido.");
        }

        // Separamos los bytes que deben estar protegidos por el HMAC.
        int longitudContenido = paquete.length - TAMANO_HMAC;
        byte[] contenido = new byte[longitudContenido];

        System.arraycopy(
                paquete, 0, contenido, 0, longitudContenido);

        byte[] esperado = HmacSha256.calcular(claveHmac, contenido);

        // Comparamos todos los bytes sin detenernos en la primera diferencia.
        int diferencia = 0;

        for (int i = 0; i < TAMANO_HMAC; i++) {
            diferencia |=
                    esperado[i] ^ paquete[longitudContenido + i];
        }

        if (diferencia != 0) {
            throw new IllegalArgumentException("Mensaje inválido.");
        }

        // Solo extraemos y desciframos después de verificar el HMAC.
        byte[] iv = new byte[TAMANO_IV];
        byte[] cifrado = new byte[longitudCifrado];

        System.arraycopy(contenido, 0, iv, 0, TAMANO_IV);
        System.arraycopy(
                contenido, TAMANO_IV, cifrado, 0, longitudCifrado);

        return TripleDesCbc.descifrar(cifrado, claveCifrado, iv);
    }

    private static void validarClaves(
            byte[] claveCifrado, byte[] claveHmac) {

        if (claveCifrado == null || claveCifrado.length != 24) {
            throw new IllegalArgumentException(
                    "La clave de cifrado debe contener 24 bytes.");
        }

        if (claveHmac == null || claveHmac.length != 32) {
            throw new IllegalArgumentException(
                    "La clave HMAC debe contener 32 bytes.");
        }
    }
}