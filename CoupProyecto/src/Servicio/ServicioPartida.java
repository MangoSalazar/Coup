package Servicio;

import Dominio.Partida;
import Dominio.Sala;
import Servidor.UnCliente;
import java.io.IOException;

public class ServicioPartida {

    private UnCliente cliente;

    public ServicioPartida(UnCliente cliente) {
        this.cliente = cliente;
    }

    private void manejarAccionDeJuego(String accion, Sala sala) throws IOException {
        if (sala == null || !sala.estaEnPartida()) {
            cliente.salida().writeUTF("Error: No hay partida activa.");
            return;
        }

        Partida partida = sala.getPartida();

        if (partida.esTurnoDe(cliente)) {
            sala.broadcast(">> " + cliente.getId() + " usó " + accion);

            partida.siguienteTurno();

            UnCliente siguiente = partida.obtenerJugadorTurno();
            sala.broadcast("Turno finalizado. Sigue: " + siguiente.getId());
            siguiente.salida().writeUTF(">>> ¡ES TU TURNO! <<<");

            enviarMenuAcciones(siguiente);
            return;
        }
        cliente.salida().writeUTF("¡No es tu turno! Espera a " + partida.obtenerJugadorTurno().getId());
    }

    public void manejarInicioPartida(UnCliente cliente,Sala sala) throws IOException {
        if (sala != null) {
            if (sala.getAdministrador().equals(cliente)) {
                Partida nuevaPartida = new Partida(sala.obtenerIntegrantes());
                sala.setPartida(nuevaPartida);
                sala.broadcast(">>> ¡LA PARTIDA HA COMENZADO! <<<");

                UnCliente primerJugador = nuevaPartida.obtenerJugadorTurno();
                sala.broadcast("Turno de: " + primerJugador.getId());

                enviarMenuAcciones(primerJugador);
                return;
            }
            cliente.salida().writeUTF("Error: Solo el administrador puede iniciar.");
            return;
        }
        cliente.salida().writeUTF("Error: No estás en una sala.");
    }

    private void enviarMenuAcciones(UnCliente cliente) throws IOException {
        cliente.salida().writeUTF("\n--- ACCIONES DISPONIBLES ---");
        cliente.salida().writeUTF("/ingresos -> (1 moneda) Nadie puede bloquear.");
        cliente.salida().writeUTF("/ayuda    -> (2 monedas) Bloqueable por Duque.");
        cliente.salida().writeUTF("/golpe    -> (7 monedas) Elimina un personaje rival.");
        cliente.salida().writeUTF("----------------------------\n");
    }
}
