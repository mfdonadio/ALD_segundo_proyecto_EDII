package org.example.seguridad;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class IntercambioClaves {

    //las claves públicas del grupo ocupan como máximo 256 bytes.
    private static final int LONGITUD_CLAVE_PUBLICA = 256;

    //Constructor
    private IntercambioClaves() {}

    public static BigInteger intercambiar(DataInputStream entrada,
                                          DataOutputStream salida) throws IOException {

        //Genera claves nuevas para la conexión.
        DiffieHellman participantes = new DiffieHellman();

        byte[] publicaLocal =
                codificarPublica(participantes.getClavePublica());

        //Ambos participantes envían de primero su clave pública.
        salida.write(publicaLocal);
        salida.flush();

        //Espera los 256 bytes completos de la clave remota.
        byte[] publicaRemotaBytes = new byte[LONGITUD_CLAVE_PUBLICA];
        entrada.readFully(publicaRemotaBytes);

        //El 1 indica que los bytes representan un numero positivo :)
        BigInteger publicaRemota =
                new BigInteger(1, publicaRemotaBytes);

        try {
            //Validamos la clave remota y calculamos el secreto localmente.
            return participantes.calcularSecreto(publicaRemota);
        } catch (IllegalArgumentException e) {
            //Una clave inválida cancela por completo.
            throw new IOException(
                    "La clave pública recibida es inválida.", e);
        }
    }

    private static byte[] codificarPublica(BigInteger publica) {

        byte[] original = publica.toByteArray();
        byte[] resultado = new byte[LONGITUD_CLAVE_PUBLICA];

        // BigInteger puede añadir un byte cero para representar el signo.
        int inicio = 0;

        if (original.length == LONGITUD_CLAVE_PUBLICA + 1
                && original[0] == 0) {
            inicio = 1;
        }

        int cantidad = original.length - inicio;

        if (cantidad > LONGITUD_CLAVE_PUBLICA) {
            throw new IllegalArgumentException(
                    "La clave pública supera el tamaño permitido.");
        }

        // Alinea los bytes a la derecha y conserva ceros a la izquierda.
        int destino = LONGITUD_CLAVE_PUBLICA - cantidad;

        for (int i = 0; i < cantidad; i++) {
            resultado[destino + i] = original[inicio + i];
        }

        return resultado;
    }

    //Confirmacion del secreto compartido.
    public static void confirmar(
            DataInputStream entrada,
            DataOutputStream salida,
            BigInteger secretoCompartido,
            boolean esCliente) throws IOException {

        // Deriva una clave dedicada exclusivamente a esta confirmación.
        byte[] claveConfirmacion = HmacSha256.calcular(
                secretoCompartido.toByteArray(),
                "biblioteca-dh14-v1-confirmacion"
                        .getBytes(StandardCharsets.UTF_8));

        // Usa mensajes diferentes para cada dirección.
        String mensajeLocal = esCliente
                ? "confirmacion-cliente-v1"
                : "confirmacion-servidor-v1";

        String mensajeRemoto = esCliente
                ? "confirmacion-servidor-v1"
                : "confirmacion-cliente-v1";

        // Calcula la prueba que enviaremos al otro participante.
        byte[] pruebaLocal = HmacSha256.calcular(
                claveConfirmacion,
                mensajeLocal.getBytes(StandardCharsets.UTF_8));

        // Calcula la prueba que esperamos recibir.
        byte[] pruebaEsperada = HmacSha256.calcular(
                claveConfirmacion,
                mensajeRemoto.getBytes(StandardCharsets.UTF_8));

        // Envía primero los 32 bytes de nuestra prueba.
        salida.write(pruebaLocal);
        salida.flush();

        // Espera la prueba completa del otro participante.
        byte[] pruebaRecibida = new byte[32];
        entrada.readFully(pruebaRecibida);

        // Compara todos los bytes sin salir en la primera diferencia.
        int diferencias = 0;

        for (int i = 0; i < pruebaEsperada.length; i++) {
            diferencias |= pruebaEsperada[i] ^ pruebaRecibida[i];
        }

        if (diferencias != 0) {
            throw new IOException(
                    "Falló la confirmación del secreto compartido.");
        }
    }
}
