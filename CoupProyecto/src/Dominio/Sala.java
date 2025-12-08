package Dominio;

import Servidor.UnCliente;
import java.util.ArrayList;
import java.util.List;

public class Sala {
    String nombre;
    List<UnCliente> integrantes = new ArrayList<>();
    Boolean enEspera = true;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<UnCliente> getIntegrantes() {
        return integrantes;
    }

    public void setIntegrantes(List<UnCliente> integrantes) {
        this.integrantes = integrantes;
    }

    public Boolean getEnEspera() {
        return enEspera;
    }

    public void setEnEspera(Boolean enEspera) {
        this.enEspera = enEspera;
    }
    
}
