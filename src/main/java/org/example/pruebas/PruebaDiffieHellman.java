package org.example.pruebas;

import org.example.seguridad.DiffieHellman;

import java.math.BigInteger;

public class PruebaDiffieHellman {

    public static void main(String[] args) {

        // Cada participante genera su propio par de claves.
        DiffieHellman cliente = new DiffieHellman();
        DiffieHellman servidor = new DiffieHellman();

        // Cada lado recibe únicamente la clave pública del otro.
        BigInteger secretoCliente =
                cliente.calcularSecreto(servidor.getClavePublica());

        BigInteger secretoServidor =
                servidor.calcularSecreto(cliente.getClavePublica());

        // Ambos cálculos deben producir el mismo resultado.
        if (!secretoCliente.equals(secretoServidor)) {
            throw new AssertionError(
                    "Cliente y servidor calcularon secretos diferentes.");
        }

        // Evita aceptar un resultado trivial.
        if (secretoCliente.compareTo(BigInteger.ONE) <= 0) {
            throw new AssertionError(
                    "El secreto compartido es inválido.");
        }

        // Confirma el resultado sin mostrar el secreto.
        System.out.println("OK: ambos participantes comparten el secreto.");

        comprobarRechazo(cliente, BigInteger.ZERO);
        comprobarRechazo(cliente, BigInteger.ONE);
        comprobarRechazo(cliente, BigInteger.valueOf(-1));

        // Un número de 2049 bits supera el primo de nuestro grupo.
        comprobarRechazo(cliente, BigInteger.ONE.shiftLeft(2048));

        System.out.println("Todas las pruebas de Diffie-Hellman pasaron.");
    }

    private static void comprobarRechazo(
            DiffieHellman participante,
            BigInteger claveInvalida) {

        boolean rechazada = false;

        try {
            participante.calcularSecreto(claveInvalida);
        } catch (IllegalArgumentException e) {
            // La validación debe rechazar esta clave.
            rechazada = true;
        }

        if (!rechazada) {
            throw new AssertionError(
                    "Se aceptó una clave pública inválida.");
        }

        System.out.println("OK: clave fuera de rango rechazada.");
    }
}