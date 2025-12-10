package Dominio;

import Coup.Carta;
import Coup.Jugador;
import Coup.Rol;
import Servidor.UnCliente;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class Partida {
    private List<Jugador> jugadores;
    private Stack<Carta> mazo;
    private int indiceTurnoActual;

    // para controlar quién debe responder (elegir carta a perder)
    private Jugador jugadorVictima;

    public Partida(List<UnCliente> clientes) {
        this.jugadores = new ArrayList<>();
        for (UnCliente c : clientes) {
            this.jugadores.add(new Jugador(c));
        }
        this.indiceTurnoActual = 0;
        this.jugadorVictima = null;
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

    public Jugador getJugadorVictima() {
        return jugadorVictima;
    }

    public void siguienteTurno() {
        if (jugadores.isEmpty()) return;
        do {
            indiceTurnoActual = (indiceTurnoActual + 1) % jugadores.size();
        } while (!jugadores.get(indiceTurnoActual).estaVivo());
        jugadorVictima = null; // Limpiamos víctima al cambiar turno
    }

    public boolean esTurnoDe(UnCliente cliente) {
        // Si hay una víctima pendiente, el turno "lógico" sigue siendo del atacante,
        // pero necesitamos que la víctima pueda responder.
        Jugador actual = obtenerJugadorTurno();
        return actual != null && actual.getCliente().equals(cliente);
    }

    public void accionIngresos(Jugador j) {
        j.modificarMonedas(1);
    }

    public void accionAyudaExterior(Jugador j) {
        j.modificarMonedas(2);
    }

    // Solo cobra y marca a la víctima (NO elimina carta aún)
    public boolean iniciarGolpe(Jugador atacante, Jugador victima) {
        if (atacante.getMonedas() >= 7) {
            atacante.modificarMonedas(-7);
            this.jugadorVictima = victima; // Marcamos quién debe elegir carta
            return true;
        }
        return false;
    }

    public void accionImpuestos(Jugador j) {
        j.modificarMonedas(3);
    }

    // Solo cobra y marca a la víctima
    public boolean iniciarAsesinato(Jugador atacante, Jugador victima) {
        if (atacante.getMonedas() >= 3) {
            atacante.modificarMonedas(-3);
            this.jugadorVictima = victima;
            return true;
        }
        return false;
    }

    public void accionExtorsion(Jugador atacante, Jugador victima) {
        int monedasRobadas = 2;
        if (victima.getMonedas() < 2) {
            monedasRobadas = victima.getMonedas();
        }
        victima.modificarMonedas(-monedasRobadas);
        atacante.modificarMonedas(monedasRobadas);
    }

    public void accionCambio(Jugador j) {
        if (mazo.isEmpty()) return;
        List<Carta> cartasTemporales = new ArrayList<>();
        int cartasARobar = Math.min(2, mazo.size());
        for (int i = 0; i < cartasARobar; i++) {
            cartasTemporales.add(mazo.pop());
        }
        for (Carta c : cartasTemporales) j.recibirCarta(c);

        List<Carta> mano = j.getMano();
        Collections.shuffle(mano);

        for (int i = 0; i < cartasARobar; i++) {
            Carta aDevolver = mano.get(0);
            mano.remove(0);
            mazo.push(aDevolver);
        }
        Collections.shuffle(mazo);
    }

    // Ejecuta la pérdida de la carta elegida
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

    public boolean debeDarGolpe(Jugador j) {
        return j.getMonedas() >= 10;
    }
}