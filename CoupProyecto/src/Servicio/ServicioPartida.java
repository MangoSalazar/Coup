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
        Jugador jugadorActual = partida.getJugador(cliente);

        // --- MANEJO ESPECIAL: RESPUESTA DE VÍCTIMA (/revelar) ---
        if (comando.startsWith("/revelar")) {
            manejarRevelacion(comando, sala, partida);
            return;
        }

        // --- VALIDACIONES DE TURNO ---
        // Si hay una víctima pendiente, nadie puede jugar hasta que esa persona pierda su carta
        if (partida.getJugadorVictima() != null) {
            cliente.salida().writeUTF("¡Esperando a que " + partida.getJugadorVictima().getId() + " elija carta a perder!");
            return;
        }

        if (!partida.esTurnoDe(cliente)) {
            cliente.salida().writeUTF("¡No es tu turno! Espera a " + partida.obtenerJugadorTurno().getId());
            return;
        }

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
                    if (partida.iniciarGolpe(jugadorActual, victima)) {
                        sala.broadcast("!!! " + victima.getId() + " ha recibido un GOLPE DE ESTADO.");
                        solicitarCartaAPerder(victima); // Pedimos a la víctima que elija
                    } else {
                        cliente.salida().writeUTF("No tienes suficientes monedas (7).");
                    }
                }
                break;

            case "/impuestos":
                partida.accionImpuestos(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " cobró IMPUESTOS (Duque) (+3 monedas).");
                avanzarTurno(partida, sala);
                break;

            case "/asesinar":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "ASESINATO")) {
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    if (partida.iniciarAsesinato(jugadorActual, victima)) {
                        sala.broadcast("!!! " + victima.getId() + " ha sido ASESINADO.");
                        solicitarCartaAPerder(victima); // Pedimos a la víctima que elija
                    } else {
                        cliente.salida().writeUTF("No tienes suficientes monedas (3).");
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
                partida.accionCambio(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " realizó un CAMBIO de cartas (Embajador).");
                avanzarTurno(partida, sala);
                break;

            default:
                cliente.salida().writeUTF("Acción no válida.");
        }
    }

    // Método auxiliar para avisar a la víctima que debe elegir
    private void solicitarCartaAPerder(Jugador victima) throws IOException {
        String cartas = "";
        for (Carta c : victima.getMano()) {
            if (!c.estaRevelada()) cartas += "[" + c.verNombre() + "] ";
        }
        victima.getCliente().salida().writeUTF("\n!!! HAS PERDIDO UNA INFLUENCIA !!!");
        victima.getCliente().salida().writeUTF("Tus cartas vivas: " + cartas);
        victima.getCliente().salida().writeUTF("Escribe: /revelar [NombreCarta] para continuar.");
        victima.getCliente().salida().writeUTF("Ejemplo: /revelar DUQUE\n");
    }

    // Nuevo método para procesar la elección de la víctima
    private void manejarRevelacion(String comando, Sala sala, Partida partida) throws IOException {
        Jugador victima = partida.getJugadorVictima();

        // Validar si hay alguien que deba revelar y si es quien envió el comando
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

            if (!victima.estaVivo()) {
                sala.broadcast("☠ JUGADOR ELIMINADO: " + victima.getId());
            }

            // Solo después de revelar, avanzamos el turno
            avanzarTurno(partida, sala);
        } else {
            cliente.salida().writeUTF("Error: No tienes esa carta o ya está revelada. Intenta de nuevo.");
        }
    }

    // ... (El resto de métodos procesarAtaque, obtenerVictima, avanzarTurno, etc. se mantienen igual)
    // Asegúrate de incluir los métodos auxiliares que ya tenías en el código anterior.

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
        if (nombreAccion.equals("EXTORSIÓN")) {
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
                "--- ACCIONES DISPONIBLES ---\n" +
                "1. /ingresos       -> +1 moneda\n" +
                "2. /ayuda          -> +2 monedas\n" +
                "3. /impuestos      -> +3 monedas (Duque)\n" +
                "4. /asesinar [jug] -> Costo 3 (Asesino)\n" +
                "5. /extorsionar [jug] -> Roba 2 (Capitán)\n" +
                "6. /cambio         -> Cartas (Embajador)\n" +
                "7. /golpe [jug]    -> Costo 7\n" +
                "----------------------------\n";
        cliente.salida().writeUTF(menu);
    }
}