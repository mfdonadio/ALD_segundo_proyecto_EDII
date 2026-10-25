package org.example.seguridad;

public class Pbkdf2Sha256 {

    private Pbkdf2Sha256() {}

    //Deriva un resultado de exactamente 32 bytes.
    public static byte[] derivar(
            byte[] contrasena,
            byte[] sal,
            int iteraciones){
        if (contrasena == null || sal == null){
            throw new IllegalArgumentException(
                    "La contraseña y la sal no pueden ser null.");
        }
        if (iteraciones < 1) {
            throw new IllegalArgumentException(
                    "Las iteraciones deben ser positivas.");
        }

        // Evita desbordar el tamaño al añadir cuatro bytes.
        if (sal.length > Integer.MAX_VALUE - 4) {
            throw new IllegalArgumentException(
                    "La sal es demasiado grande.");
        }

        // Prepara la sal seguida del número de bloque.
        byte[] entradaInicial = new byte[sal.length + 4];

        for (int i = 0; i < sal.length; i++) {
            entradaInicial[i] = sal[i];
        }

        // Para generar 32 bytes basta el bloque 1: 00 00 00 01.
        entradaInicial[entradaInicial.length - 1] = 1;

        // Calcula U1 usando la contraseña como clave.
        byte[] u = HmacSha256.calcular(contrasena, entradaInicial);

        // Copia U1 para iniciar el resultado acumulado.
        byte[] resultado = new byte[32];

        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = u[i];
        }

        // La primera iteración ya se calculó.
        for (int ronda = 1; ronda < iteraciones; ronda++) {

            // Calcula el siguiente valor a partir del anterior.
            u = HmacSha256.calcular(contrasena, u);

            // Combina cada valor con el resultado mediante XOR.
            for (int i = 0; i < resultado.length; i++) {
                resultado[i] = (byte) (resultado[i] ^ u[i]);
            }
        }

        return resultado;
    }
}
