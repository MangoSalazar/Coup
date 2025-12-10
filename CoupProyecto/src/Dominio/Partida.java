package Dominio;

import Coup.Carta;
import Coup.Jugador;
import Coup.Rol;
import Servidor.UnCliente;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class Partida {
    private List<Jugador> jugadores;
    private Stack<Carta> mazo;
    private int indiceTurnoActual;

    private Jugador jugadorVictima;
    private Jugador jugadorIntercambio;

    public Partida(List<UnCliente> clientes) {
        this.jugadores = new ArrayList<>();
        for (UnCliente c : clientes) {
            this.jugadores.add(new Jugador(c));
        }
        this.indiceTurnoActual = 0;
        this.jugadorVictima = null;
        this.jugadorIntercambio = null;
        inicializarJuego();
    }

    private void inicializarJuego() {
        mazo = new Stack<>();
        for (Rol rol : Rol.values()) {
            for (int i = 0; i < 3; i++) {
                mazo.push(new Carta(rol));
            }
        }
        Collections.shuffle(mazo);

        for (Jugador j : jugadores) {
            j.recibirCarta(mazo.pop());
            j.recibirCarta(mazo.pop());
        }
    }

    public Jugador obtenerJugadorTurno() {
        if (jugadores.isEmpty()) return null;
        return jugadores.get(indiceTurnoActual);
    }

    public Jugador getJugador(UnCliente cliente) {
        for(Jugador j : jugadores) {
            if(j.getCliente().equals(cliente)) return j;
        }
        return null;
    }

    public Jugador getJugadorVictima() { return jugadorVictima; }
    public Jugador getJugadorIntercambio() { return jugadorIntercambio; }

    public void siguienteTurno() {
        if (jugadores.isEmpty()) return;
        do {
            indiceTurnoActual = (indiceTurnoActual + 1) % jugadores.size();
        } while (!jugadores.get(indiceTurnoActual).estaVivo());

        jugadorVictima = null;
        jugadorIntercambio = null;
    }

    public boolean esTurnoDe(UnCliente cliente) {
        Jugador actual = obtenerJugadorTurno();
        return actual != null && actual.getCliente().equals(cliente);
    }

    public void accionIngresos(Jugador j) { j.modificarMonedas(1); }
    public void accionAyudaExterior(Jugador j) { j.modificarMonedas(2); }
    public void accionImpuestos(Jugador j) { j.modificarMonedas(3); }

    public boolean iniciarGolpe(Jugador atacante, Jugador victima) {
        if (atacante.getMonedas() >= 7) {
            atacante.modificarMonedas(-7);
            this.jugadorVictima = victima;
            return true;
        }
        return false;
    }

    public boolean iniciarAsesinato(Jugador atacante, Jugador victima) {
        if (atacante.getMonedas() >= 3) {
            atacante.modificarMonedas(-3);
            this.jugadorVictima = victima;
            return true;
        }
        return false;
    }

    public void accionExtorsion(Jugador atacante, Jugador victima) {
        int robo = (victima.getMonedas() < 2) ? victima.getMonedas() : 2;
        victima.modificarMonedas(-robo);
        atacante.modificarMonedas(robo);
    }

    // logica embajador
    // Robar cartas y añadirlas temporalmente a la mano
    public void iniciarEmbajador(Jugador j) {
        if (mazo.isEmpty()) return;

        int cartasARobar = Math.min(2, mazo.size());
        for (int i = 0; i < cartasARobar; i++) {
            j.recibirCarta(mazo.pop());
        }
        this.jugadorIntercambio = j;
    }

    public boolean concretarIntercambio(Jugador j, String carta1, String carta2) {
        List<Carta> mano = j.getMano();
        List<Carta> manoTemporal = new ArrayList<>(mano);

        Carta c1Encontrada = buscarYRemover(manoTemporal, carta1);
        Carta c2Encontrada = null;

        int totalCartasEnMano = j.getInfluenciaActiva();
        int cartasNecesarias = totalCartasEnMano - 2;

        if (cartasNecesarias > 1) {
            c2Encontrada = buscarYRemover(manoTemporal, carta2);
        }

        int cartasEncontradas = (c1Encontrada != null ? 1 : 0) + (c2Encontrada != null ? 1 : 0);

        if (cartasEncontradas < cartasNecesarias) {
            return false;
        }
        j.getMano().clear();

        if (c1Encontrada != null) j.recibirCarta(c1Encontrada);
        if (c2Encontrada != null) j.recibirCarta(c2Encontrada);

        for (Carta c : manoTemporal) {
            if (!c.estaRevelada()) {
                mazo.push(c);
            } else {
                j.recibirCarta(c);
            }
        }

        Collections.shuffle(mazo);
        this.jugadorIntercambio = null;
        return true;
    }
    private Carta buscarYRemover(List<Carta> lista, String nombre) {
        if (nombre == null) return null;
        Iterator<Carta> it = lista.iterator();
        while (it.hasNext()) {
            Carta c = it.next();
            if (!c.estaRevelada() && c.getRol().toString().equalsIgnoreCase(nombre)) {
                it.remove();
                return c;
            }
        }
        return null;
    }

    public boolean concretarPerdida(Jugador victima, String nombreCarta) {
        for (Carta c : victima.getMano()) {
            if (!c.estaRevelada() && c.getRol().toString().equalsIgnoreCase(nombreCarta)) {
                c.revelar();
                this.jugadorVictima = null;
                return true;
            }
        }
        return false;
    }

    public boolean debeDarGolpe(Jugador j) { return j.getMonedas() >= 10; }
}