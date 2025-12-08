package Dominio;
public class Sesion {
    private String nombre;
    private String contra;

    public Sesion(String nombre, String contra) {
        this.nombre = nombre;
        this.contra = contra;
    }

    public String getContra() {
        return contra;
    }

    public String getNombre() {
        return nombre;
    }

    public void setContra(String contra) {
        this.contra = contra;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre + " " + contra;
    }
    
}
