package org.example.pruebas;

import java.io.IOException;
import java.nio.file.Path;

// Ejecuta todas las pruebas Prueba* y falla si alguna falla.
public class EjecutarPruebas {

    // Nombres simples de cada prueba; cada una corre en su propia JVM.
    private static final String[] PRUEBAS = {
            "PruebaAutenticacion",
            "PruebaCanalCifrado",
            "PruebaClavesSesion",
            "PruebaCredenciales",
            "PruebaDes",
            "PruebaDescargaLocal",
            "PruebaDiffieHellman",
            "PruebaHkdfSha256",
            "PruebaHmacSha256",
            "PruebaHuffman",
            "PruebaLecturaLibros",
            "PruebaLibros",
            "PruebaMensajeCifrado",
            "PruebaPbkdf2Sha256",
            "PruebaRellenoPkcs5",
            "PruebaSha256",
            "PruebaTripleDes",
            "PruebaTripleDesCbc",
            "PruebaUsuarios"
    };

    public static void main(String[] args) throws InterruptedException {

        // Reutilizamos la misma JVM y el mismo classpath que lanzó este proceso.
        String java = Path.of(System.getProperty("java.home"), "bin", "java")
                .toString();
        String classpath = System.getProperty("java.class.path");

        int fallidas = 0;

        for (String prueba : PRUEBAS) {
            String clase = "org.example.pruebas." + prueba;

            ProcessBuilder constructor = new ProcessBuilder(
                    java, "-Dfile.encoding=UTF-8", "-cp", classpath, clase);

            // Heredamos la entrada y salida para ver el detalle de cada prueba.
            constructor.inheritIO();

            int codigo;

            try {
                Process proceso = constructor.start();
                codigo = proceso.waitFor();
            } catch (IOException e) {
                // Si no se pudo lanzar la prueba, la contamos como fallida.
                System.err.println(
                        "No se pudo ejecutar " + prueba + ": " + e.getMessage());
                codigo = 1;
            }

            if (codigo == 0) {
                System.out.println("[OK] " + prueba);
            } else {
                System.out.println("[FALLA] " + prueba + " (código " + codigo + ")");
                fallidas++;
            }
        }

        System.out.println();
        System.out.println("Pruebas ejecutadas: " + PRUEBAS.length
                + ", fallidas: " + fallidas);

        // Si alguna prueba falló, terminamos con código distinto de cero.
        if (fallidas > 0) {
            System.exit(1);
        }
    }
}
