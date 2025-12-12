package Dominio;

import Servidor.UnCliente;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Sala {
    private String nombre;
    private UnCliente administrador;
    private List<UnCliente> integrantes = new ArrayList<>();

    private Partida partida;
    private boolean enPartida = false;

    public Sala(String nombre, UnCliente administrador) {
        this.nombre = nombre;
        this.administrador = administrador;
    }

    public void broadcast(String mensaje, UnCliente remitente) {
        for (UnCliente integrante : integrantes) {
            if (remitente == null || !integrante.equals(remitente)) {
                try {
                    integrante.salida().writeUTF(mensaje);
                } catch (IOException e) {
                }
            }
        }
    }

    public void broadcast(String mensaje) {
        broadcast(mensaje, null);
    }

    public String obtenerNombre() {
        return nombre;
    }

    public List<UnCliente> obtenerIntegrantes() {
        return integrantes;
    }

    public void agregarIntegrante(UnCliente integrante) {
        integrantes.add(integrante);
    }
    public void eliminarIntegrante(UnCliente integrante) {
        integrante.setEnSala(false);
        integrantes.remove(integrante);
    }

    public UnCliente getAdministrador() {
        return administrador;
    }
    public void vaciarSala(){
        for(UnCliente integrante : integrantes){
            integrante.setEnSala(false);
            integrantes.remove(integrante);
        }
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
        this.enPartida = (partida != null);
    }

    public boolean estaEnPartida() {
        return enPartida;
    }
}