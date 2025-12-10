package Coup;

import java.util.List;

public class Accion {
    private Jugador jugador;
    private Jugador jugadorObjetivo;
    private EstadoDelJuego estadoJuego;

    public Accion(Jugador jugador, EstadoDelJuego estadoJuego) {
        this.jugador = jugador;
        this.estadoJuego = estadoJuego;
    }

    public Accion(Jugador jugador, Jugador jugadorObjetivo, EstadoDelJuego estadoJuego) {
        this.jugador = jugador;
        this.jugadorObjetivo = jugadorObjetivo;
        this.estadoJuego = estadoJuego;
    }

    public String ingreso(){
        jugador.agregarMonedas(1);
        return jugador.obtenerNombre() + " tomó Ingreso (+1 moneda).";
    }

    public String ayudaExtrangera(){
        jugador.agregarMonedas(2);
        return jugador.obtenerNombre() + " pidió Ayuda Extranjera (+2 monedas).";
    }

    public String golpe(int cartaAEliminar){
        if (jugador.obtenerMonedas() >= 7) {
            jugador.quitarMonedas(7);
            jugadorObjetivo.perderCarta(cartaAEliminar);
            return "¡GOLPE DE ESTADO! " + jugador.obtenerNombre() + " atacó a " + jugadorObjetivo.obtenerNombre();
        } else {
            return "ERROR: No tienes 7 monedas para el Golpe.";
        }
    }

    public String impuestos(){
        jugador.agregarMonedas(3);
        return jugador.obtenerNombre() + " cobró Impuestos como Duque (+3 monedas).";
    }

    public String asesinato(int cartaAEliminar){
        if (jugador.obtenerMonedas() >= 3) {
            jugador.quitarMonedas(3);
            jugadorObjetivo.perderCarta(cartaAEliminar);
            return "¡ASESINATO! " + jugador.obtenerNombre() + " pagó para eliminar carta de " + jugadorObjetivo.obtenerNombre();
        } else {
            return "ERROR: No tienes 3 monedas para Asesinato.";
        }
    }

    public String extorision(){
        int monto = Math.min(2, jugadorObjetivo.obtenerMonedas());
        jugadorObjetivo.quitarMonedas(monto);
        jugador.agregarMonedas(monto);
        return jugador.obtenerNombre() + " extorsionó (Capitán) a " + jugadorObjetivo.obtenerNombre() + " robando " + monto + " monedas.";
    }

    public String cambio(){
        Carta c1 = estadoJuego.tomarCartaDelMazo();
        Carta c2 = estadoJuego.tomarCartaDelMazo();
        if(c1 != null) jugador.agregarCarta(c1);
        if(c2 != null) jugador.agregarCarta(c2);

        devolverCartasExceso();
        return jugador.obtenerNombre() + " realizó Cambio de cartas (Embajador).";
    }

    private void devolverCartasExceso() {
        List<Carta> mano = jugador.obtenerCartas();
        while (mano.size() > 2) {
            Carta c = mano.remove(mano.size() - 1);
            estadoJuego.devolverCartaAlMazo(c);
        }
    }
}