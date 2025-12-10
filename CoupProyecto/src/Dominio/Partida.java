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

    public Partida(List<UnCliente> clientes) {
        this.jugadores = new ArrayList<>();
        for (UnCliente c : clientes) {
            this.jugadores.add(new Jugador(c));
        }
        this.indiceTurnoActual = 0;
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

    public void siguienteTurno() {
        if (jugadores.isEmpty()) return;
        do {
            indiceTurnoActual = (indiceTurnoActual + 1) % jugadores.size();
        } while (!jugadores.get(indiceTurnoActual).estaVivo());
    }

    public boolean esTurnoDe(UnCliente cliente) {
        Jugador actual = obtenerJugadorTurno();
        return actual != null && actual.getCliente().equals(cliente);
    }

    // acciones generales
    public void accionIngresos(Jugador j) {
        j.modificarMonedas(1);
    }

    public void accionAyudaExterior(Jugador j) {
        j.modificarMonedas(2);
    }

    public boolean accionGolpe(Jugador atacante, Jugador victima) {
        if (atacante.getMonedas() >= 7) {
            atacante.modificarMonedas(-7);
            victima.perderInfluencia();
            return true;
        }
        return false;
    }

//acciones de personaje
    // duque toma 3 monedas
    public void accionImpuestos(Jugador j) {
        j.modificarMonedas(3);
    }

    // asesino paga 3 monedas para eliminar
    public boolean accionAsesinato(Jugador atacante, Jugador victima) {
        if (atacante.getMonedas() >= 3) {
            atacante.modificarMonedas(-3);
            victima.perderInfluencia();
            return true;
        }
        return false;
    }

    // capitán roba 2 monedas de otro jugador
    public void accionExtorsion(Jugador atacante, Jugador victima) {
        int monedasRobadas = 2;
        if (victima.getMonedas() < 2) {
            monedasRobadas = victima.getMonedas();
        }
        victima.modificarMonedas(-monedasRobadas);
        atacante.modificarMonedas(monedasRobadas);
    }

    // embajador cambia cartas con el mazo
    //falta implementar que el jugador pueda escoger las cartas que regresará
    public void accionCambio(Jugador j) {
        if (mazo.isEmpty()) return;

        // Toma 2 cartas del mazo o menos si no hay
        List<Carta> cartasTemporales = new ArrayList<>();
        int cartasARobar = Math.min(2, mazo.size());
        for (int i = 0; i < cartasARobar; i++) {
            cartasTemporales.add(mazo.pop());
        }

        // las añade a la mano temporalmente tiene 3 o 4 cartas
        for (Carta c : cartasTemporales) {
            j.recibirCarta(c);
        }

        // Mezcla la mano
        List<Carta> mano = j.getMano();
        Collections.shuffle(mano);

        // devuelve el exceso al mazo
        // El jugador debe quedarse con tantas cartas como tenía vivas + reveladas
        for (int i = 0; i < cartasARobar; i++) {
            Carta aDevolver = mano.get(0);
            mano.remove(0);
            mazo.push(aDevolver);
        }
        Collections.shuffle(mazo);
    }

    public boolean debeDarGolpe(Jugador j) {
        return j.getMonedas() >= 10;
    }
}