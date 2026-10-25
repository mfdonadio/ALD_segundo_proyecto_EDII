package org.example.seguridad;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*
 * Envía y recibe mensajes cifrados por una conexión.
 * Verifica el tamaño antes de reservar memoria.
 * Usa contadores protegidos para rechazar mensajes repetidos o fuera de orden.
 * Cada conexión debe tener su propia instancia, utilizada por un solo hilo.
 */

public final class CanalCifrado {

    private static final int TAMANO_CONTADOR = 8;

    // El contador también ocupa espacio dentro del contenido cifrado.
    private static final int MAXIMO_MENSAJE =
            1024 * 1024 - TAMANO_CONTADOR;

    // IV de 8 bytes, al menos un bloque cifrado y HMAC de 32 bytes.
    private static final int MINIMO_PAQUETE = 48;

    // Máximo admitido por MensajeCifrado, incluyendo relleno.
    private static final int MAXIMO_PAQUETE = 1024 * 1024 + 48;

    private final DataInputStream entrada;
    private final DataOutputStream salida;

    private final byte[] cifradoEnvio;
    private final byte[] hmacEnvio;
    private final byte[] cifradoRecepcion;
    private final byte[] hmacRecepcion;

    private long contadorEnvio = 0;
    private long contadorRecepcion = 0;

    private boolean utilizable = true;

    public CanalCifrado(
            DataInputStream entrada,
            DataOutputStream salida,
            ClavesSesion claves) {

        if (entrada == null || salida == null || claves == null) {
            throw new IllegalArgumentException(
                    "La entrada, la salida y las claves son obligatorias.");
        }

        this.entrada = entrada;
        this.salida = salida;

        // Los getters entregan copias de las claves.
        this.cifradoEnvio = claves.getCifradoEnvio();
        this.hmacEnvio = claves.getHmacEnvio();
        this.cifradoRecepcion = claves.getCifradoRecepcion();
        this.hmacRecepcion = claves.getHmacRecepcion();
    }

    public void enviar(byte[] mensaje) throws IOException {
        comprobarDisponible();

        if (mensaje == null || mensaje.length > MAXIMO_MENSAJE) {
            throw new IllegalArgumentException(
                    "El mensaje supera el tamaño permitido o es nulo.");
        }

        if (contadorEnvio == Long.MAX_VALUE) {
            utilizable = false;
            throw new IOException("Se agotó el contador de envío.");
        }

        byte[] contenido = new byte[TAMANO_CONTADOR + mensaje.length];

        // Guardamos el contador en ocho bytes, del más significativo al menor.
        for (int i = 0; i < TAMANO_CONTADOR; i++) {
            contenido[i] = (byte) (contadorEnvio >>> (56 - i * 8));
        }

        System.arraycopy(
                mensaje, 0, contenido, TAMANO_CONTADOR, mensaje.length);

        byte[] paquete = MensajeCifrado.cifrar(
                contenido, cifradoEnvio, hmacEnvio);

        try {
            // La longitud permite saber cuántos bytes debe leer el receptor.
            salida.writeInt(paquete.length);
            salida.write(paquete);
            salida.flush();

            contadorEnvio++;
        } catch (IOException e) {
            // Un envío parcial deja el canal en un estado incierto.
            utilizable = false;
            throw e;
        }
    }

    public byte[] recibir() throws IOException {
        comprobarDisponible();

        if (contadorRecepcion == Long.MAX_VALUE) {
            utilizable = false;
            throw new IOException("Se agotó el contador de recepción.");
        }

        try {
            int longitud = entrada.readInt();

            // Nunca reservamos memoria usando una longitud sin validar.
            if (longitud < MINIMO_PAQUETE
                    || longitud > MAXIMO_PAQUETE
                    || (longitud - 40) % 8 != 0) {

                throw new IOException("Longitud de paquete inválida.");
            }

            byte[] paquete = new byte[longitud];
            entrada.readFully(paquete);

            // MensajeCifrado verifica el HMAC antes de descifrar.
            byte[] contenido = MensajeCifrado.descifrar(
                    paquete, cifradoRecepcion, hmacRecepcion);

            if (contenido.length < TAMANO_CONTADOR) {
                throw new IOException("Falta el contador del mensaje.");
            }

            long contadorRecibido = 0;

            // Reconstruimos el contador sin interpretar los bytes como texto.
            for (int i = 0; i < TAMANO_CONTADOR; i++) {
                contadorRecibido =
                        (contadorRecibido << 8) | (contenido[i] & 0xFFL);
            }

            if (contadorRecibido != contadorRecepcion) {
                throw new IOException(
                        "Mensaje repetido o fuera de orden.");
            }

            byte[] mensaje = new byte[contenido.length - TAMANO_CONTADOR];

            System.arraycopy(
                    contenido, TAMANO_CONTADOR,
                    mensaje, 0, mensaje.length);

            // Solo avanzamos después de validar el mensaje completo.
            contadorRecepcion++;

            return mensaje;

        } catch (IllegalArgumentException e) {
            utilizable = false;
            throw new IOException("Mensaje cifrado inválido.", e);

        } catch (IOException e) {
            // Después de un fallo, debe cerrarse la conexión.
            utilizable = false;
            throw e;
        }
    }

    private void comprobarDisponible() throws IOException {
        if (!utilizable) {
            throw new IOException(
                    "El canal ya no puede utilizarse. Cierra la conexión.");
        }
    }
}