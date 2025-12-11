package Coup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class EstadoDelJuego {
    private List<Jugador> jugadores;
    private Stack<Carta> mazo;
    private int turnoActual;

    public EstadoDelJuego(List<Jugador> jugadores) {
        this.jugadores = jugadores;
        this.turnoActual = 0;
        this.mazo = new Stack<>();
        inicializarMazo();
        repartirCartas();
    }

    private void inicializarMazo() {
        for (Rol rol : Rol.values()) {
            for (int i = 0; i < 3; i++) {
                mazo.push(new Carta(rol));
            }
        }
        Collections.shuffle(mazo);
    }

    private void repartirCartas() {
        for (Jugador j : jugadores) {
            j.recibirCarta(tomarCartaDelMazo());
            j.recibirCarta(tomarCartaDelMazo());
        }
    }

    public Carta tomarCartaDelMazo() {
        if (mazo.isEmpty()) return null;
        return mazo.pop();
    }

    public void devolverCartaAlMazo(Carta carta) {
        mazo.push(carta);
        Collections.shuffle(mazo);
    }

    public Jugador obtenerJugadorActual() {
        return jugadores.get(turnoActual);
    }

    public List<Jugador> obtenerJugadores() {
        return jugadores;
    }

    public void siguienteTurno() {
        do {
            turnoActual = (turnoActual + 1) % jugadores.size();
        } while (!jugadores.get(turnoActual).estaVivo());
    }
}