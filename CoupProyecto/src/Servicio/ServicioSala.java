package Servicio;

import Dominio.Sala;
import Servidor.UnCliente;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServicioSala {

    public static List<Sala> salas = new ArrayList<>();
    private UnCliente cliente;

    public ServicioSala(UnCliente cliente) {
        this.cliente = cliente;
    }

    public void crear(UnCliente cliente) throws IOException {
        if (existeSala(cliente.getId()) == -1) {
            Sala nuevaSala = new Sala(cliente.getId(), cliente);
            nuevaSala.agregarIntegrante(cliente);
            salas.add(nuevaSala);

            cliente.setEnSala(true);

            cliente.salida().writeUTF("Sala " + cliente.getId() + " creada. Esperando jugadores...");
            return;
        }
        cliente.salida().writeUTF("Error: La sala ya existe.");
    }

    public void unirse(String nombreSala) throws IOException {
        int indice = existeSala(nombreSala);
        if (indice == -1) {
            cliente.salida().writeUTF("Error: Sala no encontrada.");
            return;
        }
        Sala s = salas.get(indice);
        if (cliente.isEnSala()) {
            cliente.salida().writeUTF("ya formas parte de una sala");
            return;
        }
        s.agregarIntegrante(cliente);
        cliente.setEnSala(true);
        cliente.salida().writeUTF("Te has unido a la sala " + nombreSala);
        s.broadcast(">>> " + cliente.getId() + " se ha unido.", cliente);
    }

    public void salir(Sala sala) throws IOException {
        if (sala == null) {
            cliente.salida().writeUTF("no estas dentro de ninguna sala");
            return;
        }
        if (sala.getAdministrador().getId().equals(cliente.getId())) {
            sala.vaciarSala(sala);
            cliente.setEnSala(false);
            salas.remove(sala);//quiñonez
            cliente.salida().writeUTF("Has salido de la sala, limpiando...");
            sala.broadcast("<< " + " la sala ya no existe, saliendo...", null);
            return;
        }
        sala.obtenerIntegrantes().remove(cliente);
        cliente.setEnSala(false);
        cliente.salida().writeUTF("Has salido de la sala.");

        if (sala.obtenerIntegrantes().isEmpty()) {
            salas.remove(sala);
        }
        sala.broadcast("<< " + cliente.getId() + " ha salido de la sala.", null);

    }

    public void expulsar(String jugador, Sala sala) throws IOException {
        for (UnCliente integrante : sala.obtenerIntegrantes()) {
            if (integrante.getId().equals(jugador)) {
                sala.eliminarIntegrante(integrante);
                cliente.salida().writeUTF("<< " + "se ha expulsado a " + integrante.getId());
                return;
            }
        }
        cliente.salida().writeUTF(jugador + " no esta en la sala");
    }

    public void ver() throws IOException {
        if (salas.isEmpty()) {
            cliente.salida().writeUTF("No hay salas disponibles.");
            return;
        }
        String lista = "";
        for (Sala s : salas) {
            lista += s.obtenerNombre() + " ";
        }
        cliente.salida().writeUTF("Salas disponibles: " + lista);
    }

    public Sala obtenerSalaDelCliente() {
        for (Sala s : salas) {
            if (s.obtenerIntegrantes().contains(cliente)) {
                return s;
            }
        }
        return null;
    }

    public int existeSala(String nombreSala) {
        for (int i = 0; i < salas.size(); i++) {
            if (salas.get(i).obtenerNombre().equals(nombreSala)) {
                return i;
            }
        }
        return -1;
    }
}
