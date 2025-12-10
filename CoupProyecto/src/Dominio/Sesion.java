package Dominio;
public class Sesion {
    private int id;
    private String nombre;
    private String contra;
    private int ranking;
    
    public Sesion(String nombre, String contra) {
        this.nombre = nombre;
        this.contra = contra;
    }
    public Sesion(int id,String nombre, String contra,int ranking) {
        this.id = id;
        this.nombre = nombre;
        this.contra = contra;
        this.ranking = ranking;
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
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
