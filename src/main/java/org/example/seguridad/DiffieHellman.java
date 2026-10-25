package org.example.seguridad;

import java.math.BigInteger;
import java.security.SecureRandom;

//Aplicacion de Diffie-Hellman para el intercambio seguro de claves entre clientes y servidor.

public class DiffieHellman {

    //Primo público del grupo 14 de RFC 3526.
    /*Este numero nos permite crear un canal seguro de comunicacion. Pernite que, tanto el cliente, como el servidor,
    puedan acordar una clave secreta para descifrar y cifrar los mensajes.*/
    private static final BigInteger P = new BigInteger(
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
                    + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
                    + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
                    + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
                    + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
                    + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
                    + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
                    + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
                    + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
                    + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
                    + "15728E5A8AACAA68FFFFFFFFFFFFFFFF",
                16);

    //Generador publico definido por el mismo grupo
    private static final BigInteger G = BigInteger.TWO;

    //Orden del subgrupo usado para validar las claves publicas.
    private static final BigInteger Q = P.subtract(BigInteger.ONE).divide(BigInteger.TWO);

    private static final SecureRandom ALEATORIO = new SecureRandom();

    //Cada objeto tiene que mantener su propia clave privada.
    private final BigInteger clavePrivada;
    private final BigInteger clavePublica;

    //Constructor
    public DiffieHellman(){
        BigInteger candidata;

        //Generamos un exponente aleatorio de hasta 256 bits.
        do {
            candidata = new BigInteger(256, ALEATORIO);
        } while (candidata.compareTo(BigInteger.TWO) < 0);

        clavePrivada = candidata;

        //Clave publica = generador elevado a la clave primada, modulo de P.
        clavePublica = G.modPow(clavePrivada, P);
    }

    public BigInteger getClavePublica(){
        return clavePublica;
    }

    public BigInteger calcularSecreto(BigInteger publicaRemota){
        if(publicaRemota == null){
            throw new IllegalArgumentException("La clave publica remota es obligatoria.");
        }

        //Rechaza cualquier valor fuera del intervalo permitido
        if (publicaRemota.compareTo(BigInteger.TWO) < 0
                || publicaRemota.compareTo(
                P.subtract(BigInteger.TWO)) > 0) {

            throw new IllegalArgumentException(
                    "La clave pública remota está fuera de rango.");
        }

        // Comprueba que la clave pertenece al subgrupo esperado.
        if (!publicaRemota.modPow(Q, P).equals(BigInteger.ONE)) {
            throw new IllegalArgumentException(
                    "La clave pública remota no pertenece al subgrupo.");
        }

        // Ambos participantes obtienen el mismo secreto.
        return publicaRemota.modPow(clavePrivada, P);
    }
}
