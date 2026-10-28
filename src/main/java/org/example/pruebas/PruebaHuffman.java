package org.example.pruebas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.example.compresion.Huffman;

/*
 * Comprueba la recuperación exacta después de comprimir y descomprimir.
 * Incluye casos especiales y un texto largo generado por la prueba.
 * También verifica el rechazo de archivos con formato inválido.
 */
public class PruebaHuffman {

    public static void main(String[] args) throws IOException {
        comprobarRecuperacion("Archivo vacío", new byte[0]);

        comprobarRecuperacion(
                "Un solo byte", new byte[]{65});

        // Probamos un árbol que contiene únicamente un símbolo.
        byte[] repetidos = new byte[10_000];

        for (int i = 0; i < repetidos.length; i++) {
            repetidos[i] = 65;
        }

        comprobarRecuperacion("Un único símbolo repetido", repetidos);

        // Incluimos todos los valores posibles de un byte.
        // Sus frecuencias iguales también prueban el desempate del árbol.
        byte[] todosLosValores = new byte[256];

        for (int i = 0; i < todosLosValores.length; i++) {
            todosLosValores[i] = (byte) i;
        }

        comprobarRecuperacion(
                "Los 256 valores posibles", todosLosValores);

        comprobarRecuperacion(
                "Acentos y saltos de línea",
                "¡Hola, Marco!\r\nÁrbol, compresión y ñ.\n\nFin.\n"
                        .getBytes(StandardCharsets.UTF_8));

        comprobarRecuperacion(
                "Ceros al final",
                new byte[]{10, -1, 20, -128, 0, 0, 0});

        // Generamos un texto propio parecido a un libro para no depender de descargas.
        StringBuilder libro = new StringBuilder();

        for (int i = 1; i <= 200; i++) {
            libro.append("Capítulo ").append(i)
                    .append(": áéíóú ñ compresión y recuperación.\n");
        }

        comprobarRecuperacion(
                "Texto largo generado",
                libro.toString().getBytes(StandardCharsets.UTF_8));

        // Preparamos un archivo válido para modificar copias.
        byte[] valido = Huffman.comprimir(
                "Prueba de formato Huffman."
                        .getBytes(StandardCharsets.UTF_8));

        byte[] firmaIncorrecta = valido.clone();
        firmaIncorrecta[0] ^= 1;

        comprobarRechazo(
                "Firma incorrecta", firmaIncorrecta);

        byte[] truncado = new byte[valido.length - 1];
        System.arraycopy(valido, 0, truncado, 0, truncado.length);

        comprobarRechazo(
                "Archivo truncado", truncado);

        byte[] conByteExtra = new byte[valido.length + 1];
        System.arraycopy(valido, 0, conByteExtra, 0, valido.length);

        comprobarRechazo(
                "Bytes adicionales", conByteExtra);

        // La longitud original ocupa los bytes del índice 4 al 7.
        byte[] longitudInvalida = valido.clone();

        for (int i = 4; i < 8; i++) {
            longitudInvalida[i] = (byte) 0xFF;
        }

        comprobarRechazo(
                "Longitud original negativa", longitudInvalida);

        // Un símbolo genera un bit útil y siete bits de relleno.
        byte[] rellenoInvalido = Huffman.comprimir(new byte[]{65});
        rellenoInvalido[rellenoInvalido.length - 1] |= 1;

        comprobarRechazo(
                "Relleno de bits alterado", rellenoInvalido);

        System.out.println("Todas las pruebas de Huffman pasaron.");
    }

    private static void comprobarRecuperacion(
            String descripcion, byte[] original) throws IOException {

        byte[] copiaOriginal = original.clone();

        byte[] comprimido = Huffman.comprimir(original);
        byte[] copiaComprimido = comprimido.clone();

        byte[] recuperado = Huffman.descomprimir(comprimido);

        // Las operaciones no deben modificar los arreglos recibidos.
        comprobarIguales(copiaOriginal, original,
                descripcion + ": original modificado");

        comprobarIguales(copiaComprimido, comprimido,
                descripcion + ": comprimido modificado");

        comprobarIguales(copiaOriginal, recuperado,
                descripcion + ": recuperación incorrecta");

        System.out.println(
                "OK: " + descripcion
                        + " | Original: " + original.length
                        + " bytes | Comprimido: " + comprimido.length
                        + " bytes");
    }

    private static void comprobarIguales(
            byte[] esperado, byte[] recibido, String error) {

        if (esperado.length != recibido.length) {
            throw new AssertionError(error + ": longitudes diferentes.");
        }

        for (int i = 0; i < esperado.length; i++) {
            if (esperado[i] != recibido[i]) {
                throw new AssertionError(
                        error + ": diferencia en el byte " + i + ".");
            }
        }
    }

    private static void comprobarRechazo(
            String descripcion, byte[] archivo) {

        boolean rechazado = false;

        try {
            Huffman.descomprimir(archivo);
        } catch (IOException e) {
            rechazado = true;
        }

        if (!rechazado) {
            throw new AssertionError(
                    "Se aceptó un archivo inválido: " + descripcion);
        }

        System.out.println("OK: rechazado — " + descripcion);
    }
}