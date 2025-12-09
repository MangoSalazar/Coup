package Dominio;

import Servidor.UnCliente;
import java.util.List;

public class Partida {
    private List<UnCliente> jugadores;
    private int indiceTurnoActual;

    public Partida(List<UnCliente> jugadores) {
        this.jugadores = jugadores;
        this.indiceTurnoActual = 0; 
    }

    public UnCliente obtenerJugadorTurno() {
        if (jugadores.isEmpty()) return null;
        return jugadores.get(indiceTurnoActual);
    }

    public void siguienteTurno() {
        if (jugadores.isEmpty()) return;
        indiceTurnoActual = (indiceTurnoActual + 1) % jugadores.size();
    }
    
    public boolean esTurnoDe(UnCliente cliente) {
        return obtenerJugadorTurno() != null && obtenerJugadorTurno().equals(cliente);
    }
}