package Coup;

import java.util.Collections;
import java.util.List;

public class Accion {
    private Jugador actor;
    private Jugador objetivo;
    private EstadoDelJuego juego;

    public Accion(Jugador actor, Jugador objetivo, EstadoDelJuego juego) {
        this.actor = actor;
        this.objetivo = objetivo;
        this.juego = juego;
    }

    public void ingreso() {
        actor.modificarMonedas(1);
        System.out.println(actor.obtenerNombre() + " tomó Ingresos.");
    }

    public void ayudaExtrangera() {
        actor.modificarMonedas(2);
        System.out.println(actor.obtenerNombre() + " tomó Ayuda Extranjera.");
    }

    public void golpe(int cartaAfectada) {
        if (actor.getMonedas() >= 7) {
            actor.modificarMonedas(-7);
            objetivo.perderCarta(cartaAfectada);
            System.out.println(actor.obtenerNombre() + " dio un Golpe de Estado a " + objetivo.obtenerNombre());
        } else {
            System.out.println("No tienes suficientes monedas para un Golpe (Necesitas 7).");
        }
    }

    public void impuestos() {
        actor.modificarMonedas(3);
        System.out.println(actor.obtenerNombre() + " cobró Impuestos (como Duque).");
    }

    public void asesinato(int cartaAfectada) {
        if (actor.getMonedas() >= 3) {
            actor.modificarMonedas(-3);
            objetivo.perderCarta(cartaAfectada);
            System.out.println(actor.obtenerNombre() + " asesinó a un personaje de " + objetivo.obtenerNombre());
        } else {
            System.out.println("No tienes suficientes monedas para Asesinato (Necesitas 3).");
        }
    }

    public void extorision() {
        int robo = Math.min(2, objetivo.getMonedas());
        objetivo.modificarMonedas(-robo);
        actor.modificarMonedas(robo);
        System.out.println(actor.obtenerNombre() + " extorsionó " + robo + " monedas a " + objetivo.obtenerNombre());
    }

    public void cambio() {
        System.out.println(actor.obtenerNombre() + " cambia sus cartas con el mazo (Embajador).");

        // tomar cartas del mazo
        Carta c1 = juego.tomarCartaDelMazo();
        Carta c2 = juego.tomarCartaDelMazo();

        if (c1 != null) actor.recibirCarta(c1);
        if (c2 != null) actor.recibirCarta(c2);

        // obtener la mano
        List<Carta> mano = actor.getMano();
        Collections.shuffle(mano);

//devolver sobrantes
        int aDevolver = (c1 != null ? 1 : 0) + (c2 != null ? 1 : 0);

        for(int i = 0; i < aDevolver; i++) {
            if (!mano.isEmpty()) {
                Carta cartaDevuelta = mano.remove(0);
                juego.devolverCartaAlMazo(cartaDevuelta);
            }
        }
    }
}