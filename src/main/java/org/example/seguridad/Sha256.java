package org.example.seguridad;

/*ALGORITMO EXTRAIDO DE Stack Overflow, adaptado con codex, e interpretado y escrito por nosotros :)*/


public final class Sha256 {

    // Constantes de las 64 rondas, definidas por SHA-256.
    private static final int[] K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
            0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
            0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
            0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
            0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    // Esta clase tendrá métodos estáticos; no necesitamos crear objetos.
    private Sha256() {
    }

    private static byte[] prepararMensaje(byte[] mensaje) {

        if (mensaje == null) {
            throw new IllegalArgumentException(
                    "El mensaje no puede ser null.");
        }

        // Reserva un byte inicial de relleno y ocho para la longitud.
        long longitudNecesaria = (long) mensaje.length + 1 + 8;

        // Redondea hacia arriba hasta un múltiplo de 64 bytes.
        long longitudTotal = ((longitudNecesaria + 63) / 64) * 64;

        // El tamaño del arreglo debe poder representarse con un int.
        if (longitudTotal > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "El mensaje es demasiado grande para esta implementación.");
        }

        // Java inicializa las posiciones del arreglo con ceros.
        byte[] preparado = new byte[(int) longitudTotal];

        // Conserva los bytes originales sin modificarlos.
        for (int i = 0; i < mensaje.length; i++) {
            preparado[i] = mensaje[i];
        }

        // Añade el bit 1 seguido de siete bits 0: 10000000.
        preparado[mensaje.length] = (byte) 0x80;

        // SHA-256 guarda la longitud original en bits.
        long longitudBits = (long) mensaje.length * 8;

        // Escribe esa longitud en los últimos ocho bytes.
        // El byte más significativo queda primero.
        for (int i = 0; i < 8; i++) {
            preparado[preparado.length - 1 - i] =
                    (byte) (longitudBits >>> (8 * i));
        }

        return preparado;
    }

    // Gira los bits hacia la derecha.
    // Los bits que salen por la derecha regresan por la izquierda.
    //Propiedad del SHA-256
    private static int rotarDerecha(int valor, int posiciones) {
        return (valor >>> posiciones) | (valor << (32 - posiciones));
    }

    // Cada bit de x elige entre el bit correspondiente de y o de z.
    private static int elegir(int x, int y, int z) {
        return (x & y) ^ (~x & z);
    }

    // Cada bit del resultado toma el valor mayoritario entre x, y y z.
    private static int mayoria(int x, int y, int z) {
        return (x & y) ^ (x & z) ^ (y & z);
    }

    // Mezcla una palabra mediante tres rotaciones.
    private static int sigmaMayuscula0(int valor) {
        return rotarDerecha(valor, 2)
                ^ rotarDerecha(valor, 13)
                ^ rotarDerecha(valor, 22);
    }

    // Usa otras rotaciones para mezclar una palabra.
    private static int sigmaMayuscula1(int valor) {
        return rotarDerecha(valor, 6)
                ^ rotarDerecha(valor, 11)
                ^ rotarDerecha(valor, 25);
    }

    // Ayuda a generar las palabras usadas en las rondas.
    private static int sigmaMinuscula0(int valor) {
        return rotarDerecha(valor, 7)
                ^ rotarDerecha(valor, 18)
                ^ (valor >>> 3);
    }

    // Complementa la generación de palabras para las rondas.
    private static int sigmaMinuscula1(int valor) {
        return rotarDerecha(valor, 17)
                ^ rotarDerecha(valor, 19)
                ^ (valor >>> 10);
    }


    // Genera el hash SHA-256 para un mensaje.
    public static byte[] calcular(byte[] mensaje) {

        // Prepara el mensaje para procesarlo en bloques de 64 bytes.
        byte[] preparado = prepararMensaje(mensaje);

        // Estado inicial definido por SHA-256.
        // Es local para que cada cálculo sea independiente.
        int[] estado = {
                0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
                0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
        };

        // Cada bloque genera 64 palabras de 32 bits.
        int[] palabras = new int[64];

        for (int inicio = 0; inicio < preparado.length; inicio += 64) {

            // Convierte los 64 bytes del bloque en 16 palabras.
            for (int i = 0; i < 16; i++) {
                int posicion = inicio + i * 4;

                // Lee primero el byte más significativo.
                // & 0xff evita extender el signo de los bytes.
                palabras[i] =
                        ((preparado[posicion] & 0xff) << 24)
                                | ((preparado[posicion + 1] & 0xff) << 16)
                                | ((preparado[posicion + 2] & 0xff) << 8)
                                | (preparado[posicion + 3] & 0xff);
            }

            // Genera las otras 48 palabras a partir de las anteriores.
            for (int i = 16; i < 64; i++) {
                palabras[i] =
                        sigmaMinuscula1(palabras[i - 2])
                                + palabras[i - 7]
                                + sigmaMinuscula0(palabras[i - 15])
                                + palabras[i - 16];
            }

            // Copia el estado actual a las variables de trabajo.
            int a = estado[0];
            int b = estado[1];
            int c = estado[2];
            int d = estado[3];
            int e = estado[4];
            int f = estado[5];
            int g = estado[6];
            int h = estado[7];

            // Ejecuta las 64 rondas del bloque.
            for (int i = 0; i < 64; i++) {
                int temporal1 =
                        h
                                + sigmaMayuscula1(e)
                                + elegir(e, f, g)
                                + K[i]
                                + palabras[i];

                int temporal2 =
                        sigmaMayuscula0(a)
                                + mayoria(a, b, c);

                // Actualiza las variables respetando este orden.
                h = g;
                g = f;
                f = e;
                e = d + temporal1;
                d = c;
                c = b;
                b = a;
                a = temporal1 + temporal2;
            }

            // Acumula el resultado del bloque en el estado.
            estado[0] += a;
            estado[1] += b;
            estado[2] += c;
            estado[3] += d;
            estado[4] += e;
            estado[5] += f;
            estado[6] += g;
            estado[7] += h;
        }

        // Convierte las ocho palabras del estado en 32 bytes.
        byte[] resultado = new byte[32];

        for (int i = 0; i < estado.length; i++) {
            int posicion = i * 4;

            resultado[posicion] = (byte) (estado[i] >>> 24);
            resultado[posicion + 1] = (byte) (estado[i] >>> 16);
            resultado[posicion + 2] = (byte) (estado[i] >>> 8);
            resultado[posicion + 3] = (byte) estado[i];
        }

        return resultado;
    }

    //Covertimos el hash a hexadecimal
    public static String aHexadecimal(byte[] datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos no pueden ser null.");
        }

        // Cada grupo de cuatro bits corresponde a un dígito hexadecimal.
        char[] digitos = "0123456789abcdef".toCharArray();
        char[] resultado = new char[datos.length * 2];

        for (int i = 0; i < datos.length; i++) {
            // Interpreta el byte como un valor entre 0 y 255.
            int valor = datos[i] & 0xff;

            // Cada byte produce dos caracteres hexadecimales.
            resultado[i * 2] = digitos[valor >>> 4];
            resultado[i * 2 + 1] = digitos[valor & 0x0f];
        }

        return new String(resultado);
    }
}
