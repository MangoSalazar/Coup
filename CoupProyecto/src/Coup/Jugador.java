package Coup;

import java.util.ArrayList;
import java.util.List;

public class Jugador {

    private String nombre;
    private int monedas;
    private List<Carta> cartas = new ArrayList<>();

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.monedas = 2; // Inician con 2 monedas
    }

    public void agregarCarta(Carta carta) {
        cartas.add(carta);
    }

    public String obtenerNombre() {
        return nombre;
    }

    public int obtenerMonedas() {
        return monedas;
    }

    public void agregarMonedas(int monedas) {
        this.monedas += monedas;
    }

    public void quitarMonedas(int monedas) {
        this.monedas -= monedas;
        if (this.monedas < 0) {
            this.monedas = 0;
        }
    }

    public List<Carta> obtenerCartas() {
        return cartas;
    }

    public void perderCarta(int indiceCarta) {
        if (indiceCarta >= 0 && indiceCarta < cartas.size()) {
            Carta c = cartas.get(indiceCarta);
            if (c.estaViva()) {
                c.matar();
            } else {
                for (Carta otra : cartas) {
                    if (otra.estaViva()) {
                        otra.matar();
                        break;
                    }
                }
            }
        }
    }

    public boolean estaVivo() {
        for (Carta c : cartas) {
            if (c.estaViva()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return nombre + " [Monedas: " + monedas + "] Cartas: " + cartas;
    }
}