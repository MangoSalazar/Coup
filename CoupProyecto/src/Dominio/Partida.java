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

    // --- VARIABLES PARA MANEJO DE ESTADOS DE DESAFÍO Y PÉRDIDA ---
    private Jugador jugadorVictima;      // Quien está muriendo/perdiendo influencia
    private Jugador jugadorIntercambio;  // Quien está haciendo embajador

    // Variables para la acción que está "en espera" de ser desafiada
    private String accionPendiente = null;
    private Jugador actorPendiente = null;
    private Jugador objetivoPendiente = null;
    private String cartaRequeridaPendiente = null;

    public Partida(List<UnCliente> clientes) {
        this.jugadores = new ArrayList<>();
        for (UnCliente c : clientes) {
            this.jugadores.add(new Jugador(c));
        }
        this.indiceTurnoActual = 0;
        this.jugadorVictima = null;
        this.jugadorIntercambio = null;
        limpiarAccionPendiente();
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

    public Jugador obtenerGanador() {
        int vivos = 0;
        Jugador ganador = null;
        for (Jugador j : jugadores) {
            if (j.estaVivo()) {
                vivos++;
                ganador = j;
            }
        }
        return (vivos == 1) ? ganador : null;
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
        limpiarAccionPendiente();
    }

    public boolean esTurnoDe(UnCliente cliente) {
        Jugador actual = obtenerJugadorTurno();
        return actual != null && actual.getCliente().equals(cliente);
    }

    public void setAccionPendiente(String accion, Jugador actor, Jugador objetivo, String cartaRequerida) {
        this.accionPendiente = accion;
        this.actorPendiente = actor;
        this.objetivoPendiente = objetivo;
        this.cartaRequeridaPendiente = cartaRequerida;
    }

    public void limpiarAccionPendiente() {
        this.accionPendiente = null;
        this.actorPendiente = null;
        this.objetivoPendiente = null;
        this.cartaRequeridaPendiente = null;
    }

    public boolean hayAccionPendiente() {
        return accionPendiente != null;
    }

    public Jugador getActorPendiente() { return actorPendiente; }
    public String getAccionPendiente() { return accionPendiente; }
    public Jugador getObjetivoPendiente() { return objetivoPendiente; }
    public String getCartaRequeridaPendiente() { return cartaRequeridaPendiente; }

    public void setJugadorVictima(Jugador j) { this.jugadorVictima = j; }

    /**
     * Verifica si el jugador tiene la carta en mano (sin revelarla aún).
     * Se usa para resolver el desafío automáticamente.
     */
    public boolean tieneCarta(Jugador j, String nombreCarta) {
        for (Carta c : j.getMano()) {
            if (!c.estaRevelada() && c.getRol().toString().equalsIgnoreCase(nombreCarta)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Si el jugador gana el desafío (tenía la carta), según las reglas oficiales:
     * 1. Revela la carta (para demostrarlo).
     * 2. Baraja esa carta en el mazo.
     * 3. Toma una nueva carta.
     * Esto evita que se queden marcados con "Tiene el Asesino" para siempre.
     */
    public void cambiarCartaPorGanarDesafio(Jugador j, String nombreCarta) {
        Iterator<Carta> it = j.getMano().iterator();
        while (it.hasNext()) {
            Carta c = it.next();
            if (!c.estaRevelada() && c.getRol().toString().equalsIgnoreCase(nombreCarta)) {
                // 1. La sacamos de la mano
                it.remove();
                mazo.push(c);
                Collections.shuffle(mazo);
                // 2. Le damos una nueva
                if (!mazo.isEmpty()) {
                    j.recibirCarta(mazo.pop());
                }
                return;
            }
        }
    }

//acciones del jeugo
    public void accionIngresos(Jugador j) { j.modificarMonedas(1); }
    public void accionAyudaExterior(Jugador j) { j.modificarMonedas(2); }
    public void accionImpuestos(Jugador j) { j.modificarMonedas(3); }

    public int iniciarGolpe(Jugador atacante, Jugador victima) {
        if (atacante.getMonedas() < 7) return 0;
        atacante.modificarMonedas(-7);
        if (victima.getInfluenciaActiva() == 1) {
            victima.perderInfluencia();
            return 2;
        }
        this.jugadorVictima = victima;
        return 1;
    }

    public int iniciarAsesinato(Jugador atacante, Jugador victima) {
        if (atacante.getMonedas() < 3) return 0;
        atacante.modificarMonedas(-3);
        if (victima.getInfluenciaActiva() == 1) {
            victima.perderInfluencia();
            return 2;
        }
        this.jugadorVictima = victima;
        return 1;
    }

    public void accionExtorsion(Jugador atacante, Jugador victima) {
        int robo = (victima.getMonedas() < 2) ? victima.getMonedas() : 2;
        victima.modificarMonedas(-robo);
        atacante.modificarMonedas(robo);
    }

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
        if (cartasEncontradas < cartasNecesarias) return false;

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