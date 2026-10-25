package org.example.seguridad;

/*
 * Deriva claves a partir de un secreto mediante HKDF-SHA-256.
 * Primero extrae una clave intermedia y después genera los bytes solicitados.
 * La información de contexto permite separar claves según su propósito.
 * Utiliza nuestra implementación de HMAC-SHA-256.
 */
public final class HkdfSha256 {

    private static final int TAMANO_HASH = 32;

    // HKDF permite generar como máximo 255 bloques del hash.
    private static final int MAXIMO_SALIDA = 255 * TAMANO_HASH;

    private HkdfSha256() {
        // Evitamos crear objetos de esta clase.
    }

    public static byte[] derivar(
            byte[] secreto,
            byte[] sal,
            byte[] contexto,
            int longitud) {

        if (secreto == null || contexto == null) {
            throw new IllegalArgumentException(
                    "El secreto y el contexto son obligatorios.");
        }

        if (longitud < 1 || longitud > MAXIMO_SALIDA) {
            throw new IllegalArgumentException(
                    "La longitud debe estar entre 1 y "
                            + MAXIMO_SALIDA + " bytes.");
        }

        // Reservamos espacio para el bloque anterior y el contador.
        if (contexto.length > Integer.MAX_VALUE - TAMANO_HASH - 1) {
            throw new IllegalArgumentException(
                    "El contexto supera el tamaño permitido.");
        }

        // RFC 5869 define una sal de ceros cuando no se proporciona una. --> estándar de la NIST.
        byte[] salEfectiva = sal == null
                ? new byte[TAMANO_HASH]
                : sal;

        // Extracción: el secreto es el mensaje y la sal es la clave HMAC.
        byte[] claveIntermedia = HmacSha256.calcular(
                salEfectiva, secreto);

        byte[] resultado = new byte[longitud];
        byte[] bloqueAnterior = new byte[0];

        int escritos = 0;
        int contador = 1;

        while (escritos < longitud) {
            // Cada bloque depende del anterior, del contexto y del contador.
            byte[] entrada = new byte[
                    bloqueAnterior.length + contexto.length + 1];

            System.arraycopy(
                    bloqueAnterior, 0,
                    entrada, 0,
                    bloqueAnterior.length);

            System.arraycopy(
                    contexto, 0,
                    entrada, bloqueAnterior.length,
                    contexto.length);

            // El contador ocupa un byte y comienza en uno.
            entrada[entrada.length - 1] = (byte) contador;

            bloqueAnterior = HmacSha256.calcular(
                    claveIntermedia, entrada);

            // El último bloque puede aportar menos de 32 bytes.
            int cantidad = Math.min(
                    TAMANO_HASH, longitud - escritos);

            System.arraycopy(
                    bloqueAnterior, 0,
                    resultado, escritos,
                    cantidad);

            escritos += cantidad;
            contador++;
        }

        return resultado;
    }
}
