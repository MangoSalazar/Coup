package Coup;

import Servidor.UnCliente;
import java.util.ArrayList;
import java.util.List;

public class Jugador {
    private UnCliente cliente;
    private String nombreLocal; // Para cuando jugamos en local sin servidor
    private List<Carta> mano;
    private int monedas;
    private boolean vivo;
//servidor
    public Jugador(UnCliente cliente) {
        this.cliente = cliente;
        this.mano = new ArrayList<>();
        this.monedas = 2;
        this.vivo = true;
    }

    //local
    public Jugador(String nombre) {
        this.nombreLocal = nombre;
        this.cliente = null; // No hay conexión real
        this.mano = new ArrayList<>();
        this.monedas = 2;
        this.vivo = true;
    }

    //identificacion
    public String getId() {
        if (cliente != null) {
            return cliente.getId();
        }
        return nombreLocal; // Devuelve el nombre local si no hay cliente
    }

    // Método identifiacion local
    public String obtenerNombre() {
        return getId();
    }

    public UnCliente getCliente() {
        return cliente;
    }

    public List<Carta> getMano() {
        return mano;
    }

    public int getMonedas() {
        return monedas;
    }

    public void modificarMonedas(int cantidad) {
        this.monedas += cantidad;
        if (this.monedas < 0) this.monedas = 0;
    }

    public void recibirCarta(Carta c) {
        mano.add(c);
    }

    public int getInfluenciaActiva() {
        int count = 0;
        for (Carta c : mano) {
            if (!c.estaRevelada()) count++;
        }
        return count;
    }

    public boolean estaVivo() {
        return getInfluenciaActiva() > 0;
    }
//logica online
    public void perderInfluencia() {
        for (Carta c : mano) {
            if (!c.estaRevelada()) {
                c.revelar();
                break;
            }
        }
        checkVidas();
    }
//logica local
    public void perderCarta(int indice) {
        if (indice >= 0 && indice < mano.size()) {
            Carta c = mano.get(indice);
            if (!c.estaRevelada()) {
                c.revelar();
            } else {
                // Si ya estaba revelada, intenta perder otra
                perderInfluencia();
            }
        } else {
            perderInfluencia();
        }
        checkVidas();
    }

    private void checkVidas() {
        if (getInfluenciaActiva() == 0) {
            this.vivo = false;
        }
    }

    @Override
    public String toString() {
        return getId();
    }
}