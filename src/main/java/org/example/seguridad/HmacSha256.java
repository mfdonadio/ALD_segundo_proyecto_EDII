package org.example.seguridad;

//Algoritmo de Hash con Clave.
//Nace del SHA-256
/*La intención de implementarlo es, en primer lugar, conocer algoritmos nuevos y usados en la industria.
Segundo, garantizar la seguridad de nuestro programa y ponernos retos cada vez mejores.
Su implementación la sacamos igual que la de SHA-256, utilizamos un código de Stack Overflow, lo adaptamos con codex,
y por último lo implementamos manualmente :D.*/

public final class HmacSha256 {
    // SHA-256 procesa bloques de 64 bytes.
    private static final int TAMANO_BLOQUE = 64;

    private HmacSha256() {
    }

    public static byte[] calcular(byte[] clave, byte[] mensaje) {

        if (clave == null || mensaje == null) {
            throw new IllegalArgumentException(
                    "La clave y el mensaje no pueden ser null.");
        }

        // Evita desbordar el tamaño al reservar el arreglo interior.
        if (mensaje.length > Integer.MAX_VALUE - TAMANO_BLOQUE) {
            throw new IllegalArgumentException(
                    "El mensaje es demasiado grande.");
        }

        // Las claves mayores que el bloque se reducen usando SHA-256.
        byte[] claveBase = clave;

        if (clave.length > TAMANO_BLOQUE) {
            claveBase = Sha256.calcular(clave);
        }

        // Completa la clave con ceros hasta alcanzar 64 bytes.
        byte[] claveBloque = new byte[TAMANO_BLOQUE];

        for (int i = 0; i < claveBase.length; i++) {
            claveBloque[i] = claveBase[i];
        }

        // Prepara la clave interior seguida del mensaje.
        byte[] interior = new byte[TAMANO_BLOQUE + mensaje.length];

        for (int i = 0; i < TAMANO_BLOQUE; i++) {
            interior[i] = (byte) (claveBloque[i] ^ 0x36);
        }

        for (int i = 0; i < mensaje.length; i++) {
            interior[TAMANO_BLOQUE + i] = mensaje[i];
        }

        // Calcula el primer hash.
        byte[] hashInterior = Sha256.calcular(interior);

        // Prepara la clave exterior seguida del primer hash.
        byte[] exterior =
                new byte[TAMANO_BLOQUE + hashInterior.length];

        for (int i = 0; i < TAMANO_BLOQUE; i++) {
            exterior[i] = (byte) (claveBloque[i] ^ 0x5c);
        }

        for (int i = 0; i < hashInterior.length; i++) {
            exterior[TAMANO_BLOQUE + i] = hashInterior[i];
        }

        // El segundo hash es el resultado de HMAC.
        return Sha256.calcular(exterior);
    }
}
