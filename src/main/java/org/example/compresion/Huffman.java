package org.example.compresion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*
 * Comprime y descomprime bytes mediante Huffman.
 * Construye el árbol usando arreglos propios.
 * Guarda las frecuencias para reconstruir el árbol al descomprimir.
 * Conserva exactamente los bytes del contenido original.
 */
public final class Huffman {

    private static final int FIRMA = 0x48554631; // HUF1
    private static final int SIMBOLOS = 256;
    private static final int CABECERA = 4 + 4 + SIMBOLOS * 4;

    // Coincide con el límite actual de los libros originales.
    private static final int MAXIMO_ORIGINAL = 1024 * 1024;

    private Huffman() {
        // Evitamos crear objetos de esta clase.
    }


    //Creamos los nodos del arbol de Huffman.
    private static final class Nodo {
        private final int simbolo;
        private final int frecuencia;
        private final Nodo izquierdo;
        private final Nodo derecho;

        private Nodo(
                int simbolo,
                int frecuencia,
                Nodo izquierdo,
                Nodo derecho) {

            this.simbolo = simbolo;
            this.frecuencia = frecuencia;
            this.izquierdo = izquierdo;
            this.derecho = derecho;
        }

        private boolean esHoja() {
            return izquierdo == null && derecho == null;
        }
    }

    public static byte[] comprimir(byte[] original) throws IOException {
        if (original == null || original.length > MAXIMO_ORIGINAL) {
            throw new IllegalArgumentException(
                    "El original es nulo o supera 1 MiB.");
        }

        int[] frecuencias = new int[SIMBOLOS];

        // Convertimos cada byte a un índice entre 0 y 255.
        for (byte valor : original) {
            frecuencias[valor & 0xFF]++;
        }

        //Empezamos a construir el arbol de Huffman.
        Nodo raiz = construirArbol(frecuencias);
        String[] codigos = new String[SIMBOLOS];

        generarCodigos(raiz, "", codigos);

        ByteArrayOutputStream memoria = new ByteArrayOutputStream();

        try (DataOutputStream salida = new DataOutputStream(memoria)) {
            salida.writeInt(FIRMA);
            salida.writeInt(original.length);

            // Guardamos las frecuencias en el orden de los símbolos.
            for (int frecuencia : frecuencias) {
                salida.writeInt(frecuencia);
            }

            int acumulado = 0;
            int bitsOcupados = 0;

            //Por cada byte del original, generamos el codigo correspondiente.
            for (byte valor : original) {
                String codigo = codigos[valor & 0xFF];

                for (int i = 0; i < codigo.length(); i++) {
                    // Agregamos un bit al extremo derecho del acumulador.
                    acumulado =
                            (acumulado << 1) | (codigo.charAt(i) - '0');

                    bitsOcupados++;

                    if (bitsOcupados == 8) {
                        salida.writeByte(acumulado);
                        acumulado = 0;
                        bitsOcupados = 0;
                    }
                }
            }

            if (bitsOcupados > 0) {
                // Los bits sobrantes del último byte se completan con ceros.
                acumulado <<= 8 - bitsOcupados;
                salida.writeByte(acumulado);
            }
        }

        return memoria.toByteArray();
    }

