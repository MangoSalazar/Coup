package Servicio;

import Dominio.Partida;
import Dominio.Sala;
import Servidor.ServidorMulti;
import Servidor.UnCliente;
import java.io.IOException;

public class Mensaje {

    public static void manejarEntrada(UnCliente cliente, String entrada, ServicioSala servicioSala) throws IOException {
        if (entrada.startsWith("/")) {
            procesarComando(cliente, entrada, servicioSala);
        } else {
            procesarChat(cliente, entrada, servicioSala);
        }
    }

    private static void procesarChat(UnCliente cliente, String texto, ServicioSala servicioSala) {
        Sala salaActual = servicioSala.obtenerSalaDelCliente();

        if (salaActual != null) {
            String formato = "[" + cliente.getId() + " @ " + salaActual.obtenerNombre() + "]: " + texto;
            salaActual.broadcast(formato);
        } else {
            String formato = "[LOBBY - " + cliente.getId() + "]: " + texto;
            ServidorMulti.broadcastGlobal(formato);
        }
    }

    private static void procesarComando(UnCliente cliente, String entrada, ServicioSala servicioSala) throws IOException {
        String[] partes = entrada.split(" ");
        String comando = partes[0];
        Sala salaActual = servicioSala.obtenerSalaDelCliente();

        switch (comando) {
            case "/crear":
                servicioSala.crear(cliente);
                break;
                
            case "/unirse":
                if (partes.length > 1) {
                    servicioSala.unirse(partes[1]);
                } else {
                    cliente.salida().writeUTF("Uso correcto: /unirse [nombreSala]");
                }
                break;
                
            case "/ver":
                servicioSala.ver();
                break;
                
            case "/salir":
              //  cliente.salida().writeUTF("Función salir pendiente de implementar.");
                break;

            case "/iniciar":
                manejarInicioPartida(cliente, salaActual);
                break;

            case "/ingresos":
            case "/golpe":
            case "/ayuda":
                manejarAccionDeJuego(cliente, comando, salaActual);
                break;

            default:
                cliente.salida().writeUTF("Comando no reconocido: " + comando);
        }
    }

    private static void manejarInicioPartida(UnCliente cliente, Sala sala) throws IOException {
        if (sala != null) {
            if (sala.getAdministrador().equals(cliente)) {
                Partida nuevaPartida = new Partida(sala.obtenerIntegrantes());
                sala.setPartida(nuevaPartida);
                sala.broadcast(">>> ¡LA PARTIDA HA COMENZADO! <<<");
                
                UnCliente primerJugador = nuevaPartida.obtenerJugadorTurno();
                sala.broadcast("Turno de: " + primerJugador.getId());
                
                enviarMenuAcciones(primerJugador);
                
            } else {
                cliente.salida().writeUTF("Error: Solo el administrador puede iniciar.");
            }
        } else {
            cliente.salida().writeUTF("Error: No estás en una sala.");
        }
    }

    private static void manejarAccionDeJuego(UnCliente cliente, String accion, Sala sala) throws IOException {
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
            
        } else {
            cliente.salida().writeUTF("¡No es tu turno! Espera a " + partida.obtenerJugadorTurno().getId());
        }
    }

    private static void enviarMenuAcciones(UnCliente cliente) throws IOException {
        cliente.salida().writeUTF("\n--- ACCIONES DISPONIBLES ---");
        cliente.salida().writeUTF("/ingresos -> (1 moneda) Nadie puede bloquear.");
        cliente.salida().writeUTF("/ayuda    -> (2 monedas) Bloqueable por Duque.");
        cliente.salida().writeUTF("/golpe    -> (7 monedas) Elimina un personaje rival.");
        cliente.salida().writeUTF("----------------------------\n");
    }
}