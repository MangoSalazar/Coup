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

        if (comando.startsWith("/revelar")) {
            manejarRevelacion(comando, sala, partida);
            return;
        }

        if (comando.startsWith("/seleccionar")) {
            manejarSeleccionEmbajador(comando, sala, partida);
            return;
        }


        if (partida.getJugadorVictima() != null) {
            cliente.salida().writeUTF("¡Juego pausado! Esperando a que " + partida.getJugadorVictima().getId() + " elija carta a perder.");
            return;
        }

        if (partida.getJugadorIntercambio() != null) {
            if (partida.getJugadorIntercambio().getCliente().equals(cliente)) {
                cliente.salida().writeUTF("Estás en medio de un cambio. Usa: /seleccionar [Carta1] [Carta2]");
            } else {
                cliente.salida().writeUTF("Esperando a que " + partida.getJugadorIntercambio().getId() + " termine su cambio.");
            }
            return;
        }

        if (!partida.esTurnoDe(cliente)) {
            cliente.salida().writeUTF("¡No es tu turno!");
            return;
        }

        Jugador jugadorActual = partida.getJugador(cliente);
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
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    int res = partida.iniciarGolpe(jugadorActual, victima);
                    if (res == 0) cliente.salida().writeUTF("Faltan monedas (7).");
                    else if (res == 1) {
                        sala.broadcast("!!! " + victima.getId() + " recibió un GOLPE DE ESTADO.");
                        solicitarCartaAPerder(victima);
                    } else {
                        sala.broadcast("!!! " + victima.getId() + " recibió un GOLPE y perdió su última carta.");
                        if(!victima.estaVivo()) sala.broadcast("☠ JUGADOR ELIMINADO: " + victima.getId());
                        avanzarTurno(partida, sala);
                    }
                }
                break;
            case "/impuestos":
                partida.accionImpuestos(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " cobró IMPUESTOS (+3 monedas).");
                avanzarTurno(partida, sala);
                break;
            case "/asesinar":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "ASESINATO")) {
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    int res = partida.iniciarAsesinato(jugadorActual, victima);
                    if (res == 0) cliente.salida().writeUTF("Faltan monedas (3).");
                    else if (res == 1) {
                        sala.broadcast("!!! " + victima.getId() + " ha sido ASESINADO.");
                        solicitarCartaAPerder(victima);
                    } else {
                        sala.broadcast("!!! " + victima.getId() + " fue ASESINADO y perdió su última carta.");
                        if(!victima.estaVivo()) sala.broadcast("☠ JUGADOR ELIMINADO: " + victima.getId());
                        avanzarTurno(partida, sala);
                    }
                }
                break;
            case "/extorsionar":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "EXTORSIÓN")) {
                    partida.accionExtorsion(jugadorActual, obtenerVictima(partes, sala, partida));
                    avanzarTurno(partida, sala);
                }
                break;
            case "/cambio":
                partida.iniciarEmbajador(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " inició un CAMBIO de cartas (Embajador).");
                mostrarOpcionesEmbajador(jugadorActual);
                break;
            default:
                cliente.salida().writeUTF("Acción no válida.");
        }
    }

    private void avanzarTurno(Partida partida, Sala sala) throws IOException {
        Jugador ganador = partida.obtenerGanador();
        if (ganador != null) {
            sala.broadcast("\n*****************************************");
            sala.broadcast("   ¡FELICIDADES " + ganador.getId().toUpperCase() + "!   ");
            sala.broadcast("        HAS GANADO LA PARTIDA            ");
            sala.broadcast("*****************************************\n");

            sala.setPartida(null); // Esto pone enPartida = false y libera la sala
            sala.broadcast("La partida ha finalizado. La sala está abierta de nuevo.");
            return;
        }

        // Si no hay ganador, seguimos
        partida.siguienteTurno();
        Jugador siguiente = partida.obtenerJugadorTurno();
        sala.broadcast("------------------------------------------------");
        sala.broadcast("Turno de: " + siguiente.getId() + " | Monedas: " + siguiente.getMonedas());

        StringBuilder cartas = new StringBuilder();
        for(Carta c : siguiente.getMano()) if(!c.estaRevelada()) cartas.append("[").append(c.verNombre()).append("] ");
        siguiente.getCliente().salida().writeUTF("Tus cartas: " + cartas.toString());
        enviarMenuAcciones(siguiente.getCliente());
    }


    private void mostrarOpcionesEmbajador(Jugador j) throws IOException {
        String opciones = "";
        for(Carta c : j.getMano()) {
            if(!c.estaRevelada()) opciones += "[" + c.verNombre() + "] ";
        }
        j.getCliente().salida().writeUTF("\n--- CARTAS DISPONIBLES (Incluyendo las del mazo) ---");
        j.getCliente().salida().writeUTF(opciones);
        int vidasReales = j.getInfluenciaActiva() - 2;
        j.getCliente().salida().writeUTF("Selecciona las cartas que te quieres QUEDAR.");
        j.getCliente().salida().writeUTF("Usa: /seleccionar [Carta1] " + (vidasReales > 1 ? "[Carta2]" : ""));
    }

    private void manejarSeleccionEmbajador(String comando, Sala sala, Partida partida) throws IOException {
        Jugador actor = partida.getJugadorIntercambio();
        if (actor == null || !actor.getCliente().equals(cliente)) {
            cliente.salida().writeUTF("No estás realizando un intercambio.");
            return;
        }
        String[] partes = comando.split(" ");
        String c1 = (partes.length > 1) ? partes[1] : null;
        String c2 = (partes.length > 2) ? partes[2] : null;
        if (c1 == null) {
            cliente.salida().writeUTF("Debes elegir al menos una carta.");
            return;
        }
        if (partida.concretarIntercambio(actor, c1, c2)) {
            sala.broadcast(">> " + actor.getId() + " finalizó el cambio de cartas.");
            avanzarTurno(partida, sala);
        } else {
            cliente.salida().writeUTF("Error: Selección inválida.");
        }
    }

    private void manejarRevelacion(String comando, Sala sala, Partida partida) throws IOException {
        Jugador victima = partida.getJugadorVictima();
        if (victima == null || !victima.getCliente().equals(cliente)) {
            cliente.salida().writeUTF("No tienes cartas pendientes por revelar.");
            return;
        }
        String[] partes = comando.split(" ");
        if (partes.length < 2) {
            cliente.salida().writeUTF("Uso correcto: /revelar [NombreCarta]");
            return;
        }
        String cartaElegida = partes[1];
        if (partida.concretarPerdida(victima, cartaElegida)) {
            sala.broadcast("☠ " + victima.getId() + " ha muerto una influencia: " + cartaElegida.toUpperCase());
            if (!victima.estaVivo()) sala.broadcast("☠ JUGADOR ELIMINADO: " + victima.getId());
            avanzarTurno(partida, sala);
        } else {
            cliente.salida().writeUTF("Error: No tienes esa carta o ya está revelada.");
        }
    }

    private void solicitarCartaAPerder(Jugador victima) throws IOException {
        String cartas = "";
        for (Carta c : victima.getMano()) {
            if (!c.estaRevelada()) cartas += "[" + c.verNombre() + "] ";
        }
        victima.getCliente().salida().writeUTF("\n!!! HAS PERDIDO UNA INFLUENCIA !!!");
        victima.getCliente().salida().writeUTF("Tus cartas vivas: " + cartas);
        victima.getCliente().salida().writeUTF("Escribe: /revelar [NombreCarta]");
    }

    private boolean procesarAtaque(String[] partes, Sala sala, Partida partida, Jugador atacante, String nombreAccion) throws IOException {
        if (partes.length < 2) {
            cliente.salida().writeUTF("Falta el objetivo. Uso: /" + nombreAccion.toLowerCase() + " [jugador]");
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
        if (nombreAccion.equals("EXTORSIÓN")) sala.broadcast(">> " + atacante.getId() + " quiere EXTORSIONAR a " + victima.getId());
        return true;
    }

    private Jugador obtenerVictima(String[] partes, Sala sala, Partida partida) {
        String nombreVictima = partes[1];
        for (UnCliente c : sala.obtenerIntegrantes()) {
            if (c.getId().equals(nombreVictima)) return partida.getJugador(c);
        }
        return null;
    }

    public void manejarInicioPartida(UnCliente cliente, Sala sala) throws IOException {
        if (sala != null) {
            if (sala.getAdministrador().equals(cliente)) {
                Partida nuevaPartida = new Partida(sala.obtenerIntegrantes());
                sala.setPartida(nuevaPartida);
                sala.broadcast(">>> ¡LA PARTIDA HA COMENZADO! <<< [Coup]");
                Jugador primerJugador = nuevaPartida.obtenerJugadorTurno();
                sala.broadcast("Turno de: " + primerJugador.getId() + " | Monedas: " + primerJugador.getMonedas());
                enviarMenuAcciones(primerJugador.getCliente());
                return;
            }
            cliente.salida().writeUTF("Error: Solo el administrador puede iniciar.");
            return;
        }
        cliente.salida().writeUTF("Error: No estás en una sala.");
    }

    private void enviarMenuAcciones(UnCliente cliente) throws IOException {
        String menu = "\n" +
                "--- ACCIONES ---\n" +
                "1. /ingresos       -> +1 moneda\n" +
                "2. /ayuda          -> +2 monedas\n" +
                "3. /impuestos      -> +3 monedas (Duque)\n" +
                "4. /asesinar [jug] -> Costo 3 (Asesino)\n" +
                "5. /extorsionar [jug] -> Roba 2 (Capitán)\n" +
                "6. /cambio         -> Cartas (Embajador)\n" +
                "7. /golpe [jug]    -> Costo 7\n" +
                "----------------\n";
        cliente.salida().writeUTF(menu);
    }
}