    public static byte[] descomprimir(byte[] archivo) throws IOException {
        if (archivo == null || archivo.length < CABECERA) {
            throw new IOException("Archivo Huffman incompleto.");
        }

        int longitud;
        int[] frecuencias = new int[SIMBOLOS];

        try (DataInputStream entrada = new DataInputStream(
                new ByteArrayInputStream(archivo))) {

            //Si la entrada no lleva nuestra firma, no es un archivo Huffman.
            if (entrada.readInt() != FIRMA) {
                throw new IOException("Formato Huffman no reconocido.");
            }

            longitud = entrada.readInt();

            // Validamos antes de reservar memoria para el resultado.
            if (longitud < 0 || longitud > MAXIMO_ORIGINAL) {
                throw new IOException("Longitud original inválida.");
            }

            long suma = 0;

            for (int i = 0; i < SIMBOLOS; i++) {
                frecuencias[i] = entrada.readInt();

                if (frecuencias[i] < 0 || frecuencias[i] > longitud) {
                    throw new IOException("Frecuencia Huffman inválida.");
                }

                suma += frecuencias[i];
            }

            if (suma != longitud) {
                throw new IOException(
                        "Las frecuencias no coinciden con la longitud original.");
            }
        }

        Nodo raiz = construirArbol(frecuencias);
        String[] codigos = new String[SIMBOLOS];

        generarCodigos(raiz, "", codigos);

        // Calculamos cuántos bits debe contener este archivo.
        long totalBits = 0;

        for (int i = 0; i < SIMBOLOS; i++) {
            if (frecuencias[i] > 0) {
                totalBits +=
                        (long) frecuencias[i] * codigos[i].length();
            }
        }

        long bytesEsperados = (totalBits + 7) / 8;

        if (archivo.length != CABECERA + bytesEsperados) {
            throw new IOException(
                    "El contenido Huffman está incompleto o tiene bytes adicionales.");
        }

        // Los bits de relleno deben ser ceros.
        int bitsUsadosAlFinal = (int) (totalBits % 8);

        if (bitsUsadosAlFinal != 0) {
            int mascara = (1 << (8 - bitsUsadosAlFinal)) - 1;

            if ((archivo[archivo.length - 1] & mascara) != 0) {
                throw new IOException("Relleno de bits Huffman inválido.");
            }
        }

        byte[] resultado = new byte[longitud];

        // Un original vacío no contiene árbol ni bits.
        if (longitud == 0) {
            return resultado;
        }

        int[] observadas = new int[SIMBOLOS];
        int escritos = 0;
        Nodo actual = raiz;

        for (long posicion = 0; posicion < totalBits; posicion++) {
            int indice = CABECERA + (int) (posicion / 8);
            int desplazamiento = 7 - (int) (posicion % 8);

            int bit = ((archivo[indice] & 0xFF) >>> desplazamiento) & 1;

            if (raiz.esHoja()) {
                // Un único símbolo utiliza el código cero.
                if (bit != 0) {
                    throw new IOException(
                            "Código inválido para un único símbolo.");
                }
            } else {
                actual = bit == 0 ? actual.izquierdo : actual.derecho;
            }

            if (actual.esHoja()) {
                if (escritos >= resultado.length) {
                    throw new IOException(
                            "El archivo produce más bytes de los declarados.");
                }

                resultado[escritos++] = (byte) actual.simbolo;
                observadas[actual.simbolo]++;

                // Cada símbolo nuevo comienza desde la raíz.
                actual = raiz;
            }
        }

        if (escritos != longitud || actual != raiz) {
            throw new IOException("La secuencia Huffman está incompleta.");
        }

        // Comprobamos que el resultado respete las frecuencias declaradas.
        for (int i = 0; i < SIMBOLOS; i++) {
            if (observadas[i] != frecuencias[i]) {
                throw new IOException(
                        "El contenido no coincide con las frecuencias Huffman.");
            }
        }

        return resultado;
    }

    //Construimos el arbol de Huffman a partir de las frecuencias.
    private static Nodo construirArbol(int[] frecuencias) {
        Nodo[] nodos = new Nodo[SIMBOLOS];
        int activos = 0;

        // Creamos una hoja para cada símbolo presente.
        for (int i = 0; i < SIMBOLOS; i++) {
            if (frecuencias[i] > 0) {
                nodos[i] = new Nodo(i, frecuencias[i], null, null);
                activos++;
            }
        }

        if (activos == 0) {
            return null;
        }

        while (activos > 1) {
            int primero = indiceMenor(nodos);
            Nodo izquierdo = nodos[primero];
            nodos[primero] = null;

            int segundo = indiceMenor(nodos);
            Nodo derecho = nodos[segundo];
            nodos[segundo] = null;

            // Unimos los dos nodos menos frecuentes.
            nodos[primero] = new Nodo(
                    -1,
                    izquierdo.frecuencia + derecho.frecuencia,
                    izquierdo,
                    derecho);

            activos--;
        }

        return nodos[indiceMenor(nodos)];
    }

    private static int indiceMenor(Nodo[] nodos) {
        int menor = -1;

        for (int i = 0; i < nodos.length; i++) {
            if (nodos[i] != null
                    && (menor == -1
                    || nodos[i].frecuencia < nodos[menor].frecuencia)) {

                menor = i;
            }
        }

        // En un empate conservamos el índice menor.
        // Esto permite reconstruir siempre el mismo árbol.
        return menor;
    }

    //Generamos los codigos de Huffman para cada simbolo (byte).
    private static void generarCodigos(
            Nodo nodo, String prefijo, String[] codigos) {

        if (nodo == null) {
            return;
        }

        if (nodo.esHoja()) {
            // Si existe un solo símbolo, le asignamos un bit.
            codigos[nodo.simbolo] = prefijo.isEmpty() ? "0" : prefijo;
            return;
        }

        generarCodigos(nodo.izquierdo, prefijo + "0", codigos);
        generarCodigos(nodo.derecho, prefijo + "1", codigos);
    }
}