package Dominio;
 
import Servidor.UnCliente;
import java.util.ArrayList;
import java.util.List;
 
public class Sala {
    String nombre;
    UnCliente administrado;
    List<UnCliente> integrantes = new ArrayList<>();
    Boolean lista = false;
 
    public Sala(String nombre, UnCliente administrado) {
        this.nombre = nombre;
        this.administrado = administrado;
    }
 
    public String obtenerNombre() {
        return nombre;
    }
 
    public void ponerNombre(String nombre) {
        this.nombre = nombre;
    }
 
    public List<UnCliente> obtenerIntegrantes() {
        return integrantes;
    }
 
    public void agregarIntegrante(UnCliente integrante) {
       integrantes.add(integrante);
    }
 
    public Boolean obtenerLista() {
        return lista;
    }
    public void asignarLista(Boolean enEspera) {
        this.lista = lista;
    }
}