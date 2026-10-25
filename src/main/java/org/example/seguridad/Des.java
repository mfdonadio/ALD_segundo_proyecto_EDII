package org.example.seguridad;

/*ALGORITMO TOMADO DE DIVERSAS FUENTES, optimizado y estructurado con codex e implementado a mano*/
//Documentacion oficial: https://csrc.nist.gov/files/pubs/fips/46-3/final/docs/fips46-3.pdf
//YYYY esta base es necesaria para 3DES, que es el algoritmo que nos solicitaron.
public final class Des {

    // Orden de los bits al comenzar el procesamiento del bloque.
    private static final int[] IP = {
            58, 50, 42, 34, 26, 18, 10, 2,
            60, 52, 44, 36, 28, 20, 12, 4,
            62, 54, 46, 38, 30, 22, 14, 6,
            64, 56, 48, 40, 32, 24, 16, 8,
            57, 49, 41, 33, 25, 17, 9, 1,
            59, 51, 43, 35, 27, 19, 11, 3,
            61, 53, 45, 37, 29, 21, 13, 5,
            63, 55, 47, 39, 31, 23, 15, 7
    };

    // Permutación inversa de IP, aplicada después de las rondas.
    private static final int[] FP = {
            40, 8, 48, 16, 56, 24, 64, 32,
            39, 7, 47, 15, 55, 23, 63, 31,
            38, 6, 46, 14, 54, 22, 62, 30,
            37, 5, 45, 13, 53, 21, 61, 29,
            36, 4, 44, 12, 52, 20, 60, 28,
            35, 3, 43, 11, 51, 19, 59, 27,
            34, 2, 42, 10, 50, 18, 58, 26,
            33, 1, 41, 9, 49, 17, 57, 25
    };

    // Selecciona 56 bits de la clave y excluye los ocho bits de paridad.
    private static final int[] PC1 = {
            57, 49, 41, 33, 25, 17, 9,
            1, 58, 50, 42, 34, 26, 18,
            10, 2, 59, 51, 43, 35, 27,
            19, 11, 3, 60, 52, 44, 36,
            63, 55, 47, 39, 31, 23, 15,
            7, 62, 54, 46, 38, 30, 22,
            14, 6, 61, 53, 45, 37, 29,
            21, 13, 5, 28, 20, 12, 4
    };

    // Selecciona los 48 bits de cada subclave.
    private static final int[] PC2 = {
            14, 17, 11, 24, 1, 5,
            3, 28, 15, 6, 21, 10,
            23, 19, 12, 4, 26, 8,
            16, 7, 27, 20, 13, 2,
            41, 52, 31, 37, 47, 55,
            30, 40, 51, 45, 33, 48,
            44, 49, 39, 56, 34, 53,
            46, 42, 50, 36, 29, 32
    };

    // Cantidad de posiciones que rotamos en cada ronda.
    private static final int[] ROTACIONES = {
            1, 1, 2, 2, 2, 2, 2, 2,
            1, 2, 2, 2, 2, 2, 2, 1
    };

    // Expande los 32 bits a 48, repitiendo algunas posiciones.
    private static final int[] E = {
            32, 1, 2, 3, 4, 5,
            4, 5, 6, 7, 8, 9,
            8, 9, 10, 11, 12, 13,
            12, 13, 14, 15, 16, 17,
            16, 17, 18, 19, 20, 21,
            20, 21, 22, 23, 24, 25,
            24, 25, 26, 27, 28, 29,
            28, 29, 30, 31, 32, 1
    };

    // Reordena los 32 bits producidos por las cajas S.
    private static final int[] P = {
            16, 7, 20, 21,
            29, 12, 28, 17,
            1, 15, 23, 26,
            5, 18, 31, 10,
            2, 8, 24, 14,
            32, 27, 3, 9,
            19, 13, 30, 6,
            22, 11, 4, 25
    };

    // Ocho cajas S; cada una tiene cuatro filas y dieciséis columnas.
    private static final int[][][] CAJAS_S = {
            { // S1
                    {14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7},
                    {0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8},
                    {4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0},
                    {15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13}
            },
            { // S2
                    {15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10},
                    {3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5},
                    {0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15},
                    {13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9}
            },
            { // S3
                    {10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8},
                    {13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1},
                    {13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7},
                    {1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12}
            },
            { // S4
                    {7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15},
                    {13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9},
                    {10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4},
                    {3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14}
            },
            { // S5
                    {2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9},
                    {14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6},
                    {4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14},
                    {11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3}
            },
            { // S6
                    {12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11},
                    {10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8},
                    {9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6},
                    {4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13}
            },
            { // S7
                    {4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1},
                    {13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6},
                    {1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2},
                    {6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12}
            },
            { // S8
                    {13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7},
                    {1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2},
                    {7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8},
                    {2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11}
            }
    };

    private Des() {
    }

