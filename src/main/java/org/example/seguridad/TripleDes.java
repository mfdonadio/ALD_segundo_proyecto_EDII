package org.example.seguridad;

//Este no tiene mayor ciencia, es literalmente hacer DES 3 veces.

public final class TripleDes {

    private TripleDes() {
    }

    public static long cifrarBloque(
            long bloque,
            long clave1,
            long clave2,
            long clave3) {

        // Primera etapa: cifra con K1.
        long paso1 = Des.cifrarBloque(bloque, clave1);

        // Segunda etapa: aplica el descifrado DES con K2.
        long paso2 = Des.descifrarBloque(paso1, clave2);

        // Tercera etapa: cifra con K3.
        return Des.cifrarBloque(paso2, clave3);
    }

    public static long descifrarBloque(
            long bloque,
            long clave1,
            long clave2,
            long clave3) {

        // Invierte la última etapa del cifrado.
        long paso1 = Des.descifrarBloque(bloque, clave3);

        // Invierte la etapa intermedia.
        long paso2 = Des.cifrarBloque(paso1, clave2);

        // Recupera el bloque original.
        return Des.descifrarBloque(paso2, clave1);
    }
}
