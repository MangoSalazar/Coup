package Coup;

public class Carta {

    private Rol rol;
    private boolean conVida = true;

    public Carta(Rol rol) {
        this.rol = rol;
    }

    public Rol obtenerRol() {
        return rol;
    }

    public boolean estaViva() {
        return conVida;
    }

    public void matar() {
        this.conVida = false;
    }

    @Override
    public String toString() {
        return rol +" "+ (conVida?"Viva":"Muerta");
    }
    
}