    private static long permutar(
            long entrada,
            int bitsEntrada,
            int[] tabla) {

        long resultado = 0;

        // Cada posición de la tabla indica qué bit debemos tomar.
        for (int i = 0; i < tabla.length; i++) {

            // Las tablas numeran desde 1, comenzando por la izquierda.
            int desplazamiento = bitsEntrada - tabla[i];

            // Mueve el bit elegido al extremo derecho y lo extrae.
            long bit = (entrada >>> desplazamiento) & 1L;

            // Abre una posición y agrega el bit al resultado.
            resultado = (resultado << 1) | bit;
        }

        return resultado;
    }

    private static int rotarIzquierda28(int valor, int posiciones) {

        // Gira los bits dentro de una palabra de 28 bits.
        // La máscara elimina cualquier bit que quede fuera.
        return ((valor << posiciones)
                | (valor >>> (28 - posiciones))) & 0x0fffffff;
    }

    private static long[] generarSubclaves(long clave) {

        // Reduce la representación de 64 bits a los 56 bits usados por DES.
        long clavePermutada = permutar(clave, 64, PC1);

        // Divide el resultado en dos mitades de 28 bits.
        int izquierda = (int) (clavePermutada >>> 28);
        int derecha = (int) (clavePermutada & 0x0fffffffL);

        long[] subclaves = new long[16];

        for (int ronda = 0; ronda < subclaves.length; ronda++) {

            // Cada ronda parte de las mitades rotadas de la ronda anterior.
            izquierda = rotarIzquierda28(
                    izquierda, ROTACIONES[ronda]);

            derecha = rotarIzquierda28(
                    derecha, ROTACIONES[ronda]);

            // Une las mitades para formar nuevamente 56 bits.
            long combinada = ((long) izquierda << 28)
                    | (derecha & 0x0fffffffL);

            // Selecciona los 48 bits usados en esta ronda.
            subclaves[ronda] = permutar(combinada, 56, PC2);
        }

        return subclaves;
    }

    private static int funcionRonda(int derecha, long subclave) {

        // Conserva los 32 bits sin extender el signo al convertir a long.
        long derechaSinSigno = derecha & 0xffffffffL;

        // Expande a 48 bits y combina con la subclave de la ronda.
        long mezclado = permutar(derechaSinSigno, 32, E) ^ subclave;

        int sustituido = 0;

        for (int caja = 0; caja < 8; caja++) {

            // Extrae grupos de seis bits, comenzando por la izquierda.
            int desplazamiento = 42 - caja * 6;
            int grupo = (int) ((mezclado >>> desplazamiento) & 0x3fL);

            // El primer y último bit forman la fila: de 0 a 3.
            int fila = ((grupo & 0x20) >>> 4) | (grupo & 1);

            // Los cuatro bits centrales forman la columna: de 0 a 15.
            int columna = (grupo >>> 1) & 0x0f;

            // Cada caja convierte seis bits en cuatro.
            int valor = CAJAS_S[caja][fila][columna];

            // Acumula los cuatro bits producidos por esta caja.
            sustituido = (sustituido << 4) | valor;
        }

        // Reordena los 32 bits resultantes.
        return (int) permutar(sustituido & 0xffffffffL, 32, P);
    }

    public static long cifrarBloque(long bloque, long clave) {

        // Cifra usando las subclaves desde la primera hasta la última.
        return procesarBloque(bloque, clave, false);
    }

    public static long descifrarBloque(long bloque, long clave) {

        // Descifra usando las mismas subclaves en orden inverso.
        return procesarBloque(bloque, clave, true);
    }

    private static long procesarBloque(
            long bloque,
            long clave,
            boolean descifrar) {

        // Genera las dieciséis subclaves de esta clave DES.
        long[] subclaves = generarSubclaves(clave);

        // Reordena los bits antes de iniciar las rondas.
        long bloquePermutado = permutar(bloque, 64, IP);

        // Divide el bloque en dos mitades de 32 bits.
        int izquierda = (int) (bloquePermutado >>> 32);
        int derecha = (int) bloquePermutado;

        for (int ronda = 0; ronda < 16; ronda++) {

            // Al descifrar, recorre las subclaves desde la última.
            int indice = descifrar ? 15 - ronda : ronda;

            // Calcula ambas mitades sin perder los valores anteriores.
            int nuevaIzquierda = derecha;
            int nuevaDerecha =
                    izquierda ^ funcionRonda(derecha, subclaves[indice]);

            izquierda = nuevaIzquierda;
            derecha = nuevaDerecha;
        }

        // Coloca la mitad derecha primero al terminar las rondas.
        long combinado =
                ((derecha & 0xffffffffL) << 32)
                        | (izquierda & 0xffffffffL);

        // Aplica la permutación final y devuelve los 64 bits.
        return permutar(combinado, 64, FP);
    }
}
