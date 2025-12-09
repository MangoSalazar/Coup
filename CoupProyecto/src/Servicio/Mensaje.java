package Servicio;

import Dominio.Partida;
import Dominio.Sala;
import Servidor.ServidorMulti;
import Servidor.UnCliente;
import java.io.IOException;

public class Mensaje {
//
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
            String formato = "[" + cliente.getId() + "]: " + texto;
            salaActual.broadcast(formato, cliente); 
        } else {
            String formato = "[LOBBY - " + cliente.getId() + "]: " + texto;
            ServidorMulti.broadcastGlobal(formato, cliente);
        }
    }

    private static void procesarComando(UnCliente cliente, String entrada, ServicioSala servicioSala) throws IOException {
        String[] partes = entrada.split(" ");
        String comando = partes[0];
        Sala salaActual = servicioSala.obtenerSalaDelCliente();

        switch (comando) {
            case "/crear":
                // Crear sala y cambiar estado
                servicioSala.crear(cliente);
                break;
            case "/unirse":
                if (partes.length > 1) servicioSala.unirse(partes[1]);
                else cliente.salida().writeUTF("Uso: /unirse [nombreSala]");
                break;
            case "/ver":
                servicioSala.ver();
                break;
            case "/salir":
                // Implementar lógica de salir si es necesario
                cliente.salida().writeUTF("Comando salir no implementado.");
                break;
            case "/iniciar":
                manejarInicioPartida(cliente, salaActual);
                break;
            // Acciones de juego
            case "/ingresos":
            case "/golpe":
            case "/ayuda":
                manejarAccionDeJuego(cliente, comando, salaActual);
                break;
            default:
                cliente.salida().writeUTF("Comando desconocido.");
        }
    }

    private static void manejarInicioPartida(UnCliente cliente, Sala sala) throws IOException {
        if (sala != null) {
            if (sala.getAdministrador().equals(cliente)) {
                Partida nuevaPartida = new Partida(sala.obtenerIntegrantes());
                sala.setPartida(nuevaPartida);
                
                sala.broadcast(">>> ¡LA PARTIDA HA COMENZADO! <<<", null);
                
                UnCliente primerJugador = nuevaPartida.obtenerJugadorTurno();
                sala.broadcast("Turno de: " + primerJugador.getId(), null);
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
            sala.broadcast(">> " + cliente.getId() + " usó " + accion, null);
            
            partida.siguienteTurno();
            
            UnCliente siguiente = partida.obtenerJugadorTurno();
            sala.broadcast("Turno finalizado. Sigue: " + siguiente.getId(), null);
            siguiente.salida().writeUTF(">>> ¡ES TU TURNO! <<<");
            enviarMenuAcciones(siguiente);
            
        } else {
            cliente.salida().writeUTF("¡No es tu turno! Espera a " + partida.obtenerJugadorTurno().getId());
        }
    }

    private static void enviarMenuAcciones(UnCliente cliente) throws IOException {
        cliente.salida().writeUTF("\n--- ACCIONES ---");
        cliente.salida().writeUTF("/ingresos (1 moneda)");
        cliente.salida().writeUTF("/ayuda (2 monedas)");
        cliente.salida().writeUTF("/golpe (7 monedas)");
        cliente.salida().writeUTF("----------------\n");
    }
}