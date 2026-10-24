package org.example.modelo;

public class Usuario {

    // Datos usados para personalizar el saludo y el perfil.
    private final String nombre;
    private final String apellido;

    // Identificador usado para iniciar sesión.
    private final String correoElectronico;

    // Hash almacenado para verificar la contraseña.
    private final String hashContrasena;

    // Indica si tiene acceso a las funciones premium.
    private final boolean premium;

    // Recibe los datos previamente validados.
    //Constructor
    public Usuario(
            String nombre,
            String apellido,
            String correoElectronico,
            String hashContrasena,
            boolean premium) {

        this.nombre = nombre;
        this.apellido = apellido;
        this.correoElectronico = correoElectronico;
        this.hashContrasena = hashContrasena;
        this.premium = premium;
    }

    //Getters
    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public boolean isPremium() {
        return premium;
    }
}