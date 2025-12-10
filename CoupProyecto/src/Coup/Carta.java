package Coup;

public class Carta {
    private final Rol rol;
    private boolean revelada;

    public Carta(Rol rol) {
        this.rol = rol;
        this.revelada = false;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean estaRevelada() {
        return revelada;
    }

    public void revelar() {
        this.revelada = true;
    }

    public String verNombre() {
        return rol.toString();
    }

    @Override
    public String toString() {
        return revelada ? "REVELADA" : "OCULTA";
    }
}