package Servicio;

import Coup.Carta;
import Coup.Jugador;
import Dominio.Partida;
import Dominio.Sala;
import Servidor.UnCliente;
import java.io.IOException;

public class ServicioPartida {

    private UnCliente cliente;

    public ServicioPartida(UnCliente cliente) {
        this.cliente = cliente;
    }

    public void manejarAccionDeJuego(String comando, Sala sala) throws IOException {
        if (sala == null || !sala.estaEnPartida()) {
            cliente.salida().writeUTF("Error: No hay partida activa.");
            return;
        }

        Partida partida = sala.getPartida();

        if (!partida.esTurnoDe(cliente)) {
            cliente.salida().writeUTF("¡No es tu turno! Espera a " + partida.obtenerJugadorTurno().getId());
            return;
        }

        Jugador jugadorActual = partida.getJugador(cliente);

        // 10 monedas obligan a hacer un golpe
        if (partida.debeDarGolpe(jugadorActual) && !comando.startsWith("/golpe")) {
            cliente.salida().writeUTF("¡Tienes 10 o más monedas! Debes usar: /golpe [jugador]");
            return;
        }

        String[] partes = comando.split(" ");
        String accion = partes[0];

        switch (accion) {
            case "/ingresos":
                partida.accionIngresos(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " tomó INGRESOS (+1 moneda).");
                avanzarTurno(partida, sala);
                break;

            case "/ayuda":
                partida.accionAyudaExterior(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " pidió AYUDA EXTERIOR (+2 monedas).");
                avanzarTurno(partida, sala);
                break;

            case "/golpe":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "GOLPE")) {
                    if (partida.accionGolpe(jugadorActual, obtenerVictima(partes, sala, partida))) {
                        avanzarTurno(partida, sala);
                    } else {
                        cliente.salida().writeUTF("No tienes suficientes monedas (7).");
                    }
                }
                break;

            // acciones de personajes
            case "/impuestos": // Duque
                partida.accionImpuestos(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " cobró IMPUESTOS (Duque) (+3 monedas).");
                avanzarTurno(partida, sala);
                break;

            case "/asesinar": // Asesino
                if (procesarAtaque(partes, sala, partida, jugadorActual, "ASESINATO")) {
                    if (partida.accionAsesinato(jugadorActual, obtenerVictima(partes, sala, partida))) {
                        avanzarTurno(partida, sala);
                    } else {
                        cliente.salida().writeUTF("No tienes suficientes monedas (3).");
                    }
                }
                break;

            case "/extorsionar": // Capitán
                if (procesarAtaque(partes, sala, partida, jugadorActual, "EXTORSIÓN")) {
                    partida.accionExtorsion(jugadorActual, obtenerVictima(partes, sala, partida));
                    avanzarTurno(partida, sala);
                }
                break;

            case "/cambio": // Embajador
                partida.accionCambio(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " realizó un CAMBIO de cartas (Embajador).");
                cliente.salida().writeUTF("Has cambiado tus cartas con el mazo.");
                avanzarTurno(partida, sala);
                break;

            default:
                cliente.salida().writeUTF("Acción no válida. Revisa el menú.");
                enviarMenuAcciones(cliente);
        }
    }

    // Metodo para validar comandos completos
    private boolean procesarAtaque(String[] partes, Sala sala, Partida partida, Jugador atacante, String nombreAccion) throws IOException {
        if (partes.length < 2) {
            cliente.salida().writeUTF("Falta el objetivo. Uso correcto: /" + nombreAccion.toLowerCase() + " [jugador]");
            return false;
        }
        Jugador victima = obtenerVictima(partes, sala, partida);
        if (victima == null || !victima.estaVivo()) {
            cliente.salida().writeUTF("Jugador no encontrado o eliminado.");
            return false;
        }
        if (victima.equals(atacante)) {
            cliente.salida().writeUTF("No puedes atacarte a ti mismo.");
            return false;
        }

        // Mensaje global de la acción
        if (nombreAccion.equals("GOLPE")) {
            sala.broadcast(">> " + atacante.getId() + " ejecutó " + nombreAccion + " contra " + victima.getId());
        } else if (nombreAccion.equals("ASESINATO")) {
            sala.broadcast(">> " + atacante.getId() + " quiere ASESINAR a " + victima.getId() + " (-3 monedas)");
        } else if (nombreAccion.equals("EXTORSIÓN")) {
            sala.broadcast(">> " + atacante.getId() + " quiere EXTORSIONAR a " + victima.getId());
        }

        return true;
    }

    private Jugador obtenerVictima(String[] partes, Sala sala, Partida partida) {
        String nombreVictima = partes[1];
        for (UnCliente c : sala.obtenerIntegrantes()) {
            if (c.getId().equals(nombreVictima)) {
                return partida.getJugador(c);
            }
        }
        return null;
    }

    private void avanzarTurno(Partida partida, Sala sala) throws IOException {
        partida.siguienteTurno();
        Jugador siguiente = partida.obtenerJugadorTurno();

        sala.broadcast("------------------------------------------------");
        sala.broadcast("Turno de: " + siguiente.getId() + " | Monedas: " + siguiente.getMonedas());

        // Info privada para el jugador actual
        StringBuilder cartas = new StringBuilder();
        for(Carta c : siguiente.getMano()) {
            if(!c.estaRevelada()) cartas.append("[").append(c.verNombre()).append("] ");
        }
        siguiente.getCliente().salida().writeUTF("Tus cartas: " + cartas.toString());
        enviarMenuAcciones(siguiente.getCliente());
    }

    public void manejarInicioPartida(UnCliente cliente, Sala sala) throws IOException {
        if (sala != null) {
            if (sala.getAdministrador().equals(cliente)) {
                Partida nuevaPartida = new Partida(sala.obtenerIntegrantes());
                sala.setPartida(nuevaPartida);
                sala.broadcast(">>> ¡LA PARTIDA HA COMENZADO! <<< [Coup]");
                avanzarTurno(nuevaPartida, sala);
                return;
            }
            cliente.salida().writeUTF("Error: Solo el administrador puede iniciar.");
            return;
        }
        cliente.salida().writeUTF("Error: No estás en una sala.");
    }

    private void enviarMenuAcciones(UnCliente cliente) throws IOException {
        String menu = "\n" +
                "--- ACCIONES DISPONIBLES (Puedes mentir) ---\n" +
                "1. /ingresos       -> +1 moneda (Seguro)\n" +
                "2. /ayuda          -> +2 monedas (Bloqueable por Duque)\n" +
                "3. /impuestos      -> +3 monedas (Requiere Duque)\n" +
                "4. /asesinar [jug] -> Elimina carta, costo 3 (Requiere Asesino)\n" +
                "5. /extorsionar [jug] -> Roba 2 monedas (Requiere Capitán)\n" +
                "6. /cambio         -> Cambia cartas (Requiere Embajador)\n" +
                "7. /golpe [jug]    -> Elimina carta, costo 7 (Imparable)\n" +
                "--------------------------------------------\n";
        cliente.salida().writeUTF(menu);
    }
}