package Coup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EstadoDelJuego {
    private List<Jugador> jugadores = new ArrayList<>();
    private List<Carta> baraja = new ArrayList<>();
    private int jugadorActual = 0;

    public EstadoDelJuego(List<Jugador> jugadores) {
        this.jugadores = jugadores;
        // Crear baraja
        for (Rol rol : Rol.values()) {
            baraja.add(new Carta(rol));
            baraja.add(new Carta(rol));
            baraja.add(new Carta(rol));
        }

        Collections.shuffle(baraja);

        // Repartir 2 cartas por jugador
        for (Jugador jugador : jugadores) {
            jugador.agregarCarta(baraja.remove(0));
            jugador.agregarCarta(baraja.remove(0));
        }
    }

    public Jugador obtenerJugadorActual() {
        return jugadores.get(jugadorActual);
    }

    public void siguienteTurno() {
        do {
            jugadorActual = (jugadorActual + 1) % jugadores.size();
        } while (!jugadores.get(jugadorActual).estaVivo()); // Saltar jugadores muertos
    }

    public List<Jugador> obtenerJugadores() {
        return jugadores;
    }

    public Carta tomarCartaDelMazo() {
        if (!baraja.isEmpty()) {
            return baraja.remove(0);
        }
        return null;
    }

    public void devolverCartaAlMazo(Carta carta) {
        if (carta != null) {
            baraja.add(carta);
            Collections.shuffle(baraja);
        }
    }
}